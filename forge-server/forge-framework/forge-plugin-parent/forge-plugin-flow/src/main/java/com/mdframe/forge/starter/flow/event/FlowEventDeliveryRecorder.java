package com.mdframe.forge.starter.flow.event;

import com.mdframe.forge.starter.flow.mapper.FlowEventDeliveryMapper;
import org.springframework.stereotype.Component;

@Component
public class FlowEventDeliveryRecorder {
    private final FlowEventDeliveryProperties properties;
    private final FlowEventDeliveryMapper mapper;

    public FlowEventDeliveryRecorder(FlowEventDeliveryProperties properties, FlowEventDeliveryMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public FlowEventDeliveryIdentity snapshot(FlowEventOutboxEntry event) {
        String consumer = properties.consumerFor(event);
        return consumer == null ? null : new FlowEventDeliveryIdentity(event.eventId(), event.tenantId(), consumer);
    }

    public void record(FlowEventDeliveryIdentity identity) {
        if (identity != null && mapper.insert(identity) != 1) {
            throw new FlowTerminalEventPersistenceException("FLOW_EVENT_DELIVERY_INSERT_FAILED");
        }
    }

    public boolean hasBoundDelivery(String eventId, Long tenantId) {
        return eventId != null && !eventId.isBlank() && tenantId != null && tenantId > 0
                && mapper.selectBoundConsumer(eventId, tenantId) != null;
    }
}
