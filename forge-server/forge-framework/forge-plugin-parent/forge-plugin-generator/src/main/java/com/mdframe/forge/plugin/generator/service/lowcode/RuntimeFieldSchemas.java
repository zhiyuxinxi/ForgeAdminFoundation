package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.RelationLookupMeta;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.mapValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.resolveEditFieldSetting;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findRuntimeField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.isSystemField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.applyAlignment;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.applySelectionLabelProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.buildPlaceholder;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.copyBasicProp;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.isTextComponent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.normalizeFixed;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.resolveDefaultRenderType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.resolveEditComponentType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.resolveSearchComponentType;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.sanitizeFieldBasicProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.applyRelationLookupProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.buildRelationDisplayAlias;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.buildRelationLookupConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolveRelationLookup;

/** Search, column and edit contracts for runtime field consumers. */
final class RuntimeFieldSchemas {

    private RuntimeFieldSchemas() {
    }

    static Map<String, Object> buildSearchField(LowcodeFieldSchema field) {
        return buildSearchField(field, Map.of(), null, null);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildSearchField(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        String configuredQueryFieldName = StringUtils.defaultIfBlank(text(pageSetting.get("queryField")), field.getField());
        LowcodeFieldSchema queryField = findRuntimeField(modelSchema, pageSchema, configuredQueryFieldName);
        String queryFieldName = queryField == null ? field.getField() : configuredQueryFieldName;
        LowcodeFieldSchema effectiveField = queryField == null ? field : queryField;
        String queryType = StringUtils.defaultIfBlank(text(pageSetting.get("queryType")),
                StringUtils.defaultIfBlank(effectiveField.getQueryType(), StringUtils.defaultIfBlank(field.getQueryType(), "eq")))
                .toLowerCase(Locale.ROOT);
        item.put("field", queryFieldName);
        item.put("label", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        if (field.getLabelI18n() != null && !field.getLabelI18n().isEmpty()) {
            item.put("labelI18n", new LinkedHashMap<>(field.getLabelI18n()));
        }
        if (field.getPlaceholderI18n() != null && !field.getPlaceholderI18n().isEmpty()) {
            item.put("placeholderI18n", new LinkedHashMap<>(field.getPlaceholderI18n()));
        }
        String componentType = resolveSearchComponentType(effectiveField, queryType, pageSetting);
        item.put("type", componentType);
        item.put("queryType", queryType);
        applyAlignment(item, pageSetting);
        if (pageSetting.containsKey("defaultValue")) {
            item.put("defaultValue", pageSetting.get("defaultValue"));
        }
        if (pageSetting.containsKey("collapsed")) {
            item.put("collapsed", booleanWithDefault(pageSetting.get("collapsed"), false));
        }
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            item.put("type", "select");
            item.put("queryType", "eq");
            item.put("relationLookup", buildRelationLookupConfig(lookupMeta));
        }
        if ("daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)) {
            item.put("startPlaceholder", "开始" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
            item.put("endPlaceholder", "结束" + StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        String dictType = StringUtils.defaultIfBlank(text(pageSetting.get("dictType")), effectiveField.getDictType());
        if (StringUtils.isNotBlank(dictType)) {
            item.put("dictType", dictType);
        }
        Map<String, Object> props = sanitizeFieldBasicProps(field);
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            props.putAll((Map<String, Object>) designerPropsMap);
        }
        if (!props.isEmpty()) {
            item.put("props", props);
        }
        if (lookupMeta != null) {
            applyRelationLookupProps(item, lookupMeta, StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        }
        return item;
    }

    static Map<String, Object> buildTableColumn(LowcodeFieldSchema field) {
        return buildTableColumn(field, Map.of());
    }

    static Map<String, Object> buildTableColumn(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildTableColumn(field, pageSetting, null, null);
    }

    static Map<String, Object> buildTableColumn(LowcodeFieldSchema field,
                                                 Map<String, Object> pageSetting,
                                                 LowcodeModelSchema modelSchema,
                                                 LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", field.getField());
        item.put("title", StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        if (field.getLabelI18n() != null && !field.getLabelI18n().isEmpty()) {
            item.put("titleI18n", new LinkedHashMap<>(field.getLabelI18n()));
        }
        if (field.getPlaceholderI18n() != null && !field.getPlaceholderI18n().isEmpty()) {
            item.put("placeholderI18n", new LinkedHashMap<>(field.getPlaceholderI18n()));
        }
        item.put("dataIndex", field.getField());
        applyAlignment(item, pageSetting);
        Integer settingWidth = integerValue(pageSetting.get("width"));
        if (settingWidth != null && settingWidth > 0) {
            item.put("width", settingWidth);
        } else if (field.getWidth() != null && field.getWidth() > 0) {
            item.put("width", field.getWidth());
        }
        String fixed = normalizeFixed(text(pageSetting.get("fixed")));
        if (StringUtils.isNotBlank(fixed)) {
            item.put("fixed", fixed);
        }
        Object sortable = pageSetting.get("sortable");
        if (Boolean.TRUE.equals(sortable) || Boolean.TRUE.equals(field.getSortable())) {
            item.put("sorter", true);
        }
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), field.getComponentType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        String renderType = StringUtils.defaultIfBlank(text(pageSetting.get("renderType")),
                resolveDefaultRenderType(field, componentType));
        String targetField = StringUtils.defaultIfBlank(text(pageSetting.get("targetField")), field.getField() + "Name");
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        if (lookupMeta != null) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "relationName");
            render.put("targetField", buildRelationDisplayAlias(field.getField()));
            render.put("relationModelCode", lookupMeta.modelCode());
            render.put("displayField", lookupMeta.displayField());
            item.put("render", render);
        } else if ("dictTag".equals(renderType) || (StringUtils.isBlank(renderType) && StringUtils.isNotBlank(field.getDictType()))) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "dictTag");
            render.put("dictType", field.getDictType());
            item.put("render", render);
        } else if ("orgName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "orgName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("userName".equals(renderType) || "postName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", renderType);
            if ("postName".equals(renderType) && field.getBasicProps() != null) {
                targetField = StringUtils.defaultIfBlank(text(field.getBasicProps().get("labelValueField")), targetField);
            }
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("regionName".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "regionName");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("fileUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "fileUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        } else if ("imageUpload".equals(renderType)) {
            Map<String, Object> render = new LinkedHashMap<>();
            render.put("type", "imageUpload");
            render.put("targetField", targetField);
            item.put("render", render);
        }
        copyTableColumnDesignerSettings(item, pageSetting);
        return item;
    }

    static void copyTableColumnDesignerSettings(Map<String, Object> item, Map<String, Object> pageSetting) {
        putIfNotBlank(item, "renderType", text(pageSetting.get("renderType")));
        putIfNotBlank(item, "titleField", text(pageSetting.get("titleField")));
        putIfNotBlank(item, "subtitleField", text(pageSetting.get("subtitleField")));
        copyBasicProp(pageSetting, item, "avatar");
        putIfNotBlank(item, "targetField", text(pageSetting.get("targetField")));
        putIfNotBlank(item, "textColor", text(pageSetting.get("textColor")));
        String clickAction = StringUtils.defaultIfBlank(text(pageSetting.get("clickAction")), "none");
        if (!"none".equals(clickAction)) {
            item.put("clickAction", clickAction);
            putIfNotBlank(item, "targetPageKey", text(pageSetting.get("targetPageKey")));
            putIfNotBlank(item, "targetFormKey", text(pageSetting.get("targetFormKey")));
            putIfNotBlank(item, "targetParamName", text(pageSetting.get("targetParamName")));
            putIfNotBlank(item, "targetParamField", text(pageSetting.get("targetParamField")));
        }
    }

    static Map<String, Object> buildEditField(LowcodeFieldSchema field) {
        return buildEditField(field, Map.of());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildEditField(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        return buildEditField(field, pageSetting, null, null);
    }

    private record EditValidation(boolean required, boolean readonly,
                                  List<Map<String, Object>> rules, String requiredMessage, Object trigger) {
    }

    static Map<String, Object> buildEditField(LowcodeFieldSchema field,
                                            Map<String, Object> pageSetting,
                                            LowcodeModelSchema modelSchema,
                                            LowcodePageSchema pageSchema) {
        Map<String, Object> item = new LinkedHashMap<>();
        String label = StringUtils.defaultIfBlank(text(pageSetting.get("label")),
                StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        RelationLookupMeta lookupMeta = resolveRelationLookup(modelSchema, pageSchema, field.getField());
        String componentType = lookupMeta == null ? resolveEditComponentType(field, pageSetting) : "select";
        Map<String, Object> formulaConfig = field.getFormulaConfig();
        boolean formulaField = formulaConfig != null && !formulaConfig.isEmpty();
        applyEditIdentity(item, field, pageSetting, label, componentType);
        EditValidation validation = initializeEditValidation(item, field, pageSetting, formulaField);
        applyEditMetadata(item, field, pageSetting, formulaConfig, formulaField);
        Map<String, Object> props = buildEditProps(item, field, pageSetting, componentType, label, validation.readonly());
        item.put("props", props);
        if (lookupMeta != null) {
            item.put("relationLookup", buildRelationLookupConfig(lookupMeta));
            applyRelationLookupProps(item, lookupMeta, label);
        }
        finishEditValidation(item, validation, componentType, label);
        return item;
    }

    private static void applyEditIdentity(Map<String, Object> item, LowcodeFieldSchema field,
                                          Map<String, Object> pageSetting, String label, String componentType) {
        item.put("field", field.getField());
        item.put("label", label);
        if (field.getLabelI18n() != null && !field.getLabelI18n().isEmpty()) {
            item.put("labelI18n", new LinkedHashMap<>(field.getLabelI18n()));
        }
        if (field.getPlaceholderI18n() != null && !field.getPlaceholderI18n().isEmpty()) {
            item.put("placeholderI18n", new LinkedHashMap<>(field.getPlaceholderI18n()));
        }
        item.put("type", componentType);
        applyAlignment(item, pageSetting);
    }

    private static EditValidation initializeEditValidation(Map<String, Object> item, LowcodeFieldSchema field,
                                                           Map<String, Object> pageSetting, boolean formulaField) {
        boolean required = pageSetting.containsKey("required")
                ? booleanWithDefault(pageSetting.get("required"), false)
                : Boolean.TRUE.equals(field.getRequired());
        List<Map<String, Object>> validationRules = resolveRuntimeValidationRules(pageSetting);
        if (!pageSetting.containsKey("required")
                && validationRules.stream().anyMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
            required = true;
        }
        boolean readonly = pageSetting.containsKey("readonly")
                ? booleanWithDefault(pageSetting.get("readonly"), false)
                : Boolean.TRUE.equals(field.getReadonly());
        if (formulaField) {
            readonly = true;
            required = false;
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        if (!required) {
            validationRules.removeIf(rule -> booleanWithDefault(rule.get("required"), false));
        }
        item.put("required", !isSystemField(field) && required);
        String requiredMessage = StringUtils.defaultIfBlank(text(pageSetting.get("requiredMessage")),
                resolveRequiredRuleMessage(validationRules));
        if (StringUtils.isNotBlank(requiredMessage)) {
            item.put("requiredMessage", requiredMessage);
        }
        Object trigger = StringUtils.isNotBlank(text(pageSetting.get("trigger")))
                ? pageSetting.get("trigger")
                : resolveRequiredRuleTrigger(validationRules);
        if (trigger != null) {
            item.put("trigger", trigger);
        }
        if (isSystemField(field) || readonly) {
            item.put("disabled", true);
            item.put("readonly", true);
        }
        return new EditValidation(required, readonly, validationRules, requiredMessage, trigger);
    }

    private static void applyEditMetadata(Map<String, Object> item, LowcodeFieldSchema field,
                                          Map<String, Object> pageSetting, Map<String, Object> formulaConfig,
                                          boolean formulaField) {
        copyRuntimeSetting(item, pageSetting, "hidden");
        copyRuntimeSetting(item, pageSetting, "formVisible");
        copyRuntimeSetting(item, pageSetting, "runtimeRules");
        if (formulaField) {
            item.put("formulaConfig", new LinkedHashMap<>(formulaConfig));
        }
        if (field.getAdvancedProps() != null && !field.getAdvancedProps().isEmpty()) {
            item.put("advancedProps", new LinkedHashMap<>(field.getAdvancedProps()));
        }
        String dictType = StringUtils.defaultIfBlank(text(pageSetting.get("dictType")), field.getDictType());
        if (StringUtils.isNotBlank(dictType)) {
            item.put("dictType", dictType);
        }
        if (pageSetting.containsKey("defaultValue")) {
            item.put("defaultValue", pageSetting.get("defaultValue"));
        } else if (field.getDefaultValue() != null) {
            item.put("defaultValue", field.getDefaultValue());
        }
        Object span = pageSetting.get("span");
        if (span != null) {
            item.put("span", span);
        }
        Object formItemStyle = pageSetting.get("formItemStyle");
        if (formItemStyle != null) {
            item.put("formItemStyle", formItemStyle);
        }
        Object gridStyle = pageSetting.get("gridStyle");
        if (gridStyle != null) {
            item.put("gridStyle", gridStyle);
        }
        Object labelWidth = pageSetting.get("labelWidth");
        if (labelWidth != null) {
            item.put("labelWidth", labelWidth);
        }
        copyRuntimeSetting(item, pageSetting, "componentStyle");
        copyRuntimeSetting(item, pageSetting, "componentClass");
        copyRuntimeSetting(item, pageSetting, "formItemClass");
        copyRuntimeSetting(item, pageSetting, "showFeedback");
        copyRuntimeSetting(item, pageSetting, "showLabel");

    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildEditProps(Map<String, Object> item, LowcodeFieldSchema field,
                                                      Map<String, Object> pageSetting, String componentType,
                                                      String label, boolean readonly) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("placeholder", buildPlaceholder(componentType, label));
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        if (field.getLength() != null && field.getLength() > 0 && isTextComponent(componentType)) {
            props.put("maxlength", field.getLength());
        }
        if (field.getPrecision() != null && field.getPrecision() >= 0 && "number".equals(componentType)) {
            props.put("precision", field.getPrecision());
        }
        props.putAll(sanitizeFieldBasicProps(field));
        Object designerProps = pageSetting.get("props");
        if (designerProps instanceof Map<?, ?> designerPropsMap) {
            Map<String, Object> sanitizedDesignerProps = new LinkedHashMap<>((Map<String, Object>) designerPropsMap);
            Map<String, Object> formCreateMeta = mapValue(sanitizedDesignerProps.get("__fc"));
            sanitizedDesignerProps.remove("__fc");
            sanitizedDesignerProps.remove("__fcType");
            sanitizedDesignerProps.remove("fieldBinding");
            props.putAll(sanitizedDesignerProps);
            applyFormCreateMeta(item, formCreateMeta, props);
        }
        copyRuntimePropsToField(item, props);
        applySelectionLabelProps(props, field.getField(), componentType);
        if (isSystemField(field) || readonly) {
            props.put("disabled", true);
            props.put("readonly", true);
        }
        return props;
    }

    private static void finishEditValidation(Map<String, Object> item, EditValidation validation,
                                             String componentType, String label) {
        boolean required = validation.required();
        String requiredMessage = validation.requiredMessage();
        Object trigger = validation.trigger();
        List<Map<String, Object>> validationRules = validation.rules();
        if (required) {
            String message = StringUtils.defaultIfBlank(requiredMessage, buildPlaceholder(componentType, label));
            if (validationRules.stream().noneMatch(rule -> booleanWithDefault(rule.get("required"), false))) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("required", true);
                rule.put("message", message);
                rule.put("trigger", trigger == null ? List.of("blur", "change") : trigger);
                validationRules.add(0, rule);
            } else {
                validationRules.forEach(rule -> {
                    if (booleanWithDefault(rule.get("required"), false) && StringUtils.isBlank(text(rule.get("message")))) {
                        rule.put("message", message);
                    }
                });
            }
            item.put("requiredMessage", message);
        }
        if (!validationRules.isEmpty()) {
            item.put("rules", validationRules);
        }
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> resolveRuntimeValidationRules(Map<String, Object> pageSetting) {
        Object source = pageSetting.get("rules");
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                rules.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return rules;
    }

    static String resolveRequiredRuleMessage(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> text(rule.get("message")))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    static Object resolveRequiredRuleTrigger(List<Map<String, Object>> validationRules) {
        return validationRules.stream()
                .filter(rule -> booleanWithDefault(rule.get("required"), false))
                .map(rule -> rule.get("trigger"))
                .filter(value -> value != null && StringUtils.isNotBlank(String.valueOf(value)))
                .findFirst()
                .orElse(null);
    }

    static void copyRuntimeSetting(Map<String, Object> item, Map<String, Object> pageSetting, String key) {
        if (pageSetting.containsKey(key)) {
            item.put(key, pageSetting.get(key));
        }
    }

    static void copyRuntimePropsToField(Map<String, Object> item, Map<String, Object> props) {
        List.of("placeholder", "clearable", "filterable", "multiple", "size", "maxlength", "showCount",
                        "rows", "autosize", "min", "max", "step", "precision", "showButton",
                        "checkedValue", "uncheckedValue", "checkedText", "uncheckedText", "format",
                        "valueFormat", "startPlaceholder", "endPlaceholder", "showFeedback", "showLabel")
                .forEach(key -> {
                    if (props.containsKey(key)) {
                        item.put(key, props.get(key));
                    }
                });
    }

    static void applyFormCreateMeta(Map<String, Object> item, Map<String, Object> formCreateMeta, Map<String, Object> props) {
        if (formCreateMeta == null || formCreateMeta.isEmpty()) {
            return;
        }
        Object style = firstPresent(formCreateMeta.get("style"), props.get("style"));
        if (style != null) {
            item.put("componentStyle", style);
        }
        Object componentClass = firstPresent(props.get("className"), props.get("class"));
        if (componentClass != null) {
            item.put("componentClass", componentClass);
        }
        Object formItemClass = firstPresent(formCreateMeta.get("className"), formCreateMeta.get("class"));
        if (formItemClass != null) {
            item.put("formItemClass", formItemClass);
        }
        Map<String, Object> wrap = mapValue(formCreateMeta.get("wrap"));
        if (wrap.get("style") != null) {
            item.put("formItemStyle", wrap.get("style"));
        }
        if (wrap.containsKey("labelWidth")) {
            item.put("labelWidth", wrap.get("labelWidth"));
        }
        if (wrap.containsKey("show") && !booleanWithDefault(wrap.get("show"), true)) {
            item.put("showLabel", false);
        }
    }

    static boolean isEditFieldVisibleAtDesignTime(LowcodePageSchema pageSchema,
                                                   LowcodeFieldSchema field) {
        if (field == null || Boolean.TRUE.equals(field.getSystemField())
                || Boolean.TRUE.equals(field.getReadonly())) {
            return false;
        }
        if (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible())) {
            return true;
        }
        Map<String, Object> setting = resolveEditFieldSetting(pageSchema, field.getField());
        return containsVisibilityRuntimeRules(setting.get("runtimeRules"))
                || containsVisibilityRuntimeRules(mapValue(setting.get("props")).get("runtimeRules"));
    }

    static boolean containsVisibilityRuntimeRules(Object value) {
        if (!(value instanceof List<?> rules)) {
            return false;
        }
        return rules.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> (Map<?, ?>) item)
                .anyMatch(rule -> {
                    Object effectValue = rule.get("effect");
                    if (effectValue instanceof Map<?, ?> effect) {
                        return effect.containsKey("visible") || effect.containsKey("hidden");
                    }
                    return rule.containsKey("visible") || rule.containsKey("hidden");
                });
    }
}
