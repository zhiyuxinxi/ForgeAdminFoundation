package com.mdframe.forge.starter.flow.service.support;

import org.springframework.stereotype.Component;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import org.flowable.bpmn.model.FlowNode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Interprets reusable BPMN/model form metadata; never embeds customer form implementation. */
@Component
public class FlowFormConfiguration {
    private static final String FORM_TYPE_BUSINESS = "business";
    private final FlowFormMetadata metadata;
    private final FlowFormSnapshots snapshots;

    public FlowFormConfiguration(FlowFormMetadata metadata, FlowFormSnapshots snapshots) {
        this.metadata = metadata;
        this.snapshots = snapshots;
    }

    public FlowNode resolveFormFlowNode(String processDefinitionId, String taskDefKey) {
        return metadata.resolveFormFlowNode(processDefinitionId, taskDefKey);
    }

    public void applyFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel, FlowNode flowNode) {
        FlowFormMetadata.NodeFormConfig nodeForm = metadata.readNodeFormConfig(flowNode);
        formInfo.setFormFieldPermissions(nodeForm.formFieldPermissions);

        if (isBusinessNodeForm(flowModel, nodeForm.formMode)) {
            applyBusinessNodeFormConfiguration(formInfo, flowModel, nodeForm);
        } else if (!isBlank(nodeForm.formUrl)) {
            formInfo.setFormType("external");
            formInfo.setFormUrl(nodeForm.formUrl);
            formInfo.setFormTarget(!isBlank(nodeForm.formTarget) ? nodeForm.formTarget : "modal");
            applyNodeFormReference(formInfo, nodeForm, Map.of());
        } else if (!isBlank(nodeForm.formKey) || !isBlank(nodeForm.formJson)) {
            formInfo.setFormType("dynamic");
            formInfo.setFormKey(nodeForm.formKey);
            formInfo.setFormJson(snapshots.resolveFormJson(nodeForm.formKey, nodeForm.formJson));
            applyNodeFormReference(formInfo, nodeForm, Map.of());
        } else if (flowModel != null) {
            applyModelFormConfiguration(formInfo, flowModel);
        } else if (!isBlank(formInfo.getFormJson()) || !isBlank(formInfo.getFormKey())) {
            formInfo.setFormType("dynamic");
            formInfo.setFormJson(snapshots.resolveFormJson(formInfo.getFormKey(), formInfo.getFormJson()));
        } else {
            formInfo.setFormType("none");
        }
    }

    private void applyModelFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel) {
        if (flowModel == null) {
            formInfo.setFormType("none");
            return;
        }
        String formType = flowModel.getFormType();
        formInfo.setFormType(formType);
        if ("dynamic".equals(formType)) {
            formInfo.setFormKey(flowModel.getFormId());
            formInfo.setFormJson(snapshots.resolveModelFormJson(flowModel.getFormId(), flowModel.getFormJson()));
            return;
        }
        if ("external".equals(formType)) {
            formInfo.setFormUrl(flowModel.getFormId());
            formInfo.setFormTarget("modal");
            return;
        }
        if (FORM_TYPE_BUSINESS.equals(formType)) {
            Map<String, Object> formRef = metadata.readBusinessGlobalFormRef(flowModel.getFormJson());
            formInfo.setFormType(FORM_TYPE_BUSINESS);
            formInfo.setFormKey(firstNonBlank(textValue(formRef.get("formKey")), flowModel.getFormId()));
            formInfo.setFormMode(firstNonBlank(
                    textValue(formRef.get("formMode")),
                    textValue(formRef.get("type"))));
            formInfo.setFormName(textValue(formRef.get("formName")));
            formInfo.setProviderKey(textValue(formRef.get("providerKey")));
            formInfo.setFormUrl(textValue(formRef.get("formUrl")));
            formInfo.setViewKey(firstNonBlank(textValue(formRef.get("viewKey")), "default"));
            formInfo.setFormRef(new LinkedHashMap<>(formRef));
            formInfo.setFormTarget("modal");
            formInfo.setFormJson(flowModel.getFormJson());
        }
    }

    private void applyBusinessNodeFormConfiguration(TaskFormInfo formInfo, FlowModel flowModel,
                                                    FlowFormMetadata.NodeFormConfig nodeForm) {
        formInfo.setFormType(FORM_TYPE_BUSINESS);
        Map<String, Object> formRef = flowModel == null ? Map.of() : metadata.readBusinessGlobalFormRef(flowModel.getFormJson());
        Map<String, Object> mergedFormRef = mergeFormRef(formRef, nodeForm.formRef);
        formInfo.setFormKey(firstNonBlank(
                nodeForm.formKey,
                textValue(mergedFormRef.get("formKey")),
                flowModel == null ? null : flowModel.getFormId()));
        formInfo.setFormMode(firstNonBlank(
                nodeForm.formMode,
                textValue(mergedFormRef.get("formMode")),
                textValue(mergedFormRef.get("type"))));
        formInfo.setFormName(firstNonBlank(nodeForm.formName, textValue(mergedFormRef.get("formName"))));
        formInfo.setProviderKey(firstNonBlank(nodeForm.providerKey, textValue(mergedFormRef.get("providerKey"))));
        formInfo.setFormUrl(firstNonBlank(nodeForm.formUrl, textValue(mergedFormRef.get("formUrl"))));
        formInfo.setViewKey(firstNonBlank(nodeForm.viewKey, textValue(mergedFormRef.get("viewKey")), "default"));
        putIfPresent(mergedFormRef, "formKey", formInfo.getFormKey());
        putIfPresent(mergedFormRef, "formMode", formInfo.getFormMode());
        putIfPresent(mergedFormRef, "type", formInfo.getFormMode());
        putIfPresent(mergedFormRef, "formName", formInfo.getFormName());
        putIfPresent(mergedFormRef, "providerKey", formInfo.getProviderKey());
        putIfPresent(mergedFormRef, "formUrl", formInfo.getFormUrl());
        putIfPresent(mergedFormRef, "viewKey", formInfo.getViewKey());
        formInfo.setFormRef(mergedFormRef);
        formInfo.setFormTarget("modal");
        formInfo.setFormJson(flowModel == null ? null : flowModel.getFormJson());
    }

    private boolean isBusinessNodeForm(FlowModel flowModel, String formMode) {
        String normalizedMode = normalizeFormMode(formMode);
        if ("BUSINESS_CODE_FORM".equals(normalizedMode) || "BUSINESS_OBJECT_FORM".equals(normalizedMode)) {
            return true;
        }
        if (flowModel == null || !FORM_TYPE_BUSINESS.equalsIgnoreCase(flowModel.getFormType())) {
            return false;
        }
        return true;
    }

    private String normalizeFormMode(String value) {
        String mode = textValue(value);
        return mode == null ? null : mode.toUpperCase(Locale.ROOT);
    }

    private void applyNodeFormReference(TaskFormInfo formInfo, FlowFormMetadata.NodeFormConfig nodeForm, Map<String, Object> fallbackRef) {
        Map<String, Object> mergedFormRef = mergeFormRef(fallbackRef, nodeForm.formRef);
        formInfo.setFormMode(firstNonBlank(nodeForm.formMode, textValue(mergedFormRef.get("formMode")), textValue(mergedFormRef.get("type"))));
        formInfo.setFormName(firstNonBlank(nodeForm.formName, textValue(mergedFormRef.get("formName"))));
        formInfo.setProviderKey(firstNonBlank(nodeForm.providerKey, textValue(mergedFormRef.get("providerKey"))));
        formInfo.setViewKey(firstNonBlank(nodeForm.viewKey, textValue(mergedFormRef.get("viewKey")), "default"));
        if (!mergedFormRef.isEmpty()) {
            putIfPresent(mergedFormRef, "formKey", formInfo.getFormKey());
            putIfPresent(mergedFormRef, "formMode", formInfo.getFormMode());
            putIfPresent(mergedFormRef, "type", formInfo.getFormMode());
            putIfPresent(mergedFormRef, "formName", formInfo.getFormName());
            putIfPresent(mergedFormRef, "providerKey", formInfo.getProviderKey());
            putIfPresent(mergedFormRef, "formUrl", formInfo.getFormUrl());
            putIfPresent(mergedFormRef, "viewKey", formInfo.getViewKey());
            formInfo.setFormRef(mergedFormRef);
        }
    }

    private Map<String, Object> mergeFormRef(Map<String, Object> base, Map<String, Object> overrides) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (overrides != null) {
            overrides.forEach((key, value) -> {
                if (value != null && !isBlank(String.valueOf(value))) {
                    merged.put(key, value);
                }
            });
        }
        return merged;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || value == null || isBlank(String.valueOf(value))) {
            return;
        }
        target.put(key, value);
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
