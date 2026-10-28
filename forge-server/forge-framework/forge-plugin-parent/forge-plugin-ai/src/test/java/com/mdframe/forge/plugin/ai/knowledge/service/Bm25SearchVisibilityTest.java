package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledge;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiStoreInstance;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchRequest;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreFactory;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreService;
import com.mdframe.forge.plugin.ai.rag.search.RagSearchContext;
import com.mdframe.forge.plugin.ai.rag.search.handler.Bm25SearchHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Bm25SearchVisibilityTest {
    private final AiKnowledgeMapper knowledge = mock(AiKnowledgeMapper.class);
    private final AiKnowledgeChunkMapper chunks = mock(AiKnowledgeChunkMapper.class);
    private final AiStoreInstanceService instances = mock(AiStoreInstanceService.class);
    private final VectorStoreFactory factory = mock(VectorStoreFactory.class);
    private final VectorStoreService store = mock(VectorStoreService.class);
    private final KnowledgeSearchService search = new KnowledgeSearchService(knowledge, chunks,
            null, null, null, null, factory, instances);
    private final Bm25SearchHandler handler = new Bm25SearchHandler(knowledge, instances, factory, search);

    @Test
    void bm25OnlyReturnsCurrentDatabaseChunksAndStableChunkIds() {
        RagSearchContext context = context();
        AiKnowledgeChunk live = new AiKnowledgeChunk();
        live.setId(21L);
        live.setVectorId("current-attempt");
        live.setDocumentId(2L);
        live.setContent("current database content");
        when(chunks.selectSearchableByVectorIds(eq(1L), anyList())).thenReturn(List.of(live));
        when(store.bm25Search(any())).thenReturn(List.of(hit("orphan-attempt", 2L),
                hit("deleted-document", 3L), hit("current-attempt", 99L), hit("current-attempt", 2L)));
        handler.handle(context);
        assertEquals(1, context.getBm25Results().size());
        assertEquals("21", context.getBm25Results().get(0).getChunkId());
        assertEquals("current database content", context.getBm25Results().get(0).getContent());
        verify(chunks).selectSearchableByVectorIds(eq(1L), anyList());
    }

    @Test
    void unavailableDatabaseNeverFallsBackToUnverifiedIndexPayload() {
        RagSearchContext context = context();
        when(store.bm25Search(any())).thenReturn(List.of(hit("unverified", 2L)));
        when(chunks.selectSearchableByVectorIds(eq(1L), anyList())).thenThrow(new IllegalStateException("database unavailable"));
        handler.handle(context);
        assertTrue(context.getBm25Results().isEmpty());
    }

    private RagSearchContext context() {
        AiKnowledge library = new AiKnowledge();
        library.setId(1L);
        library.setVectorStoreInstanceId(4L);
        when(knowledge.selectById(1L)).thenReturn(library);
        AiStoreInstance instance = new AiStoreInstance();
        when(instances.getById(4L)).thenReturn(instance);
        when(factory.getService(instance)).thenReturn(store);
        KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        request.setKnowledgeId(1L);
        request.setQuery("test");
        return new RagSearchContext(request);
    }

    private VectorStoreService.SearchResult hit(String id, Long documentId) {
        VectorStoreService.SearchResult hit = new VectorStoreService.SearchResult();
        hit.setId(id);
        hit.setDocumentId(documentId);
        hit.setContent("stale index content");
        return hit;
    }
}
