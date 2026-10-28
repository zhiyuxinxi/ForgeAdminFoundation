package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Short state writes that must survive rejection after the upload transaction has committed. */
@Service
@RequiredArgsConstructor
public class DocumentProcessState {
    private final AiKnowledgeDocumentMapper documentMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean reject(Long documentId, String attempt, String message) {
        return documentMapper.finishAttempt(documentId, attempt, "pending", "failed", message, null) == 1;
    }
}
