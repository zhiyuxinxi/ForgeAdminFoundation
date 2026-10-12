package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessApprovalResultEvent;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.readJsonObject;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.resolveUsername;

/** Applies a flow callback and dispatches its configured action and result events in the caller's transaction. */
@Slf4j
@RequiredArgsConstructor
final class BusinessFlowCallbackResults {
    private final BusinessFlowRuntimeMetadata runtimeMetadata;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessFlowStatusUpdates statusUpdates;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<BusinessActionExecutionService> actionExecutionServiceProvider;

    void handle(AiBusinessFlowInstanceLink link, BusinessFlowCallbackDTO dto) {
        if (isEndedLink(link)) {
            log.info("流程回调已处理，跳过重复回调: processInstanceId={}, result={}",
                    link.getProcessInstanceId(), link.getResult());
            publishBusinessProcessApprovalResult(link, link.getResult());
            return;
        }
        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByObjectCode(
                link.getTenantId(), link.getObjectCode());
        AiCrudConfig runtimeConfig = documentConfig == null
                ? runtimeMetadata.resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode())
                : null;
        AiBusinessBinding binding = runtimeMetadata.selectMainFlowBindingForConfig(link.getTenantId(), link.getObjectCode());
        JSONObject bindingConfig = binding == null ? new JSONObject() : readJsonObject(binding.getBindingConfig());
        runtimeMetadata.ensureBusinessBinding(bindingConfig, link.getTenantId(), link.getObjectCode());
        String configKey = documentConfig != null ? documentConfig.getConfigKey() : runtimeConfig == null ? null : runtimeConfig.getConfigKey();
        Map<String, Object> previousData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        String result = normalizeCallbackResult(dto);
        Map<String, Object> startVariables = readJsonObject(link.getVariablesSnapshot());
        AiCrudConfig statusRuntimeConfig = runtimeConfig != null
                ? runtimeConfig : runtimeMetadata.resolvePublishedRuntimeConfig(link.getTenantId(), link.getObjectCode());
        if (StringUtils.isBlank(statusUpdates.managedField(startVariables))) {
            statusUpdates.update(documentConfig, runtimeConfig, bindingConfig, link.getRecordId(), result);
        }
        statusUpdates.syncManaged(statusRuntimeConfig, link.getRecordId(), startVariables, result);

        link.setFlowStatus(result);
        link.setResult(result);
        link.setEndTime(LocalDateTime.now());
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            link.setVariablesSnapshot(JSON.toJSONString(dto.getVariables()));
        }
        flowInstanceLinkMapper.updateById(link);

        Map<String, Object> currentData = StringUtils.isBlank(configKey)
                ? null
                : dynamicCrudService.selectById(configKey, link.getRecordId());
        executeFlowCallbackAction(link, bindingConfig, result, dto);
        if (StringUtils.isNotBlank(configKey)) {
            currentData = dynamicCrudService.selectById(configKey, link.getRecordId());
        }
        if (documentConfig != null) {
            publishFlowResultEvent(link, documentConfig.getSuiteCode(), documentConfig.getConfigKey(),
                    result, previousData, currentData, dto);
        } else if (runtimeConfig != null) {
            publishFlowResultEvent(link, null, runtimeConfig.getConfigKey(), result, previousData, currentData, dto);
        }
        publishBusinessProcessApprovalResult(link, result);
    }

    private void publishBusinessProcessApprovalResult(AiBusinessFlowInstanceLink link, String result) {
        if (link == null || link.getTenantId() == null
                || StringUtils.isAnyBlank(link.getProcessInstanceId(), result)) {
            return;
        }
        String normalized = result.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED", "CANCELED", "FAILED").contains(normalized)) {
            return;
        }
        applicationEventPublisher.publishEvent(new BusinessProcessApprovalResultEvent(
                link.getTenantId(), link.getProcessInstanceId(), normalized));
    }

    AiBusinessFlowInstanceLink findCallbackLink(Long tenantId, BusinessFlowCallbackDTO dto) {
        if (StringUtils.isNotBlank(dto.getProcessInstanceId())) {
            AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectByProcessInstanceId(
                    tenantId, dto.getProcessInstanceId());
            if (link != null) {
                return link;
            }
        }
        if (StringUtils.isNotBlank(dto.getBusinessKey())) {
            return flowInstanceLinkMapper.selectLatestByBusinessKey(tenantId, dto.getBusinessKey());
        }
        return null;
    }

    private void executeFlowCallbackAction(AiBusinessFlowInstanceLink link,
                                           JSONObject bindingConfig,
                                           String result,
                                           BusinessFlowCallbackDTO dto) {
        String actionCode = resolveFlowCallbackActionCode(bindingConfig, result);
        if (StringUtils.isBlank(actionCode)) {
            return;
        }
        BusinessActionExecutionService actionExecutionService = actionExecutionServiceProvider.getIfAvailable();
        if (actionExecutionService == null) {
            throw new BusinessException("动作执行服务未启用，无法执行流程回调动作");
        }
        BusinessActionExecuteDTO request = new BusinessActionExecuteDTO();
        request.setObjectCode(link.getObjectCode());
        request.setRecordId(link.getRecordId() == null ? null : String.valueOf(link.getRecordId()));
        request.setActionCode(actionCode);
        request.setIdempotencyKey(buildFlowCallbackActionIdempotencyKey(link, result, actionCode));
        request.setContext(buildFlowCallbackActionContext(link, result, dto));
        try {
            actionExecutionService.execute(request);
        } catch (BusinessException e) {
            log.warn("[低代码流程回调] 动作执行失败: objectCode={}, recordId={}, result={}, actionCode={}, error={}",
                    link.getObjectCode(), link.getRecordId(), result, actionCode, e.getMessage());
            throw new BusinessException(UiMessages.translate("流程回调动作执行失败: ") + e.getMessage());
        }
    }

    private String resolveFlowCallbackActionCode(JSONObject bindingConfig, String result) {
        if (bindingConfig == null || StringUtils.isBlank(result)) {
            return null;
        }
        JSONObject options = bindingConfig.getJSONObject("options");
        JSONObject callbackActions = options == null ? null : options.getJSONObject("callbackActions");
        if (callbackActions == null || callbackActions.isEmpty()) {
            callbackActions = bindingConfig.getJSONObject("callbackActions");
        }
        if (callbackActions == null || callbackActions.isEmpty()) {
            return null;
        }
        String normalizedResult = StringUtils.defaultString(result).toUpperCase();
        return StringUtils.firstNonBlank(
                callbackActions.getString(normalizedResult),
                callbackActions.getString(normalizedResult.toLowerCase()),
                switch (normalizedResult) {
                    case "APPROVED" -> callbackActions.getString("approvedActionCode");
                    case "REJECTED" -> callbackActions.getString("rejectedActionCode");
                    case "CANCELED" -> callbackActions.getString("canceledActionCode");
                    default -> null;
                }
        );
    }

    private String buildFlowCallbackActionIdempotencyKey(AiBusinessFlowInstanceLink link, String result, String actionCode) {
        return "flowCallback:"
                + StringUtils.defaultString(link.getProcessInstanceId(), link.getBusinessKey())
                + ":" + StringUtils.defaultString(result)
                + ":" + StringUtils.defaultString(actionCode);
    }

    private Map<String, Object> buildFlowCallbackActionContext(AiBusinessFlowInstanceLink link,
                                                               String result,
                                                               BusinessFlowCallbackDTO dto) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("source", "FLOW_CALLBACK");
        context.put("flowResult", result);
        context.put("processInstanceId", link.getProcessInstanceId());
        context.put("businessKey", link.getBusinessKey());
        context.put("flowModelKey", link.getFlowModelKey());
        context.put("operatorId", dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId());
        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            context.put("variables", dto.getVariables());
        }
        return context;
    }

    private String normalizeCallbackResult(BusinessFlowCallbackDTO dto) {
        String value = StringUtils.firstNonBlank(dto.getResult(), dto.getFlowStatus());
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("流程回调缺少结果状态");
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("COMPLETED") || normalized.contains("APPROVED") || "APPROVE".equals(normalized)) {
            return "APPROVED";
        }
        if (normalized.contains("REJECT")) {
            return "REJECTED";
        }
        if (normalized.contains("CANCEL") || normalized.contains("WITHDRAW") || normalized.contains("TERMINAT")) {
            return "CANCELED";
        }
        throw new BusinessException(UiMessages.translate("不支持的流程回调结果: ") + value);
    }

    private boolean isEndedLink(AiBusinessFlowInstanceLink link) {
        return link.getEndTime() != null
                || "APPROVED".equalsIgnoreCase(link.getResult())
                || "REJECTED".equalsIgnoreCase(link.getResult())
                || "CANCELED".equalsIgnoreCase(link.getResult());
    }

    private void publishFlowResultEvent(AiBusinessFlowInstanceLink link,
                                        String suiteCode,
                                        String configKey,
                                        String result,
                                        Map<String, Object> previousData,
                                        Map<String, Object> currentData,
                                        BusinessFlowCallbackDTO dto) {
        String eventType = switch (result) {
            case "APPROVED" -> BusinessEvent.FLOW_APPROVED;
            case "REJECTED" -> BusinessEvent.FLOW_REJECTED;
            case "CANCELED" -> BusinessEvent.FLOW_CANCELED;
            default -> null;
        };
        if (eventType == null) {
            return;
        }
        BusinessEvent event = BusinessEvent.builder()
                .eventType(eventType)
                .suiteCode(suiteCode)
                .objectCode(link.getObjectCode())
                .configKey(configKey)
                .recordId(String.valueOf(link.getRecordId()))
                .recordData(currentData)
                .previousData(previousData)
                .operatorId(dto.getOperatorId() != null ? dto.getOperatorId() : link.getStartUserId())
                .operatorName(resolveUsername())
                .tenantId(link.getTenantId())
                .build();
        applicationEventPublisher.publishEvent(event);
    }
}
