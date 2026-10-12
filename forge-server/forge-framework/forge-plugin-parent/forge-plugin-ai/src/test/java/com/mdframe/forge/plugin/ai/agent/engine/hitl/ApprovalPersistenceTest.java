package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatMessageToolCall;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatMessageToolCallService;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ApprovalPersistenceTest {
    @Test
    void resumeMustKeepPriorContentAndCompletedCallsAndFinishOriginalWaitingCall() {
        AiChatRecordService records = mock(AiChatRecordService.class);
        AiChatMessageToolCallService calls = mock(AiChatMessageToolCallService.class);
        AgentChatPersister persistence = new AgentChatPersister(records, calls, mock(AiChatSessionService.class));
        AiChatRecord assistant = AiChatRecord.builder().id(1L).sessionId("session").userId(10L).tenantId(20L)
                .content("earlier ").reasoning("prior reasoning").status("waiting_confirm").build();
        AiChatMessageToolCall completed = AiChatMessageToolCall.builder().id(2L).seq(0).status("success")
                .toolResultJson("original result").build();
        AiChatMessageToolCall waiting = AiChatMessageToolCall.builder().id(3L).seq(1).status("waiting_confirm").build();
        when(records.findByInterruptId("interrupt")).thenReturn(assistant);
        when(calls.listByRecord(1L)).thenReturn(List.of(completed, waiting));
        persistence.openForResume("interrupt", true);
        assertEquals("earlier ", assistant.getContent());
        assertEquals("running", waiting.getStatus());
        persistence.onEvent(AgentEvent.of("session", 0, AgentEventType.TOOL_RESULT_TEXT_DELTA, "{\"content\":\"new result\"}"));
        persistence.onEvent(AgentEvent.of("session", 0, AgentEventType.TOOL_RESULT_END, "{}"));
        persistence.onEvent(AgentEvent.of("session", 1, AgentEventType.TEXT_BLOCK_DELTA, "{\"text\":\"continued\"}"));
        persistence.onEvent(AgentEvent.of("session", 1, AgentEventType.AGENT_END, "{}"));
        assertEquals("earlier continued", assistant.getContent());
        assertEquals("prior reasoning", assistant.getReasoning());
        assertEquals("success", waiting.getStatus());
        assertEquals("new result", waiting.getToolResultJson());
        assertEquals("original result", completed.getToolResultJson());
        verify(calls, never()).removeByIds(anyCollection());
    }
}
