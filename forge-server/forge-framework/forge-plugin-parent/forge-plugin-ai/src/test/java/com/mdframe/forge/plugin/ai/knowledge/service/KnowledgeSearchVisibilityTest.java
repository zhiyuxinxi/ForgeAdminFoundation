package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.mapper.*;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.*;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeSearchVisibilityTest {
    @Test
    void deletedForeignAndMismatchedHitsDoNotReturnVectorPayloads() {
        AiKnowledgeChunkMapper chunks = mock(AiKnowledgeChunkMapper.class);
        KnowledgeSearchService service = new KnowledgeSearchService(mock(AiKnowledgeMapper.class), chunks,
                mock(AiModelMapper.class), mock(AiProviderMapper.class), mock(AiModelAdapterRegistry.class),
                mock(AiSecretCrypto.class), mock(VectorStoreFactory.class), mock(AiStoreInstanceService.class));
        AiKnowledgeChunk live = new AiKnowledgeChunk();
        live.setId(21L);
        live.setVectorId("live");
        live.setDocumentId(2L);
        live.setContent("current database content");
        when(chunks.selectSearchableByVectorIds(eq(1L), anyList())).thenReturn(List.of(live));
        List<KnowledgeSearchResult> results = ReflectionTestUtils.invokeMethod(service, "searchableResults", 1L,
                List.of(hit("deleted", 3L), hit("foreign", 4L), hit("live", 999L), hit("live", 2L)));
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("21", results.get(0).getChunkId());
        assertEquals("current database content", results.get(0).getContent());
        assertEquals(2L, results.get(0).getDocumentId());
    }

    private VectorStoreService.SearchResult hit(String id, Long documentId) {
        VectorStoreService.SearchResult hit = new VectorStoreService.SearchResult();
        hit.setId(id);
        hit.setDocumentId(documentId);
        hit.setContent("stale vector payload");
        hit.setScore(0.8);
        return hit;
    }
}
