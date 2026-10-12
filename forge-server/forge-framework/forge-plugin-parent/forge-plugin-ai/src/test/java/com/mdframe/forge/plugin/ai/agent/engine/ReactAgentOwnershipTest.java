package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.sse.AgentEventWebFluxStream;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReactAgentOwnershipTest {
    private final ReactLoop loop = mock(ReactLoop.class);
    private final InterruptStore interrupts = mock(InterruptStore.class);
    private final AiChatSessionMapper mapper = mock(AiChatSessionMapper.class);
    private final AiSessionAccessService access = new AiSessionAccessService(mapper);
    private final ReactAgent agent = new ReactAgent(loop, interrupts, access);

    @Test
    void foreignStopAndSubscriptionMustNotAffectOwnersActiveSession() throws Exception {
        setupSession();
        when(loop.run(any())).thenReturn(Flux.never());
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            ReactContext context = context();
            when(loop.activeContext("session")).thenReturn(context);
            doAnswer(call -> {
                context.setCancelled(true);
                return null;
            }).when(loop).cancel(context);
            agent.execute(context);
            AgentEventWebFluxStream stream = new AgentEventWebFluxStream(access);
            var delivery = stream.subscribe("session").next().toFuture();
            identity.when(SessionHelper::getUserId).thenReturn(11L);
            assertThrows(BusinessException.class, () -> agent.cancel("session"));
            assertThrows(BusinessException.class, () -> stream.subscribe("session"));
            assertFalse(context.isCancelled());
            stream.emit(AgentEvent.of("session", 0, AgentEventType.AGENT_END, "owner-only"));
            assertEquals("owner-only", delivery.get().data());
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            agent.cancel("session");
            assertTrue(context.isCancelled());
        }
    }

    @Test
    void resumeMustCheckOwnerBeforePersistenceCallbackRemovalOrExecution() {
        setupSession();
        when(interrupts.get("interrupt")).thenReturn(new InterruptStore.InterruptState(context()));
        when(loop.run(any(), any())).thenAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return Flux.empty();
        });
        when(interrupts.consume(org.mockito.ArgumentMatchers.eq("interrupt"), any())).thenReturn(true);
        AtomicBoolean persisted = new AtomicBoolean();
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(11L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            assertThrows(BusinessException.class, () -> agent.resume("interrupt", true, () -> persisted.set(true)));
            assertFalse(persisted.get());
            verify(interrupts, never()).consume(any(), any());
            verifyNoInteractions(loop);
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            agent.resume("interrupt", true, () -> persisted.set(true));
            assertTrue(persisted.get());
            verify(interrupts).consume(org.mockito.ArgumentMatchers.eq("interrupt"), any());
            verify(loop).run(any(), any());
        }
    }

    @Test
    void contextWithDifferentTenantCannotExecuteUnderOwnedSession() {
        setupSession();
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            ReactContext context = context();
            context.setTenantId(21L);
            assertThrows(BusinessException.class, () -> agent.execute(context));
            verifyNoInteractions(loop);
        }
    }

    private void setupSession() {
        when(mapper.selectById("session")).thenReturn(AiChatSession.builder().id("session")
                .userId(10L).tenantId(20L).status("0").build());
    }

    private ReactContext context() {
        ReactContext context = new ReactContext();
        context.setSessionId("session");
        context.setUserId(10L);
        context.setTenantId(20L);
        context.setPendingToolCalls(java.util.List.of(new com.mdframe.forge.plugin.ai.agent.engine.hitl.PendingToolCall("tool", "{}")));
        return context;
    }
}
