package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.chunker.ChunkerRegistry;
import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.*;
import com.mdframe.forge.plugin.ai.knowledge.mapper.*;
import com.mdframe.forge.plugin.ai.knowledge.parser.DocumentParserRegistry;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.*;
import com.mdframe.forge.plugin.ai.model.adapter.*;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentProcessServiceIntegrityTest {
    private final AiKnowledgeMapper knowledgeMapper = mock(AiKnowledgeMapper.class);
    private final AiKnowledgeDocumentMapper documentMapper = mock(AiKnowledgeDocumentMapper.class);
    private final AiKnowledgeChunkMapper chunkMapper = mock(AiKnowledgeChunkMapper.class);
    private final AiModelMapper modelMapper = mock(AiModelMapper.class);
    private final AiProviderMapper providerMapper = mock(AiProviderMapper.class);
    private final AiModelAdapterRegistry adapters = mock(AiModelAdapterRegistry.class);
    private final VectorStoreFactory stores = mock(VectorStoreFactory.class);
    private final AiStoreInstanceService instances = mock(AiStoreInstanceService.class);
    private final VectorStoreService store = mock(VectorStoreService.class);
    private final ChunkerRegistry chunker = mock(ChunkerRegistry.class);
    private final DocumentVectorIndexer indexer = new DocumentVectorIndexer(documentMapper, chunkMapper,
            modelMapper, providerMapper, adapters, mock(AiSecretCrypto.class), stores, instances);
    private final DocumentProcessService service = new DocumentProcessService(knowledgeMapper, documentMapper,
            chunkMapper, mock(DocumentParserRegistry.class), chunker, mock(FileManager.class),
            indexer, mock(DocumentProcessDispatcher.class));

    @Test
    void duplicatePendingWorkerDoesNotParseOrIndexTwice() {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setSourceType("manual");
        when(documentMapper.selectById(2L)).thenReturn(document);
        when(documentMapper.claimPendingDocument(2L)).thenReturn(1, 0);
        when(chunker.chunk(anyString(), any(), any())).thenReturn(List.of());
        ReflectionTestUtils.invokeMethod(service, "processDocument", 2L, knowledge());
        ReflectionTestUtils.invokeMethod(service, "processDocument", 2L, knowledge());
        verify(chunker).chunk(anyString(), any(), any());
        verify(documentMapper).updateProcessStatus(2L, "success", null, 0);
    }

    @Test
    void emptyReprocessingClearsOldIndexInsteadOfPublishingStaleChunks() {
        configureStore();
        AiKnowledgeChunk old = new AiKnowledgeChunk();
        old.setVectorId("doc_2_chunk_0");
        when(chunkMapper.selectByDocumentId(2L)).thenReturn(List.of(old));
        indexer.replaceChunks(2L, knowledge(), List.of());
        verify(store).delete(argThat(request -> request.getIds().equals(List.of("doc_2_chunk_0"))));
        verify(chunkMapper).deleteByDocumentId(2L);
        verify(store, never()).insert(any());
    }

    @Test
    void vectorDeletionFailurePreservesDocumentAndRetryInformation() {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setKnowledgeId(1L);
        when(documentMapper.selectById(2L)).thenReturn(document);
        AiKnowledge knowledge = knowledge();
        when(knowledgeMapper.selectById(1L)).thenReturn(knowledge);
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setVectorId("doc_2_chunk_0");
        when(chunkMapper.selectByDocumentId(2L)).thenReturn(List.of(chunk));
        configureStore();
        doThrow(new BusinessException("unavailable")).when(store).delete(any());
        assertThrows(BusinessException.class, () -> service.deleteDocument(2L));
        verify(chunkMapper, never()).deleteByDocumentId(any());
        verify(documentMapper, never()).deleteById(anyLong());
    }

    @Test
    void incompatibleSharedCollectionIsNeverRecreatedBySingleDocumentImport() {
        AiModel model = new AiModel();
        model.setProviderId(4L);
        model.setModelId("embedding");
        when(modelMapper.selectEnabledById(3L)).thenReturn(model);
        AiProvider provider = new AiProvider();
        provider.setBaseUrl("http://model.invalid");
        provider.setApiKey("test-only");
        when(providerMapper.selectById(4L)).thenReturn(provider);
        AiEmbeddingModelAdapter adapter = mock(AiEmbeddingModelAdapter.class);
        when(adapters.getEmbedding("embedding")).thenReturn(adapter);
        when(adapter.embed(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(List.of(List.of(1.0F, 2.0F)));
        configureStore();
        doAnswer(call -> {
            VectorStoreService.CreateCollectionRequest request = call.getArgument(0);
            assertFalse(request.isForceRecreate());
            assertEquals("knowledge_1", request.getCollectionName());
            throw new BusinessException("existing collection has incompatible dimension");
        }).when(store).createCollectionIfAbsent(any());
        assertThrows(BusinessException.class, () -> indexer.replaceChunks(2L, knowledge(), List.of(ChunkResult.of(0, "content", 1))));
        verify(chunkMapper, never()).insert(any(AiKnowledgeChunk.class));
        verify(store, never()).insert(any());
    }

    private AiKnowledge knowledge() {
        AiKnowledge knowledge = new AiKnowledge();
        knowledge.setId(1L);
        knowledge.setEmbeddingModelId(3L);
        knowledge.setVectorStoreInstanceId(5L);
        return knowledge;
    }

    private void configureStore() {
        AiStoreInstance instance = new AiStoreInstance();
        instance.setConfigJson("{}");
        when(instances.getById(5L)).thenReturn(instance);
        when(stores.getService(instance)).thenReturn(store);
    }
}
