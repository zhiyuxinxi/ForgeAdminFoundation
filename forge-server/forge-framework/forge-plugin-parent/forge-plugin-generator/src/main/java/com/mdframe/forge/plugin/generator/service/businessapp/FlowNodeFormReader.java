package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads task form metadata, falling back to and merging process metadata without changing failure semantics. */
@Slf4j
@RequiredArgsConstructor
final class FlowNodeFormReader {
    private final FlowClient flowClient;
    private final FlowBindingConfiguration bindingConfiguration = new FlowBindingConfiguration();

    Map<String, Object> load(String runtimeBusinessKey, JSONObject bindingConfig,
                             BusinessTaskFormContextQueryDTO query) {
        Map<String, Object> taskFormInfo = loadTaskFormInfo(query.getTaskId());
        if (isCompleteFlowNodeFormInfo(taskFormInfo)) {
            return taskFormInfo;
        }
        Map<String, Object> processFormInfo = loadProcessFormInfo(runtimeBusinessKey, bindingConfig, query);
        if (taskFormInfo.isEmpty()) {
            return processFormInfo;
        }
        if (processFormInfo.isEmpty()) {
            return taskFormInfo;
        }
        Map<String, Object> merged = new LinkedHashMap<>(processFormInfo);
        taskFormInfo.forEach((key, value) -> {
            if (hasTextValue(value) || value instanceof Map<?, ?> || value instanceof List<?>) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    private boolean isCompleteFlowNodeFormInfo(Map<String, Object> formInfo) {
        if (formInfo == null || formInfo.isEmpty()) {
            return false;
        }
        Object formType = formInfo.get("formType");
        if (!hasTextValue(formType)) {
            return false;
        }
        if ("none".equalsIgnoreCase(String.valueOf(formType))) {
            return true;
        }
        return hasTextValue(formInfo.get("formKey"))
                || hasTextValue(formInfo.get("formUrl"))
                || hasTextValue(formInfo.get("formJson"))
                || hasTextValue(formInfo.get("formFieldPermissions"));
    }

    private Map<String, Object> loadTaskFormInfo(String taskId) {
        if (flowClient == null || StringUtils.isBlank(taskId)) {
            return Map.of();
        }
        try {
            FlowResult<Map<String, Object>> result = flowClient.getTaskFormInfo(taskId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Map.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("读取流程节点表单配置失败: taskId={}, error={}", taskId, e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> loadProcessFormInfo(String runtimeBusinessKey, JSONObject bindingConfig,
                                                   BusinessTaskFormContextQueryDTO query) {
        if (flowClient == null || query == null) {
            return Map.of();
        }
        String processInstanceId = StringUtils.trimToNull(query.getProcessInstanceId());
        String businessKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getBusinessKey()),
                StringUtils.trimToNull(runtimeBusinessKey));
        String processDefKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(query.getProcessDefKey()),
                bindingConfig == null ? null : bindingConfiguration.resolveFlowModelKey(bindingConfig));
        String taskId = StringUtils.trimToNull(query.getTaskId());
        String taskDefKey = StringUtils.trimToNull(query.getTaskDefKey());
        if (StringUtils.isBlank(processInstanceId)
                && StringUtils.isBlank(businessKey)
                && StringUtils.isBlank(processDefKey)
                && StringUtils.isBlank(taskId)
                && StringUtils.isBlank(taskDefKey)) {
            return Map.of();
        }
        try {
            FlowResult<Map<String, Object>> result = flowClient.getProcessFormInfo(
                    processInstanceId,
                    businessKey,
                    processDefKey,
                    taskId,
                    taskDefKey);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return Map.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("读取流程实例表单配置失败: processInstanceId={}, businessKey={}, processDefKey={}, taskDefKey={}, error={}",
                    processInstanceId, businessKey, processDefKey, taskDefKey, e.getMessage());
            return Map.of();
        }
    }

    private boolean hasTextValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence sequence) {
            return StringUtils.isNotBlank(sequence.toString());
        }
        return true;
    }
}
