package com.mdframe.forge.plugin.ai.knowledge.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mdframe.forge.plugin.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.mdframe.forge.plugin.ai.knowledge.service.dto.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeRetrievalCountTest {
    @Test
    void auxiliaryFailuresContinueAndProduceOneSanitizedSummary() {
        AiKnowledgeChunkMapper mapper = mock(AiKnowledgeChunkMapper.class);
        doThrow(new DataAccessResourceFailureException("private database detail"))
                .when(mapper).incrementRetrievalCount(1L);
        var service = new KnowledgeSearchService(null, mapper, null, null, null, null, null, null);
        Logger logger = (Logger) LoggerFactory.getLogger(KnowledgeSearchService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ReflectionTestUtils.invokeMethod(service, "incrementRetrievalCount",
                    List.of(hit("1"), hit("invalid"), hit("3")));
            verify(mapper).incrementRetrievalCount(3L);
            assertEquals(1, appender.list.size());
            String message = appender.list.get(0).getFormattedMessage();
            assertTrue(message.contains("2 of 3"));
            assertTrue(message.contains("DataAccessResourceFailureException"));
            assertFalse(message.contains("private database detail"));
            assertNull(appender.list.get(0).getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void allSuccessfulCountsAreAttempted() {
        AiKnowledgeChunkMapper mapper = mock(AiKnowledgeChunkMapper.class);
        var service = new KnowledgeSearchService(null, mapper, null, null, null, null, null, null);
        ReflectionTestUtils.invokeMethod(service, "incrementRetrievalCount", List.of(hit("1"), hit("2")));
        verify(mapper).incrementRetrievalCount(1L);
        verify(mapper).incrementRetrievalCount(2L);
        verifyNoMoreInteractions(mapper);
    }

    private static KnowledgeSearchResult hit(String id) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setChunkId(id);
        return result;
    }
}
