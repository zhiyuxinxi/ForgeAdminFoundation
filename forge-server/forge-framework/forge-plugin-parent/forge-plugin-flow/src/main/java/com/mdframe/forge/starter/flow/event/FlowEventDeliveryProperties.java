package com.mdframe.forge.starter.flow.event;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Server-owned authentication and immutable event-target binding. Never supplied by event payloads. */
@Data
@Component
@ConfigurationProperties(prefix = "flow.event.delivery")
public class FlowEventDeliveryProperties {
    private boolean enabled;
    private List<Consumer> consumers = new ArrayList<>();

    public String consumerFor(FlowEventOutboxEntry event) {
        if (!enabled) {
            return null;
        }
        List<Consumer> matched = consumers.stream().filter(consumer -> consumer.matches(event)).toList();
        if (matched.size() > 1) {
            throw new IllegalStateException("FLOW_EVENT_CONSUMER_BINDING_AMBIGUOUS");
        }
        if (matched.isEmpty()) {
            return null;
        }
        matched.get(0).requireConfigured();
        return matched.get(0).id;
    }

    public String requireConsumer(String loginId, Long tenantId) {
        List<Consumer> matched = consumers.stream().filter(consumer ->
                Objects.equals(loginId, consumer.loginId) && Objects.equals(tenantId, consumer.tenantId)).toList();
        if (!enabled || matched.size() != 1) {
            throw new IllegalArgumentException("FLOW_EVENT_CONSUMER_IDENTITY_DENIED");
        }
        matched.get(0).requireConfigured();
        return matched.get(0).id;
    }

    @Data
    public static class Consumer {
        private String id;
        private String loginId;
        private Long tenantId;
        private List<String> modelKeys = new ArrayList<>();
        private String destinationType;
        private String destination;

        private boolean matches(FlowEventOutboxEntry event) {
            return Objects.equals(tenantId, event.tenantId()) && modelKeys.contains(event.processDefKey())
                    && Objects.equals(destinationType, event.destinationType())
                    && Objects.equals(destination, event.destination());
        }

        private void requireConfigured() {
            if (id == null || !id.matches("[A-Za-z0-9_.-]{1,100}")
                    || loginId == null || loginId.isBlank() || tenantId == null || tenantId <= 0
                    || modelKeys.isEmpty() || modelKeys.stream().anyMatch(key -> key == null || key.isBlank())
                    || !("redis".equals(destinationType) || "webhook".equals(destinationType))
                    || destination == null || destination.isBlank()) {
                throw new IllegalStateException("FLOW_EVENT_CONSUMER_BINDING_INVALID");
            }
        }
    }
}
