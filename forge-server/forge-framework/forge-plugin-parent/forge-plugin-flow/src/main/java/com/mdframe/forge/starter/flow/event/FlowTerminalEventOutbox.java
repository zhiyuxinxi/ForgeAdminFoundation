package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/** Records terminal events in the original transaction; this component never sends or retries them. */
@Component
public class FlowTerminalEventOutbox {
    private static final Set<String> TERMINAL_TYPES = Set.of(FlowEventMessage.PROCESS_COMPLETED,
            FlowEventMessage.PROCESS_REJECTED, FlowEventMessage.PROCESS_CANCELED);
    private final FlowModelMapper modelMapper;
    private final FlowEventOutboxMapper outboxMapper;
    private final FlowEventDeliveryRecorder deliveryRecorder;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public FlowTerminalEventOutbox(FlowModelMapper modelMapper, FlowEventOutboxMapper outboxMapper) {
        this(modelMapper, outboxMapper, null);
    }

    @Autowired
    public FlowTerminalEventOutbox(FlowModelMapper modelMapper, FlowEventOutboxMapper outboxMapper,
                                  FlowEventDeliveryRecorder deliveryRecorder) {
        this.modelMapper = modelMapper;
        this.outboxMapper = outboxMapper;
        this.deliveryRecorder = deliveryRecorder;
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void recordTerminalEvent(FlowTaskNotifyEvent event) {
        FlowEventMessage message = event.getEventMessage();
        if (event.getType() != FlowTaskNotifyEvent.Type.EVENT_PUBLISH || message == null
                || !TERMINAL_TYPES.contains(message.getEventType())) {
            return;
        }
        requireTransaction();
        try {
            Long tenantId = Long.valueOf(message.getTenantId());
            if (tenantId <= 0 || message.getProcessInstanceId() == null || message.getProcessInstanceId().isBlank()) {
                throw new FlowTerminalEventPersistenceException("FLOW_TERMINAL_EVENT_IDENTITY_REQUIRED");
            }
            FlowEventOutboxEntry entry = TenantContextHolder.executeWithTenant(tenantId,
                    () -> snapshot(message, event.getProcessDefKey(), tenantId));
            if (entry != null) {
                FlowEventDeliveryIdentity delivery = deliveryRecorder == null ? null : deliveryRecorder.snapshot(entry);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        persist(entry, readOnly);
                        if (delivery != null) {
                            TenantContextHolder.executeWithTenant(delivery.tenantId(), () -> deliveryRecorder.record(delivery));
                        }
                    }
                });
            }
        } catch (FlowTerminalEventPersistenceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new FlowTerminalEventPersistenceException(exception);
        }
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new FlowTerminalEventPersistenceException("FLOW_TERMINAL_EVENT_TRANSACTION_REQUIRED");
        }
    }

    private FlowEventOutboxEntry snapshot(FlowEventMessage message, String processDefKey, Long tenantId) {
        FlowModel model = modelMapper.selectByModelKey(processDefKey);
        String destination = destination(model);
        if (destination == null) {
            return null;
        }
        if (message.getEventId() == null || message.getEventId().isBlank()) {
            message.setEventId(UUID.randomUUID().toString());
        }
        try {
            return new FlowEventOutboxEntry(message.getEventId(), tenantId, message.getProcessInstanceId(),
                    message.getEventType(), processDefKey, model.getNotifyType().toLowerCase(java.util.Locale.ROOT),
                    destination, json.writeValueAsString(message), LocalDateTime.now());
        } catch (JsonProcessingException exception) {
            throw new FlowTerminalEventPersistenceException(exception);
        }
    }

    private String destination(FlowModel model) {
        if (model == null) {
            return null;
        }
        if ("redis".equalsIgnoreCase(model.getNotifyType())) {
            return FlowEventPublisher.CHANNEL_ALL;
        }
        if ("webhook".equalsIgnoreCase(model.getNotifyType())
                && model.getWebhookUrl() != null && !model.getWebhookUrl().isBlank()) {
            return model.getWebhookUrl();
        }
        return null;
    }

    private void persist(FlowEventOutboxEntry entry, boolean readOnly) {
        if (readOnly) {
            throw new FlowTerminalEventPersistenceException("FLOW_TERMINAL_EVENT_WRITABLE_TRANSACTION_REQUIRED");
        }
        TenantContextHolder.executeWithTenant(entry.tenantId(), () -> {
            if (outboxMapper.insert(entry) != 1) {
                throw new FlowTerminalEventPersistenceException("FLOW_TERMINAL_EVENT_INSERT_FAILED");
            }
        });
    }
}
