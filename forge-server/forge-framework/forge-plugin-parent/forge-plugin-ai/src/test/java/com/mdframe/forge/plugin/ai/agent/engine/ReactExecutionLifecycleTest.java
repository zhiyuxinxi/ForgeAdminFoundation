package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.PendingToolCall;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ReactToolDispatcher;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReactExecutionLifecycleTest {
    @Test
    void completedModelRetainsTextAndReasoningEventContract() {
        try (Fixture fixture = new Fixture(5000)) {
            var output = org.springframework.ai.chat.messages.AssistantMessage.builder().content("hello")
                    .properties(java.util.Map.of("reasoning_content", "why")).build();
            var response = new ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(output)));
            when(fixture.model.stream(any(Prompt.class))).thenReturn(Flux.just(response));
            var events = fixture.agent.execute(fixture.context("success")).collectList().block(Duration.ofSeconds(2));
            assertNotNull(events);
            var types = events.stream().map(AgentEvent::getEventType).toList();
            assertTrue(types.containsAll(List.of(
                    com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType.THINKING_BLOCK_DELTA,
                    com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType.TEXT_BLOCK_START,
                    com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType.TEXT_BLOCK_DELTA,
                    com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType.TEXT_BLOCK_END)));
            assertFalse(events.stream().anyMatch(e -> "{\"aborted\":true}".equals(e.getData())));
        }
    }

    @Test
    void admissionRejectsOverlapAndExhaustionBeforePersistenceThenOwnedStopCancelsModel() throws Exception {
        try (Fixture fixture = new Fixture(5000)) {
            AtomicInteger writes = new AtomicInteger();
            var result = fixture.agent.execute(fixture.context("one"), writes::incrementAndGet).collectList().toFuture();
            assertTrue(fixture.subscribed.await(2, TimeUnit.SECONDS));
            assertThrows(BusinessException.class, () -> fixture.agent.execute(fixture.context("one"), writes::incrementAndGet));
            assertThrows(BusinessException.class, () -> fixture.agent.execute(fixture.context("two"), writes::incrementAndGet));
            assertEquals(1, writes.get());
            fixture.agent.cancel("one");
            assertTrue(fixture.cancelled.await(2, TimeUnit.SECONDS));
            assertTrue(result.get(2, TimeUnit.SECONDS).stream().anyMatch(e -> "{\"aborted\":true}".equals(e.getData())));
            assertNull(fixture.pool.activeContext("one"));
        }
    }

    @Test
    void deadlineCancelsActualSubscriptionAndReleasesCapacity() throws Exception {
        try (Fixture fixture = new Fixture(150)) {
            List<AgentEvent> result = fixture.agent.execute(fixture.context("deadline")).collectList().block(Duration.ofSeconds(3));
            assertTrue(fixture.cancelled.await(1, TimeUnit.SECONDS));
            assertNotNull(result);
            assertTrue(result.stream().anyMatch(e -> "{\"aborted\":true}".equals(e.getData())));
            when(fixture.model.stream(any(Prompt.class))).thenReturn(Flux.empty());
            assertNotNull(fixture.agent.execute(fixture.context("next")).collectList().block(Duration.ofSeconds(2)));
        }
    }

    @Test
    void subscriberDisconnectCancelsActualModelWork() throws Exception {
        try (Fixture fixture = new Fixture(5000)) {
            var subscriber = fixture.agent.execute(fixture.context("disconnect")).subscribe();
            assertTrue(fixture.subscribed.await(2, TimeUnit.SECONDS));
            subscriber.dispose();
            assertTrue(fixture.cancelled.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void busySessionDoesNotConsumeApprovalCheckpoint() throws Exception {
        try (Fixture fixture = new Fixture(5000)) {
            var active = fixture.agent.execute(fixture.context("one")).collectList().toFuture();
            assertTrue(fixture.subscribed.await(2, TimeUnit.SECONDS));
            ReactContext approved = fixture.context("one");
            approved.setPendingToolCalls(List.of(new PendingToolCall("tool", "{}")));
            when(fixture.interrupts.get("approval")).thenReturn(new InterruptStore.InterruptState(approved));
            AtomicInteger writes = new AtomicInteger();
            assertThrows(BusinessException.class, () -> fixture.agent.resume("approval", true, writes::incrementAndGet));
            verify(fixture.interrupts, never()).consume(any(), any());
            assertEquals(0, writes.get());
            fixture.agent.cancel("one");
            active.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void persistenceFailureReleasesReservationWithoutCallingModel() {
        try (Fixture fixture = new Fixture(5000)) {
            assertThrows(IllegalStateException.class, () -> fixture.agent.execute(fixture.context("one"),
                    () -> { throw new IllegalStateException("write failed"); }));
            assertNull(fixture.pool.activeContext("one"));
            verify(fixture.model, never()).stream(any(Prompt.class));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final ReactExecutionPool pool;
        final ChatModel model = mock(ChatModel.class);
        final CountDownLatch subscribed = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final InterruptStore interrupts = mock(InterruptStore.class);
        final ReactAgent agent;

        Fixture(long timeoutMillis) {
            pool = new ReactExecutionPool(1, timeoutMillis);
            when(model.stream(any(Prompt.class))).thenReturn(Flux.<ChatResponse>never()
                    .doOnSubscribe(ignored -> subscribed.countDown()).doOnCancel(cancelled::countDown));
            ReactLoop loop = new ReactLoop(mock(AiAgentService.class), mock(AgentEventPublisher.class),
                    mock(ReactToolDispatcher.class), mock(AiModelInvocationRecorder.class), pool);
            AiSessionAccessService access = mock(AiSessionAccessService.class);
            when(access.requireOwned(any())).thenAnswer(call -> AiChatSession.builder()
                    .id(call.getArgument(0)).userId(10L).tenantId(20L).build());
            agent = new ReactAgent(loop, interrupts, access);
        }

        ReactContext context(String session) {
            ReactContext context = new ReactContext();
            context.setSessionId(session);
            context.setUserId(10L);
            context.setTenantId(20L);
            context.setMaxIters(1);
            context.setChatModel(model);
            return context;
        }

        public void close() {
            pool.close();
        }
    }
}
