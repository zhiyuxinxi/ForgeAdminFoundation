package com.mdframe.forge.starter.flow.event;

public record FlowPendingDelivery(String eventId, Long tenantId, String consumerId, int attemptCount) {
    public FlowEventDeliveryIdentity identity() {
        return new FlowEventDeliveryIdentity(eventId, tenantId, consumerId);
    }
}
