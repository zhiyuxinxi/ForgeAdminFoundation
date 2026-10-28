package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Objects;

/** Reconciles one explicit start using original remote evidence in the caller's local transaction. */
@RequiredArgsConstructor
final class BusinessFlowStartRecovery {
    private final BusinessFlowInstanceLinkMapper links;
    private final BusinessFlowCallbackResults callbacks;

    FlowEventMessage prepare(Long tenantId, String businessKey, String instanceId,
                             BusinessFlowStartDTO request, FlowClient client) {
        var latest = links.selectLatestByBusinessKeyForUpdate(tenantId, businessKey);
        if (latest != null && !Objects.equals(latest.getProcessInstanceId(), instanceId)
                && !Objects.equals(latest.getProcessInstanceId(), request.getPreviousProcessInstanceId())) {
            throw new BusinessException(409, UiMessages.message("flow.start.recovery.round.conflict"));
        }
        try {
            var response = client.getProcessStartRecovery(businessKey, request.getStartRequestId());
            var snapshot = response == null || !response.isSuccess() ? null : response.getData();
            var outcome = snapshot == null ? null : snapshot.outcome();
            if (outcome == null || outcome.state() != FlowStartOutcome.State.STARTED
                    || !Objects.equals(outcome.businessKey(), businessKey)
                    || !Objects.equals(outcome.startRequestId(), request.getStartRequestId())
                    || !Objects.equals(outcome.processInstanceId(), instanceId)) {
                throw unavailable();
            }
            String status = Objects.toString(outcome.processStatus(), "").toLowerCase(Locale.ROOT);
            String expectedEvent = switch (status) {
                case "approved" -> FlowEventMessage.PROCESS_COMPLETED;
                case "rejected" -> FlowEventMessage.PROCESS_REJECTED;
                case "canceled", "terminated" -> FlowEventMessage.PROCESS_CANCELED;
                case "running" -> null;
                default -> throw unavailable();
            };
            FlowEventMessage event = snapshot.terminalEvent();
            if (expectedEvent == null) {
                if (event != null) throw unavailable();
                return null;
            }
            if (event == null || event.getEventId() == null || event.getEventId().isBlank()
                    || !Objects.equals(event.getEventType(), expectedEvent)
                    || !Objects.equals(event.getProcessInstanceId(), instanceId)
                    || !Objects.equals(event.getBusinessKey(), businessKey)
                    || !Objects.equals(event.getTenantId(), String.valueOf(tenantId))) {
                throw unavailable();
            }
            return event;
        } catch (FlowClientException exception) {
            throw new BusinessException(503, UiMessages.message("flow.start.recovery.unavailable"), exception);
        }
    }

    void reconcileExisting(Long tenantId, String businessKey, AiBusinessFlowInstanceLink link,
                           BusinessFlowStartDTO request, FlowClient client) {
        if ("RUNNING".equalsIgnoreCase(link.getFlowStatus())) {
            apply(link, prepare(tenantId, businessKey, link.getProcessInstanceId(), request, client));
        }
    }

    void apply(AiBusinessFlowInstanceLink link, FlowEventMessage event) {
        if (event == null) return;
        BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
        callback.setTenantId(link.getTenantId());
        callback.setProcessInstanceId(event.getProcessInstanceId());
        callback.setBusinessKey(event.getBusinessKey());
        callback.setFlowStatus(event.getEventType());
        callback.setNodeKey(event.getTaskDefKey());
        callback.setNodeName(event.getTaskName());
        if (event.getAssigneeId() != null && !event.getAssigneeId().isBlank()) {
            try {
                callback.setOperatorId(Long.valueOf(event.getAssigneeId()));
            } catch (NumberFormatException exception) {
                throw new BusinessException(503, UiMessages.message("flow.start.recovery.unavailable"), exception);
            }
        }
        callback.setVariables(event.getVariables());
        callbacks.handle(link, callback);
    }

    private BusinessException unavailable() {
        return new BusinessException(503, UiMessages.message("flow.start.recovery.unavailable"));
    }
}
