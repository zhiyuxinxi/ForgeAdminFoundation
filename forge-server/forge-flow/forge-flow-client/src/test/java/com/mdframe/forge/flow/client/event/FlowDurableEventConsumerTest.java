package com.mdframe.forge.flow.client.event;

import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.helper.FlowEventSubscriber;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowDurableEventConsumerTest {
    @Test
    void subscriberCommitsCallbackWithMarkerAndDuplicateSkipsProxyInvocation() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            var callback = fixture.add("approved");
            var consumer = fixture.consumer();
            var subscriber = new FlowEventSubscriber(fixture.context);
            subscriber.setDurableConsumer(consumer);
            TenantContextHolder.executeWithTenant(98L, () -> {
                subscriber.dispatch(FlowInboxFixture.event());
                subscriber.dispatch(FlowInboxFixture.event());
                assertEquals(98L, TenantContextHolder.getTenantId());
            });
            assertEquals(1, fixture.effects());
            assertEquals(1, fixture.completed());
            assertEquals(1, callback.calls.get());
            assertEquals(1, callback.advice.get(), "Must call through the JDK Spring proxy");
        }
    }

    @Test
    void callbackFailureRollsBackBusinessAndMarkerThenSameEventRetries() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            var callback = fixture.add("approved");
            callback.fail = true;
            var consumer = fixture.consumer();
            TenantContextHolder.executeWithTenant(98L, () -> {
                assertThrows(IllegalStateException.class, () -> consumer.consume(FlowInboxFixture.event()));
                assertEquals(98L, TenantContextHolder.getTenantId());
            });
            assertEquals(0, fixture.effects());
            assertEquals(0, fixture.completed());
            callback.fail = false;
            assertEquals("event-one", consumer.consume(FlowInboxFixture.event()).eventId());
            assertEquals(1, fixture.effects());
            assertEquals(1, fixture.completed());
        }
    }

    @Test
    void failedCompletionWriteAlsoRollsBackTheBusinessCallback() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            fixture.add("approved");
            var consumer = fixture.consumer();
            fixture.jdbc.execute("ALTER TABLE sys_flow_event_inbox ADD CONSTRAINT reject_completion "
                    + "CHECK (status <> 'COMPLETED')");
            assertThrows(RuntimeException.class, () -> consumer.consume(FlowInboxFixture.event()));
            assertEquals(0, fixture.effects());
            assertEquals(0, fixture.completed());
        }
    }

    @Test
    void partialFailureRetriesOnlyTheUncommittedCallback() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            var first = fixture.add("first");
            var second = fixture.add("second");
            second.fail = true;
            var consumer = fixture.consumer();
            assertThrows(IllegalStateException.class, () -> consumer.consume(FlowInboxFixture.event()));
            assertEquals(1, fixture.effects());
            assertEquals(1, fixture.completed());
            second.fail = false;
            assertEquals(2, consumer.consume(FlowInboxFixture.event()).callbacks());
            assertEquals(1, first.calls.get());
            assertEquals(2, second.calls.get());
            assertEquals(2, fixture.effects());
            assertEquals(2, fixture.completed());
        }
    }

    @Test
    void concurrentDuplicatesExecuteOnceBehindTheDatabaseUniqueKey() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            var callback = fixture.add("approved");
            callback.entered = new CountDownLatch(1);
            callback.release = new CountDownLatch(1);
            var consumer = fixture.consumer();
            var pool = Executors.newFixedThreadPool(2);
            try {
                var first = pool.submit(() -> consumer.consume(FlowInboxFixture.event()));
                assertTrue(callback.entered.await(5, TimeUnit.SECONDS));
                CountDownLatch secondStarted = new CountDownLatch(1);
                var second = pool.submit(() -> {
                    secondStarted.countDown();
                    return consumer.consume(FlowInboxFixture.event());
                });
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                callback.release.countDown();
                assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                assertEquals(1, callback.calls.get());
                assertEquals(1, fixture.effects());
                assertEquals(1, fixture.completed());
            } finally {
                callback.release.countDown();
                pool.shutdownNow();
            }
        }
    }

    @Test
    void wrongTenantMissingHandlerAndChangedPayloadNeverReportCompletion() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            fixture.add("approved");
            var consumer = fixture.consumer();
            var event = FlowInboxFixture.event();
            event.setTenantId(98L);
            assertThrows(IllegalStateException.class, () -> consumer.consume(event));
            event.setTenantId(97L);
            event.setProcessDefKey("unknown");
            assertThrows(IllegalStateException.class, () -> consumer.consume(event));
            assertEquals(0, fixture.effects());
            consumer.consume(FlowInboxFixture.event());
            event.setProcessDefKey("approval");
            event.setBusinessKey("tampered");
            assertThrows(IllegalStateException.class, () -> consumer.consume(event));
            assertEquals(1, fixture.effects());
        }
    }

    @Test
    void independentCallbackTransactionIsRejectedBeforeItCanCommit() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            fixture.addBean("unsafe", new IndependentCallback());
            var consumer = fixture.consumer();
            assertThrows(IllegalStateException.class, () -> consumer.consume(FlowInboxFixture.event()));
            assertEquals(0, fixture.completed());
        }
    }

    @Test
    void absentConfigurationDoesNotRequireDatabaseOrEnableDurableConsumption() {
        try (var context = new AnnotationConfigApplicationContext(FlowEventConsumerConfiguration.class)) {
            assertTrue(context.getBeansOfType(FlowDurableEventConsumer.class).isEmpty());
        }
        assertThrows(IllegalStateException.class, () -> new FlowEventConsumerProperties().requireConfigured());
        assertFalse(FlowDurableEventConsumer.isTerminal(new FlowEventContext()));
    }

    @FlowBind(modelKey = "approval")
    public static class IndependentCallback implements FlowInboxFixture.CallbackContract {
        @Override
        @FlowCallback
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void finish(FlowEventContext context) {
            throw new AssertionError("Must reject before invoking an independently committed callback");
        }
    }
}
