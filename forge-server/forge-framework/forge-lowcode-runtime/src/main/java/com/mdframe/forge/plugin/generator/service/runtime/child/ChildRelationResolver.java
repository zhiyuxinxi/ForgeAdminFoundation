package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compiles published model relationships and child field rules into runtime metadata. */
public final class ChildRelationResolver {
    private final DynamicCrudRepository repository;

    public ChildRelationResolver(DynamicCrudRepository repository) {
        this.repository = repository;
    }

    public record ChildResolution(RuntimeChildRelation relation, List<RuntimeFieldRef> projections) {
    }

    public ChildResolution buildChildRelation(String primaryModelCode,
                                                    LowcodePageModelRef ref,
                                                    List<LowcodeRelationSchema> primaryRelations,
                                                    String tableAlias,
                                                    Map<String, String> primaryColumnMapping) {
        Map<String, String> childColumnMapping = repository.getColumnMapping(ref.getTableName());
        LowcodeRelationSchema relation = findRelationToPrimary(ref.getRelations(), primaryModelCode);
        boolean relationFromPrimary = false;
        if (relation == null) {
            LowcodeRelationSchema primaryRelation = findRelationFromPrimary(primaryRelations, ref.getModelCode());
            LowcodeRelationSchema inferredRelation = inferRelation(primaryModelCode, ref);
            if (shouldPreferInferredChildRelation(primaryRelation, inferredRelation, childColumnMapping)) {
                relation = inferredRelation;
            } else {
                relation = primaryRelation != null ? primaryRelation : inferredRelation;
                relationFromPrimary = primaryRelation != null && relation == primaryRelation;
            }
        }
        if (relation == null) {
            return null;
        }

        String mainField = relationFromPrimary ? relation.getSourceField() : relation.getTargetField();
        String childField = relationFromPrimary ? relation.getTargetField() : relation.getSourceField();
        String mainColumn = resolvePrimaryColumn(mainField, primaryColumnMapping);
        String childColumn = resolveChildColumn(childField, childColumnMapping);
        if (StringUtils.isBlank(mainColumn) || StringUtils.isBlank(childColumn)) {
            return null;
        }

        Map<String, RuntimeFieldRef> childFields = new LinkedHashMap<>();
        List<RuntimeFieldRef> projections = new ArrayList<>();
        Map<String, RuntimeChildFieldRule> childFieldRules = new LinkedHashMap<>();
        for (Map<String, Object> source : ref.getFields()) {
            String sourceField = StringUtils.defaultIfBlank(text(source.get("sourceField")), text(source.get("field")));
            if (StringUtils.isBlank(sourceField)) {
                continue;
            }
            String fieldName = StringUtils.defaultIfBlank(text(source.get("fieldRef")), safeKey(ref.getModelCode()) + "__" + sourceField);
            String columnName = resolveChildColumn(StringUtils.defaultIfBlank(text(source.get("columnName")), sourceField), childColumnMapping);
            if (StringUtils.isBlank(columnName)) {
                continue;
            }
            RuntimeFieldRef fieldRef = new RuntimeFieldRef(
                    fieldName, ref.getModelCode(), sourceField, columnName, ref.getTableName(), tableAlias, false);
            projections.add(fieldRef);
            childFields.put(fieldName, fieldRef);
            childFieldRules.put(fieldName, RuntimeChildFieldRule.from(source, fieldRef));
        }
        if (childFields.isEmpty()) {
            return null;
        }
        String saveMode = normalizeChildSaveMode(ref.getProps() == null ? null : ref.getProps().get("saveMode"));
        RuntimeChildRelation resolved = new RuntimeChildRelation(ref.getModelCode(), ref.getTableName(), tableAlias,
                childColumn, mainColumn, saveMode, childFields, childFieldRules);
        return new ChildResolution(resolved, projections);
    }

    private String normalizeChildSaveMode(Object value) {
        return "merge".equalsIgnoreCase(text(value)) ? "merge" : "replace";
    }

    private boolean shouldPreferInferredChildRelation(LowcodeRelationSchema primaryRelation,
                                                      LowcodeRelationSchema inferredRelation,
                                                      Map<String, String> childColumnMapping) {
        if (primaryRelation == null || inferredRelation == null) {
            return false;
        }
        String configuredChildColumn = resolveChildColumn(primaryRelation.getTargetField(), childColumnMapping);
        String inferredChildColumn = resolveChildColumn(inferredRelation.getSourceField(), childColumnMapping);
        return "id".equals(configuredChildColumn) && StringUtils.isNotBlank(inferredChildColumn)
                && !"id".equals(inferredChildColumn);
    }

    public static String resolvePrimaryColumn(String fieldName, Map<String, String> primaryColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = primaryColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return primaryColumnMapping.containsValue(column) ? column : null;
    }

    public static String resolveChildColumn(String fieldName, Map<String, String> childColumnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = childColumnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return childColumnMapping.containsValue(column) ? column : null;
    }

    public static LowcodeRelationSchema findRelationFromPrimary(List<LowcodeRelationSchema> relations, String targetModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && targetModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema findRelationToPrimary(List<LowcodeRelationSchema> relations, String primaryModelCode) {
        if (relations == null) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && primaryModelCode.equals(relation.getTargetObjectCode()))
                .findFirst()
                .orElse(null);
    }

    private LowcodeRelationSchema inferRelation(String primaryModelCode, LowcodePageModelRef ref) {
        String expectedCamel = DynamicQueryGenerator.snakeToCamel(primaryModelCode) + "Id";
        String expectedSnake = DynamicQueryGenerator.camelToSnake(primaryModelCode) + "_id";
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

    public static List<LowcodeRelationSchema> mergeRelations(List<LowcodeRelationSchema> first, List<LowcodeRelationSchema> second) {
        List<LowcodeRelationSchema> result = new ArrayList<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }

    private String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }


}
