package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.businessapp.FormAssetValues.*;

/** Per-call node form selection and projection; holds metadata values, never services or callbacks. */
final class FlowNodeFormResolution {
    private final NodeFormConfiguration configuration = new NodeFormConfiguration();
    private final String configKey;
    private final Map<String, Object> formInfo;
    private final String taskDefKey;
    private final String configuredFormKey;
    private final JSONObject runtimeFormRef;
    private final String runtimeFormKey;
    private String formKey;
    private List<Map<String, Object>> permissions;
    private JSONObject flowFormRef;

    FlowNodeFormResolution(String configKey, Map<String, Object> formInfo, BusinessTaskFormContextQueryDTO query) {
        this.configKey = configKey;
        this.formInfo = formInfo;
        this.taskDefKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("taskDefKey"))),
                StringUtils.trimToNull(query.getTaskDefKey()));
        this.configuredFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formKey"))),
                StringUtils.trimToNull(query.getFormKey()));
        this.runtimeFormRef = resolveRuntimeBusinessFormRef(formInfo);
        this.runtimeFormKey = StringUtils.trimToNull(runtimeFormRef.getString("formKey"));
    }

    String runtimeFormKey() { return runtimeFormKey; }

    String formKey() { return formKey; }

    void selectRuntimeAsset(JSONObject runtimeAsset) {
        boolean useRuntimePageForm = StringUtils.isNotBlank(runtimeFormKey) && !runtimeAsset.isEmpty();
        formKey = useRuntimePageForm ? runtimeFormKey : configuredFormKey;
        permissions = configuration.normalizeFieldPermissions(formInfo.get("formFieldPermissions"));
        flowFormRef = readNestedObject(formInfo.get("formRef"));
        if (useRuntimePageForm) {
            JSONObject effectiveRuntimeRef = new JSONObject();
            effectiveRuntimeRef.putAll(runtimeAsset);
            effectiveRuntimeRef.putAll(runtimeFormRef);
            flowFormRef = effectiveRuntimeRef;
            // A reusable Flowable model can still carry permissions for a different
            // page. When the outer business-process node selected another concrete
            // application page, rebuild permissions from that page's field catalog.
            if (!StringUtils.equals(runtimeFormKey, configuredFormKey)) {
                permissions = List.of();
            }
        }
    }

    JSONObject resolve(JSONObject asset) {
        if (asset.isEmpty() && StringUtils.isBlank(formKey) && permissions.isEmpty()) {
            if (StringUtils.isNotBlank(configKey)) {
                JSONObject defaultNodeForm = new JSONObject();
                FlowBindingSchema.putText(defaultNodeForm, "taskDefKey", taskDefKey);
                FlowBindingSchema.putText(defaultNodeForm, "taskName", textValue(formInfo.get("taskName")));
                defaultNodeForm.put("formMode", "BUSINESS_OBJECT_FORM");
                defaultNodeForm.put("editMode", "EDITABLE");
                defaultNodeForm.put("viewKey", "default");
                copyApprovalPolicy(defaultNodeForm);
                return defaultNodeForm;
            }
            return new JSONObject();
        }

        JSONObject nodeForm = new JSONObject();
        String formMode = populateIdentity(nodeForm, asset);
        copyApprovalPolicy(nodeForm);
        boolean businessObjectDefaultWritable = permissions.isEmpty()
                && "BUSINESS_OBJECT_FORM".equals(configuration.normalizeNodeFormMode(formMode));
        nodeForm.put("editMode", permissions.stream().anyMatch(item -> readBooleanValue(item.get("writable"), false))
                || businessObjectDefaultWritable ? "EDITABLE" : "READONLY");
        populateFormReference(nodeForm, asset);
        if (!permissions.isEmpty()) {
            nodeForm.put("fieldPermissions", permissions);
        }
        return nodeForm;
    }

    private String populateIdentity(JSONObject nodeForm, JSONObject asset) {
        FlowBindingSchema.putText(nodeForm, "taskDefKey", taskDefKey);
        FlowBindingSchema.putText(nodeForm, "taskName", textValue(formInfo.get("taskName")));
        FlowBindingSchema.putText(nodeForm, "formKey", StringUtils.firstNonBlank(
                formKey,
                StringUtils.trimToNull(flowFormRef.getString("formKey")),
                asset.getString("formKey")));
        FlowBindingSchema.putText(nodeForm, "formName", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formName"))),
                StringUtils.trimToNull(flowFormRef.getString("formName")),
                asset.getString("formName")));
        FlowBindingSchema.putText(nodeForm, "providerKey", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("providerKey"))),
                StringUtils.trimToNull(flowFormRef.getString("providerKey")),
                asset.getString("providerKey")));
        FlowBindingSchema.putText(nodeForm, "formUrl", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formUrl"))),
                StringUtils.trimToNull(flowFormRef.getString("formUrl")),
                asset.getString("formUrl")));
        FlowBindingSchema.putText(nodeForm, "viewKey", StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("viewKey"))),
                StringUtils.trimToNull(flowFormRef.getString("viewKey")),
                asset.getString("viewKey"),
                "default"));
        String formMode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(formInfo.get("formMode"))),
                StringUtils.trimToNull(flowFormRef.getString("formMode")),
                StringUtils.trimToNull(flowFormRef.getString("type")),
                asset.getString("formMode"),
                configKey == null ? "BUSINESS_CODE_FORM" : "BUSINESS_OBJECT_FORM");
        FlowBindingSchema.putText(nodeForm, "formMode", configuration.normalizeNodeFormMode(formMode));
        return formMode;
    }

    private void populateFormReference(JSONObject nodeForm, JSONObject asset) {
        JSONObject effectiveFormRef = new JSONObject();
        if (!asset.isEmpty()) {
            effectiveFormRef.putAll(asset);
        }
        if (!flowFormRef.isEmpty()) {
            effectiveFormRef.putAll(flowFormRef);
        }
        FlowBindingSchema.putText(effectiveFormRef, "formKey", nodeForm.getString("formKey"));
        FlowBindingSchema.putText(effectiveFormRef, "formMode", nodeForm.getString("formMode"));
        FlowBindingSchema.putText(effectiveFormRef, "type", nodeForm.getString("formMode"));
        FlowBindingSchema.putText(effectiveFormRef, "formName", nodeForm.getString("formName"));
        FlowBindingSchema.putText(effectiveFormRef, "providerKey", nodeForm.getString("providerKey"));
        FlowBindingSchema.putText(effectiveFormRef, "formUrl", nodeForm.getString("formUrl"));
        FlowBindingSchema.putText(effectiveFormRef, "viewKey", nodeForm.getString("viewKey"));
        if (!effectiveFormRef.isEmpty()) {
            nodeForm.put("formRef", effectiveFormRef);
        }
    }

    private void copyApprovalPolicy(JSONObject target) {
        for (String key : List.of("allowApprove", "allowDelegate", "allowReject", "allowRejectToStart",
                "allowReturn", "allowTerminate", "requireSignature", "requireComment")) {
            if (formInfo.containsKey(key)) {
                Boolean value = readNullableBooleanValue(formInfo.get(key));
                if (value != null) {
                    target.put(key, value);
                }
            }
        }
    }

    private JSONObject resolveRuntimeBusinessFormRef(Map<String, Object> formInfo) {
        JSONObject variables = readNestedObject(formInfo == null ? null : formInfo.get("variables"));
        if (variables.isEmpty()) {
            return new JSONObject();
        }
        JSONObject formRef = readNestedObject(variables.get("businessFormRef"));
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(variables.getString("businessFormKey")),
                StringUtils.trimToNull(formRef.getString("formKey")),
                StringUtils.trimToNull(variables.getString("formKey")));
        if (formKey == null) {
            return new JSONObject();
        }
        formRef.put("formKey", formKey);
        return formRef;
    }

    static JSONObject findBindingNode(JSONObject bindingConfig, String taskDefKey) {
        JSONArray nodeForms = bindingConfig == null ? null : bindingConfig.getJSONArray("nodeForms");
        if (nodeForms == null || nodeForms.isEmpty() || StringUtils.isBlank(taskDefKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < nodeForms.size(); i++) {
            JSONObject nodeForm = nodeForms.getJSONObject(i);
            if (nodeForm != null && taskDefKey.equals(nodeForm.getString("taskDefKey"))) {
                return nodeForm;
            }
        }
        return new JSONObject();
    }
}
