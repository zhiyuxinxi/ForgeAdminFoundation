package com.mdframe.forge.flow.client.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.mapper.FlowEventInboxMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/** Local inbox and callbacks only; returning a result is not a remote ACK. */
public class FlowDurableEventConsumer {
    private static final Set<String> TERMINAL = Set.of(FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED, FlowCallback.ON_CANCELED);
    private final FlowEventConsumerProperties properties;
    private final FlowCallbackCatalog catalog;
    private final FlowEventInboxMapper mapper;
    private final DataSource dataSource;
    private final TransactionTemplate transaction;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public FlowDurableEventConsumer(FlowEventConsumerProperties properties, FlowCallbackCatalog catalog,
                                    FlowEventInboxMapper mapper, DataSource dataSource,
                                    PlatformTransactionManager transactionManager) {
        properties.requireConfigured();
        this.properties = properties;
        this.catalog = catalog;
        this.mapper = mapper;
        this.dataSource = dataSource;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static boolean isTerminal(FlowEventContext event) {
        return event != null && event.getEvent() != null && TERMINAL.contains(event.getEvent());
    }

    public Consumption consume(FlowEventContext event) {
        requireIdentity(event);
        String snapshot = serialize(event);
        String fingerprint = digest(snapshot);
        return TenantContextHolder.executeWithTenant(event.getTenantId(), () -> {
            var callbacks = catalog.resolve(event);
            if (callbacks.isEmpty()) {
                throw new IllegalStateException("FLOW_EVENT_CALLBACK_NOT_FOUND");
            }
            for (FlowCallbackInvocation callback : callbacks) {
                var entry = new FlowEventInboxEntry(event.getTenantId(), properties.getConsumerId(),
                        event.getEventId(), digest(callback.identity()), fingerprint, "PROCESSING");
                transaction.executeWithoutResult(status -> invokeOnce(entry, callback, snapshot));
            }
            return new Consumption(event.getEventId(), event.getTenantId(), properties.getConsumerId(), callbacks.size());
        });
    }

    private void invokeOnce(FlowEventInboxEntry entry, FlowCallbackInvocation callback, String snapshot) {
        if (!TransactionSynchronizationManager.hasResource(dataSource)) {
            throw new IllegalStateException("FLOW_EVENT_INBOX_REQUIRES_LOCAL_DATASOURCE_TRANSACTION");
        }
        mapper.reserve(entry);
        FlowEventInboxEntry existing = mapper.selectForUpdate(entry);
        if (existing == null || !entry.payloadHash().equals(existing.payloadHash())) {
            throw new IllegalStateException("FLOW_EVENT_PAYLOAD_CHANGED");
        }
        if ("COMPLETED".equals(existing.status())) {
            return;
        }
        callback.invoke(deserialize(snapshot));
        if (mapper.complete(entry) != 1) {
            throw new IllegalStateException("FLOW_EVENT_INBOX_COMPLETION_FAILED");
        }
    }

    private void requireIdentity(FlowEventContext event) {
        if (!isTerminal(event) || event.getTenantId() == null
                || !event.getTenantId().equals(properties.getTenantId())
                || event.getEventId() == null || !event.getEventId().matches("[A-Za-z0-9_.-]{1,64}")
                || event.getProcessInstanceId() == null || event.getProcessInstanceId().isBlank()
                || event.getProcessDefKey() == null || event.getProcessDefKey().isBlank()) {
            throw new IllegalStateException("FLOW_EVENT_CONSUMER_IDENTITY_MISMATCH");
        }
    }

    private String serialize(FlowEventContext event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FLOW_EVENT_SERIALIZATION_FAILED", exception);
        }
    }

    private FlowEventContext deserialize(String snapshot) {
        try {
            return json.readValue(snapshot, FlowEventContext.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FLOW_EVENT_SERIALIZATION_FAILED", exception);
        }
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Consumption(String eventId, Long tenantId, String consumerId, int callbacks) {
    }
}
