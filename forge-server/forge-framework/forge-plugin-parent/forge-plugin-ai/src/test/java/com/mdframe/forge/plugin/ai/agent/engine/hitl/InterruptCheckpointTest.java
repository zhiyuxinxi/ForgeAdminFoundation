package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterruptCheckpointTest {
    @Test
    void checkpointMustExcludeRuntimeObjectsAndRetainExactPendingCall() {
        ApprovalRedis redis = new ApprovalRedis();
        ReactContext context = context();
        context.setChatModel(mock(ChatModel.class));
        context.setBoundTools(List.of(mock(AgentTool.class)));
        context.setFileUrlResolver(id -> { throw new AssertionError("runtime function must not be serialized"); });
        context.setHistory(List.of(new UserMessage("earlier text")));
        context.setToolCallResults(new ArrayList<>(List.of("previous tool result")));
        String id = redis.store.save(context);
        String json = redis.values.values().iterator().next();
        assertFalse(json.contains("chatModel"));
        assertFalse(json.contains("boundTools"));
        assertFalse(json.contains("fileUrlResolver"));
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            ReactContext restored = redis.store.get(id).getContext();
            assertNull(restored.getChatModel());
            assertTrue(restored.getBoundTools().isEmpty());
            assertEquals(context.getPendingToolCalls(), restored.getPendingToolCalls());
            assertEquals("earlier text", restored.getHistory().get(0).getText());
            assertEquals(context.getToolCallResults(), restored.getToolCallResults());
            assertFalse(restored.isPendingToolApproved());
        }
    }

    @Test
    void twoReadersCanConsumeOnlyOnceAndForeignOwnerCannotClaim() {
        ApprovalRedis redis = new ApprovalRedis();
        String id = redis.store.save(context());
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            var first = redis.store.get(id);
            var second = redis.store.get(id);
            identity.when(SessionHelper::getUserId).thenReturn(11L);
            assertNull(redis.store.get(id));
            assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                    () -> redis.store.consume(id, first));
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            assertTrue(redis.store.consume(id, first));
            assertFalse(redis.store.consume(id, second));
            assertNull(redis.store.get(id));
        }
    }

    static ReactContext context() {
        ReactContext context = new ReactContext();
        context.setSessionId("session");
        context.setUserId(10L);
        context.setTenantId(20L);
        context.setPendingToolCalls(List.of(new PendingToolCall("delete_item", "{\"id\":7}")));
        return context;
    }
}
