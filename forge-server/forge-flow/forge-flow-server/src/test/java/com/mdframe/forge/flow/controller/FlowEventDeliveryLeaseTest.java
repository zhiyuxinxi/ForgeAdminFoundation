package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.flow.event.FlowEventDeliveryDispatcher;
import com.mdframe.forge.starter.flow.event.FlowEventOutboxEntry;
import com.mdframe.forge.starter.flow.event.FlowRecordedEventSender;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowEventDeliveryLeaseTest {
    @Test
    void abandonedLeaseRecoversWithNewDispatcherAndCompetingWorkerCannotDoubleClaim() throws Exception {
        try (var flow = new FlowEventAckFixture()) {
            var outbox = FlowDeliveryRoundTripTest.prepareOutbox(flow.source);
            LocalDateTime now = LocalDateTime.now().plusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            var event = new FlowEventOutboxEntry("event-lease", 97L, "instance", "PROCESS_COMPLETED",
                    "approval", "redis", "flow:event:all", "{}", now);
            outbox.insert(event);
            flow.pending(event.eventId(), "business-admin");
            var candidate = flow.mapper.selectDue(now, 20).get(0);
            assertEquals(1, flow.mapper.claim(candidate, "lost-process", now, now.plusSeconds(60)));
            var sender = mock(FlowRecordedEventSender.class);
            var dispatcher = new FlowEventDeliveryDispatcher(flow.mapper, outbox, flow.properties, sender);
            assertEquals(0, dispatcher.dispatchAt(now.plusSeconds(59)));
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            doAnswer(call -> {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return null;
            })
                    .when(sender).send(any());
            var worker = Executors.newSingleThreadExecutor();
            try {
                var restarted = new FlowEventDeliveryDispatcher(flow.mapper, outbox, flow.properties, sender);
                var execution = worker.submit(() -> restarted.dispatchAt(now.plusSeconds(61)));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(0, dispatcher.dispatchAt(now.plusSeconds(61)));
                release.countDown();
                assertEquals(1, execution.get(5, TimeUnit.SECONDS));
                assertEquals("PENDING", flow.status(event.eventId()));
                assertEquals(0, dispatcher.dispatchAt(now.plusSeconds(62)), "Must respect retry backoff");
                verify(sender, times(1)).send(event);
            } finally {
                release.countDown();
                worker.shutdownNow();
            }
        }
    }

    @Test
    void dispatcherIsAbsentWithoutExplicitRetryConfiguration() {
        try (var context = new AnnotationConfigApplicationContext(FlowEventDeliveryDispatcher.class)) {
            assertTrue(context.getBeansOfType(FlowEventDeliveryDispatcher.class).isEmpty());
        }
    }

    @Test
    void webhookUsesOriginalPayloadAndExistingOutboundPolicyAndRejectsHttpFailure() {
        var outbound = mock(SecureOutboundClient.class);
        var sender = new FlowRecordedEventSender(new StaticListableBeanFactory()
                .getBeanProvider(StringRedisTemplate.class), outbound);
        var entry = new FlowEventOutboxEntry("event-http", 97L, "instance", "PROCESS_COMPLETED", "approval",
                "webhook", "https://consumer.example/flow-events", "{\"businessKey\":\"sample:one\"}", LocalDateTime.now());
        when(outbound.execute(any())).thenReturn(new OutboundResponse(204, Map.of(), new byte[0]))
                .thenReturn(new OutboundResponse(503, Map.of(), new byte[0]));
        sender.send(entry);
        var request = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(outbound).execute(request.capture());
        assertEquals(OutboundScenes.FLOW_API, request.getValue().getScene());
        assertEquals(entry.destination(), request.getValue().getUrl());
        assertEquals("sample:one", request.getValue().getHeaders().get("X-Flow-Business-Key"));
        assertArrayEquals(entry.payload().getBytes(StandardCharsets.UTF_8), request.getValue().getBody());
        assertThrows(IllegalStateException.class, () -> sender.send(entry));
    }
}
