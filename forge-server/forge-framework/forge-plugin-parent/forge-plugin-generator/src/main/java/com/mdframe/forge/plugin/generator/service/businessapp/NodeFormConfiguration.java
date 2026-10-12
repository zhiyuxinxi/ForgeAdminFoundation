package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Canonical node form configuration shared by binding save, read, and task form consumers. */
final class NodeFormConfiguration {

    List<Map<String, Object>> normalizeNodeForms(List<Map<String, Object>> nodeForms) {
        if (nodeForms == null || nodeForms.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> source : nodeForms) {
            if (source == null) {
                continue;
            }
            String taskDefKey = StringUtils.trimToNull(textValue(source.get("taskDefKey")));
            if (taskDefKey == null || !seen.add(taskDefKey)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            putIfText(item, "taskDefKey", taskDefKey);
            putIfText(item, "taskName", source.get("taskName"));
            String formMode = normalizeNodeFormMode(textValue(source.get("formMode")));
            putIfText(item, "formMode", formMode);
            putIfText(item, "formKey", source.get("formKey"));
            putIfText(item, "formName", source.get("formName"));
            putIfText(item, "providerKey", source.get("providerKey"));
            putIfText(item, "formUrl", source.get("formUrl"));
            putIfText(item, "viewKey", source.get("viewKey"));
            putIfText(item, "editMode", normalizeNodeEditMode(textValue(source.get("editMode"))));
            Object formRef = source.get("formRef");
            if (formRef instanceof Map<?, ?> || formRef instanceof JSONObject) {
                item.put("formRef", readNestedObject(formRef));
            }
            List<Map<String, Object>> fieldPermissions = normalizeFieldPermissions(source.get("fieldPermissions"));
            if (fieldPermissions.isEmpty()) {
                fieldPermissions = normalizeFieldSelections(source);
            }
            if (!fieldPermissions.isEmpty()) {
                item.put("fieldPermissions", fieldPermissions);
            }
            result.add(item);
        }
        return result;
    }

    String normalizeNodeFormMode(String formMode) {
        String normalized = StringUtils.defaultIfBlank(formMode, "BUSINESS_OBJECT_FORM").trim().toUpperCase();
        if ("BUSINESS_CODE_FORM".equals(normalized) || "EXTERNAL".equals(normalized)) {
            return normalized;
        }
        return "BUSINESS_OBJECT_FORM";
    }

    String normalizeNodeEditMode(String editMode) {
        String normalized = StringUtils.defaultIfBlank(editMode, "READONLY").trim().toUpperCase();
        if ("EDITABLE".equals(normalized) || "MODIFY_RESUBMIT".equals(normalized)) {
            return normalized;
        }
        return "READONLY";
    }

    List<Map<String, Object>> normalizeFieldPermissions(Object permissions) {
        if (permissions instanceof Map<?, ?> || permissions instanceof JSONObject) {
            JSONObject object = readNestedObject(permissions);
            JSONArray fields = readNestedArray(object.get("fields"));
            if (!fields.isEmpty()) {
                return normalizeFieldPermissions(fields);
            }
            return normalizeFieldSelections(object);
        }
        JSONArray array = readNestedArray(permissions);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject source = array.getJSONObject(i);
            if (source == null) {
                continue;
            }
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(source.getString("field")),
                    StringUtils.trimToNull(source.getString("fieldCode")),
                    StringUtils.trimToNull(source.getString("code")));
            if (field == null || !seen.add(field)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            item.put("fieldCode", field);
            putIfText(item, "label", source.getString("label"));
            boolean readable = readBooleanValue(source.get("readable"), readBooleanValue(source.get("visible"), true));
            boolean writable = readable && readBooleanValue(source.get("writable"), readBooleanValue(source.get("editable"), true));
            applyPermissionFlags(item, readable, writable, readBooleanValue(source.get("required"), false));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> normalizeFieldSelections(Map<String, Object> source) {
        return buildFieldPermissionsFromSelections(
                readFieldSet(source, "visibleFields", "visible", "readableFields", "readable"),
                readFieldSet(source, "writableFields", "writable"),
                readFieldSet(source, "requiredFields", "required"));
    }

    private List<Map<String, Object>> buildFieldPermissionsFromSelections(Set<String> visible,
                                                                          Set<String> writable,
                                                                          Set<String> required) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(visible);
        fields.addAll(writable);
        fields.addAll(required);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String field : fields) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            boolean readable = visible.isEmpty() || visible.contains(field);
            boolean editable = readable && writable.contains(field);
            item.put("fieldCode", field);
            applyPermissionFlags(item, readable, editable, required.contains(field));
            result.add(item);
        }
        return result;
    }

    private Set<String> readFieldSet(Map<String, Object> source, String... keys) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null || keys == null) {
            return result;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            JSONArray array = readNestedArray(value);
            for (int i = 0; i < array.size(); i++) {
                String field = StringUtils.trimToNull(array.getString(i));
                if (field != null) {
                    result.add(field);
                }
            }
        }
        return result;
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

    private JSONArray readNestedArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof List<?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONArray() : JSON.parseArray(text);
            } catch (Exception e) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    private void putIfText(Map<String, Object> target, String key, Object value) {
        String text = StringUtils.trimToNull(value == null ? null : String.valueOf(value));
        if (text != null && !"null".equalsIgnoreCase(text)) {
            target.put(key, text);
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

    private void applyPermissionFlags(Map<String, Object> item, boolean readable,
                                      boolean writable, boolean required) {
        boolean editable = readable && writable;
        item.put("visible", readable);
        item.put("editable", editable);
        item.put("readable", readable);
        item.put("writable", editable);
        item.put("required", editable && required);
    }
}
