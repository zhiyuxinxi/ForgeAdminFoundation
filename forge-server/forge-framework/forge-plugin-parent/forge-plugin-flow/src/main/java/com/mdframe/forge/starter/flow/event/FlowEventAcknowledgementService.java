package com.mdframe.forge.starter.flow.event;

import com.mdframe.forge.starter.flow.mapper.FlowEventDeliveryMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowEventAcknowledgementService {
    private final FlowEventDeliveryProperties properties;
    private final FlowEventDeliveryMapper mapper;

    public FlowEventAcknowledgementService(FlowEventDeliveryProperties properties, FlowEventDeliveryMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public String acknowledge(String eventId, String authenticatedLoginId, Long authenticatedTenantId) {
        if (eventId == null || !eventId.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("FLOW_EVENT_ID_REQUIRED");
        }
        String consumer = properties.requireConsumer(authenticatedLoginId, authenticatedTenantId);
        var identity = new FlowEventDeliveryIdentity(eventId, authenticatedTenantId, consumer);
        return TenantContextHolder.executeWithTenant(authenticatedTenantId, () -> {
            String status = mapper.selectStatus(identity);
            if ("ACKNOWLEDGED".equals(status)) {
                return eventId;
            }
            if (!"PENDING".equals(status) || mapper.acknowledge(identity) != 1) {
                throw new IllegalArgumentException("FLOW_EVENT_ACK_TARGET_DENIED");
            }
            return eventId;
        });
    }
}
