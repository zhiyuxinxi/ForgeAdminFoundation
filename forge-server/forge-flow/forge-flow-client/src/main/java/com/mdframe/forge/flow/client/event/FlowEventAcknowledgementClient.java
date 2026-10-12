package com.mdframe.forge.flow.client.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClientProperties;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;

import java.io.IOException;
import java.util.Map;

/** Uses only the configured service token; never delegates an interactive user's token. */
public class FlowEventAcknowledgementClient {
    private final FlowEventConsumerProperties consumer;
    private final FlowClientProperties client;
    private final SecureOutboundClient outbound;
    private final ObjectMapper json = new ObjectMapper();

    public FlowEventAcknowledgementClient(FlowEventConsumerProperties consumer,
            FlowClientProperties client, SecureOutboundClient outbound) {
        consumer.requireConfigured();
        if (!consumer.isAcknowledgementEnabled() || consumer.getAcknowledgementToken() == null
                || consumer.getAcknowledgementToken().isBlank()
                || client.getUrl() == null || client.getUrl().isBlank()) {
            throw new IllegalStateException("FLOW_EVENT_ACK_CLIENT_NOT_CONFIGURED");
        }
        this.consumer = consumer;
        this.client = client;
        this.outbound = outbound;
    }

    public void acknowledge(FlowDurableEventConsumer.Consumption result) {
        if (!consumer.getTenantId().equals(result.tenantId())
                || !consumer.getConsumerId().equals(result.consumerId()) || result.callbacks() < 1) {
            throw new IllegalArgumentException("FLOW_EVENT_ACK_LOCAL_IDENTITY_MISMATCH");
        }
        String base = client.getUrl().replaceAll("/+$", "");
        try {
            var request = OutboundRequest.builder().scene(OutboundScenes.FLOW_API)
                    .url(base + "/api/flow/events/ack").method("POST").contentType("application/json")
                    .headers(Map.of("Authorization", "Bearer " + consumer.getAcknowledgementToken().trim(),
                            "Accept", "application/json"))
                    .body(json.writeValueAsBytes(Map.of("eventId", result.eventId()))).build();
            var response = outbound.execute(request);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new IllegalStateException("FLOW_EVENT_ACK_HTTP_FAILED");
            }
            FlowResult<String> receipt = json.readValue(response.getBody(), new TypeReference<>() { });
            if (receipt == null || !receipt.isSuccess() || !result.eventId().equals(receipt.getData())) {
                throw new IllegalStateException("FLOW_EVENT_ACK_NOT_CONFIRMED");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("FLOW_EVENT_ACK_RESPONSE_INVALID", exception);
        }
    }
}
