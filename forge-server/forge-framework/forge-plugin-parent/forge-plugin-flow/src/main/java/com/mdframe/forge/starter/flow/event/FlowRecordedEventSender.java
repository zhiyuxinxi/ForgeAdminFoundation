package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Sends exactly the stored destination and bytes; transport success is never a consumer ACK. */
@Component
public class FlowRecordedEventSender {
    private final ObjectProvider<StringRedisTemplate> redis;
    private final SecureOutboundClient outbound;
    private final ObjectMapper json = new ObjectMapper();

    public FlowRecordedEventSender(ObjectProvider<StringRedisTemplate> redis, SecureOutboundClient outbound) {
        this.redis = redis;
        this.outbound = outbound;
    }

    public void send(FlowEventOutboxEntry event) {
        if ("redis".equals(event.destinationType())) {
            StringRedisTemplate template = redis.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException("FLOW_EVENT_REDIS_UNAVAILABLE");
            }
            template.convertAndSend(event.destination(), event.payload());
        } else if ("webhook".equals(event.destinationType())) {
            var response = outbound.execute(OutboundRequest.builder().scene(OutboundScenes.FLOW_API)
                    .url(event.destination()).method("POST").contentType("application/json")
                    .headers(headers(event)).body(event.payload().getBytes(StandardCharsets.UTF_8)).build());
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new IllegalStateException("FLOW_EVENT_WEBHOOK_HTTP_FAILED");
            }
        } else {
            throw new IllegalArgumentException("FLOW_EVENT_DESTINATION_UNSUPPORTED");
        }
    }

    private Map<String, String> headers(FlowEventOutboxEntry event) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Flow-Event-Type", event.eventType());
        headers.put("X-Flow-Process-Key", event.processDefKey());
        try {
            JsonNode key = json.readTree(event.payload()).get("businessKey");
            if (key != null && !key.isNull()) {
                headers.put("X-Flow-Business-Key", key.asText());
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FLOW_EVENT_STORED_PAYLOAD_INVALID", exception);
        }
        return headers;
    }
}
