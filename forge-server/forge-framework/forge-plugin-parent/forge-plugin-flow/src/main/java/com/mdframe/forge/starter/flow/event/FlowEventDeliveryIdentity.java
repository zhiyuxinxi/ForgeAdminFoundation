package com.mdframe.forge.starter.flow.event;

/** A server-bound delivery target, persisted in the same transaction as its outbox event. */
public record FlowEventDeliveryIdentity(String eventId, Long tenantId, String consumerId) {
}
