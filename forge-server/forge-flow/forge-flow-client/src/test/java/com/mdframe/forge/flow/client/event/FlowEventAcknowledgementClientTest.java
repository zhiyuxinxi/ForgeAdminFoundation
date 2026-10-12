package com.mdframe.forge.flow.client.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClientProperties;
import com.mdframe.forge.flow.client.helper.FlowEventSubscriber;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowEventAcknowledgementClientTest {
    @Test
    void lostAckResponseRepeatsOnlyAckAfterBusinessAndInboxHaveCommitted() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            var callback = fixture.add("approved");
            var consumer = fixture.consumer();
            var outbound = mock(SecureOutboundClient.class);
            fixture.properties.setAcknowledgementEnabled(true);
            fixture.properties.setAcknowledgementToken("test-service-token");
            var client = new FlowEventAcknowledgementClient(fixture.properties, clientProperties(), outbound);
            var subscriber = new FlowEventSubscriber(fixture.context);
            subscriber.setDurableConsumer(consumer);
            subscriber.setAcknowledgementClient(client);
            when(outbound.execute(any())).thenThrow(new IllegalStateException("fixture-lost-response"))
                    .thenReturn(response(200, "{\"code\":200,\"data\":\"event-one\"}"));
            assertThrows(IllegalStateException.class, () -> subscriber.dispatch(FlowInboxFixture.event()));
            assertEquals(1, fixture.completed());
            subscriber.dispatch(FlowInboxFixture.event());
            assertEquals(1, callback.calls.get());
            assertEquals(1, fixture.effects());
            ArgumentCaptor<OutboundRequest> sent = ArgumentCaptor.forClass(OutboundRequest.class);
            verify(outbound, times(2)).execute(sent.capture());
            var request = sent.getValue();
            assertEquals(OutboundScenes.FLOW_API, request.getScene());
            assertEquals("https://flow.example/api/flow/events/ack", request.getUrl());
            assertEquals("Bearer test-service-token", request.getHeaders().get("Authorization"));
            assertFalse(request.getHeaders().containsKey("X-Inner-Call"));
            assertEquals(Map.of("eventId", "event-one"), new ObjectMapper().readValue(request.getBody(), Map.class));
        }
    }

    @Test
    void callbackFailureDoesNotSendAck() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            fixture.add("approved").fail = true;
            var consumer = fixture.consumer();
            var outbound = mock(SecureOutboundClient.class);
            fixture.properties.setAcknowledgementEnabled(true);
            fixture.properties.setAcknowledgementToken("test-service-token");
            var subscriber = new FlowEventSubscriber(fixture.context);
            subscriber.setDurableConsumer(consumer);
            subscriber.setAcknowledgementClient(new FlowEventAcknowledgementClient(
                    fixture.properties, clientProperties(), outbound));
            assertThrows(IllegalStateException.class, () -> subscriber.dispatch(FlowInboxFixture.event()));
            verifyNoInteractions(outbound);
            assertEquals(0, fixture.completed());
        }
    }

    @Test
    void remoteHttpBusinessAndWrongEventResultsAreNotAcknowledgements() throws Exception {
        try (var fixture = new FlowInboxFixture()) {
            fixture.properties.setAcknowledgementEnabled(true);
            fixture.properties.setAcknowledgementToken("test-service-token");
            var outbound = mock(SecureOutboundClient.class);
            var client = new FlowEventAcknowledgementClient(fixture.properties, clientProperties(), outbound);
            var result = new FlowDurableEventConsumer.Consumption("event-one", 97L, "business-admin", 1);
            when(outbound.execute(any())).thenReturn(response(403, "{}"))
                    .thenReturn(response(200, "{\"code\":500,\"data\":\"event-one\"}"))
                    .thenReturn(response(200, "{\"code\":200,\"data\":\"another-event\"}"));
            assertThrows(IllegalStateException.class, () -> client.acknowledge(result));
            assertThrows(IllegalStateException.class, () -> client.acknowledge(result));
            assertThrows(IllegalStateException.class, () -> client.acknowledge(result));
        }
    }

    private FlowClientProperties clientProperties() {
        var properties = new FlowClientProperties();
        properties.setUrl("https://flow.example/");
        properties.setToken("must-not-use-interactive-fallback");
        return properties;
    }

    private OutboundResponse response(int status, String body) {
        return new OutboundResponse(status, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    }
}
