package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Field catalogs and layout components for published business forms. */
final class FormAssetFields {
    private static final TaskFormProjection TYPES = new TaskFormProjection();
    private FormAssetFields() {}

    static List<Map<String, Object>> applyRuntimeCrudFormLayout(List<Map<String, Object>> fields, JSONArray layout) {
        if (fields == null || fields.isEmpty() || layout == null || layout.isEmpty()) {
            return fields == null ? List.of() : fields;
        }
        Map<String, Map<String, Object>> fieldMap = new LinkedHashMap<>();
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(FormAssetValues.textValue(field.get("field")));
            if (fieldCode != null) {
                fieldMap.put(fieldCode, field);
            }
        }
        Set<String> usedFields = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> layoutNodes = FormAssetValues.readMapList(layout);
        for (Map<String, Object> node : layoutNodes) {
            Map<String, Object> hydrated = hydrateRuntimeCrudLayoutNode(node, fieldMap, usedFields);
            if (hydrated != null) {
                result.add(hydrated);
            }
        }
        for (Map<String, Object> field : fields) {
            String fieldCode = StringUtils.trimToNull(FormAssetValues.textValue(field.get("field")));
            if (fieldCode != null && !usedFields.contains(fieldCode)) {
                result.add(field);
            }
        }
        return result;
    }

    static Map<String, Object> hydrateRuntimeCrudLayoutNode(Map<String, Object> node,
                                                             Map<String, Map<String, Object>> fieldMap,
                                                             Set<String> usedFields) {
        if (node == null || node.isEmpty()) {
            return null;
        }
        String fieldCode = StringUtils.trimToNull(FormAssetValues.textValue(node.get("field")));
        String nodeType = resolveRuntimeCrudLayoutNodeType(node);
        if (fieldCode != null && ("field".equals(nodeType) || fieldMap.containsKey(fieldCode))) {
            Map<String, Object> field = fieldMap.get(fieldCode);
            if (field == null) {
                return null;
            }
            usedFields.add(fieldCode);
            Map<String, Object> item = new LinkedHashMap<>(field);
            item.put("nodeType", "field");
            item.put("key", StringUtils.defaultIfBlank(FormAssetValues.textValue(node.get("key")), fieldCode));
            if (node.get("span") != null) {
                item.put("span", node.get("span"));
            }
            if (node.get("gridStyle") != null) {
                item.put("gridStyle", node.get("gridStyle"));
            }
            return item;
        }

        List<Map<String, Object>> children = new ArrayList<>();
        for (Map<String, Object> child : FormAssetValues.readMapList(FormAssetValues.readNestedArray(node.get("children")))) {
            Map<String, Object> hydrated = hydrateRuntimeCrudLayoutNode(child, fieldMap, usedFields);
            if (hydrated != null) {
                children.add(hydrated);
            }
        }
        if (children.isEmpty() && !isStandaloneRuntimeCrudLayoutNode(node)) {
            return null;
        }
        Map<String, Object> item = new LinkedHashMap<>(node);
        item.put("nodeType", nodeType);
        item.put("children", children);
        return item;
    }

    static boolean isStandaloneRuntimeCrudLayoutNode(Map<String, Object> node) {
        String key = StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("componentKey"))),
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("type"))),
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("nodeType"))));
        return Set.of("title", "fcTitle", "sectionTitle", "groupTitle", "groupHeader", "GroupHeader",
                "titleBlock", "section", "divider", "elDivider", "AiFormSectionTitle", "aiFormSectionTitle",
                "formSectionTitle", "FormSectionTitle", "button", "table", "tableGrid", "AiCrudPage",
                "aiCrudPage", "crud", "crudBlock").contains(key);
    }

    static String resolveRuntimeCrudLayoutNodeType(Map<String, Object> node) {
        String key = StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("componentKey"))),
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("type"))),
                StringUtils.trimToNull(FormAssetValues.textValue(node.get("nodeType"))));
        if (Set.of("title", "fcTitle", "sectionTitle", "groupTitle", "groupHeader",
                "GroupHeader", "titleBlock", "section").contains(key)) {
            return "groupTitle";
        }
        if (Set.of("divider", "elDivider", "AiFormSectionTitle", "aiFormSectionTitle",
                "formSectionTitle", "FormSectionTitle").contains(key)) {
            return "divider";
        }
        return StringUtils.defaultIfBlank(key, "layout");
    }

    static List<Map<String, Object>> normalizeRuntimeCrudFormFields(List<Map<String, Object>> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> field : fields) {
            Map<String, Object> item = normalizeRuntimeCrudFormField(field);
            String fieldCode = item == null ? null : StringUtils.trimToNull(FormAssetValues.textValue(item.get("field")));
            if (fieldCode != null && seen.add(fieldCode)) {
                result.add(item);
            }
        }
        return result;
    }

    static Map<String, Object> normalizeRuntimeCrudFormField(Map<String, Object> field) {
        if (field == null || field.isEmpty()) {
            return null;
        }
        String fieldCode = StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("field"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("fieldCode"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("prop"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("name"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("model"))));
        if (fieldCode == null
                || FormAssetValues.readBooleanValue(field.get("systemField"), false)
                || FormAssetValues.readBooleanValue(field.get("internal"), false)
                || (field.containsKey("formVisible") && !FormAssetValues.readBooleanValue(field.get("formVisible"), true))) {
            return null;
        }
        JSONObject props = FormAssetValues.readNestedObject(field.get("props"));
        Map<String, Object> item = new LinkedHashMap<>(field);
        item.put("field", fieldCode);
        item.put("fieldCode", fieldCode);
        item.put("label", StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("label"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("title"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("fieldName"))),
                fieldCode));
        String componentType = StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("componentType"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("type"))),
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("componentKey"))),
                "input");
        item.put("type", normalizeTaskFormFieldType(componentType));
        item.put("componentType", componentType);
        if (field.get("dataType") != null) {
            item.put("dataType", FormAssetValues.textValue(field.get("dataType")));
        }
        String dictType = StringUtils.firstNonBlank(
                StringUtils.trimToNull(FormAssetValues.textValue(field.get("dictType"))),
                StringUtils.trimToNull(props.getString("dictType")));
        if (dictType != null) {
            item.put("dictType", dictType);
        }
        item.put("required", FormAssetValues.readBooleanValue(field.get("required"), false));
        item.putIfAbsent("readable", true);
        item.putIfAbsent("writable", !FormAssetValues.readBooleanValue(field.get("readonly"), false));
        return item;
    }

    static JSONObject toRuntimeCrudFormComponent(Map<String, Object> field) {
        JSONObject component = new JSONObject();
        String fieldCode = StringUtils.trimToEmpty(FormAssetValues.textValue(field.get("field")));
        String componentType = StringUtils.defaultIfBlank(FormAssetValues.textValue(field.get("componentType")), "input");
        component.put("id", fieldCode);
        component.put("key", fieldCode);
        component.put("type", componentType);
        component.put("componentType", componentType);
        component.put("label", StringUtils.defaultIfBlank(FormAssetValues.textValue(field.get("label")), fieldCode));
        component.put("field", fieldCode);

        JSONObject binding = new JSONObject();
        binding.put("mode", "field");
        binding.put("fieldCode", fieldCode);
        binding.put("dataType", StringUtils.trimToEmpty(FormAssetValues.textValue(field.get("dataType"))));
        component.put("fieldBinding", binding);

        JSONObject props = FormAssetValues.readNestedObject(field.get("props"));
        props.put("field", fieldCode);
        props.put("label", component.getString("label"));
        if (field.get("dictType") != null) {
            props.put("dictType", field.get("dictType"));
        }
        component.put("props", props);

        JSONObject validation = new JSONObject();
        validation.put("required", FormAssetValues.readBooleanValue(field.get("required"), false));
        component.put("validation", validation);
        return component;
    }

    static List<Map<String, Object>> collectBusinessFormFieldCatalog(JSONObject schema) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        collectBusinessFormFieldComponents(FormAssetValues.readNestedArray(schema.get("components")), result, seen);
        if (result.isEmpty()) {
            JSONArray catalog = FormAssetValues.readNestedArray(schema.get("fieldCatalog"));
            if (catalog.isEmpty()) {
                catalog = FormAssetValues.readNestedArray(schema.get("fields"));
            }
            for (int i = 0; i < catalog.size(); i++) {
                JSONObject field = catalog.getJSONObject(i);
                if (field == null) {
                    continue;
                }
                String code = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("field")),
                        StringUtils.trimToNull(field.getString("fieldCode")),
                        StringUtils.trimToNull(FormAssetValues.readNestedObject(field.get("fieldBinding")).getString("fieldCode")));
                if (code == null || !seen.add(code)) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>(field);
                item.put("field", code);
                item.put("fieldCode", code);
                item.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("label")),
                        StringUtils.trimToNull(field.getString("fieldName")), code));
                item.put("type", normalizeTaskFormFieldType(StringUtils.firstNonBlank(
                        StringUtils.trimToNull(field.getString("type")),
                        StringUtils.trimToNull(field.getString("componentType")), "input")));
                item.putIfAbsent("componentType", item.get("type"));
                result.add(item);
            }
        }
        return result;
    }

    static List<String> buildFieldPreview(List<Map<String, Object>> fields) {
        List<String> preview = new ArrayList<>();
        if (fields == null) {
            return preview;
        }
        for (Map<String, Object> field : fields) {
            String label = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("label"))),
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("fieldName"))),
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("field"))),
                    StringUtils.trimToNull(FormAssetValues.textValue(field.get("fieldCode"))));
            if (label != null) {
                preview.add(label);
            }
            if (preview.size() >= 5) {
                break;
            }
        }
        return preview;
    }

    static void collectBusinessFormFieldComponents(JSONArray components,
                                                    List<Map<String, Object>> result,
                                                    Set<String> seen) {
        if (components == null) {
            return;
        }
        for (int i = 0; i < components.size(); i++) {
            JSONObject component = components.getJSONObject(i);
            if (component == null) {
                continue;
            }
            JSONObject binding = FormAssetValues.readNestedObject(component.get("fieldBinding"));
            JSONObject props = FormAssetValues.readNestedObject(component.get("props"));
            String field = StringUtils.firstNonBlank(
                    StringUtils.trimToNull(binding.getString("fieldCode")),
                    StringUtils.trimToNull(component.getString("field")),
                    StringUtils.trimToNull(props.getString("field")));
            if (field != null && seen.add(field)) {
                JSONObject validation = FormAssetValues.readNestedObject(component.get("validation"));
                Map<String, Object> item = new LinkedHashMap<>(component);
                item.put("field", field);
                item.put("fieldCode", field);
                item.put("label", StringUtils.firstNonBlank(
                        StringUtils.trimToNull(component.getString("label")),
                        StringUtils.trimToNull(props.getString("label")),
                        StringUtils.trimToNull(props.getString("title")),
                        field));
                String componentType = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(component.getString("type")),
                        StringUtils.trimToNull(component.getString("componentType")),
                        StringUtils.trimToNull(component.getString("componentKey")));
                item.put("type", normalizeTaskFormFieldType(componentType));
                item.put("componentType", StringUtils.defaultIfBlank(componentType, "input"));
                item.putIfAbsent("dataType", StringUtils.trimToEmpty(binding.getString("dataType")));
                String dictType = StringUtils.firstNonBlank(
                        StringUtils.trimToNull(FormAssetValues.textValue(item.get("dictType"))),
                        StringUtils.trimToNull(props.getString("dictType")));
                if (dictType != null) {
                    item.put("dictType", dictType);
                }
                item.putIfAbsent("required", FormAssetValues.readBooleanValue(validation.get("required"), false));
                result.add(item);
            }
            collectBusinessFormFieldComponents(FormAssetValues.readNestedArray(component.get("children")), result, seen);
        }
    }

    static String normalizeTaskFormFieldType(String componentType) {
        return TYPES.normalizeTaskFormFieldType(componentType);
    }
}
