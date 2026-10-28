package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Owns the consistency boundary between persisted chunks and the document vector index. */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentVectorIndexer {
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiModelMapper modelMapper;
    private final AiProviderMapper providerMapper;
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;
    private final VectorStoreFactory vectorStoreFactory;
    private final AiStoreInstanceService storeInstanceService;
    private final DocumentIndexPublisher indexPublisher;

    /**
     * 向量化并存储
     */
    public boolean replaceChunks(Long documentId, AiKnowledge knowledge, List<ChunkResult> chunks, String attempt) {
        if (attempt == null || attempt.isBlank()) {
            throw new IllegalArgumentException("Processing attempt is required");
        }
        List<String> vectorIds = chunks.stream()
                .map(chunk -> "doc_" + documentId + "_" + attempt + "_chunk_" + chunk.getIndex()).toList();
        if (!chunks.isEmpty()) {
            writeAttemptVectors(documentId, knowledge, chunks, vectorIds);
        }
        // A failed/unknown database commit must never trigger deletion of potentially published vectors.
        DocumentIndexPublisher.Publication publication = indexPublisher.publish(documentId, attempt, chunks, vectorIds);
        List<String> obsolete = publication.published()
                ? publication.supersededChunks().stream().map(AiKnowledgeChunk::getVectorId).toList() : vectorIds;
        try {
            deleteVectorIds(knowledge, obsolete);
        } catch (RuntimeException failure) {
            // Superseded chunks remain as tombstones; retrieval only trusts the newly committed live chunk IDs.
            log.warn("Document index cleanup incomplete: documentId={}, attempt={}", documentId, attempt, failure);
        }
        return publication.published();
    }

    private void writeAttemptVectors(Long documentId, AiKnowledge knowledge,
                                     List<ChunkResult> chunks, List<String> vectorIds) {
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

        // Remote output has attempt-specific IDs. It is not searchable until local publication commits.
        List<Long> docIds = new ArrayList<>();
        List<Integer> chunkIndices = new ArrayList<>();
        List<String> sourceIds = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        List<String> hideContents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunk = chunks.get(i);
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
        insertReq.setVectors(vectors);
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
        deleteVectorIds(knowledge, chunks.stream().map(AiKnowledgeChunk::getVectorId).toList());
    }

    private void deleteVectorIds(AiKnowledge knowledge, List<String> vectorIds) {
        if (vectorIds.isEmpty() || knowledge == null || knowledge.getVectorStoreInstanceId() == null) {
            return;
        }
        AiStoreInstance store = getStoreInstance(knowledge.getVectorStoreInstanceId());
        VectorStoreService.DeleteRequest request = new VectorStoreService.DeleteRequest();
        request.setCollectionName("knowledge_" + knowledge.getId());
        request.setIds(vectorIds);
        request.setConfigJson(store.getConfigJson());
        vectorStoreFactory.getService(store).delete(request);
    }

    private AiStoreInstance getStoreInstance(Long storeInstanceId) {
        return storeInstanceService.getById(storeInstanceId);
    }

}
