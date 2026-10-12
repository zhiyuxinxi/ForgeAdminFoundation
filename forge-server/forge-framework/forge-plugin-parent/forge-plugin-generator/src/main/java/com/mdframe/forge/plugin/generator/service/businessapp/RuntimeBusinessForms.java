package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Published low-code schemas and the object field registry fallback. */
@Slf4j
final class RuntimeBusinessForms {
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessFieldDesignService businessFieldDesignService;
    private final ApplicationPageFormAssets pages;

    RuntimeBusinessForms(AiCrudConfigMapper crudConfigMapper, BusinessFieldDesignService fields,
                         ApplicationPageFormAssets pages) {
        this.crudConfigMapper = crudConfigMapper;
        this.businessFieldDesignService = fields;
        this.pages = pages;
    }

    List<Map<String, Object>> collectBusinessFormAssets(BusinessObjectVO object, JSONObject formSchema) {
        if (object == null || formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendBusinessFormAsset(result, seen, object, formSchema, "default");

        JSONArray forms = FormAssetValues.readNestedArray(formSchema.get("forms"));
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            JSONObject schema = FormAssetValues.readNestedObject(form.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? form : schema, "form");
        }

        JSONObject settings = FormAssetValues.readNestedObject(formSchema.get("settings"));
        JSONArray formAssets = FormAssetValues.readNestedArray(settings.get("formAssets"));
        for (int i = 0; i < formAssets.size(); i++) {
            JSONObject asset = formAssets.getJSONObject(i);
            JSONObject schema = FormAssetValues.readNestedObject(asset.get("schema"));
            appendBusinessFormAsset(result, seen, object, schema.isEmpty() ? asset : schema, "asset");
        }
        return result;
    }

    List<Map<String, Object>> collectRuntimeCrudFormAssets(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return List.of();
        }
        List<Map<String, Object>> designerAssets = collectRuntimeDesignerFormAssets(
                object, runtimeConfig, readRuntimeCrudFormDesignerSchema(runtimeConfig));
        if (!designerAssets.isEmpty()) {
            return designerAssets;
        }
        List<Map<String, Object>> fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        if (fieldCatalog.isEmpty()) {
            return List.of();
        }
        String formKey = resolveRuntimeCrudFormKey(object, runtimeConfig);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        item.put("objectName", resolveRuntimeCrudObjectName(object, runtimeConfig));
        item.put("configKey", runtimeConfig.getConfigKey());
        item.put("formKey", formKey);
        item.put("formName", resolveRuntimeCrudFormName(object, runtimeConfig));
        item.put("viewKey", "default");
        item.put("source", "runtimeCrud");
        item.put("sourceType", "businessObjectRuntime");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", FormAssetFields.buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        return List.of(item);
    }

    List<Map<String, Object>> collectObjectFieldRegistryFormAssets(BusinessObjectVO object) {
        if (object == null || object.getId() == null || StringUtils.isBlank(object.getObjectCode())) {
            return List.of();
        }
        List<Map<String, Object>> fieldCatalog;
        try {
            fieldCatalog = FormAssetFields.normalizeRuntimeCrudFormFields(FormAssetValues.readMapList(FormAssetValues.readNestedArray(
                    businessFieldDesignService.listFields(object.getId()))));
        } catch (Exception e) {
            log.warn("读取业务对象字段目录失败: objectId={}, objectCode={}, error={}",
                    object.getId(), object.getObjectCode(), e.getMessage());
            return List.of();
        }
        if (fieldCatalog.isEmpty()) {
            return List.of();
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("configKey", object.getConfigKey());
        item.put("formKey", object.getObjectCode());
        item.put("formName", StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()) + "表单");
        item.put("viewKey", "default");
        item.put("source", "objectFieldRegistry");
        item.put("sourceType", "businessObjectFieldRegistry");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", FormAssetFields.buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        return List.of(item);
    }

    void appendObjectFieldRegistryFallback(List<Map<String, Object>> assets, BusinessObjectVO object) {
        if (assets == null || assets.stream().anyMatch(this::hasFormAssetFields)) {
            return;
        }
        List<Map<String, Object>> fallback = collectObjectFieldRegistryFormAssets(object);
        if (fallback.isEmpty()) {
            return;
        }
        if (!assets.isEmpty()) {
            Map<String, Object> existing = assets.get(0);
            Map<String, Object> generated = fallback.get(0);
            String mode = StringUtils.defaultIfBlank(FormAssetValues.textValue(existing.get("formMode")), FormAssetValues.textValue(existing.get("type")));
            String formKey = StringUtils.trimToNull(FormAssetValues.textValue(existing.get("formKey")));
            if ("BUSINESS_OBJECT_FORM".equalsIgnoreCase(mode) && formKey != null) {
                generated.put("formKey", formKey);
                generated.put("formName", StringUtils.defaultIfBlank(FormAssetValues.textValue(existing.get("formName")),
                        FormAssetValues.textValue(generated.get("formName"))));
                generated.put("providerKey", existing.get("providerKey"));
            }
        }
        FormAssetMerger.appendUniqueFormAssets(assets, fallback);
    }

    boolean hasFormAssetFields(Map<String, Object> asset) {
        if (asset == null) {
            return false;
        }
        List<Map<String, Object>> fields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(asset.get("fieldCatalog")));
        if (fields.isEmpty()) {
            fields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(asset.get("fields")));
        }
        return !fields.isEmpty();
    }

    List<Map<String, Object>> collectRuntimeDesignerFormAssets(BusinessObjectVO object,
                                                                       AiCrudConfig runtimeConfig,
                                                                       JSONObject formSchema) {
        if (runtimeConfig == null || formSchema == null || formSchema.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, formSchema, "runtimeDesignerDefault");

        JSONArray forms = FormAssetValues.readNestedArray(formSchema.get("forms"));
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            JSONObject schema = FormAssetValues.readNestedObject(form.get("schema"));
            appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, schema.isEmpty() ? form : schema,
                    "runtimeDesignerForm");
        }

        JSONObject settings = FormAssetValues.readNestedObject(formSchema.get("settings"));
        JSONArray formAssets = FormAssetValues.readNestedArray(settings.get("formAssets"));
        for (int i = 0; i < formAssets.size(); i++) {
            JSONObject asset = formAssets.getJSONObject(i);
            JSONObject schema = FormAssetValues.readNestedObject(asset.get("schema"));
            appendRuntimeDesignerFormAsset(result, seen, object, runtimeConfig, schema.isEmpty() ? asset : schema,
                    "runtimeDesignerAsset");
        }
        return result;
    }

    void appendRuntimeDesignerFormAsset(List<Map<String, Object>> result,
                                                Set<String> seen,
                                                BusinessObjectVO object,
                                                AiCrudConfig runtimeConfig,
                                                JSONObject schema,
                                                String source) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formKey")),
                StringUtils.trimToNull(schema.getString("defaultFormKey")),
                resolveRuntimeCrudFormKey(object, runtimeConfig));
        if (formKey == null || !seen.add(formKey)) {
            return;
        }
        List<Map<String, Object>> fieldCatalog = FormAssetFields.collectBusinessFormFieldCatalog(schema);
        if (fieldCatalog.isEmpty()) {
            fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        }
        if (fieldCatalog.isEmpty()) {
            return;
        }
        String objectName = resolveRuntimeCrudObjectName(object, runtimeConfig);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        item.put("objectName", objectName);
        item.put("configKey", runtimeConfig.getConfigKey());
        item.put("formKey", formKey);
        item.put("formName", StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formName")),
                StringUtils.isBlank(objectName) ? null : objectName + "表单",
                formKey));
        item.put("viewKey", "default");
        item.put("source", source);
        item.put("sourceType", "businessObjectRuntime");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", FormAssetFields.buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        result.add(item);
    }

    JSONObject buildRuntimeCrudFormSchema(BusinessObjectVO object, AiCrudConfig runtimeConfig, String requestedFormKey) {
        if (runtimeConfig == null) {
            return new JSONObject();
        }
        JSONObject designerSchema = resolveRuntimeDesignerFormSchema(
                readRuntimeCrudFormDesignerSchema(runtimeConfig), requestedFormKey);
        if (!designerSchema.isEmpty()) {
            return designerSchema;
        }
        String formKey = resolveRuntimeCrudFormKey(object, runtimeConfig);
        if (!matchesRuntimeCrudFormKey(requestedFormKey, formKey, runtimeConfig)) {
            return new JSONObject();
        }
        List<Map<String, Object>> fieldCatalog = collectRuntimeCrudFormFieldCatalog(runtimeConfig);
        if (fieldCatalog.isEmpty()) {
            return new JSONObject();
        }
        JSONObject schema = new JSONObject();
        schema.put("schemaVersion", "runtime-crud");
        schema.put("formKey", formKey);
        schema.put("defaultFormKey", formKey);
        schema.put("formName", resolveRuntimeCrudFormName(object, runtimeConfig));
        schema.put("objectCode", resolveRuntimeCrudObjectCode(object, runtimeConfig));
        schema.put("objectName", resolveRuntimeCrudObjectName(object, runtimeConfig));

        JSONObject settings = new JSONObject();
        JSONObject layout = new JSONObject();
        JSONObject runtimeOptions = FormAssetValues.readJsonObject(runtimeConfig.getOptions());
        layout.put("gridColumns", Math.max(1, FormAssetValues.integerValue(runtimeOptions.get("editGridCols"), 2)));
        layout.put("labelPlacement", StringUtils.defaultIfBlank(FormAssetValues.textValue(runtimeOptions.get("editLabelPlacement")), "left"));
        layout.put("labelWidth", StringUtils.defaultIfBlank(FormAssetValues.textValue(runtimeOptions.get("editLabelWidth")), "100"));
        settings.put("layout", layout);
        schema.put("settings", settings);

        JSONArray components = new JSONArray();
        for (Map<String, Object> field : fieldCatalog) {
            components.add(FormAssetFields.toRuntimeCrudFormComponent(field));
        }
        schema.put("components", components);
        return schema;
    }

    JSONObject resolveRuntimeDesignerFormSchema(JSONObject formSchema, String formKey) {
        if (formSchema == null || formSchema.isEmpty()) {
            return new JSONObject();
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(FormAssetValues.readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return byForms;
        }
        JSONObject settings = FormAssetValues.readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(FormAssetValues.readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return formSchema;
        }
        return new JSONObject();
    }

    boolean matchesRuntimeCrudFormKey(String requestedFormKey, String runtimeFormKey, AiCrudConfig runtimeConfig) {
        String requested = StringUtils.trimToNull(requestedFormKey);
        if (requested == null) {
            return true;
        }
        return StringUtils.equals(requested, runtimeFormKey)
                || StringUtils.equals(requested, runtimeConfig.getConfigKey())
                || StringUtils.equals(requested, runtimeConfig.getObjectCode());
    }

    AiCrudConfig resolveRuntimeConfigForBusinessForm(BusinessObjectVO object, String configKey) {
        String lookupKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(configKey),
                object == null ? null : StringUtils.trimToNull(object.getConfigKey()),
                object == null ? null : StringUtils.trimToNull(object.getObjectCode()));
        return resolvePublishedRuntimeConfig(BusinessFlowRuntimeMetadata.resolveTenantId(), lookupKey);
    }

    JSONObject readRuntimeCrudFormDesignerSchema(AiCrudConfig runtimeConfig) {
        JSONObject options = FormAssetValues.readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        return FormAssetValues.readNestedObject(options.get("formDesignerSchema"));
    }

    String resolveRuntimeCrudFormKey(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        JSONObject options = FormAssetValues.readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        JSONObject designerSchema = FormAssetValues.readNestedObject(options.get("formDesignerSchema"));
        String objectCode = resolveRuntimeCrudObjectCode(object, runtimeConfig);
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(designerSchema.getString("formKey")),
                StringUtils.trimToNull(FormAssetValues.textValue(options.get("defaultFormKey"))),
                StringUtils.trimToNull(FormAssetValues.textValue(options.get("formKey"))),
                StringUtils.isBlank(objectCode) ? null : objectCode + "_default_form",
                runtimeConfig == null ? null : runtimeConfig.getConfigKey());
    }

    String resolveRuntimeCrudFormName(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        JSONObject options = FormAssetValues.readJsonObject(runtimeConfig == null ? null : runtimeConfig.getOptions());
        JSONObject designerSchema = FormAssetValues.readNestedObject(options.get("formDesignerSchema"));
        String objectName = resolveRuntimeCrudObjectName(object, runtimeConfig);
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(designerSchema.getString("formName")),
                StringUtils.trimToNull(FormAssetValues.textValue(options.get("formName"))),
                StringUtils.isBlank(objectName) ? null : objectName + "表单",
                runtimeConfig == null ? null : runtimeConfig.getAppName(),
                resolveRuntimeCrudFormKey(object, runtimeConfig));
    }

    String resolveRuntimeCrudObjectCode(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        return StringUtils.firstNonBlank(
                object == null ? null : StringUtils.trimToNull(object.getObjectCode()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getObjectCode()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getConfigKey()));
    }

    String resolveRuntimeCrudObjectName(BusinessObjectVO object, AiCrudConfig runtimeConfig) {
        return StringUtils.firstNonBlank(
                object == null ? null : StringUtils.trimToNull(object.getObjectName()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getObjectName()),
                runtimeConfig == null ? null : StringUtils.trimToNull(runtimeConfig.getAppName()),
                resolveRuntimeCrudObjectCode(object, runtimeConfig));
    }

    List<Map<String, Object>> collectRuntimeCrudFormFieldCatalog(AiCrudConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return List.of();
        }
        JSONObject options = FormAssetValues.readJsonObject(runtimeConfig.getOptions());
        List<Map<String, Object>> fields = FormAssetValues.readMapList(FormAssetValues.readNestedArray(runtimeConfig.getEditSchema()));
        if (!fields.isEmpty()) {
            List<Map<String, Object>> layoutFields = FormAssetFields.applyRuntimeCrudFormLayout(
                    fields, FormAssetValues.readNestedArray(options.get("editFormLayout")));
            return FormAssetFields.normalizeRuntimeCrudFormFields(layoutFields.isEmpty() ? fields : layoutFields);
        }
        JSONObject modelSchema = FormAssetValues.readJsonObject(runtimeConfig.getModelSchema());
        return FormAssetFields.normalizeRuntimeCrudFormFields(FormAssetValues.readMapList(FormAssetValues.readNestedArray(modelSchema.get("fields"))));
    }

    void appendBusinessFormAsset(List<Map<String, Object>> result,
                                         Set<String> seen,
                                         BusinessObjectVO object,
                                         JSONObject schema,
                                         String source) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        String formKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(schema.getString("formKey")),
                StringUtils.trimToNull(schema.getString("defaultFormKey")));
        if (formKey == null || !seen.add(formKey)) {
            return;
        }
        String formName = StringUtils.defaultIfBlank(schema.getString("formName"), object.getObjectName() + "表单");
        List<Map<String, Object>> fieldCatalog = FormAssetFields.collectBusinessFormFieldCatalog(schema);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "BUSINESS_OBJECT_FORM");
        item.put("formMode", "BUSINESS_OBJECT_FORM");
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("formKey", formKey);
        item.put("formName", formName);
        item.put("viewKey", "default");
        item.put("source", source);
        item.put("sourceType", "businessObject");
        item.put("fieldCatalog", fieldCatalog);
        item.put("fields", fieldCatalog);
        item.put("fieldCount", fieldCatalog.size());
        item.put("fieldPreview", FormAssetFields.buildFieldPreview(fieldCatalog));
        item.put("supportsSave", true);
        result.add(item);
    }

    JSONObject resolveBusinessFormSchema(BusinessObjectVO object, String formKey, String configKey) {
        JSONObject applicationSchema = pages.resolveApplicationPageFormSchema(formKey);
        if (!applicationSchema.isEmpty()) {
            return applicationSchema;
        }
        AiCrudConfig runtimeConfig = resolveRuntimeConfigForBusinessForm(object, configKey);
        if (object == null) {
            return buildRuntimeCrudFormSchema(null, runtimeConfig, formKey);
        }
        JSONObject designerOptions = FormAssetValues.readJsonObject(object.getDesignerOptions());
        JSONObject formSchema = FormAssetValues.readNestedObject(designerOptions.get("formDesignerSchema"));
        if (formSchema.isEmpty()) {
            JSONObject runtimeSchema = buildRuntimeCrudFormSchema(object, runtimeConfig, formKey);
            return runtimeSchema.isEmpty() ? buildObjectFieldRegistryFormSchema(object, formKey) : runtimeSchema;
        }
        String targetFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formKey),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")),
                StringUtils.trimToNull(formSchema.getString("formKey")));

        JSONObject byForms = findFormSchemaInArray(FormAssetValues.readNestedArray(formSchema.get("forms")), targetFormKey);
        if (!byForms.isEmpty()) {
            return FormAssetFields.collectBusinessFormFieldCatalog(byForms).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : byForms;
        }
        JSONObject settings = FormAssetValues.readNestedObject(formSchema.get("settings"));
        JSONObject byAssets = findFormSchemaInArray(FormAssetValues.readNestedArray(settings.get("formAssets")), targetFormKey);
        if (!byAssets.isEmpty()) {
            return FormAssetFields.collectBusinessFormFieldCatalog(byAssets).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : byAssets;
        }
        String rootFormKey = StringUtils.firstNonBlank(
                StringUtils.trimToNull(formSchema.getString("formKey")),
                StringUtils.trimToNull(formSchema.getString("defaultFormKey")));
        if (StringUtils.isBlank(targetFormKey) || StringUtils.equals(targetFormKey, rootFormKey)) {
            return FormAssetFields.collectBusinessFormFieldCatalog(formSchema).isEmpty()
                    ? buildObjectFieldRegistryFormSchema(object, targetFormKey)
                    : formSchema;
        }
        JSONObject runtimeFormSchema = buildRuntimeCrudFormSchema(object, runtimeConfig, targetFormKey);
        if (!runtimeFormSchema.isEmpty()) {
            return runtimeFormSchema;
        }
        return buildObjectFieldRegistryFormSchema(object, targetFormKey);
    }

    JSONObject buildObjectFieldRegistryFormSchema(BusinessObjectVO object, String requestedFormKey) {
        if (object == null || object.getId() == null) {
            return new JSONObject();
        }
        try {
            List<Map<String, Object>> sourceFields = new ArrayList<>();
            businessFieldDesignService.listFields(object.getId()).forEach(field ->
                    sourceFields.add(new LinkedHashMap<>(
                            JSON.parseObject(JSON.toJSONString(field), JSONObject.class))));
            List<Map<String, Object>> fields = FormAssetFields.normalizeRuntimeCrudFormFields(sourceFields);
            if (fields.isEmpty()) {
                return new JSONObject();
            }
            String formKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(requestedFormKey), object.getObjectCode());
            JSONObject schema = new JSONObject();
            schema.put("formKey", formKey);
            schema.put("defaultFormKey", formKey);
            schema.put("formName", StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()) + "表单");
            JSONArray components = new JSONArray();
            fields.forEach(field -> components.add(FormAssetFields.toRuntimeCrudFormComponent(field)));
            schema.put("components", components);
            return schema;
        } catch (Exception e) {
            log.debug("读取业务对象字段注册表表单 schema 失败: objectId={}, error={}", object.getId(), e.getMessage());
            return new JSONObject();
        }
    }

    JSONObject findFormSchemaInArray(JSONArray forms, String formKey) {
        if (forms == null || forms.isEmpty() || StringUtils.isBlank(formKey)) {
            return new JSONObject();
        }
        for (int i = 0; i < forms.size(); i++) {
            JSONObject form = forms.getJSONObject(i);
            if (form == null) {
                continue;
            }
            JSONObject schema = FormAssetValues.readNestedObject(form.get("schema"));
            JSONObject candidate = schema.isEmpty() ? form : schema;
            String candidateKey = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(form.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("formKey")),
                    StringUtils.trimToNull(candidate.getString("defaultFormKey")));
            if (StringUtils.equals(formKey, candidateKey)) {
                return candidate;
            }
        }
        return new JSONObject();
    }

    AiCrudConfig resolvePublishedRuntimeConfig(Long tenantId, String objectCodeOrConfigKey) {
        if (StringUtils.isBlank(objectCodeOrConfigKey)) {
            return null;
        }
        return crudConfigMapper.selectPublishedByObjectCodeOrConfigKey(
                tenantId != null ? tenantId : BusinessFlowRuntimeMetadata.resolveTenantId(), objectCodeOrConfigKey);
    }
}
