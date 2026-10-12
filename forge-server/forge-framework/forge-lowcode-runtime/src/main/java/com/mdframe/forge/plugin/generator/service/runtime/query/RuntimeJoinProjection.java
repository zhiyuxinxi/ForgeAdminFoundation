package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeFieldRef;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeChildRelation;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver.findRelationFromPrimary;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver.resolveChildColumn;

/** Builds one plan's primary, child and reference-display projections in deterministic order. */
@lombok.RequiredArgsConstructor
final class RuntimeJoinProjection {
    private final DynamicCrudRepository repository;
    private final Map<String, RuntimeFieldRef> fields = new LinkedHashMap<>();
    private final Map<String, String> fieldColumnMapping = new LinkedHashMap<>();
    private final List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();
    private final Map<String, String> relationDisplayAliases = new LinkedHashMap<>();

    RuntimeJoinContext finish(List<RuntimeChildRelation> childRelations, List<DynamicCrudRepository.JoinSpec> joins,
                              Map<String, String> primaryColumnMapping) {
        return new RuntimeJoinContext(fields, childRelations, selectFields, joins, fieldColumnMapping,
                relationDisplayAliases, primaryColumnMapping);
    }

    void addPrimaryRuntimeFields(String tableName, String modelCode) {
        List<String> columns = new ArrayList<>(repository.getTableColumns(tableName));
        columns.sort(Comparator.naturalOrder());
        for (String column : columns) {
            String fieldName = DynamicQueryGenerator.snakeToCamel(column);
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(fieldName, modelCode, fieldName, column, tableName, "t0", true);
            fields.putIfAbsent(fieldName, fieldRef);
            fieldColumnMapping.put(fieldName, "t0." + column);
            selectFields.add(new DynamicCrudRepository.JoinField(fieldName, "t0", column));
        }
    }

    void addChildRuntimeProjections(ChildRelationResolver.ChildResolution resolved) {
        for (RuntimeFieldRef field : resolved.projections()) {
            fields.putIfAbsent(field.fieldName(), field);
            fieldColumnMapping.put(field.fieldName(), field.tableAlias() + "." + field.columnName());
            selectFields.add(new DynamicCrudRepository.JoinField(field.fieldName(), field.tableAlias(), field.columnName()));
        }
    }

    void addRelationDisplayField(LowcodeModelSchema modelSchema, List<LowcodeRelationSchema> primaryRelations,
                                 LowcodePageModelRef ref, RuntimeChildRelation relation) {
        if (ref == null || relation == null) {
            return;
        }
        LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
        if (primaryRelation == null || !isReferenceRelation(primaryRelation)) {
            return;
        }
        String sourceField = normalizeModelFieldName(modelSchema, primaryRelation.getSourceField());
        if (StringUtils.isBlank(sourceField)) {
            return;
        }
        String displayField = resolveRelationDisplayField(ref, primaryRelation);
        if (StringUtils.isBlank(displayField)) {
            return;
        }
        String aliasField = sourceField + "Name";
        if (fields.containsKey(aliasField)) {
            relationDisplayAliases.putIfAbsent(sourceField, aliasField);
            return;
        }
        Map<String, String> childColumnMapping = repository.getColumnMapping(ref.getTableName());
        String displayColumn = resolveChildColumn(displayField, childColumnMapping);
        if (StringUtils.isBlank(displayColumn)) {
            return;
        }
        RuntimeFieldRef displayRef = new RuntimeFieldRef(
                aliasField,
                ref.getModelCode(),
                displayField,
                displayColumn,
                ref.getTableName(),
                relation.tableAlias(),
                false
        );
        fields.put(aliasField, displayRef);
        fieldColumnMapping.put(aliasField, relation.tableAlias() + "." + displayColumn);
        selectFields.add(new DynamicCrudRepository.JoinField(aliasField, relation.tableAlias(), displayColumn));
        relationDisplayAliases.put(sourceField, aliasField);
    }

    private boolean isReferenceRelation(LowcodeRelationSchema relation) {
        return relation != null && "REFERENCE".equalsIgnoreCase(StringUtils.defaultString(relation.getRelationType()));
    }

    private String normalizeModelFieldName(LowcodeModelSchema modelSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName) || modelSchema == null || modelSchema.getFields() == null) {
            return fieldName;
        }
        return modelSchema.getFields().stream()
                .filter(field -> fieldName.equals(field.getField()) || fieldName.equals(field.getColumnName()))
                .map(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema::getField)
                .findFirst()
                .orElse(fieldName);
    }

    private String resolveRelationDisplayField(LowcodePageModelRef ref, LowcodeRelationSchema relation) {
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
        String preferred = pickRelationDisplayField(ref, excluded, Set.of("name", "title", "label", "orgName", "deptName"));
        if (StringUtils.isNotBlank(preferred)) {
            return preferred;
        }
        return pickRelationDisplayField(ref, excluded, Set.of());
    }

    private String pickRelationDisplayField(LowcodePageModelRef ref, Set<String> excluded, Set<String> preferredNames) {
        if (ref == null || ref.getFields() == null) {
            return null;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (StringUtils.isBlank(sourceField)
                    || excluded.contains(sourceField)
                    || excluded.contains(columnName)
                    || isSystemFieldName(sourceField)
                    || isSystemFieldName(columnName)) {
                continue;
            }
            if (!preferredNames.isEmpty() && !preferredNames.contains(sourceField)) {
                continue;
            }
            return sourceField;
        }
        return null;
    }

    private boolean hasRefSourceField(LowcodePageModelRef ref, String sourceField) {
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

    private String resolveRefSourceField(LowcodePageModelRef ref, String value) {
        if (ref == null || ref.getFields() == null || StringUtils.isBlank(value)) {
            return value;
        }
        for (Map<String, Object> field : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(field.get("sourceField")), text(field.get("field")));
            String columnName = text(field.get("columnName"));
            if (value.equals(sourceField) || value.equals(columnName)) {
                return sourceField;
            }
        }
        return value;
    }

    private boolean isSystemFieldName(String fieldName) {
        return "id".equals(fieldName)
                || "tenantId".equals(fieldName)
                || "tenant_id".equals(fieldName)
                || "createBy".equals(fieldName)
                || "create_by".equals(fieldName)
                || "createTime".equals(fieldName)
                || "create_time".equals(fieldName)
                || "createDept".equals(fieldName)
                || "create_dept".equals(fieldName)
                || "updateBy".equals(fieldName)
                || "update_by".equals(fieldName)
                || "updateTime".equals(fieldName)
                || "update_time".equals(fieldName)
                || "delFlag".equals(fieldName)
                || "del_flag".equals(fieldName);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
