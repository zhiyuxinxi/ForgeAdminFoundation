package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.MASTER_DETAIL_LAYOUT;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.SYSTEM_COLUMN_NAMES;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.SYSTEM_FIELD_NAMES;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.camelToSnake;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.safeKey;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.buildPageRefField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findZone;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.isActiveField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.isSystemField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildEditField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolvePrimaryRef;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.resolveRuntimeRelation;

/** Child table contracts from declared parent-child relationships. */
final class RuntimeMasterDetail {

    private RuntimeMasterDetail() {
    }

    static Map<String, Object> buildMasterDetailConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> config = new LinkedHashMap<>();
        LowcodePageModelRef primaryRef = resolvePrimaryRef(modelSchema, pageSchema);
        String primaryModelCode = primaryRef == null
                ? modelSchema.getObject() == null ? null : modelSchema.getObject().getCode()
                : primaryRef.getModelCode();
        primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryModelCode);

        Map<String, Object> primary = new LinkedHashMap<>();
        primary.put("modelCode", primaryModelCode);
        primary.put("modelName", primaryRef == null ? modelSchema.getBusinessName() : primaryRef.getModelName());
        primary.put("tableName", modelSchema.getTableName());
        primary.put("keyField", "id");
        config.put("primary", primary);

        List<Map<String, Object>> children = new ArrayList<>();
        if (StringUtils.isBlank(primaryModelCode) || pageSchema.getModelRefs() == null) {
            config.put("children", children);
            return config;
        }

        List<LowcodeRelationSchema> primaryRelations = primaryRef != null && primaryRef.getRelations() != null
                ? primaryRef.getRelations()
                : modelSchema.getRelations();
        List<String> selectedEditRefs = resolveSelectedEditRefs(pageSchema);
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            LowcodeRelationSchema relation = resolveRuntimeRelation(primaryModelCode, ref, primaryRelations);
            if (relation == null) {
                continue;
            }
            String childFkField = resolveChildRelationField(primaryModelCode, relation);
            List<Map<String, Object>> childFields = buildMasterDetailChildFields(ref, selectedEditRefs, childFkField);
            if (childFields.isEmpty()) {
                continue;
            }
            Map<String, Object> child = new LinkedHashMap<>();
            Map<String, Object> refProps = ref.getProps() == null ? Map.of() : ref.getProps();
            child.put("key", ref.getModelCode());
            child.put("modelCode", ref.getModelCode());
            child.put("modelName", ref.getModelName());
            child.put("tableName", ref.getTableName());
            child.put("relationType", StringUtils.defaultIfBlank(relation.getRelationType(), "ONE_TO_MANY"));
            child.put("relationKey", StringUtils.defaultIfBlank(text(refProps.get("relationKey")), ref.getModelCode()));
            child.put("sourceField", childFkField);
            child.put("targetField", resolveMainRelationField(primaryModelCode, relation));
            child.put("showInCreate", booleanWithDefault(refProps.get("inlineCreateEnabled"), true));
            child.put("showInEdit", booleanWithDefault(refProps.get("inlineEditEnabled"), true));
            child.put("showInDetail", booleanWithDefault(refProps.get("showInDetail"), true));
            child.put("saveMode", normalizeChildSaveMode(refProps.get("saveMode")));
            Object recordSelector = refProps.get("recordSelector");
            if (recordSelector instanceof Map<?, ?> selector && !selector.isEmpty()) {
                child.put("recordSelector", selector);
            }
            Object rowActions = refProps.get("rowActions");
            if (rowActions instanceof List<?> actions && !actions.isEmpty()) {
                child.put("rowActions", actions);
            }
            putIfNotBlank(child, "tabTitle", text(refProps.get("tabTitle")));
            putIfNotBlank(child, "relationName", text(refProps.get("relationName")));
            child.put("fields", childFields);
            children.add(child);
        }
        config.put("children", children);
        return config;
    }

    static String normalizeChildSaveMode(Object value) {
        return "merge".equalsIgnoreCase(text(value)) ? "merge" : "replace";
    }

    static List<Map<String, Object>> buildMasterDetailChildFields(LowcodePageModelRef ref,
                                                                   List<String> selectedEditRefs,
                                                                   String childFkField) {
        if (ref.getFields() == null) {
            return List.of();
        }
        Set<String> selectedEditRefSet = new LinkedHashSet<>(selectedEditRefs);
        boolean hasSelectedChildRefs = ref.getFields().stream()
                .map(source -> StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__"
                                + StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")))))
                .anyMatch(selectedEditRefSet::contains);
        Map<String, Integer> selectedOrder = new LinkedHashMap<>();
        for (int i = 0; i < selectedEditRefs.size(); i++) {
            selectedOrder.putIfAbsent(selectedEditRefs.get(i), i);
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map<String, Object> source : ref.getFields()) {
            LowcodeFieldSchema field = buildMasterDetailChildField(ref, source);
            if (field == null) {
                continue;
            }
            String fieldRef = StringUtils.defaultIfBlank(text(source.get("fieldRef")),
                    safeKey(ref.getModelCode()) + "__" + field.getField());
            if (hasSelectedChildRefs && !selectedEditRefSet.contains(fieldRef)) {
                continue;
            }
            if (!isChildEditFieldAllowed(field, childFkField)) {
                continue;
            }
            Map<String, Object> item = buildEditField(field);
            item.put("sourceField", field.getField());
            item.put("fieldRef", fieldRef);
            item.put("columnName", field.getColumnName());
            item.put("modelCode", ref.getModelCode());
            item.put("modelName", ref.getModelName());
            fields.add(item);
        }
        if (!selectedOrder.isEmpty()) {
            fields.sort(Comparator.comparingInt(item ->
                    selectedOrder.getOrDefault(text(item.get("fieldRef")), Integer.MAX_VALUE)));
        }
        return fields;
    }

    static LowcodeFieldSchema buildMasterDetailChildField(LowcodePageModelRef ref, Map<String, Object> source) {
        LowcodeFieldSchema field = buildPageRefField(ref, source);
        if (field == null) {
            return null;
        }
        String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
        field.setField(sourceField);
        field.setLabel(StringUtils.defaultIfBlank(text(source.get("rawLabel")),
                StringUtils.defaultIfBlank(text(source.get("label")), sourceField)));
        return field;
    }

    static boolean isChildEditFieldAllowed(LowcodeFieldSchema field, String childFkField) {
        if (field == null) {
            return false;
        }
        String fieldName = field.getField();
        String columnName = field.getColumnName();
        if (StringUtils.equals(fieldName, childFkField) || StringUtils.equals(columnName, childFkField)
                || StringUtils.equals(columnName, camelToSnake(childFkField))) {
            return false;
        }
        return !isSystemField(field)
                && isActiveField(field)
                && !SYSTEM_FIELD_NAMES.contains(fieldName)
                && !SYSTEM_COLUMN_NAMES.contains(columnName)
                && !Boolean.TRUE.equals(field.getReadonly())
                && !Boolean.TRUE.equals(field.getPrimaryKey())
                && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible()));
    }

    static List<String> resolveSelectedEditRefs(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || Boolean.FALSE.equals(editZone.getEnabled()) || editZone.getFieldRefs() == null) {
            return List.of();
        }
        return editZone.getFieldRefs().stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    static String resolveChildRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getSourceField()
                : relation.getTargetField();
    }

    static String resolveMainRelationField(String primaryModelCode, LowcodeRelationSchema relation) {
        if (relation == null) {
            return null;
        }
        return primaryModelCode.equals(relation.getTargetObjectCode())
                ? relation.getTargetField()
                : relation.getSourceField();
    }

    static boolean isMasterDetailRuntime(LowcodePageSchema pageSchema) {
        return pageSchema != null && MASTER_DETAIL_LAYOUT.equals(pageSchema.getLayoutType());
    }

    static Set<String> buildChildFieldRefs(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return Set.of();
        }
        Set<String> refs = new LinkedHashSet<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || ref.getFields() == null) {
                continue;
            }
            for (Map<String, Object> field : ref.getFields()) {
                String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
                String fieldRef = StringUtils.defaultIfBlank(text(field.get("fieldRef")),
                        safeKey(ref.getModelCode()) + "__" + sourceField);
                if (StringUtils.isNotBlank(fieldRef)) {
                    refs.add(fieldRef);
                }
            }
        }
        return refs;
    }
}
