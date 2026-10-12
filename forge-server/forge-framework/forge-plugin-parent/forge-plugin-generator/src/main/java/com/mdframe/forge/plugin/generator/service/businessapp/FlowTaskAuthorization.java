package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Supplier;

/** Authorizes a pending task and binds its canonical identity before business data access. */
@Slf4j
final class FlowTaskAuthorization {
    void validate(BusinessTaskFormContextQueryDTO query, boolean writeRequired,
                  FlowClient flowClient, Supplier<Long> userId) {
        String taskId = StringUtils.trimToNull(query.getTaskId());
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }
        query.setTaskId(taskId);
        if (flowClient == null) {
            throw new BusinessException("流程服务未配置，无法校验任务身份");
        }
        Long currentUserId = userId.get();
        if (currentUserId == null) {
            throw new BusinessException("当前登录用户不能为空");
        }

        Map<String, Object> task = loadFlowTaskDetail(flowClient, taskId);
        assertPendingTask(task);
        assertTaskActor(task, currentUserId, writeRequired);
        reconcileTaskIdentity(query, task);
    }

    private void assertPendingTask(Map<String, Object> task) {
        Integer status = readIntegerValue(task.get("status"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("当前任务已处理，不能访问待办业务表单");
        }

    }

    private void assertTaskActor(Map<String, Object> task, Long currentUserId, boolean writeRequired) {
        String currentUser = String.valueOf(currentUserId);
        String assignee = StringUtils.trimToNull(textValue(task.get("assignee")));
        boolean claimedByCurrentUser = StringUtils.equals(assignee, currentUser);
        boolean unclaimedCandidate = StringUtils.isBlank(assignee)
                && (csvContains(task.get("candidateUsers"), currentUser)
                || StringUtils.isNotBlank(textValue(task.get("candidateGroups"))));

        if (!claimedByCurrentUser && !unclaimedCandidate) {
            throw new BusinessException("无权访问当前任务业务表单");
        }
        if (writeRequired && !claimedByCurrentUser) {
            throw new BusinessException("请先签收任务后再保存业务字段");
        }

    }

    private void reconcileTaskIdentity(BusinessTaskFormContextQueryDTO query, Map<String, Object> task) {
        assertTaskFieldMatches(query.getProcessInstanceId(), task.get("processInstanceId"), "流程实例");
        assertBusinessKeyMatches(query.getBusinessKey(), task.get("businessKey"));
        assertTaskFieldMatches(query.getTaskDefKey(), task.get("taskDefKey"), "任务节点");
        assertProcessDefinitionMatches(query.getProcessDefKey(), task.get("processDefKey"));

        if (StringUtils.isBlank(query.getProcessInstanceId())) {
            query.setProcessInstanceId(StringUtils.trimToNull(textValue(task.get("processInstanceId"))));
        }
        if (StringUtils.isBlank(query.getBusinessKey())) {
            query.setBusinessKey(StringUtils.trimToNull(textValue(task.get("businessKey"))));
        }
        if (StringUtils.isBlank(query.getTaskDefKey())) {
            query.setTaskDefKey(StringUtils.trimToNull(textValue(task.get("taskDefKey"))));
        }
        if (StringUtils.isBlank(query.getProcessDefKey())) {
            query.setProcessDefKey(StringUtils.trimToNull(textValue(task.get("processDefKey"))));
        }
    }

    private Map<String, Object> loadFlowTaskDetail(FlowClient flowClient, String taskId) {
        try {
            FlowResult<Map<String, Object>> result = flowClient.getTaskDetail(taskId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                String message = result == null ? null : result.getMsg();
                throw new BusinessException(StringUtils.defaultIfBlank(message, "任务不存在或无权访问"));
            }
            return result.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("业务待办表单任务身份校验失败: taskId={}, error={}", taskId, e.getMessage());
            throw new BusinessException("任务身份校验失败，请稍后重试");
        }
    }

    private void assertTaskFieldMatches(String requestedValue, Object actualValue, String label) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested != null && actual != null && !StringUtils.equals(requested, actual)) {
            throw new BusinessException(label + UiMessages.translate("与当前任务不匹配"));
        }
    }

    private void assertBusinessKeyMatches(String requestedValue, Object actualValue) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested == null || actual == null || StringUtils.equals(requested, actual)) {
            return;
        }
        if (isSameDocumentBusinessKey(requested, actual)) {
            return;
        }
        throw new BusinessException("业务Key与当前任务不匹配");
    }

    private boolean isSameDocumentBusinessKey(String left, String right) {
        String leftKey = normalizeDocumentBusinessKey(left);
        String rightKey = normalizeDocumentBusinessKey(right);
        return StringUtils.isNotBlank(leftKey) && StringUtils.equals(leftKey, rightKey);
    }

    private String normalizeDocumentBusinessKey(String businessKey) {
        String text = StringUtils.trimToNull(businessKey);
        if (text == null) {
            return null;
        }
        int retryIndex = text.indexOf(":R");
        if (retryIndex <= 0) {
            return text;
        }
        String retryNo = text.substring(retryIndex + 2);
        if (StringUtils.isNumeric(retryNo)) {
            return text.substring(0, retryIndex);
        }
        return text;
    }

    private void assertProcessDefinitionMatches(String requestedValue, Object actualValue) {
        String requested = StringUtils.trimToNull(requestedValue);
        String actual = StringUtils.trimToNull(textValue(actualValue));
        if (requested == null || actual == null || StringUtils.equals(requested, actual)) {
            return;
        }
        String requestedKey = extractProcessDefinitionKey(requested);
        String actualKey = extractProcessDefinitionKey(actual);
        if (StringUtils.isNotBlank(requestedKey) && StringUtils.equals(requestedKey, actualKey)) {
            return;
        }
        if (isUuidLike(requested) || isUuidLike(actual)) {
            return;
        }
        log.debug("忽略流程定义标识表示差异: requested={}, actual={}", requested, actual);
    }

    private String extractProcessDefinitionKey(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null) {
            return null;
        }
        int separator = text.indexOf(':');
        return separator > 0 ? text.substring(0, separator) : text;
    }

    private boolean isUuidLike(String value) {
        String text = StringUtils.trimToNull(value);
        if (text == null || text.length() != 36) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                if (ch != '-') {
                    return false;
                }
                continue;
            }
            boolean hex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private boolean csvContains(Object csvValue, String expected) {
        String csv = StringUtils.trimToNull(textValue(csvValue));
        if (csv == null || StringUtils.isBlank(expected)) {
            return false;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            if (StringUtils.equals(StringUtils.trimToEmpty(part), expected)) {
                return true;
            }
        }
        return false;
    }

    private Integer readIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

}
