package com.mdframe.forge.plugin.ai.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class DocumentProcessingMessageTest {
    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void recoveryErrorUsesRequestLocaleAndPreservesOtherErrorText() {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setProcessError("knowledge.document.processing.interrupted");
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("Document processing timed out or was interrupted. Please retry processing.", document.getProcessError());
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        assertEquals("文档处理已超时或中断，请重新处理", document.getProcessError());
        document.setProcessError("Original provider message");
        assertEquals("Original provider message", document.getProcessError());
    }

    @Test
    void internalAttemptIsNotAnApiInputOrOutput() throws Exception {
        ObjectMapper json = new ObjectMapper();
        AiKnowledgeDocument document = json.readValue("{\"processAttempt\":\"client-chosen\"}", AiKnowledgeDocument.class);
        assertNull(document.getProcessAttempt());
        document.setProcessAttempt("internal-attempt");
        assertFalse(json.writeValueAsString(document).contains("internal-attempt"));
    }
}
