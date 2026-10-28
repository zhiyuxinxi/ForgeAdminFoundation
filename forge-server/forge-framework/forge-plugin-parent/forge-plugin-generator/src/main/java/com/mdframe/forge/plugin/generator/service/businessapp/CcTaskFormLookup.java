package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/** Reads only form identity returned after the flow service verifies the current recipient. */
final class CcTaskFormLookup {
    private CcTaskFormLookup() {
    }

    static VerifiedForm read(FlowClient client, String ccId) {
        String id = StringUtils.trimToNull(ccId);
        if (id == null) {
            throw new BusinessException(UiMessages.translate("抄送记录ID不能为空"));
        }
        if (client == null) {
            throw new BusinessException(UiMessages.translate("流程服务未配置，无法校验抄送收件身份"));
        }
        FlowResult<Map<String, Object>> result = client.getCcFormInfo(id);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            String message = result == null ? null : result.getMsg();
            throw new BusinessException(StringUtils.defaultIfBlank(message,
                    UiMessages.translate("抄送记录不存在或无权访问")));
        }
        JSONObject formInfo = new JSONObject(result.getData());
        JSONObject variables = variables(formInfo.get("variables"));
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(StringUtils.trimToNull(formInfo.getString("taskId")));
        query.setBusinessKey(StringUtils.trimToNull(formInfo.getString("businessKey")));
        query.setProcessInstanceId(StringUtils.trimToNull(formInfo.getString("processInstanceId")));
        query.setProcessDefKey(StringUtils.trimToNull(formInfo.getString("processDefKey")));
        query.setTaskDefKey(StringUtils.trimToNull(formInfo.getString("taskDefKey")));
        query.setObjectCode(StringUtils.firstNonBlank(
                StringUtils.trimToNull(formInfo.getString("objectCode")),
                StringUtils.trimToNull(variables.getString("objectCode"))));
        query.setRecordId(firstLong(formInfo.get("recordId"), variables.get("recordId")));
        query.setFormKey(StringUtils.firstNonBlank(
                StringUtils.trimToNull(formInfo.getString("formKey")),
                StringUtils.trimToNull(variables.getString("formKey"))));
        return new VerifiedForm(query, result.getData());
    }

    private static JSONObject variables(Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                JSONObject parsed = StringUtils.isBlank(text) ? null : JSON.parseObject(text);
                return parsed == null ? new JSONObject() : parsed;
            } catch (RuntimeException exception) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private static Long firstLong(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            String text = StringUtils.trimToNull(String.valueOf(value));
            if (text != null) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException exception) {
                    continue;
                }
            }
        }
        return null;
    }

    record VerifiedForm(BusinessTaskFormContextQueryDTO query, Map<String, Object> data) {
    }
}
