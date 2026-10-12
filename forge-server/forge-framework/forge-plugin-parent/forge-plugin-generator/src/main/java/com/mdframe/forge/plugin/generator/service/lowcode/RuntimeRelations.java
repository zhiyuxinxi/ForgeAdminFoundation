package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.SYSTEM_COLUMN_NAMES;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.SYSTEM_FIELD_NAMES;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.camelToSnake;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.snakeToCamel;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findField;

/** Reference relationships, joins, translations and lookup sources. */
final class RuntimeRelations {

    private RuntimeRelations() {
    }

    record RelationLookupMeta(String modelCode,
                                      String modelName,
                                      String configKey,
                                      String sourceField,
                                      String targetField,
                                      String displayField) {
    }

    static List<Map<String, Object>> buildJoinConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return List.of();
        }
        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(null);
        String fallbackPrimaryCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), fallbackPrimaryCode);
        if (StringUtils.isBlank(primaryModelCode)) {
            return List.of();
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        List<Map<String, Object>> result = new ArrayList<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            item.put("tableName", ref.getTableName());
            item.put("sourceField", relation.getSourceField());
            item.put("targetField", relation.getTargetField());
            item.put("targetObjectCode", relation.getTargetObjectCode());
            item.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "REFERENCE"));
            LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (primaryRelation != null) {
                String displaySourceField = resolveRelationDisplayField(ref, primaryRelation);
                String relationSourceField = normalizePrimaryFieldName(modelSchema, primaryRelation.getSourceField());
                putIfNotBlank(item, "displayField", displaySourceField);
                putIfNotBlank(item, "displayAlias", buildRelationDisplayAlias(relationSourceField));
            }
            result.add(item);
        }
        return result;
    }

    static LowcodePageModelRef resolvePrimaryRef(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return null;
        }
        return pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElseGet(() -> {
                    String modelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
                    return pageSchema.getModelRefs().stream()
                            .filter(ref -> StringUtils.equals(ref.getModelCode(), pageSchema.getPrimaryModelCode())
                                    || StringUtils.equals(ref.getModelCode(), modelCode))
                            .findFirst()
                            .orElse(pageSchema.getModelRefs().get(0));
                });
    }

    static LowcodeRelationSchema resolveRuntimeRelation(String primaryModelCode,
                                                         LowcodePageModelRef ref,
                                                         List<LowcodeRelationSchema> primaryRelations) {
        if (primaryRelations != null) {
            for (LowcodeRelationSchema relation : primaryRelations) {
                if (relation != null && ref.getModelCode().equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        if (ref.getRelations() != null) {
            for (LowcodeRelationSchema relation : ref.getRelations()) {
                if (relation != null && primaryModelCode.equals(relation.getTargetObjectCode())) {
                    return relation;
                }
            }
        }
        return inferRuntimeRelation(primaryModelCode, ref);
    }

    static LowcodeRelationSchema findRelationFromPrimary(List<LowcodeRelationSchema> relations, String targetModelCode) {
        if (relations == null || StringUtils.isBlank(targetModelCode)) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && targetModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    static LowcodeRelationSchema inferRuntimeRelation(String primaryModelCode, LowcodePageModelRef ref) {
        if (StringUtils.isBlank(primaryModelCode) || ref == null || ref.getFields() == null) {
            return null;
        }
        String expectedCamel = snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = camelToSnake(primaryModelCode) + "_id";
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = StringUtils.defaultIfBlank(text(field.get("columnName")), sourceField);
            if (expectedCamel.equals(sourceField) || expectedSnake.equals(columnName)) {
                LowcodeRelationSchema relation = new LowcodeRelationSchema();
                relation.setRelationType("ONE_TO_MANY");
                relation.setSourceField(sourceField);
                relation.setTargetObjectCode(primaryModelCode);
                relation.setTargetField("id");
                return relation;
            }
        }
        return null;
    }

    static String resolveRefSourceField(LowcodePageModelRef ref, String value) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(value)) {
            return value;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")), sourceField);
            String columnName = text(field.get("columnName"));
            if (value.equals(sourceField)
                    || value.equals(fieldRef)
                    || value.equals(columnName)
                    || value.equals(ref.getModelCode() + "__" + sourceField)
                    || value.equals(ref.getModelCode() + "." + sourceField)) {
                return sourceField;
            }
        }
        return value;
    }

    static Map<String, Object> buildTransConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            String componentType = StringUtils.defaultIfBlank(field.getComponentType(), "input");
            if (StringUtils.isNotBlank(field.getDictType())) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("dictType", field.getDictType());
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("orgTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "orgName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("userSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "userName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("regionTreeSelect".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", "regionName");
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            } else if ("fileUpload".equals(componentType) || "imageUpload".equals(componentType)) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("type", componentType);
                rule.put("targetField", field.getField() + "Name");
                result.put(field.getField(), rule);
            }
        }
        appendRelationDisplayTransConfig(result, modelSchema, pageSchema);
        return result;
    }

    static void appendRelationDisplayTransConfig(Map<String, Object> transConfig,
                                                  LowcodeModelSchema modelSchema,
                                                  LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef == null) {
            return;
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (relation == null || !isReferenceRelation(relation)) {
                continue;
            }
            String sourceField = normalizePrimaryFieldName(modelSchema, relation.getSourceField());
            if (StringUtils.isBlank(sourceField) || findField(modelSchema, sourceField) == null || transConfig.containsKey(sourceField)) {
                continue;
            }
            String displayField = resolveRelationDisplayField(ref, relation);
            if (StringUtils.isBlank(displayField)) {
                continue;
            }
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("type", "relationName");
            rule.put("targetField", buildRelationDisplayAlias(sourceField));
            rule.put("relationModelCode", ref.getModelCode());
            rule.put("displayField", displayField);
            transConfig.put(sourceField, rule);
        }
    }

    static String resolveRelationDisplayField(LowcodePageModelRef ref, LowcodeRelationSchema relation) {
        if (ref == null || ref.getFields() == null || ref.getFields().isEmpty()) {
            return null;
        }
        String configured = resolveRefSourceField(ref, relation == null ? null : relation.getDisplayField());
        if (hasRefSourceField(ref, configured)) {
            return configured;
        }
        Set<String> excluded = new LinkedHashSet<>();
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getTargetField()));
        excluded.add(resolveRefSourceField(ref, relation == null ? null : relation.getSourceField()));

        String matched = pickRefDisplayField(ref, excluded, Set.of("name", "title", "label", "orgName", "deptName"));
        if (StringUtils.isNotBlank(matched)) {
            return matched;
        }
        return pickRefDisplayField(ref, excluded, Set.of());
    }

    static String pickRefDisplayField(LowcodePageModelRef ref, Set<String> excluded, Set<String> preferredNames) {
        if (ref == null || ref.getFields() == null) {
            return null;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (StringUtils.isBlank(sourceField)
                    || excluded.contains(sourceField)
                    || excluded.contains(columnName)
                    || SYSTEM_FIELD_NAMES.contains(sourceField)
                    || SYSTEM_COLUMN_NAMES.contains(columnName)) {
                continue;
            }
            if (!preferredNames.isEmpty() && !preferredNames.contains(sourceField)) {
                continue;
            }
            return sourceField;
        }
        return null;
    }

    static boolean hasRefSourceField(LowcodePageModelRef ref, String sourceField) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(sourceField)) {
            return false;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String candidate = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (sourceField.equals(candidate) || sourceField.equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    static String normalizePrimaryFieldName(LowcodeModelSchema modelSchema, String value) {
        if (StringUtils.isBlank(value) || modelSchema == null || modelSchema.getFields() == null) {
            return value;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (value.equals(field.getField()) || value.equals(field.getColumnName())) {
                return field.getField();
            }
        }
        return value;
    }

    static String buildRelationDisplayAlias(String sourceField) {
        return StringUtils.isBlank(sourceField) ? null : sourceField + "Name";
    }

    static boolean isReferenceRelation(LowcodeRelationSchema relation) {
        return relation != null && "REFERENCE".equalsIgnoreCase(StringUtils.defaultString(relation.getRelationType()));
    }

    static RelationLookupMeta resolveRelationLookup(LowcodeModelSchema modelSchema,
                                                     LowcodePageSchema pageSchema,
                                                     String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null || pageSchema == null
                || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return null;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        if (primaryRef == null) {
            return null;
        }
        List<LowcodeRelationSchema> primaryRelations = primaryRef.getRelations() != null && !primaryRef.getRelations().isEmpty()
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            if (relation == null || !isReferenceRelation(relation)) {
                continue;
            }
            String sourceField = normalizePrimaryFieldName(modelSchema, relation.getSourceField());
            if (!fieldName.equals(sourceField)) {
                continue;
            }
            String displayField = resolveRelationDisplayField(ref, relation);
            Map<String, Object> props = ref.getProps() == null ? Map.of() : ref.getProps();
            return new RelationLookupMeta(
                    ref.getModelCode(),
                    ref.getModelName(),
                    text(props.get("targetConfigKey")),
                    sourceField,
                    StringUtils.defaultIfBlank(relation.getTargetField(), text(props.get("targetField"))),
                    displayField
            );
        }
        return null;
    }

    static Map<String, Object> buildRelationLookupConfig(RelationLookupMeta lookupMeta) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (lookupMeta == null) {
            return config;
        }
        putIfNotBlank(config, "modelCode", lookupMeta.modelCode());
        putIfNotBlank(config, "modelName", lookupMeta.modelName());
        putIfNotBlank(config, "configKey", lookupMeta.configKey());
        putIfNotBlank(config, "sourceField", lookupMeta.sourceField());
        putIfNotBlank(config, "targetField", lookupMeta.targetField());
        putIfNotBlank(config, "displayField", lookupMeta.displayField());
        putIfNotBlank(config, "targetFieldAlias", buildRelationDisplayAlias(lookupMeta.sourceField()));
        return config;
    }

    @SuppressWarnings("unchecked")
    static void applyRelationLookupProps(Map<String, Object> item,
                                          RelationLookupMeta lookupMeta,
                                          String label) {
        if (item == null || lookupMeta == null) {
            return;
        }
        Map<String, Object> props = item.get("props") instanceof Map<?, ?> propsMap
                ? new LinkedHashMap<>((Map<String, Object>) propsMap)
                : new LinkedHashMap<>();
        props.putIfAbsent("clearable", true);
        props.putIfAbsent("filterable", true);
        props.putIfAbsent("placeholder", "请选择" + StringUtils.defaultIfBlank(label, "关联数据"));
        putIfNotBlank(props, "labelValueField", buildRelationDisplayAlias(lookupMeta.sourceField()));
        Map<String, Object> optionSource = buildRelationOptionSource(lookupMeta);
        if (!optionSource.isEmpty()) {
            item.put("optionSource", optionSource);
            props.put("optionSource", optionSource);
        }
        item.put("props", props);
    }

    static Map<String, Object> buildRelationOptionSource(RelationLookupMeta lookupMeta) {
        if (lookupMeta == null || StringUtils.isBlank(lookupMeta.configKey())) {
            return Map.of();
        }
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", "list");
        source.put("api", "get@/ai/crud/" + lookupMeta.configKey() + "/page");
        source.put("recordsField", "records");
        source.put("valueField", StringUtils.defaultIfBlank(lookupMeta.targetField(), "id"));
        source.put("keyField", StringUtils.defaultIfBlank(lookupMeta.targetField(), "id"));
        source.put("labelField", StringUtils.defaultIfBlank(lookupMeta.displayField(), "name"));
        source.put("fallbackLabelFields", List.of(
                StringUtils.defaultIfBlank(lookupMeta.displayField(), "name"),
                "name", "title", "label", "customerName", "contactName", "objectName"
        ));
        source.put("params", Map.of(
                "pageNum", 1,
                "pageSize", 50
        ));
        return source;
    }
}
