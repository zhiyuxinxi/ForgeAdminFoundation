package com.mdframe.forge.starter.flow.event;

import com.mdframe.forge.starter.flow.mapper.FlowEventDeliveryMapper;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/** Bounded retries of explicitly bound events. Database leases survive process replacement. */
@Component
@ConditionalOnProperty(prefix = "flow.event.delivery", name = {"enabled", "retry-enabled"}, havingValue = "true")
public class FlowEventDeliveryDispatcher {
    private final FlowEventDeliveryMapper deliveries;
    private final FlowEventOutboxMapper outbox;
    private final FlowEventDeliveryProperties properties;
    private final FlowRecordedEventSender sender;

    public FlowEventDeliveryDispatcher(FlowEventDeliveryMapper deliveries, FlowEventOutboxMapper outbox,
                                      FlowEventDeliveryProperties properties, FlowRecordedEventSender sender) {
        this.deliveries = deliveries;
        this.outbox = outbox;
        this.properties = properties;
        this.sender = sender;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatch() {
        dispatchAt(LocalDateTime.now());
    }

    public int dispatchAt(LocalDateTime now) {
        if (!properties.isEnabled()) {
            return 0;
        }
        int claimed = 0;
        for (FlowPendingDelivery candidate : deliveries.selectDue(now, 20)) {
            if (candidate.tenantId() == null || candidate.tenantId() <= 0) {
                throw new IllegalStateException("FLOW_EVENT_DELIVERY_TENANT_REQUIRED");
            }
            boolean sent = TenantContextHolder.executeWithTenant(candidate.tenantId(), () -> deliver(candidate, now));
            if (sent) {
                claimed++;
            }
        }
        return claimed;
    }

    private boolean deliver(FlowPendingDelivery candidate, LocalDateTime now) {
        String claim = UUID.randomUUID().toString();
        if (deliveries.claim(candidate, claim, now, now.plusSeconds(60)) != 1) {
            return false;
        }
        String error = null;
        try {
            FlowEventOutboxEntry event = outbox.selectRecorded(candidate.tenantId(), candidate.eventId());
            if (event == null || !candidate.consumerId().equals(properties.consumerFor(event))) {
                error = "CONSUMER_BINDING_UNAVAILABLE";
            } else {
                sender.send(event);
            }
        } catch (RuntimeException exception) {
            error = "TRANSPORT_FAILED";
        }
        long delay = Math.min(300L, 5L << Math.min(candidate.attemptCount(), 6));
        deliveries.reschedule(candidate.identity(), claim, now.plusSeconds(delay), error);
        return true;
    }
}
