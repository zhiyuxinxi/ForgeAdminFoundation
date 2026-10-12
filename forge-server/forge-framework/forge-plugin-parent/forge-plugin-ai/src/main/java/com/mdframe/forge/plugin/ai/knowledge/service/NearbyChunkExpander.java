package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands retrieved hits with nearby chunks, caching documents only for this call.
 * First occurrence of a chunk wins, including when a nearby chunk precedes a later hit.
 * Callers retain their configuration, ranking, top-K and retrieval-count policies.
 */
public final class NearbyChunkExpander {

    private NearbyChunkExpander() {
    }

    public static List<KnowledgeSearchResult> expand(List<KnowledgeSearchResult> results, int nearbyCount,
                                                     AiKnowledgeChunkMapper chunkMapper) {
        Set<String> seen = new HashSet<>();
        List<KnowledgeSearchResult> expanded = new ArrayList<>();
        Map<Long, List<AiKnowledgeChunk>> chunksByDoc = new HashMap<>();
        for (KnowledgeSearchResult result : results) {
            if (seen.add(result.getChunkId())) {
                expanded.add(result);
            }
            Long documentId = result.getDocumentId();
            if (documentId == null) {
                continue;
            }
            List<AiKnowledgeChunk> chunks = chunksByDoc.computeIfAbsent(documentId, id -> {
                List<AiKnowledgeChunk> list = chunkMapper.selectByDocumentId(id);
                return list != null ? list : List.of();
            });
            appendNearby(result, nearbyCount, chunks, seen, expanded);
        }
        return expanded;
    }

    private static void appendNearby(KnowledgeSearchResult hit, int nearbyCount, List<AiKnowledgeChunk> chunks,
                                     Set<String> seen, List<KnowledgeSearchResult> expanded) {
        int chunkIndex = hit.getChunkIndex();
        for (int offset = -nearbyCount; offset <= nearbyCount; offset++) {
            int targetIndex = chunkIndex + offset;
            if (offset == 0 || targetIndex < 0) {
                continue;
            }
            for (AiKnowledgeChunk chunk : chunks) {
                if (chunk.getChunkIndex() == targetIndex && seen.add(String.valueOf(chunk.getId()))) {
                    expanded.add(nearbyResult(chunk, hit.getScore()));
                }
            }
        }
    }

    private static KnowledgeSearchResult nearbyResult(AiKnowledgeChunk chunk, double hitScore) {
        KnowledgeSearchResult nearby = new KnowledgeSearchResult();
        nearby.setChunkId(String.valueOf(chunk.getId()));
        nearby.setDocumentId(chunk.getDocumentId());
        nearby.setChunkIndex(chunk.getChunkIndex());
        nearby.setContent(chunk.getContent());
        nearby.setTitle(chunk.getTitle());
        nearby.setScore(hitScore * 0.8);
        return nearby;
    }
}
