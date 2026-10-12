package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.businessapp.FormAssetValues.*;

/** Reads and assembles an authorized low-code task form; owns no task actions or writes. */
@Slf4j
@RequiredArgsConstructor
final class BusinessObjectTaskFormReader {
    private final BusinessFlowRuntimeMetadata runtimeMetadata;
    private final BusinessFormAssetCatalog formAssets;
    private final DynamicCrudService dynamicCrudService;
    private final TaskFormProjection projection;
    private final BusinessCodeTaskFields codeTaskFields;
    private final NodeFormConfiguration nodeFormConfiguration = new NodeFormConfiguration();

    BusinessTaskFormContextVO read(BusinessTaskFormContextVO vo, JSONObject nodeForm, String formKey,
                                  Long tenantId, JSONObject bindingConfig) {
        BusinessObjectVO object = runtimeMetadata.queryBusinessObject(tenantId, vo.getObjectCode());
        vo.setBusinessObjectName(object == null ? vo.getObjectCode() : object.getObjectName());
        JSONObject formSchema = formAssets.runtime.resolveBusinessFormSchema(object, formKey, vo.getConfigKey());
        if (formSchema.isEmpty()) {
            vo.getWarnings().add("未找到节点引用的低代码表单资产: " + formKey);
            return vo;
        }

        List<Map<String, Object>> fieldCatalog = resolveBusinessTaskCrudPageFields(vo.getConfigKey(), formKey, formSchema);
        List<Map<String, Object>> permissions = projection.normalizeBusinessObjectTaskPermissions(
                fieldCatalog, nodeFormConfiguration.normalizeFieldPermissions(nodeForm.get("fieldPermissions")));
        List<Map<String, Object>> fields = projection.buildTaskFormFields(fieldCatalog, permissions);
        Map<String, Object> recordData = dynamicCrudService.selectById(vo.getConfigKey(), vo.getRecordId());
        Map<String, Object> visibleRecordData = projection.filterVisibleRecordData(recordData, fields);
        List<Map<String, Object>> childrenConfig = new BusinessTaskDetailChildren(dynamicCrudService)
                .project(vo.getConfigKey(), vo.getRecordId(), visibleRecordData);
        vo.setBusinessSummary(BusinessFlowDisplayText.resolveBusinessSummary(object, bindingConfig, recordData));

        vo.setConfigured(true);
        vo.setFormType("business-object");
        vo.setFormKey(StringUtils.firstNonBlank(formKey, formSchema.getString("formKey")));
        vo.setFormName(StringUtils.defaultIfBlank(nodeForm.getString("formName"), formSchema.getString("formName")));
        vo.setViewKey(StringUtils.defaultIfBlank(nodeForm.getString("viewKey"), "default"));
        vo.setEditMode(projection.resolveBusinessObjectTaskEditMode(nodeForm, permissions));
        applyBusinessObjectFormLayout(vo, formSchema, vo.getConfigKey());
        vo.setFormRef(readNestedObject(nodeForm.get("formRef")));
        projection.applyPageFormIdentity(vo, vo.getFormRef());
        vo.setFieldPermissions(permissions);
        vo.setFields(fields);
        vo.setFormAssets(resolveBusinessTaskFormAssets(formSchema, vo.getConfigKey(), formKey));
        vo.setChildrenConfig(childrenConfig);
        vo.setRecordData(visibleRecordData);
        codeTaskFields.applyApprovalPolicy(vo, nodeForm);
        if (fields.isEmpty()) {
            vo.getWarnings().add("当前业务表单没有可展示字段");
        }
        return vo;
    }

    private void applyBusinessObjectFormLayout(BusinessTaskFormContextVO vo, JSONObject formSchema, String configKey) {
        JSONObject settings = readNestedObject(formSchema == null ? null : formSchema.get("settings"));
        JSONObject layout = readNestedObject(settings.get("layout"));
        JSONObject runtimeOptions = readRuntimeConfigOptions(configKey);
        vo.setGridCols(Math.max(1, integerValue(
                firstNonNull(layout.get("gridCols"),
                        layout.get("gridColumns"),
                        settings.get("gridCols"),
                        settings.get("gridColumns"),
                        runtimeOptions.get("editGridCols")),
                1)));
        vo.setLabelPlacement(StringUtils.defaultIfBlank(
                StringUtils.firstNonBlank(
                        textValue(layout.get("labelPlacement")),
                        textValue(settings.get("labelPlacement")),
                        textValue(runtimeOptions.get("editLabelPlacement"))),
                "left"));
        vo.setLabelWidth(StringUtils.defaultIfBlank(
                StringUtils.firstNonBlank(
                        textValue(layout.get("labelWidth")),
                        textValue(settings.get("labelWidth")),
                        textValue(runtimeOptions.get("editLabelWidth"))),
                "100"));
    }

    List<Map<String, Object>> resolveBusinessTaskCrudPageFields(String configKey,
                                                                        String formKey,
                                                                        JSONObject formSchema) {
        List<Map<String, Object>> fallback = FormAssetFields.collectBusinessFormFieldCatalog(formSchema);
        if (StringUtils.isBlank(configKey)) {
            return fallback;
        }
        try {
            LowcodeRuntimeConfig runtimeConfig = dynamicCrudService.getRuntimeValues(configKey);
            if (runtimeConfig == null) {
                return fallback;
            }
            JSONObject options = readJsonObject(runtimeConfig.getOptions());
            if (!shouldUseCrudPageDefaultFormSchema(formKey, formSchema, options)) {
                return fallback;
            }
            List<Map<String, Object>> runtimeFields = FormAssetValues.readMapList(readNestedArray(runtimeConfig.getEditSchema()));
            if (runtimeFields.isEmpty()) {
                return fallback;
            }
            List<Map<String, Object>> layoutFields = FormAssetFields.applyRuntimeCrudFormLayout(
                    runtimeFields, readNestedArray(options.get("editFormLayout")));
            return layoutFields.isEmpty() ? runtimeFields : layoutFields;
        } catch (Exception e) {
            log.debug("读取动态 CRUD 详情表单 schema 失败: configKey={}, error={}", configKey, e.getMessage());
            return fallback;
        }
    }

    private boolean shouldUseCrudPageDefaultFormSchema(String formKey, JSONObject formSchema, JSONObject options) {
        String requestedKey = StringUtils.trimToNull(formKey);
        if (requestedKey == null) {
            return true;
        }
        String currentFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("defaultFormKey")));
        JSONObject designerSchema = readNestedObject(options == null ? null : options.get("formDesignerSchema"));
        String defaultFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(designerSchema.getString("formKey")),
                currentFormKey);
        return StringUtils.isBlank(defaultFormKey) || StringUtils.equals(requestedKey, defaultFormKey);
    }

    private List<Map<String, Object>> resolveBusinessTaskFormAssets(JSONObject formSchema,
                                                                    String configKey,
                                                                    String activeFormKey) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            LowcodeRuntimeConfig runtimeConfig = dynamicCrudService.getRuntimeValues(configKey);
            JSONObject options = runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
            List<Map<String, Object>> configuredAssets = FormAssetValues.readMapList(readNestedArray(options.get("formAssets")));
            if (!configuredAssets.isEmpty()) {
                return configuredAssets;
            }
            JSONObject designerSchema = readNestedObject(options.get("formDesignerSchema"));
            JSONArray forms = readNestedArray(designerSchema.get("forms"));
            if (forms.isEmpty()) {
                return List.of();
            }
            String currentKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(activeFormKey),
                    StringUtils.trimToNull(formSchema == null ? null : formSchema.getString("formKey")),
                    StringUtils.trimToNull(designerSchema.getString("defaultFormKey")));
            List<Map<String, Object>> assets = new ArrayList<>();
            for (int i = 0; i < forms.size(); i++) {
                JSONObject form = forms.getJSONObject(i);
                if (form == null) {
                    continue;
                }
                String itemKey = StringUtils.trimToNull(form.getString("formKey"));
                if (itemKey == null || StringUtils.equals(itemKey, currentKey)) {
                    continue;
                }
                Map<String, Object> asset = new LinkedHashMap<>();
                asset.put("formKey", itemKey);
                asset.put("formName", StringUtils.defaultIfBlank(form.getString("formName"), itemKey));
                asset.put("usage", readNestedArray(form.get("usage")));
                asset.put("schema", readNestedObject(form.get("schema")));
                assets.add(asset);
            }
            return assets;
        } catch (Exception e) {
            log.debug("读取业务表单资产失败: configKey={}, error={}", configKey, e.getMessage());
            return List.of();
        }
    }

    private JSONObject readRuntimeConfigOptions(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return new JSONObject();
        }
        try {
            LowcodeRuntimeConfig runtimeConfig = dynamicCrudService.getRuntimeValues(configKey);
            return runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
        } catch (Exception e) {
            log.debug("读取业务表单运行态布局失败: configKey={}, error={}", configKey, e.getMessage());
            return new JSONObject();
        }
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

}
