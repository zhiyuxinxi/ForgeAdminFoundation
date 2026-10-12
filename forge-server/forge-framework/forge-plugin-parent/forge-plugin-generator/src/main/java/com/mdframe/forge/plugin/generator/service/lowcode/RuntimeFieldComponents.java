package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;

/** Field component types and supported rendering properties. */
final class RuntimeFieldComponents {

    private RuntimeFieldComponents() {
    }

    static String resolveDefaultRenderType(LowcodeFieldSchema field, String componentType) {
        if (field != null && StringUtils.isNotBlank(field.getDictType())) {
            return "dictTag";
        }
        return switch (StringUtils.defaultString(componentType)) {
            case "orgTreeSelect" -> "orgName";
            case "userSelect" -> "userName";
            case "regionTreeSelect" -> "regionName";
            case "fileUpload", "imageUpload" -> componentType;
            default -> "";
        };
    }

    static void applyAlignment(Map<String, Object> item, Map<String, Object> pageSetting) {
        String align = normalizeAlign(StringUtils.defaultIfBlank(text(pageSetting.get("align")),
                text(pageSetting.get("textAlign"))));
        if (StringUtils.isNotBlank(align)) {
            item.put("align", align);
        }
    }

    static String normalizeAlign(String value) {
        String align = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "center", "right").contains(align) ? align : null;
    }

    static String normalizeFixed(String value) {
        String fixed = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        return Set.of("left", "right").contains(fixed) ? fixed : null;
    }

    static Map<String, Object> sanitizeFieldBasicProps(LowcodeFieldSchema field) {
        if (field == null || field.getBasicProps() == null || field.getBasicProps().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> props = new LinkedHashMap<>();
        copyBasicProp(field.getBasicProps(), props, "placeholder");
        copyBasicProp(field.getBasicProps(), props, "cascade");
        copyBasicProp(field.getBasicProps(), props, "cascadeConfig");
        copyBasicProp(field.getBasicProps(), props, "sourceField");
        copyBasicProp(field.getBasicProps(), props, "sourceDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictType");
        copyBasicProp(field.getBasicProps(), props, "linkedDictValue");
        copyBasicProp(field.getBasicProps(), props, "parentDictCode");
        copyBasicProp(field.getBasicProps(), props, "matchMode");
        copyBasicProp(field.getBasicProps(), props, "emptyStrategy");
        copyBasicProp(field.getBasicProps(), props, "clearOnSourceChange");
        copyBasicProp(field.getBasicProps(), props, "clearable");
        copyBasicProp(field.getBasicProps(), props, "filterable");
        copyBasicProp(field.getBasicProps(), props, "multiple");
        copyBasicProp(field.getBasicProps(), props, "optionSource");
        copyBasicProp(field.getBasicProps(), props, "labelValueField");
        copyBasicProp(field.getBasicProps(), props, "targetField");
        copyBasicProp(field.getBasicProps(), props, "rootCode");
        copyBasicProp(field.getBasicProps(), props, "dataRight");
        copyBasicProp(field.getBasicProps(), props, "virtualDisabled");
        copyBasicProp(field.getBasicProps(), props, "limit");
        copyBasicProp(field.getBasicProps(), props, "fileSize");
        copyBasicProp(field.getBasicProps(), props, "fileType");
        copyBasicProp(field.getBasicProps(), props, "storageType");
        copyBasicProp(field.getBasicProps(), props, "valueType");
        copyBasicProp(field.getBasicProps(), props, "showTip");
        copyBasicProp(field.getBasicProps(), props, "showFileList");
        copyBasicProp(field.getBasicProps(), props, "uploadButtonText");
        copyBasicProp(field.getBasicProps(), props, "businessType");
        copyBasicProp(field.getBasicProps(), props, "businessId");
        copyBasicProp(field.getBasicProps(), props, "referenceObjectCode");
        copyBasicProp(field.getBasicProps(), props, "referenceDisplayField");
        copyBasicProp(field.getBasicProps(), props, "referenceValueField");
        copyBasicProp(field.getBasicProps(), props, "targetObjectCode");
        copyBasicProp(field.getBasicProps(), props, "recordSelector");
        copyBasicProp(field.getBasicProps(), props, "recordSelectorConfig");
        copyBasicProp(field.getBasicProps(), props, "selector");
        copyBasicProp(field.getBasicProps(), props, "selectorConfig");
        copyBasicProp(field.getBasicProps(), props, "relationKey");
        copyBasicProp(field.getBasicProps(), props, "inlineCreateEnabled");
        copyBasicProp(field.getBasicProps(), props, "showInDetail");
        copyBasicProp(field.getBasicProps(), props, "validation");
        return props;
    }

    static void copyBasicProp(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    static String resolveSearchComponentType(LowcodeFieldSchema field, String queryType) {
        return resolveSearchComponentType(field, queryType, Map.of());
    }

    static String resolveSearchComponentType(LowcodeFieldSchema field, String queryType, Map<String, Object> pageSetting) {
        if (hasRecordSelectorConfig(field, pageSetting)) {
            return "recordSelector";
        }
        String configuredType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        if (StringUtils.isNotBlank(configuredType)) {
            return normalizeEditComponentType(configuredType);
        }
        String componentType = StringUtils.defaultIfBlank(field.getComponentType(), field.getDataType());
        componentType = StringUtils.defaultIfBlank(componentType, "input");
        componentType = normalizeEditComponentType(componentType);
        if (isBusinessSelectComponent(componentType)) {
            return componentType;
        }
        if (StringUtils.isNotBlank(field.getDictType())) {
            return "select";
        }
        if ("between".equals(queryType)) {
            if ("datetime".equals(componentType)) {
                return "datetimerange";
            }
            if ("date".equals(componentType)) {
                return "daterange";
            }
            if ("time".equals(componentType)) {
                return "timerange";
            }
        }
        if (LowcodeComponentCatalog.isFieldComponent(componentType)) {
            return componentType;
        }
        if ("number".equals(componentType)) {
            return "number";
        }
        if ("date".equals(componentType)) {
            return "date";
        }
        if ("datetime".equals(componentType)) {
            return "datetime";
        }
        if ("time".equals(componentType)) {
            return "time";
        }
        if ("treeSelect".equals(componentType)) {
            return "treeSelect";
        }
        if ("cascader".equals(componentType)) {
            return "cascader";
        }
        return "input";
    }

    static String resolveEditComponentType(LowcodeFieldSchema field) {
        if (hasRecordSelectorConfig(field, Map.of())) {
            return "recordSelector";
        }
        return normalizeEditComponentType(StringUtils.defaultIfBlank(field.getComponentType(), "input"));
    }

    static String resolveEditComponentType(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        if (hasRecordSelectorConfig(field, pageSetting)) {
            return "recordSelector";
        }
        String componentType = StringUtils.defaultIfBlank(text(pageSetting.get("componentType")), text(pageSetting.get("type")));
        componentType = StringUtils.defaultIfBlank(componentType, field.getComponentType());
        return normalizeEditComponentType(StringUtils.defaultIfBlank(componentType, "input"));
    }

    static boolean hasRecordSelectorConfig(LowcodeFieldSchema field, Map<String, Object> pageSetting) {
        if (field != null && field.getBasicProps() != null) {
            Object selector = firstPresent(
                    field.getBasicProps().get("recordSelector"),
                    field.getBasicProps().get("recordSelectorConfig"),
                    field.getBasicProps().get("selector"),
                    field.getBasicProps().get("selectorConfig"));
            if (selector instanceof Map<?, ?> map && !map.isEmpty()) {
                return true;
            }
        }
        Object props = pageSetting == null ? null : pageSetting.get("props");
        if (props instanceof Map<?, ?> propsMap) {
            Object selector = firstPresent(
                    propsMap.get("recordSelector"),
                    propsMap.get("recordSelectorConfig"),
                    propsMap.get("selector"),
                    propsMap.get("selectorConfig"));
            return selector instanceof Map<?, ?> map && !map.isEmpty();
        }
        return false;
    }

    static String normalizeEditComponentType(String componentType) {
        return switch (StringUtils.defaultString(componentType)) {
            case "inputNumber", "input-number", "inputnumber", "integer", "money" -> "number";
            case "orgSelect", "organizationSelect", "departmentSelect", "deptSelect",
                    "departmentTreeSelect", "deptTreeSelect", "elTreeSelect", "orgName", "deptName",
                    "forgeOrgTreeSelect" -> "orgTreeSelect";
            case "userPicker", "user", "userName", "sysUserSelect", "forgeUserSelect" -> "userSelect";
            default -> componentType;
        };
    }

    static void applySelectionLabelProps(Map<String, Object> props, String fieldName, String componentType) {
        if (StringUtils.isBlank(fieldName)
                || (!"orgTreeSelect".equals(componentType) && !"userSelect".equals(componentType))) {
            return;
        }
        String labelField = fieldName + "Name";
        if (StringUtils.isBlank(text(props.get("labelValueField")))
                || fieldName.equals(text(props.get("labelValueField")))) {
            props.put("labelValueField", labelField);
        }
        if (StringUtils.isBlank(text(props.get("targetField")))
                || fieldName.equals(text(props.get("targetField")))) {
            props.put("targetField", labelField);
        }
    }

    static boolean isBusinessSelectComponent(String componentType) {
        return "dictSelect".equals(componentType)
                || "treeSelect".equals(componentType)
                || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType)
                || "regionTreeSelect".equals(componentType)
                || "cascader".equals(componentType)
                || "objectReference".equals(componentType)
                || "recordSelector".equals(componentType);
    }

    static String buildPlaceholder(String componentType, String label) {
        if ("select".equals(componentType) || "radio".equals(componentType) || "radioButton".equals(componentType)
                || "checkbox".equals(componentType) || "transfer".equals(componentType) || "customSelect".equals(componentType)
                || "date".equals(componentType) || "datetime".equals(componentType) || "time".equals(componentType)
                || "daterange".equals(componentType) || "datetimerange".equals(componentType) || "timerange".equals(componentType)
                || "dictSelect".equals(componentType) || "treeSelect".equals(componentType) || "orgTreeSelect".equals(componentType)
                || "userSelect".equals(componentType) || "regionTreeSelect".equals(componentType) || "cascader".equals(componentType)
                || "objectReference".equals(componentType) || "recordSelector".equals(componentType) || "fileUpload".equals(componentType)
                || "imageUpload".equals(componentType) || "upload".equals(componentType)) {
            return "请选择" + label;
        }
        return "请输入" + label;
    }

    static boolean isTextComponent(String componentType) {
        return "input".equals(componentType) || "textarea".equals(componentType);
    }
}
