package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.plugin.ai.knowledge.chunker.ChunkerRegistry;
import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.parser.DocumentParserRegistry;
import com.mdframe.forge.plugin.ai.knowledge.parser.dto.ParsedDocument;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentProcessEvent;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.DocumentUploadRequest;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

/**
 * 文档处理服务。
 * 负责文档上传、解析、分块、向量化、入库的完整流水线。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final DocumentParserRegistry parserRegistry;
    private final ChunkerRegistry chunkerRegistry;
    private final FileManager fileManager;
    private final DocumentVectorIndexer vectorIndexer;
    private final DocumentProcessDispatcher processDispatcher;
    private final DocumentProcessState processState;

    @Value("${forge.ai.knowledge.processing.timeout-seconds:1800}")
    private int processingTimeoutSeconds = 1800;

    @PostConstruct
    void validateProcessingTimeout() {
        if (processingTimeoutSeconds < 1 || processingTimeoutSeconds > 86400) {
            throw new IllegalArgumentException("Document processing timeout must be between 1 and 86400 seconds");
        }
    }

    private final DocumentProgressPublisher progressPublisher = new DocumentProgressPublisher();

    /**
     * 订阅文档处理进度（SSE）
     */
    public Flux<ServerSentEvent<DocumentProcessEvent>> subscribeProgress(Long documentId) {
        return progressPublisher.subscribe(documentId);
    }

    /**
     * 上传文档（第一步：创建文档记录，可选立即处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocument uploadDocument(DocumentUploadRequest request) {
        AiKnowledge knowledge = knowledgeMapper.selectByIdForUpdate(request.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }

        // 推断文档类型
        String docType = request.getDocType();
        if (docType == null || docType.isBlank()) {
            docType = parserRegistry.inferDocType(request.getDocName());
        }

        // 计算内容哈希（去重）
        String contentHash = null;
        if (request.getFileId() != null) {
            contentHash = computeFileHash(request.getFileId());
        }

        // 去重检查
        if (contentHash != null && !"none".equals(knowledge.getDedupStrategy())) {
            long dupCount = documentMapper.countByContentHash(knowledge.getId(), contentHash, null);
            if (dupCount > 0) {
                handleDedup(knowledge.getDedupStrategy(), knowledge.getDedupAction(), request.getDocName());
            }
        }

        // 创建文档记录
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setKnowledgeId(knowledge.getId());
        document.setFileId(request.getFileId());
        document.setDocName(request.getDocName());
        document.setDocType(docType);
        document.setSourceType(request.getSourceType() != null ? request.getSourceType() : "upload");
        document.setSourceUrl(request.getSourceUrl());
        document.setContentHash(contentHash);
        document.setChunkCount(0);
        document.setProcessStatus("pending");
        documentMapper.insert(document);

        // 两步上传模式：不自动处理，等待确认
        if ("1".equals(knowledge.getUploadConfirm()) && !Boolean.TRUE.equals(request.getConfirm())) {
            return document;
        }

        // 立即异步处理
        requestProcessing(document.getId(), knowledge, "pending");
        return document;
    }

    /**
     * 确认并处理文档（两步上传第二步）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmAndProcess(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!"pending".equals(document.getProcessStatus())) {
            throw new BusinessException("文档不在待处理状态");
        }
        AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        requestProcessing(documentId, knowledge, "pending");
    }

    /**
     * 重新处理失败文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void reprocessDocument(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!"failed".equals(document.getProcessStatus())) {
            throw new BusinessException("仅失败状态的文档可重新处理");
        }
        AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
        if (knowledge == null) {
            throw new BusinessException("知识库不存在");
        }
        requestProcessing(documentId, knowledge, "failed");
    }

    /**
     * 查看文档分块列表（按分块序号升序）
     */
    public List<AiKnowledgeChunk> listChunks(Long documentId) {
        return chunkMapper.selectByDocumentId(documentId);
    }

    /**
     * 查看文档原始内容。
     * upload 来源重新解析文件获取全文；manual/url 来源直接返回录入文本。
     */
    public String getDocumentRawContent(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if ("manual".equals(document.getSourceType()) || "url".equals(document.getSourceType())) {
            return document.getSourceUrl() != null ? document.getSourceUrl() : "";
        }
        ParsedDocument parsed = parseDocument(document);
        return parsed.getContent() != null ? parsed.getContent() : "";
    }

    /**
     * 异步处理文档
     */
    private void requestProcessing(Long documentId, AiKnowledge knowledge, String expectedStatus) {
        String attempt = UUID.randomUUID().toString();
        if (documentMapper.requestProcessing(documentId, expectedStatus, attempt, processingTimeoutSeconds) != 1) {
            throw new BusinessException("文档不在待处理状态");
        }
        processDispatcher.dispatch(() -> processDocument(documentId, knowledge, attempt), () -> {
            String message = UiMessages.label("文档处理队列已满或正在停止，请稍后重试",
                    "Document processing is full or stopping. Please retry later.");
            if (processState.reject(documentId, attempt, message)) {
                emitProgress(documentId, "", "failed", 0, message, "failed");
            }
        });
    }

    /**
     * 同步处理文档（核心流水线）
     */
    private void processDocument(Long documentId, AiKnowledge knowledge, String attempt) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) return;

        try {
            // 1. 更新状态为处理中
            if (documentMapper.claimPendingDocument(documentId, attempt, processingTimeoutSeconds) != 1) {
                return;
            }
            emitProgress(documentId, document.getDocName(), "parsing", 10, "开始解析文档", "processing");

            // 2. 解析文档
            ParsedDocument parsed;
            if ("manual".equals(document.getSourceType()) || "url".equals(document.getSourceType())) {
                // 手动输入或 URL，直接使用内容
                parsed = ParsedDocument.of(document.getDocName(), document.getSourceUrl() != null ? document.getSourceUrl() : "");
            } else {
                parsed = parseDocument(document);
            }
            emitProgress(documentId, document.getDocName(), "parsing", 30, "文档解析完成", "processing");

            // 3. 分块
            List<ChunkResult> chunks = chunkerRegistry.chunk(
                    parsed.getContent(),
                    knowledge.getChunkStrategy(),
                    knowledge.getChunkConfigJson()
            );
            emitProgress(documentId, document.getDocName(), "chunking", 50,
                    "分块完成，共" + chunks.size() + "个分块", "processing");

            // 4. 向量化 + 入库
            if (vectorIndexer.replaceChunks(documentId, knowledge, chunks, attempt)) {
                emitProgress(documentId, document.getDocName(), "complete", 100, "处理完成", "success");
            }

        } catch (Exception e) {
            log.error("[文档处理] 处理失败: documentId={}", documentId, e);
            if (documentMapper.finishAttempt(documentId, attempt, "processing", "failed", e.getMessage(), null) == 1) {
                emitProgress(documentId, document.getDocName(), "failed", 0, "处理失败: " + e.getMessage(), "failed");
            }
        }
    }

    /**
     * 解析文档
     */
    private ParsedDocument parseDocument(AiKnowledgeDocument document) {
        if (document.getFileId() == null) {
            return ParsedDocument.of(document.getDocName(), "");
        }
        try (InputStream is = getFileInputStream(document.getFileId())) {
            return parserRegistry.parse(is, document.getDocName(), document.getDocType());
        } catch (Exception e) {
            throw new BusinessException(UiMessages.translate("文档解析失败: ") + e.getMessage());
        }
    }

    /**
     * 删除文档及其分块和向量
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) return;

        // 获取分块的向量ID
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        if (!chunks.isEmpty()) {
            AiKnowledge knowledge = knowledgeMapper.selectById(document.getKnowledgeId());
            // 必要的向量删除失败必须上抛，保留数据库记录和向量ID以便重试。
            vectorIndexer.deleteVectors(knowledge, chunks);

            // 逻辑删除分块
            chunkMapper.deleteByDocumentId(documentId);
        }

        // 逻辑删除文档
        documentMapper.deleteById(documentId);
    }

    // ===== 辅助方法 =====

    private String computeFileHash(String fileId) {
        try (InputStream is = getFileInputStream(fileId)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            log.warn("[文件哈希] 计算失败: fileId={}", fileId, e);
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void handleDedup(String strategy, String action, String docName) {
        if ("reject".equals(action)) {
            throw new BusinessException(UiMessages.translate("文档内容重复，已拒绝上传: ") + docName);
        }
        // skip / overwrite 由后续处理逻辑处理
        log.info("[去重] 文档重复，策略={}: {}", action, docName);
    }

    private void emitProgress(Long documentId, String docName, String stage, int progress, String message, String status) {
        progressPublisher.emit(DocumentProcessEvent.of(documentId, docName, stage, progress, message, status));
    }

    /**
     * 通过 FileManager 获取文件输入流。
     * fileId 为 FileManager 上传返回的 UUID 字符串。
     */
    private InputStream getFileInputStream(String fileId) {
        if (fileId == null) {
            throw new BusinessException("文件ID不能为空");
        }
        var metadata = fileManager.getFileMetadata(fileId);
        if (metadata == null) {
            throw new BusinessException(UiMessages.translate("文件不存在: ") + fileId);
        }
        FileStorage storage = fileManager.getStorage(metadata.getStorageType());
        if (storage == null) {
            throw new BusinessException(UiMessages.translate("文件存储策略不存在: ") + metadata.getStorageType());
        }
        return storage.download(fileId);
    }
}
