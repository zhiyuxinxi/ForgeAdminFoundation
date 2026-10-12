package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Projects task fields and visible values without querying or mutating business records. */
final class TaskFormProjection {
    void applyPageFormIdentity(BusinessTaskFormContextVO vo, Map<String, Object> formRef) {
        if (vo == null || formRef == null || formRef.isEmpty()) {
            return;
        }
        vo.setApplicationId(StringUtils.trimToNull(textValue(formRef.get("applicationId"))));
        vo.setPageId(StringUtils.trimToNull(textValue(formRef.get("pageId"))));
        vo.setPageCode(StringUtils.trimToNull(textValue(formRef.get("pageCode"))));
        vo.setPageName(StringUtils.trimToNull(textValue(formRef.get("pageName"))));
    }

    void makeBusinessTaskFormReadonly(BusinessTaskFormContextVO context) {
        if (context != null) {
            maskReadonly(context.getFields());
            maskReadonly(context.getFieldPermissions());
        }
    }

    private void maskReadonly(List<Map<String, Object>> fields) {
        if (fields == null) {
            return;
        }
        for (Map<String, Object> field : fields) {
            if (field != null) {
                field.put("writable", false);
                field.put("readonly", true);
                field.put("disabled", true);
            }
        }
    }

    BusinessTaskFormContextVO applyBusinessCodeFieldPermissions(BusinessTaskFormContextVO context,
                                                                        List<Map<String, Object>> permissions) {
        if (context == null) {
            return context;
        }
        Map<String, Map<String, Object>> permissionMap = permissionIndex(permissions == null ? List.of() : permissions);
        List<Map<String, Object>> filteredFields = new ArrayList<>();
        List<Map<String, Object>> sourceFields = context.getFields() == null ? List.of() : context.getFields();
        for (Map<String, Object> source : sourceFields) {
            if (source == null) {
                continue;
            }
            String field = StringUtils.trimToNull(textValue(source.get("field")));
            if (field == null) {
                continue;
            }
            if (readBooleanValue(source.get("internal"), false) || readBooleanValue(source.get("systemField"), false)) {
                continue;
            }
            Map<String, Object> permission = permissionMap.get(field);
            boolean readable = permission != null
                    ? readBooleanValue(permission.get("readable"), true)
                    : true;
            if (!readable) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(source);
            Map<String, Object> props = new LinkedHashMap<>(readNestedObject(item.get("props")));
            applyPermission(item, props, permission, false);
            filteredFields.add(item);
        }
        context.setFields(filteredFields);
        context.setRecordData(filterVisibleRecordData(context.getRecordData(), filteredFields));
        return context;
    }

    List<Map<String, Object>> buildTaskFormFields(List<Map<String, Object>> fieldCatalog,
                                                          List<Map<String, Object>> permissions) {
        Map<String, Map<String, Object>> permissionMap = permissionIndex(permissions);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> field : fieldCatalog) {
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (fieldCode == null) {
                continue;
            }
            Map<String, Object> permission = permissionMap.get(fieldCode);
            boolean readable = permission != null
                    ? readBooleanValue(permission.get("readable"), true)
                    : true;
            if (!readable) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("field", fieldCode);
            item.put("fieldCode", fieldCode);
            item.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
            String rawType = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("type"))),
                    StringUtils.trimToNull(textValue(item.get("componentType"))),
                    StringUtils.trimToNull(textValue(item.get("componentKey"))));
            String normalizedType = normalizeTaskFormFieldType(rawType);
            item.put("type", normalizedType);
            item.putIfAbsent("componentType", normalizedType);
            item.putIfAbsent("dataType", StringUtils.trimToEmpty(textValue(field.get("dataType"))));
            Map<String, Object> props = new LinkedHashMap<>(readNestedObject(item.get("props")));
            String dictType = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(item.get("dictType"))),
                    StringUtils.trimToNull(textValue(props.get("dictType"))));
            if (dictType != null) {
                item.put("dictType", dictType);
            }
            applyPermission(item, props, permission, true);
            if (item.get("dictType") != null) {
                props.put("dictType", item.get("dictType"));
            }
            result.add(item);
        }
        return result;
    }

    String resolveBusinessObjectTaskEditMode(JSONObject nodeForm, List<Map<String, Object>> permissions) {
        if (permissions != null && permissions.stream().anyMatch(item -> readBooleanValue(item.get("writable"), false))) {
            return "EDITABLE";
        }
        return normalizeNodeEditMode(nodeForm == null ? null : nodeForm.getString("editMode"));
    }

    List<Map<String, Object>> normalizeBusinessObjectTaskPermissions(List<Map<String, Object>> fieldCatalog,
                                                                             List<Map<String, Object>> permissions) {
        if (permissions != null && !permissions.isEmpty()) {
            return permissions;
        }
        if (fieldCatalog == null || fieldCatalog.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fieldCatalog) {
            if (field == null) {
                continue;
            }
            String fieldCode = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(textValue(field.get("field"))),
                    StringUtils.trimToNull(textValue(field.get("fieldCode"))));
            if (fieldCode == null || !seen.add(fieldCode)) {
                continue;
            }
            if (readBooleanValue(field.get("internal"), false) || readBooleanValue(field.get("systemField"), false)) {
                continue;
            }
            JSONObject props = readNestedObject(field.get("props"));
            boolean writable = !readBooleanValue(field.get("readonly"), false)
                    && !readBooleanValue(field.get("disabled"), false)
                    && !readBooleanValue(props.get("readonly"), false)
                    && !readBooleanValue(props.get("disabled"), false);
            Map<String, Object> permission = new LinkedHashMap<>();
            permission.put("field", fieldCode);
            permission.put("fieldCode", fieldCode);
            permission.put("label", StringUtils.defaultIfBlank(textValue(field.get("label")), fieldCode));
            permission.put("visible", true);
            permission.put("editable", writable);
            permission.put("readable", true);
            permission.put("writable", writable);
            permission.put("required", writable && readBooleanValue(field.get("required"), false));
            result.add(permission);
        }
        return result;
    }

    String normalizeTaskFormFieldType(String componentType) {
        String type = StringUtils.defaultIfBlank(componentType, "input").trim();
        return switch (type) {
            case "textarea" -> "textarea";
            case "inputNumber", "input-number", "integer", "decimal", "money", "number" -> "number";
            case "dictSelect" -> "dictSelect";
            case "select", "radio", "radioButton", "checkbox", "date", "datetime", "daterange", "datetimerange",
                    "month", "year", "time", "timerange", "switch", "imageUpload", "fileUpload", "slider", "rate",
                    "color", "regionTreeSelect", "treeSelect", "transfer", "customSelect", "objectReference",
                    "recordSelector", "userSelect", "orgTreeSelect", "cascader", "text", "slot" -> type;
            case "upload" -> "fileUpload";
            default -> "input";
        };
    }

    Map<String, Object> filterVisibleRecordData(Map<String, Object> recordData, List<Map<String, Object>> fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (recordData == null || fields == null) {
            return result;
        }
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
            if (fieldCode != null) {
                result.put(fieldCode, readRecordValue(recordData, fieldCode));
            }
            for (String displayField : collectReferenceDisplayFields(field)) {
                Object displayValue = readRecordValue(recordData, displayField);
                if (displayValue != null || containsRecordField(recordData, displayField)) {
                    result.put(displayField, displayValue);
                }
            }
        }
        Object children = recordData.get("children");
        if (children instanceof Map<?, ?> || children instanceof List<?>) {
            result.put("children", children);
        }
        return result;
    }

    private Set<String> collectReferenceDisplayFields(Map<String, Object> field) {
        Set<String> result = new LinkedHashSet<>();
        if (field == null) {
            return result;
        }
        Map<String, Object> props = new LinkedHashMap<>(readNestedObject(field.get("props")));
        boolean selectionField = isSelectionLikeTaskField(field, props);
        addTextFieldName(result, field.get("referenceDisplayField"));
        addTextFieldName(result, field.get("displayField"));
        addTextFieldName(result, field.get("labelField"));
        addTextFieldName(result, field.get("targetLabelField"));
        addTextFieldName(result, field.get("labelValueField"));
        addTextFieldName(result, field.get("targetField"));
        addTextFieldName(result, props.get("referenceDisplayField"));
        addTextFieldName(result, props.get("displayField"));
        addTextFieldName(result, props.get("labelField"));
        addTextFieldName(result, props.get("targetLabelField"));
        addTextFieldName(result, props.get("labelValueField"));
        addTextFieldName(result, props.get("targetField"));
        String fieldCode = StringUtils.trimToNull(textValue(field.get("field")));
        if (selectionField && fieldCode != null) {
            result.add(fieldCode + "Name");
            if (fieldCode.endsWith("Id")) {
                result.add(fieldCode.substring(0, fieldCode.length() - 2) + "Name");
            }
        }
        result.remove(fieldCode);
        return result;
    }

    private boolean isSelectionLikeTaskField(Map<String, Object> field, Map<String, Object> props) {
        String type = normalizeTaskFormFieldType(StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(field.get("type"))),
                StringUtils.trimToNull(textValue(field.get("componentType"))),
                StringUtils.trimToNull(textValue(field.get("componentKey")))));
        return Set.of("objectReference", "recordSelector", "userSelect", "orgTreeSelect",
                        "treeSelect", "cascader", "select", "dictSelect").contains(type)
                || StringUtils.isNotBlank(textValue(field.get("referenceObjectCode")))
                || StringUtils.isNotBlank(textValue(props.get("referenceObjectCode")))
                || StringUtils.isNotBlank(textValue(field.get("referenceDisplayField")))
                || StringUtils.isNotBlank(textValue(props.get("referenceDisplayField")));
    }

    private void addTextFieldName(Set<String> target, Object value) {
        String text = StringUtils.trimToNull(textValue(value));
        if (text != null) {
            target.add(text);
        }
    }

    void putPermissionAliases(Map<String, Map<String, Object>> permissionMap,
                                      String field,
                                      Map<String, Object> permission) {
        if (permissionMap == null || StringUtils.isBlank(field) || permission == null) {
            return;
        }
        permissionMap.putIfAbsent(field, permission);
        String camelField = snakeToCamel(field);
        if (StringUtils.isNotBlank(camelField)) {
            permissionMap.putIfAbsent(camelField, permission);
        }
        String snakeField = camelToSnake(field);
        if (StringUtils.isNotBlank(snakeField)) {
            permissionMap.putIfAbsent(snakeField, permission);
        }
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private boolean readBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    private JSONObject readNestedObject(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONObject() : JSON.parseObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    private String normalizeNodeEditMode(String editMode) {
        String normalized = StringUtils.defaultIfBlank(editMode, "READONLY").trim().toUpperCase();
        if ("EDITABLE".equals(normalized) || "MODIFY_RESUBMIT".equals(normalized)) {
            return normalized;
        }
        return "READONLY";
    }

    private Object readRecordValue(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return null;
        }
        Object value = readRecordValueFromFlatMap(recordData, field);
        if (value != null || containsRecordField(recordData, field)) {
            return value;
        }
        Object main = recordData.get("main");
        if (main instanceof Map<?, ?> mainMap) {
            Map<String, Object> mainRecord = new LinkedHashMap<>();
            mainMap.forEach((key, item) -> {
                if (key != null) {
                    mainRecord.put(String.valueOf(key), item);
                }
            });
            return readRecordValueFromFlatMap(mainRecord, field);
        }
        return null;
    }

    private Object readRecordValueFromFlatMap(Map<String, Object> recordData, String field) {
        if (recordData.containsKey(field)) {
            return recordData.get(field);
        }
        String camelField = snakeToCamel(field);
        if (recordData.containsKey(camelField)) {
            return recordData.get(camelField);
        }
        String snakeField = camelToSnake(field);
        if (recordData.containsKey(snakeField)) {
            return recordData.get(snakeField);
        }
        return null;
    }

    private boolean containsRecordField(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return false;
        }
        return recordData.containsKey(field)
                || recordData.containsKey(snakeToCamel(field))
                || recordData.containsKey(camelToSnake(field));
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private Map<String, Map<String, Object>> permissionIndex(List<Map<String, Object>> permissions) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> permission : permissions) {
            String field = StringUtils.trimToNull(textValue(permission.get("field")));
            if (field != null) {
                putPermissionAliases(result, field, permission);
            }
        }
        return result;
    }

    private void applyPermission(Map<String, Object> field, Map<String, Object> props,
                                 Map<String, Object> permission, boolean projectReadonlyProp) {
        boolean writable = permission != null && readBooleanValue(permission.get("writable"), false);
        field.put("readable", true);
        field.put("writable", writable);
        field.put("required", writable && readBooleanValue(permission.get("required"), false));
        field.put("readonly", !writable);
        field.put("disabled", !writable);
        props.put("disabled", !writable);
        if (projectReadonlyProp) {
            props.put("readonly", !writable);
        }
        field.put("props", props);
    }

}
