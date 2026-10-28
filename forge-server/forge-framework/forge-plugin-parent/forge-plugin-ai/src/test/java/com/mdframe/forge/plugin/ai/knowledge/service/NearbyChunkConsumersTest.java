package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledge;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiStoreInstance;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchRequest;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreFactory;
import com.mdframe.forge.plugin.ai.knowledge.vectorstore.VectorStoreService;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.rag.search.RagSearchContext;
import com.mdframe.forge.plugin.ai.rag.search.handler.FinalizeHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NearbyChunkConsumersTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overlappingHitsKeepOrderDeduplicateAndCacheEachDocument(boolean pipeline) {
        List<KnowledgeSearchResult> hits = List.of(hit(11, 1L, 1, 1.0), hit(12, 1L, 2, 0.9),
                hit(11, 1L, 1, 1.0), hit(20, 2L, 0, 0.7));
        Fixture fixture = new Fixture(hits, 1);
        when(fixture.chunks.selectByDocumentId(1L)).thenReturn(List.of(
                chunk(12, 1L, 2), chunk(10, 1L, 0), chunk(13, 1L, 3)));
        when(fixture.chunks.selectByDocumentId(2L)).thenReturn(List.of(chunk(21, 2L, 1)));

        List<KnowledgeSearchResult> result = fixture.search(pipeline);

        assertEquals(List.of("11", "10", "12", "13", "20", "21"), ids(result));
        assertEquals(0.8, result.get(1).getScore(), 0.0001);
        assertEquals(0.8, result.get(2).getScore(), 0.0001);
        assertEquals(0.72, result.get(3).getScore(), 0.0001);
        assertEquals("content-10", result.get(1).getContent());
        assertEquals("title-10", result.get(1).getTitle());
        assertEquals(1L, result.get(1).getDocumentId());
        assertEquals(0, result.get(1).getChunkIndex());
        assertEquals(0.9, hits.get(1).getScore());
        verify(fixture.chunks).selectByDocumentId(1L);
        verify(fixture.chunks).selectByDocumentId(2L);
        for (KnowledgeSearchResult row : result) {
            verify(fixture.chunks).incrementRetrievalCount(Long.parseLong(row.getChunkId()));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingDocumentAndNullOrEmptyChunkListsRetainHits(boolean pipeline) {
        Fixture fixture = new Fixture(List.of(hit(1, null, null, 1), hit(2, 9L, 0, 0.9),
                hit(3, 9L, 1, 0.8), hit(4, 10L, 0, 0.7)), 2);
        when(fixture.chunks.selectByDocumentId(9L)).thenReturn(null);
        when(fixture.chunks.selectByDocumentId(10L)).thenReturn(List.of());

        assertEquals(List.of("1", "2", "3", "4"), ids(fixture.search(pipeline)));
        verify(fixture.chunks, never()).selectByDocumentId(isNull());
        verify(fixture.chunks).selectByDocumentId(9L);
        verify(fixture.chunks).selectByDocumentId(10L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void disabledExpansionDoesNotLoadChunksOrDeduplicateHits(boolean pipeline) {
        Fixture fixture = new Fixture(List.of(hit(1, 1L, 0, 1), hit(1, 1L, 0, 1)), 0);

        assertEquals(List.of("1", "1"), ids(fixture.search(pipeline)));
        verify(fixture.chunks, never()).selectByDocumentId(any());
        verify(fixture.chunks, times(2)).incrementRetrievalCount(1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mapperFailurePropagatesBeforeRetrievalCounts(boolean pipeline) {
        Fixture fixture = new Fixture(List.of(hit(1, 1L, 0, 1)), 1);
        IllegalStateException unavailable = new IllegalStateException("chunk read unavailable");
        when(fixture.chunks.selectByDocumentId(1L)).thenThrow(unavailable);

        assertSame(unavailable, assertThrows(IllegalStateException.class, () -> fixture.search(pipeline)));
        verify(fixture.chunks, never()).incrementRetrievalCount(anyLong());
    }

    private static List<String> ids(List<KnowledgeSearchResult> results) {
        return results.stream().map(KnowledgeSearchResult::getChunkId).toList();
    }

    private static KnowledgeSearchResult hit(long id, Long document, Integer index, double score) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setChunkId(String.valueOf(id));
        result.setDocumentId(document);
        result.setChunkIndex(index);
        result.setScore(score);
        return result;
    }

    private static AiKnowledgeChunk chunk(long id, Long document, Integer index) {
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(document);
        chunk.setChunkIndex(index);
        chunk.setContent("content-" + id);
        chunk.setTitle("title-" + id);
        return chunk;
    }

    private static final class Fixture {
        private final AiKnowledgeChunkMapper chunks = mock(AiKnowledgeChunkMapper.class);
        private final AiKnowledgeMapper knowledge = mock(AiKnowledgeMapper.class);
        private final KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        private final List<KnowledgeSearchResult> hits;

        private Fixture(List<KnowledgeSearchResult> hits, int nearbyCount) {
            this.hits = hits;
            AiKnowledge library = new AiKnowledge();
            library.setId(5L);
            library.setEmbeddingModelId(6L);
            library.setVectorStoreInstanceId(8L);
            library.setSearchConfigJson("{\"nearby_count\":" + nearbyCount + "}");
            when(knowledge.selectById(5L)).thenReturn(library);
            request.setKnowledgeId(5L);
            request.setQuery("query");
            request.setNearbyCount(nearbyCount);
            request.setLostInMiddle(false);
            request.setRerankEnable(false);
        }

        private List<KnowledgeSearchResult> search(boolean pipeline) {
            if (pipeline) {
                RagSearchContext context = new RagSearchContext(request);
                context.setFinalResults(hits);
                new FinalizeHandler(knowledge, chunks).handle(context);
                return context.getFinalResults();
            }
            return standalone().search(request);
        }

        private KnowledgeSearchService standalone() {
            AiModelMapper models = mock(AiModelMapper.class);
            AiModel model = new AiModel();
            model.setModelId("embedding");
            model.setProviderId(7L);
            when(models.selectEnabledById(6L)).thenReturn(model);
            AiProviderMapper providers = mock(AiProviderMapper.class);
            when(providers.selectById(7L)).thenReturn(new AiProvider());
            AiModelAdapterRegistry adapters = mock(AiModelAdapterRegistry.class, RETURNS_DEEP_STUBS);
            when(adapters.getEmbedding("embedding").embed(null, null, "embedding", List.of("query")))
                    .thenReturn(List.of(List.of(1.0f)));
            AiStoreInstanceService stores = mock(AiStoreInstanceService.class);
            AiStoreInstance store = new AiStoreInstance();
            when(stores.getById(8L)).thenReturn(store);
            VectorStoreFactory factory = mock(VectorStoreFactory.class);
            VectorStoreService vectors = mock(VectorStoreService.class);
            when(factory.getService(store)).thenReturn(vectors);
            when(vectors.search(any())).thenReturn(hits.stream().map(Fixture::vectorResult).toList());
            List<String> vectorIds = hits.stream().map(KnowledgeSearchResult::getChunkId).distinct().toList();
            when(chunks.selectSearchableByVectorIds(5L, vectorIds)).thenReturn(hits.stream().map(hit -> {
                AiKnowledgeChunk live = chunk(Long.parseLong(hit.getChunkId()), hit.getDocumentId(), hit.getChunkIndex());
                live.setVectorId(hit.getChunkId());
                return live;
            }).toList());
            return new KnowledgeSearchService(knowledge, chunks, models, providers, adapters,
                    mock(AiSecretCrypto.class), factory, stores);
        }

        private static VectorStoreService.SearchResult vectorResult(KnowledgeSearchResult hit) {
            VectorStoreService.SearchResult result = new VectorStoreService.SearchResult();
            result.setId(hit.getChunkId());
            result.setDocumentId(hit.getDocumentId());
            result.setChunkIndex(hit.getChunkIndex());
            result.setScore(hit.getScore());
            return result;
        }
    }
}
