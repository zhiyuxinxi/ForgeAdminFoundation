package com.mdframe.forge.starter.flow.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.domain.FlowStartRecoverySnapshot;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.dto.FlowStartRoundRequest;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/** Read one persisted start round without starting or compensating any process. */
@Service
public class FlowStartRecoveryQuery {
    private final FlowBusinessMapper businesses;
    private final FlowEventOutboxMapper outbox;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public FlowStartRecoveryQuery(FlowBusinessMapper businesses, FlowEventOutboxMapper outbox) {
        this.businesses = businesses;
        this.outbox = outbox;
    }

    /** Reads recorded evidence only; it never starts a process or acknowledges delivery. */
    public FlowStartRecoverySnapshot findRecovery(Long tenantId, Long userId, String businessKey, String requestId) {
        FlowStartOutcome outcome = find(tenantId, userId, businessKey, requestId);
        String eventType = terminalEventType(outcome.processStatus());
        if (outcome.state() != FlowStartOutcome.State.STARTED || eventType == null) {
            return new FlowStartRecoverySnapshot(outcome, null);
        }
        return TenantContextHolder.executeWithTenant(tenantId, () -> {
            var entry = outbox.selectTerminal(tenantId, outcome.processInstanceId(), eventType);
            if (entry == null) {
                throw new BusinessException(503, "FLOW_START_TERMINAL_EVENT_UNAVAILABLE");
            }
            try {
                FlowEventMessage event = json.readValue(entry.payload(), FlowEventMessage.class);
                if (event == null || !Objects.equals(event.getEventId(), entry.eventId())
                        || !Objects.equals(event.getTenantId(), String.valueOf(tenantId))
                        || !Objects.equals(event.getProcessInstanceId(), outcome.processInstanceId())
                        || !Objects.equals(event.getBusinessKey(), businessKey)
                        || !Objects.equals(event.getEventType(), eventType)) {
                    throw new BusinessException(503, "FLOW_START_TERMINAL_EVENT_INVALID");
                }
                return new FlowStartRecoverySnapshot(outcome, event);
            } catch (JsonProcessingException exception) {
                throw new BusinessException(503, "FLOW_START_TERMINAL_EVENT_INVALID", exception);
            }
        });
    }

    private String terminalEventType(String status) {
        return switch (status == null ? "" : status.toLowerCase(Locale.ROOT)) {
            case "approved" -> FlowEventMessage.PROCESS_COMPLETED;
            case "rejected" -> FlowEventMessage.PROCESS_REJECTED;
            case "canceled", "terminated" -> FlowEventMessage.PROCESS_CANCELED;
            default -> null;
        };
    }

    public FlowStartOutcome find(Long tenantId, Long userId, String businessKey, String requestId) {
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(401, "FLOW_START_QUERY_SESSION_REQUIRED");
        }
        if (businessKey == null || businessKey.isBlank() || businessKey.length() > 100) {
            throw new BusinessException(400, "FLOW_START_BUSINESS_KEY_REQUIRED");
        }
        new FlowStartRoundRequest(requestId, null);
        return TenantContextHolder.executeWithTenant(tenantId, () -> findOwned(tenantId, userId, businessKey, requestId));
    }

    private FlowStartOutcome findOwned(Long tenantId, Long userId, String businessKey, String requestId) {
        var business = businesses.selectByStartRequest(tenantId, businessKey, requestId);
        if (business == null) {
            return new FlowStartOutcome(FlowStartOutcome.State.NOT_FOUND, businessKey, requestId, null, null, null);
        }
        if (!String.valueOf(userId).equals(business.getApplyUserId())) {
            throw new BusinessException(403, "FLOW_START_QUERY_FORBIDDEN");
        }
        String instanceId = business.getProcessInstanceId();
        var state = instanceId == null || instanceId.isBlank()
                ? FlowStartOutcome.State.IN_PROGRESS : FlowStartOutcome.State.STARTED;
        return new FlowStartOutcome(state, businessKey, requestId, instanceId, business.getStatus(), business.getRoundNo());
    }
}
