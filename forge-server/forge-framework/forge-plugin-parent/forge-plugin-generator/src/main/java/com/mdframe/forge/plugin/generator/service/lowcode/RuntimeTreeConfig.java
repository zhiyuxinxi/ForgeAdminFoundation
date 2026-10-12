package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findZone;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildEditField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildSearchField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolvePrimaryRef;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolveRefSourceField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolveRuntimeRelation;

/** Tree topology, source fields and runtime selectors. */
final class RuntimeTreeConfig {

    private RuntimeTreeConfig() {
    }

    static boolean isEmbeddedTreeTableRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        return isTreeRuntime(modelSchema, pageSchema) && !"tree-crud".equals(StringUtils.defaultIfBlank(
                pageSchema == null ? null : pageSchema.getLayoutType(), "simple-crud"));
    }

    static boolean isTreeRuntime(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String appType = StringUtils.defaultIfBlank(modelSchema.getAppType(), "SINGLE").toUpperCase(Locale.ROOT);
        return "TREE".equals(appType)
                || (modelSchema.getTreeConfig() != null && Boolean.TRUE.equals(modelSchema.getTreeConfig().getEnabled()))
                || (pageSchema != null && "tree-crud".equals(pageSchema.getLayoutType()))
                || extractTreeConfigOverrides(pageSchema) instanceof Map<?, ?>;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildTreeConfig(LowcodeModelSchema modelSchema,
                                                LowcodePageSchema pageSchema,
                                                Object overrides) {
        Map<String, Object> treeConfig = new LinkedHashMap<>();
        if (modelSchema.getTreeConfig() != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", modelSchema.getTreeConfig().getSourceModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", modelSchema.getTreeConfig().getSourceModelName());
            putIfNotBlank(treeConfig, "sourceTableName", modelSchema.getTreeConfig().getSourceTableName());
            putIfNotBlank(treeConfig, "keyField", modelSchema.getTreeConfig().getKeyField());
            putIfNotBlank(treeConfig, "parentField", modelSchema.getTreeConfig().getParentField());
            putIfNotBlank(treeConfig, "labelField", modelSchema.getTreeConfig().getLabelField());
            putIfNotBlank(treeConfig, "filterField", modelSchema.getTreeConfig().getFilterField());
            putIfNotBlank(treeConfig, "targetField", modelSchema.getTreeConfig().getTargetField());
            putIfNotBlank(treeConfig, "childrenField", modelSchema.getTreeConfig().getChildrenField());
            putIfNotBlank(treeConfig, "treeTitle", modelSchema.getTreeConfig().getTreeTitle());
            putIfNotBlank(treeConfig, "loadMode", modelSchema.getTreeConfig().getLoadMode());
            if (modelSchema.getTreeConfig().getEnabled() != null) {
                treeConfig.put("enabled", modelSchema.getTreeConfig().getEnabled());
            }
        }
        if (overrides instanceof Map<?, ?> map) {
            putIfNotBlank(treeConfig, "sourceModelCode", text(map.get("sourceModelCode")));
            putIfNotBlank(treeConfig, "sourceModelName", text(map.get("sourceModelName")));
            putIfNotBlank(treeConfig, "sourceTableName", text(map.get("sourceTableName")));
            putIfNotBlank(treeConfig, "keyField", text(map.get("keyField")));
            putIfNotBlank(treeConfig, "parentField", text(map.get("parentField")));
            putIfNotBlank(treeConfig, "labelField", text(map.get("labelField")));
            putIfNotBlank(treeConfig, "filterField", text(map.get("filterField")));
            putIfNotBlank(treeConfig, "targetField", text(map.get("targetField")));
            putIfNotBlank(treeConfig, "childrenField", text(map.get("childrenField")));
            putIfNotBlank(treeConfig, "treeTitle", text(map.get("treeTitle")));
            putIfNotBlank(treeConfig, "loadMode", text(map.get("loadMode")));
            if (StringUtils.isBlank(text(treeConfig.get("loadMode")))
                    && map.get("lazy") instanceof Boolean lazy
                    && lazy) {
                treeConfig.put("loadMode", "lazy");
            }
            if (map.get("enabled") instanceof Boolean enabled) {
                treeConfig.put("enabled", enabled);
            }
        }
        LowcodePageModelRef sourceRef = resolveTreeSourceRef(pageSchema, text(treeConfig.get("sourceModelCode")));
        if (sourceRef != null) {
            putIfNotBlank(treeConfig, "sourceModelCode", sourceRef.getModelCode());
            putIfNotBlank(treeConfig, "sourceModelName", sourceRef.getModelName());
            putIfNotBlank(treeConfig, "sourceTableName", sourceRef.getTableName());
            normalizeTreeSourceField(treeConfig, "keyField", sourceRef);
            normalizeTreeSourceField(treeConfig, "parentField", sourceRef);
            normalizeTreeSourceField(treeConfig, "labelField", sourceRef);
            normalizeTreeSourceField(treeConfig, "targetField", sourceRef);
        }
        treeConfig.putIfAbsent("keyField", "id");
        treeConfig.putIfAbsent("parentField", inferTreeParentField(modelSchema, sourceRef));
        treeConfig.putIfAbsent("labelField", inferTreeLabelField(modelSchema, sourceRef));
        LowcodeRelationSchema relation = sourceRef == null || Boolean.TRUE.equals(sourceRef.getPrimary())
                ? null
                : resolveRuntimeRelation(resolveTreePrimaryModelCode(modelSchema, pageSchema),
                sourceRef,
                resolvePrimaryTreeRelations(modelSchema, pageSchema));
        treeConfig.putIfAbsent("filterField", relation == null
                ? text(treeConfig.get("parentField"))
                : relation.getSourceField());
        treeConfig.putIfAbsent("targetField", relation == null
                ? text(treeConfig.get("keyField"))
                : relation.getTargetField());
        normalizePrimaryTreeField(treeConfig, "filterField", modelSchema);
        treeConfig.putIfAbsent("childrenField", "children");
        treeConfig.putIfAbsent("loadMode", "full");
        String defaultTreeTitle = StringUtils.defaultIfBlank(text(treeConfig.get("sourceModelName")), modelSchema.getBusinessName());
        treeConfig.putIfAbsent("treeTitle", StringUtils.isBlank(defaultTreeTitle) ? "树形导航" : defaultTreeTitle + "树");
        return treeConfig;
    }

    static String inferTreeParentField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        return treeSourceFields(modelSchema, sourceRef).stream()
                .filter(field -> "parentId".equals(field) || "pid".equals(field) || "parentCode".equals(field))
                .findFirst()
                .orElse("parentId");
    }

    static String inferTreeLabelField(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        List<String> fields = treeSourceFields(modelSchema, sourceRef);
        return fields.stream()
                .filter(field -> "name".equals(field) || "title".equals(field) || "label".equals(field))
                .findFirst()
                .orElseGet(() -> fields.isEmpty() ? "name" : fields.get(0));
    }

    static LowcodePageModelRef resolveTreeSourceRef(LowcodePageSchema pageSchema, String sourceModelCode) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        if (StringUtils.isNotBlank(sourceModelCode)) {
            for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
                if (ref != null && sourceModelCode.equals(ref.getModelCode())) {
                    return ref;
                }
            }
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> ref != null && !Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> pageSchema.getModelRefs().stream()
                        .filter(ref -> ref != null && Boolean.TRUE.equals(ref.getPrimary()))
                        .findFirst()
                        .orElse(pageSchema.getModelRefs().get(0)));
    }

    static List<String> treeSourceFields(LowcodeModelSchema modelSchema, LowcodePageModelRef sourceRef) {
        if (sourceRef != null && sourceRef.getFields() != null && !sourceRef.getFields().isEmpty()) {
            return sourceRef.getFields().stream()
                    .map(field -> StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field"))))
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        return modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    static void normalizeTreeSourceField(Map<String, Object> treeConfig, String key, LowcodePageModelRef sourceRef) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value) || sourceRef == null || sourceRef.getFields() == null) {
            return;
        }
        String normalized = resolveRefSourceField(sourceRef, value);
        if (StringUtils.isNotBlank(normalized)) {
            treeConfig.put(key, normalized);
        }
    }

    static void normalizePrimaryTreeField(Map<String, Object> treeConfig, String key, LowcodeModelSchema modelSchema) {
        String value = text(treeConfig.get(key));
        if (StringUtils.isBlank(value)) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (value.equals(field.getField()) || value.equals(field.getColumnName())) {
                treeConfig.put(key, field.getField());
                return;
            }
        }
    }

    static String resolveTreePrimaryModelCode(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        String code = pageSchema == null ? null : pageSchema.getPrimaryModelCode();
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && StringUtils.isNotBlank(primaryRef.getModelCode())) {
            return primaryRef.getModelCode();
        }
        return modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
    }

    static List<LowcodeRelationSchema> resolvePrimaryTreeRelations(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef != null && primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()) {
            return primaryRef.getRelations();
        }
        return modelSchema.getRelations();
    }

    static void appendTreeRuntimeField(List<Map<String, Object>> fields,
                                        LowcodeModelSchema modelSchema,
                                        LowcodePageSchema pageSchema,
                                        String zoneKey) {
        if (!isTreeRuntime(modelSchema, pageSchema)) {
            return;
        }
        String filterField = String.valueOf(buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)).get("filterField"));
        boolean exists = fields.stream().anyMatch(item -> filterField.equals(item.get("field"))
                || filterField.equals(item.get("prop"))
                || filterField.equals(item.get("dataIndex"))
                || filterField.equals(item.get("key")));
        if (exists) {
            return;
        }
        LowcodeFieldSchema fieldSchema = findField(modelSchema, filterField);
        if (fieldSchema == null) {
            return;
        }
        Map<String, Object> runtimeField = "edit".equals(zoneKey) ? buildEditField(fieldSchema) : buildSearchField(fieldSchema);
        if (!"edit".equals(zoneKey)) {
            runtimeField.put("hidden", true);
        }
        runtimeField.put("queryType", "eq");
        runtimeField.put("required", false);
        fields.add(runtimeField);
    }

    @SuppressWarnings("unchecked")
    static void decorateTreeRuntimeFields(List<Map<String, Object>> fields,
                                           String configKey,
                                           LowcodeModelSchema modelSchema,
                                           LowcodePageSchema pageSchema) {
        if (!isTreeRuntime(modelSchema, pageSchema) || StringUtils.isBlank(configKey)) {
            return;
        }
        Map<String, Object> treeConfig = buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema));
        String filterField = text(treeConfig.get("filterField"));
        if (StringUtils.isBlank(filterField)) {
            return;
        }
        for (Map<String, Object> item : fields) {
            if (!filterField.equals(text(item.get("field")))) {
                continue;
            }
            String label = StringUtils.defaultIfBlank(text(item.get("label")), filterField);
            item.put("type", "treeSelect");
            item.put("queryType", "eq");
            Map<String, Object> props = new LinkedHashMap<>();
            Object sourceProps = item.get("props");
            if (sourceProps instanceof Map<?, ?> sourcePropsMap) {
                props.putAll((Map<String, Object>) sourcePropsMap);
            }
            props.putIfAbsent("placeholder", "请选择" + label);
            props.putIfAbsent("clearable", true);
            props.putIfAbsent("filterable", true);
            props.put("optionSource", buildTreeOptionSource(configKey, treeConfig));
            item.put("props", props);
        }
    }

    static Map<String, Object> buildTreeOptionSource(String configKey, Map<String, Object> treeConfig) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "tree");
        source.put("api", "get@/ai/crud/" + configKey + "/tree");
        source.put("keyField", "key");
        source.put("valueField", "targetValue");
        source.put("labelField", "label");
        source.put("childrenField", StringUtils.defaultIfBlank(text(treeConfig.get("childrenField")), "children"));
        source.put("params", Map.of("loadMode", "full"));
        return source;
    }

    static Object extractTreeConfigOverrides(LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        if (tableZone != null && tableZone.getProps() != null && tableZone.getProps().get("treeConfig") != null) {
            return tableZone.getProps().get("treeConfig");
        }
        return extractGridTreeConfigOverrides(pageSchema);
    }

    static Object extractGridTreeConfigOverrides(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return null;
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> itemList)) {
            return null;
        }
        for (Object item : itemList) {
            if (!(item instanceof Map<?, ?> block)) {
                continue;
            }
            if (!"tree-panel".equals(String.valueOf(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (props instanceof Map<?, ?>) {
                return props;
            }
        }
        return null;
    }
}
