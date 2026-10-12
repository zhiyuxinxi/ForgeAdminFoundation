package com.mdframe.forge.starter.flow.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import org.springframework.lang.Nullable;

/** Reads optional stored schemas and instance snapshots with the existing fallback contract. */
@Slf4j
@Component
public class FlowFormSnapshots {
    private static final String FORM_TYPE_BUSINESS = "business";
    private final FlowFormService flowFormService;
    private final FlowFormInstanceMapper flowFormInstanceMapper;

    public FlowFormSnapshots(@Nullable FlowFormService flowFormService,
                             @Nullable FlowFormInstanceMapper flowFormInstanceMapper) {
        this.flowFormService = flowFormService;
        this.flowFormInstanceMapper = flowFormInstanceMapper;
    }

    public void hydrateFormInstanceSnapshotIfNecessary(TaskFormInfo formInfo, String processInstanceId) {
        if (formInfo == null || FORM_TYPE_BUSINESS.equalsIgnoreCase(formInfo.getFormType())) {
            return;
        }
        if (!"dynamic".equalsIgnoreCase(formInfo.getFormType())
                && isBlank(formInfo.getFormKey())
                && isBlank(formInfo.getFormJson())) {
            return;
        }
        hydrateFormInstanceSnapshot(formInfo, processInstanceId);
    }

    private void hydrateFormInstanceSnapshot(TaskFormInfo formInfo, String processInstanceId) {
        if (flowFormInstanceMapper == null || processInstanceId == null || processInstanceId.isEmpty()) {
            return;
        }
        try {
            FlowFormInstance instance = flowFormInstanceMapper.selectByProcessInstanceId(processInstanceId);
            if (instance == null) {
                return;
            }
            formInfo.setFormInstanceId(instance.getId());
            formInfo.setSchemaSnapshot(instance.getSchemaSnapshot());
            formInfo.setFormData(instance.getFormData());
            formInfo.setDataMode(instance.getDataMode());
            formInfo.setObjectCode(instance.getObjectCode());
            formInfo.setRecordId(instance.getRecordId());
            if (formInfo.getFormJson() == null || formInfo.getFormJson().isEmpty()) {
                formInfo.setFormJson(instance.getSchemaSnapshot());
            }
            if (formInfo.getFormKey() == null || formInfo.getFormKey().isEmpty()) {
                formInfo.setFormKey(instance.getFormKey());
            }
        } catch (Exception e) {
            log.warn("加载流程表单实例快照失败: processInstanceId={}", processInstanceId, e);
        }
    }

    public String resolveFormJson(String formKey, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formKey == null || formKey.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            return flowFormService.getFormSchema(formKey);
        } catch (Exception e) {
            log.warn("根据 formKey 获取动态表单失败：formKey={}", formKey, e);
            return inlineFormJson;
        }
    }

    public String resolveModelFormJson(String formId, String inlineFormJson) {
        if (inlineFormJson != null && !inlineFormJson.isEmpty()) {
            return inlineFormJson;
        }
        if (formId == null || formId.isEmpty() || flowFormService == null) {
            return inlineFormJson;
        }
        try {
            FlowForm form = flowFormService.getById(Long.valueOf(formId));
            return form != null ? form.getFormSchema() : inlineFormJson;
        } catch (NumberFormatException e) {
            return resolveFormJson(formId, inlineFormJson);
        } catch (Exception e) {
            log.warn("根据 formId 获取模型动态表单失败：formId={}", formId, e);
            return inlineFormJson;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
