package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreFactory;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Owns the consistency boundary between persisted chunks and the document vector index. */
@Component
@RequiredArgsConstructor
public class DocumentVectorIndexer {
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiModelMapper modelMapper;
    private final AiProviderMapper providerMapper;
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;
    private final VectorStoreFactory vectorStoreFactory;
    private final AiStoreInstanceService storeInstanceService;

    /**
     * 向量化并存储
     */
    public void replaceChunks(Long documentId, AiKnowledge knowledge, List<ChunkResult> chunks) {
        if (chunks.isEmpty()) {
            deleteVectors(knowledge, chunkMapper.selectByDocumentId(documentId));
            chunkMapper.deleteByDocumentId(documentId);
            return;
        }

        // 来源标识：URL来源存 source_url，否则回退文档名（库表导入将来存表/行标识）
        AiKnowledgeDocument document = documentMapper.selectById(documentId);
        String sourceId = document != null && document.getSourceUrl() != null && !document.getSourceUrl().isBlank()
                ? document.getSourceUrl() : (document != null ? document.getDocName() : null);

        // 获取 Embedding 模型信息
        AiModel embeddingModel = modelMapper.selectEnabledById(knowledge.getEmbeddingModelId());
        if (embeddingModel == null) {
            throw new BusinessException("Embedding模型不存在或未启用");
        }
        AiProvider provider = providerMapper.selectById(embeddingModel.getProviderId());
        if (provider == null) {
            throw new BusinessException("Embedding模型供应商不存在");
        }

        // 解密 API Key
        String apiKey = aiSecretCrypto.isEncrypted(provider.getApiKey())
                ? aiSecretCrypto.decrypt(provider.getApiKey()) : provider.getApiKey();

        // 获取适配器
        var adapter = modelAdapterRegistry.getEmbedding(embeddingModel.getModelId());

        // 获取向量存储
        VectorStoreService vectorStore = vectorStoreFactory.getService(
                knowledge.getVectorStoreInstanceId() != null
                        ? getStoreInstance(knowledge.getVectorStoreInstanceId()) : null
        );

        // 集合名称
        String collectionName = "knowledge_" + knowledge.getId();

        // 先批量嵌入，再按模型实际输出维度建集合：
        // 知识库未配置向量维度时以模型真实输出为准（避免硬编码 1536 与 text-embedding-v3 实际输出 1024 不符）
        List<String> texts = chunks.stream().map(ChunkResult::getContent).toList();
        List<List<Float>> vectors = adapter.embed(
                provider.getBaseUrl(), apiKey, embeddingModel.getModelId(), texts
        );
        int actualDim = vectors.isEmpty() ? 0 : vectors.get(0).size();
        Integer configuredDim = knowledge.getDimensionOfVectorModel();
        int dimension;
        if (configuredDim != null && configuredDim > 0) {
            if (actualDim > 0 && actualDim != configuredDim) {
                throw new BusinessException(UiMessages.translate("Embedding模型输出维度(") + actualDim + UiMessages.translate(")与知识库配置维度(") + configuredDim + UiMessages.translate(")不一致"));
            }
            dimension = configuredDim;
        } else {
            if (actualDim <= 0) {
                throw new BusinessException("Embedding模型未返回有效向量");
            }
            dimension = actualDim;
        }

        // 单文档导入不得重建共享集合。维度/schema 不匹配必须保留原集合并报告失败。
        String configJson = knowledge.getVectorStoreInstanceId() != null
                ? getStoreInstance(knowledge.getVectorStoreInstanceId()).getConfigJson() : "{}";
        VectorStoreService.CreateCollectionRequest createReq = new VectorStoreService.CreateCollectionRequest();
        createReq.setCollectionName(collectionName);
        createReq.setDimension(dimension);
        createReq.setConfigJson(configJson);
        createReq.setForceRecreate(false);
        vectorStore.createCollectionIfAbsent(createReq);

        // 清理失败处理留下的同文档输出，重试不能积累重复分块或残留向量。
        deleteVectors(knowledge, chunkMapper.selectByDocumentId(documentId));
        chunkMapper.deleteByDocumentId(documentId);

        // 保存分块到数据库
        List<String> vectorIds = new ArrayList<>();
        List<List<Float>> vectorList = new ArrayList<>();
        List<Long> docIds = new ArrayList<>();
        List<Integer> chunkIndices = new ArrayList<>();
        List<String> sourceIds = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<String> hideContents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunk = chunks.get(i);
            AiKnowledgeChunk entity = new AiKnowledgeChunk();
            entity.setKnowledgeId(knowledge.getId());
            entity.setDocumentId(documentId);
            entity.setChunkIndex(chunk.getIndex());
            entity.setContent(chunk.getContent());
            entity.setTitle(chunk.getTitle());
            entity.setTokenCount(chunk.getTokenCount());
            entity.setRefCount(1);
            entity.setContentHash(hashContent(chunk.getContent()));
            entity.setRetrievalCount(0);

            String vectorId = "doc_" + documentId + "_chunk_" + chunk.getIndex();
            entity.setVectorId(vectorId);
            chunkMapper.insert(entity);

            vectorIds.add(vectorId);
            vectorList.add(vectors.get(i));
            docIds.add(documentId);
            chunkIndices.add(chunk.getIndex());
            sourceIds.add(sourceId);
            titles.add(chunk.getTitle());
            // 分块暂无"隐藏内容"概念，预留空串（参考项目存医生介绍等不参与检索的补充详情）
            hideContents.add(null);
        }

        // 插入向量存储
        VectorStoreService.InsertRequest insertReq = new VectorStoreService.InsertRequest();
        insertReq.setCollectionName(collectionName);
        insertReq.setIds(vectorIds);
        insertReq.setVectors(vectorList);
        insertReq.setContents(texts);
        insertReq.setDocumentIds(docIds);
        insertReq.setChunkIndices(chunkIndices);
        insertReq.setSourceIds(sourceIds);
        insertReq.setTitles(titles);
        insertReq.setHideContents(hideContents);
        insertReq.setConfigJson(configJson);
        vectorStore.insert(insertReq);
    }

    public void deleteVectors(AiKnowledge knowledge, List<AiKnowledgeChunk> chunks) {
        if (chunks.isEmpty() || knowledge == null || knowledge.getVectorStoreInstanceId() == null) {
            return;
        }
        AiStoreInstance store = getStoreInstance(knowledge.getVectorStoreInstanceId());
        VectorStoreService.DeleteRequest request = new VectorStoreService.DeleteRequest();
        request.setCollectionName("knowledge_" + knowledge.getId());
        request.setIds(chunks.stream().map(AiKnowledgeChunk::getVectorId).toList());
        request.setConfigJson(store.getConfigJson());
        vectorStoreFactory.getService(store).delete(request);
    }

    private AiStoreInstance getStoreInstance(Long storeInstanceId) {
        return storeInstanceService.getById(storeInstanceId);
    }

    private String hashContent(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }
}
