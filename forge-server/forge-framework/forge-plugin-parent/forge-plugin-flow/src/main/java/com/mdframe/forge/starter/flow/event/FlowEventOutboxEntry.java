package com.mdframe.forge.starter.flow.event;

import java.time.LocalDateTime;

/** Immutable terminal-event snapshot. RECORDED entries are not automatically dispatched. */
public record FlowEventOutboxEntry(String eventId, Long tenantId, String processInstanceId,
                                   String eventType, String processDefKey, String destinationType,
                                   String destination, String payload, LocalDateTime createdAt) {
}
