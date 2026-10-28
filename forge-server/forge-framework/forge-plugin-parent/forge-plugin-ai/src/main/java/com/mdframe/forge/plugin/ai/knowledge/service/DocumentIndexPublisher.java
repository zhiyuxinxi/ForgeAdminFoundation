package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.chunker.dto.ChunkResult;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeDocument;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Publishes one completed index attempt atomically; performs no remote calls while holding the row lock. */
@Service
@RequiredArgsConstructor
public class DocumentIndexPublisher {
    private final AiKnowledgeDocumentMapper documentMapper;
    private final AiKnowledgeChunkMapper chunkMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Publication publish(Long documentId, String attempt, List<ChunkResult> chunks, List<String> vectorIds) {
        if (attempt == null || attempt.isBlank() || chunks.size() != vectorIds.size()) {
            throw new IllegalArgumentException("A processing attempt and matching chunk/vector counts are required");
        }
        AiKnowledgeDocument document = documentMapper.selectByIdForUpdate(documentId);
        if (document == null || !attempt.equals(document.getProcessAttempt())) {
            return new Publication(false, List.of());
        }
        if ("success".equals(document.getProcessStatus())) {
            return new Publication(true, List.of());
        }
        if (!"processing".equals(document.getProcessStatus())) {
            return new Publication(false, List.of());
        }
        List<AiKnowledgeChunk> previous = chunkMapper.selectByDocumentId(documentId);
        chunkMapper.deleteByDocumentId(documentId);
        for (int i = 0; i < chunks.size(); i++) {
            if (chunkMapper.insert(toEntity(document, chunks.get(i), vectorIds.get(i))) != 1) {
                throw new IllegalStateException("Document chunk was not persisted");
            }
        }
        if (documentMapper.finishAttempt(documentId, attempt, "processing", "success", null, chunks.size()) != 1) {
            throw new IllegalStateException("Document attempt changed before index publication");
        }
        return new Publication(true, List.copyOf(previous));
    }

    private AiKnowledgeChunk toEntity(AiKnowledgeDocument document, ChunkResult chunk, String vectorId) {
        AiKnowledgeChunk entity = new AiKnowledgeChunk();
        entity.setTenantId(document.getTenantId());
        entity.setKnowledgeId(document.getKnowledgeId());
        entity.setDocumentId(document.getId());
        entity.setChunkIndex(chunk.getIndex());
        entity.setContent(chunk.getContent());
        entity.setTitle(chunk.getTitle());
        entity.setTokenCount(chunk.getTokenCount());
        entity.setRefCount(1);
        entity.setContentHash(hashContent(chunk.getContent()));
        entity.setRetrievalCount(0);
        entity.setVectorId(vectorId);
        return entity;
    }

    private String hashContent(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    public record Publication(boolean published, List<AiKnowledgeChunk> supersededChunks) { }
}
