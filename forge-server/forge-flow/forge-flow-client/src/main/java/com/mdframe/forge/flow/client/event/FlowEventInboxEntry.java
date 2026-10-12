package com.mdframe.forge.flow.client.event;

/** One local transactional completion marker per logical callback. */
public record FlowEventInboxEntry(Long tenantId, String consumerId, String eventId,
                                  String callbackId, String payloadHash, String status) {
}
