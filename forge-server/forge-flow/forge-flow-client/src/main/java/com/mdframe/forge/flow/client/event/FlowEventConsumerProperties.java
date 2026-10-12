package com.mdframe.forge.flow.client.event;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit opt-in; the same logical consumer identity is shared by all its replicas. */
@Data
@ConfigurationProperties(prefix = "forge.flow.client.events")
public class FlowEventConsumerProperties {
    private boolean enabled;
    private boolean acknowledgementEnabled;
    @lombok.ToString.Exclude
    private String acknowledgementToken;
    private String consumerId;
    private Long tenantId;

    public void requireConfigured() {
        if (!enabled || consumerId == null || !consumerId.matches("[A-Za-z0-9_.-]{1,100}")
                || tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_EVENT_CONSUMER_NOT_CONFIGURED");
        }
    }
}
