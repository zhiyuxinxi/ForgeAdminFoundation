package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.port.FormulaCrossObjects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves one-hop cross-object formula paths and enriches formula contexts.
 */
@Component
public class FormulaCrossObjectResolver implements FormulaCrossObjects {

    private static final Pattern SAFE_FIELD_CODE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");

    private static final Pattern SQL_FRAGMENT_PATTERN = Pattern.compile(
            "(?i)(;|--|/\\*|\\*/|\\bselect\\b|\\bfrom\\b|\\bwhere\\b|\\bjoin\\b|\\bunion\\b|\\binsert\\b|\\bupdate\\b|\\bdelete\\b|\\bdrop\\b|\\balter\\b)");

    private final BusinessObjectRelationMapper relationMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final DetailRecordFetcher detailRecordFetcher;
    private final ObjectMapper objectMapper;
    private final FormulaReferenceResolver referenceResolver;
    private final String testSourceField;
    private final String testTargetField;

    @Autowired
    public FormulaCrossObjectResolver(BusinessObjectRelationMapper relationMapper,
                                      BusinessObjectMapper businessObjectMapper,
                                      AiLowcodeModelMapper lowcodeModelMapper,
                                      DetailRecordFetcher detailRecordFetcher,
                                      ObjectMapper objectMapper) {
        this.relationMapper = Objects.requireNonNull(relationMapper);
        this.businessObjectMapper = Objects.requireNonNull(businessObjectMapper);
        this.lowcodeModelMapper = Objects.requireNonNull(lowcodeModelMapper);
        this.detailRecordFetcher = Objects.requireNonNull(detailRecordFetcher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.referenceResolver = null;
        this.testSourceField = null;
        this.testTargetField = null;
    }

    public FormulaCrossObjectResolver(String sourceField,
                                      String targetField,
                                      FormulaReferenceResolver referenceResolver) {
        this.relationMapper = null;
        this.businessObjectMapper = null;
        this.lowcodeModelMapper = null;
        this.detailRecordFetcher = null;
        this.objectMapper = null;
        this.referenceResolver = Objects.requireNonNull(referenceResolver);
        this.testSourceField = Objects.requireNonNull(sourceField);
        this.testTargetField = Objects.requireNonNull(targetField);
    }

    public boolean hasCrossObject(Map<String, FormulaConfig> formulaMap) {
        if (formulaMap == null || formulaMap.isEmpty()) {
            return false;
        }
        return formulaMap.values().stream().anyMatch(config -> config != null && config.hasCrossObject());
    }

    public Object resolve(CrossObjectConfig config,
                          Map<String, Object> record,
                          FormulaRuntimeContext context) {
        Map<String, Object> copy = new LinkedHashMap<>(record);
        FormulaConfig formula = FormulaConfig.builder()
                .type(com.mdframe.forge.plugin.generator.domain.formula.FormulaType.CALC)
                .mode(com.mdframe.forge.plugin.generator.domain.formula.FormulaMode.VIRTUAL)
                .expression(config.getPath())
                .crossObject(config)
                .build();
        prefetch(Map.of("_", formula), List.of(copy), context);
        return FormulaFieldAliases.readFieldValue(copy, config.getPath());
    }

    public void prefetch(Map<String, FormulaConfig> formulaMap,
                         List<Map<String, Object>> records,
                         FormulaRuntimeContext context) {
        if (formulaMap == null || formulaMap.isEmpty() || records == null || records.isEmpty()) {
            return;
        }
        for (FormulaConfig formula : formulaMap.values()) {
            if (formula == null || !formula.hasCrossObject()) {
                continue;
            }
            prefetchOne(formula.getCrossObject(), records, context);
        }
    }

    private void prefetchOne(CrossObjectConfig config,
                             List<Map<String, Object>> records,
                             FormulaRuntimeContext context) {
        validateConfig(config);
        CrossObjectPath path = CrossObjectPath.parse(config.getPath());
        validatePath(path);
        if (referenceResolver != null) {
            prefetchWithRows(config, path, records, testSourceField, testTargetField,
                    referenceResolver.fetchTargetRecords(config, collectSourceValues(records, testSourceField),
                            contextMap(context)));
            return;
        }

        FormulaRuntimeContext rtCtx = context == null
                ? new FormulaRuntimeContext(1L, "default", "", Map.of())
                : context;
        AiBusinessObjectRelation relation = resolveRelation(rtCtx, config);
        validateRelation(rtCtx, config, path, relation);
        LowcodeModelSchema targetSchema = resolveTargetSchema(rtCtx, relation);
        String targetColumn = resolveColumnNameStrict(targetSchema, relation.getTargetFieldCode());
        resolveColumnNameStrict(targetSchema, config.getReturnField());
        String targetField = relation.getTargetFieldCode();
        List<Map<String, Object>> rows = detailRecordFetcher.fetchDetailRecordsBatch(
                targetSchema.getTableName(),
                targetColumn,
                collectSourceValues(records, relation.getSourceFieldCode()),
                rtCtx.getTenantId());
        prefetchWithRows(config, path, records, relation.getSourceFieldCode(), targetField, FormulaFieldAliases.appendFieldAliases(rows));
    }

    private void prefetchWithRows(CrossObjectConfig config,
                                  CrossObjectPath path,
                                  List<Map<String, Object>> records,
                                  String sourceField,
                                  String targetField,
                                  List<Map<String, Object>> targetRows) {
        Map<String, Map<String, Object>> targetIndex = new LinkedHashMap<>();
        for (Map<String, Object> row : targetRows == null ? List.<Map<String, Object>>of() : targetRows) {
            Object targetValue = FormulaFieldAliases.readFieldValue(row, targetField);
            if (targetValue != null) {
                targetIndex.putIfAbsent(String.valueOf(targetValue), row);
            }
        }

        for (Map<String, Object> record : records) {
            Object sourceValue = FormulaFieldAliases.readFieldValue(record, sourceField);
            Map<String, Object> targetRow = sourceValue == null ? null : targetIndex.get(String.valueOf(sourceValue));
            Object value = targetRow == null ? null : FormulaFieldAliases.readFieldValue(targetRow, config.getReturnField());
            putCrossObjectValue(record, path, config.getReturnField(), value);
        }
    }

    private Collection<?> collectSourceValues(List<Map<String, Object>> records, String sourceField) {
        Set<Object> values = new LinkedHashSet<>();
        for (Map<String, Object> record : records) {
            Object value = FormulaFieldAliases.readFieldValue(record, sourceField);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private void putCrossObjectValue(Map<String, Object> record,
                                     CrossObjectPath path,
                                     String returnField,
                                     Object value) {
        Map<String, Object> nested = new LinkedHashMap<>();
        Object existing = record.get(path.getRelationAlias());
        if (existing instanceof Map<?, ?> existingMap) {
            nested.putAll((Map<String, Object>) existingMap);
        }
        putFieldAliases(nested, path.getFieldCode(), value);
        putFieldAliases(nested, returnField, value);
        record.put(path.getRelationAlias(), nested);
        record.put(path.asExpressionPath(), value);
    }

    private void putFieldAliases(Map<String, Object> target, String field, Object value) {
        if (field == null || field.isBlank()) {
            return;
        }
        target.put(field, value);
        target.put(DynamicQueryGenerator.snakeToCamel(field), value);
        target.put(DynamicQueryGenerator.camelToSnake(field), value);
    }

    private AiBusinessObjectRelation resolveRelation(FormulaRuntimeContext rtCtx, CrossObjectConfig config) {
        Long relationId = parseLong(config.getRelationCode());
        if (relationId != null) {
            return relationMapper.selectRelationById(rtCtx.getTenantId(), relationId);
        }
        List<AiBusinessObjectRelation> relations = relationMapper.selectRuntimeRelationsBySource(
                rtCtx.getTenantId(), rtCtx.getSuiteCode(), rtCtx.getSourceObjectCode());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        return relations.stream()
                .filter(relation -> relation != null && relationMatches(relation, config))
                .findFirst()
                .orElse(null);
    }

    private boolean relationMatches(AiBusinessObjectRelation relation, CrossObjectConfig config) {
        return Objects.equals(relation.getTargetObjectCode(), config.getTargetObjectCode())
                && (Objects.equals(relation.getTargetObjectCode(), config.getRelationCode())
                || Objects.equals(relation.getRelationName(), config.getRelationCode()));
    }

    private void validateRelation(FormulaRuntimeContext rtCtx,
                                  CrossObjectConfig config,
                                  CrossObjectPath path,
                                  AiBusinessObjectRelation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("Relation not found: " + config.getRelationCode());
        }
        if (!Objects.equals(relation.getSourceObjectCode(), rtCtx.getSourceObjectCode())) {
            throw new IllegalArgumentException("Relation sourceObjectCode mismatch: " + relation.getSourceObjectCode());
        }
        if (!Objects.equals(relation.getTargetObjectCode(), config.getTargetObjectCode())) {
            throw new IllegalArgumentException("Relation targetObjectCode mismatch: " + relation.getTargetObjectCode());
        }
        if (!matchesAlias(path.getRelationAlias(), relation, config)) {
            throw new IllegalArgumentException("Cross-object path alias cannot resolve relation: " + path.getRelationAlias());
        }
        if (!Objects.equals(path.getFieldCode(), config.getReturnField())) {
            throw new IllegalArgumentException("Cross-object path field must match returnField: " + config.getPath());
        }
    }

    private boolean matchesAlias(String alias, AiBusinessObjectRelation relation, CrossObjectConfig config) {
        return Objects.equals(alias, config.getRelationCode())
                || Objects.equals(alias, relation.getRelationName())
                || Objects.equals(alias, relation.getTargetObjectCode());
    }

    private LowcodeModelSchema resolveTargetSchema(FormulaRuntimeContext rtCtx, AiBusinessObjectRelation relation) {
        AiBusinessObject targetObject = businessObjectMapper.selectByObjectCode(
                rtCtx.getTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
        if (targetObject == null) {
            throw new IllegalArgumentException("Target object not found: " + relation.getTargetObjectCode());
        }
        AiLowcodeModel model = null;
        if (targetObject.getModelCode() != null) {
            model = lowcodeModelMapper.selectByModelCode(rtCtx.getTenantId(), targetObject.getModelCode());
        }
        if (model == null && targetObject.getModelId() != null) {
            model = lowcodeModelMapper.selectById(targetObject.getModelId());
        }
        if (model == null) {
            throw new IllegalArgumentException("Target model not found: " + relation.getTargetObjectCode());
        }
        try {
            LowcodeModelSchema schema = objectMapper.readValue(model.getModelSchema(), LowcodeModelSchema.class);
            if (schema.getTableName() == null || schema.getTableName().isBlank()) {
                throw new IllegalArgumentException("Target model tableName is blank: " + relation.getTargetObjectCode());
            }
            return schema;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid target model schema: " + e.getMessage(), e);
        }
    }

    private String resolveColumnNameStrict(LowcodeModelSchema schema, String fieldName) {
        if (schema.getFields() != null) {
            for (LowcodeFieldSchema field : schema.getFields()) {
                if (field != null && FormulaFieldAliases.matchesField(field, fieldName)) {
                    return firstNonBlank(field.getColumnName(), DynamicQueryGenerator.camelToSnake(field.getField()), fieldName);
                }
            }
        }
        throw new IllegalArgumentException("Target field not found in model schema: " + fieldName);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> contextMap(FormulaRuntimeContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (context != null) {
            result.put("tenantId", context.getTenantId());
            result.put("suiteCode", context.getSuiteCode());
            result.put("sourceObjectCode", context.getSourceObjectCode());
            result.putAll(context.getCurrentRow());
        }
        return result;
    }

    private void validateConfig(CrossObjectConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Cross-object config must not be null");
        }
        if (!hasText(config.getPath())) {
            throw new IllegalArgumentException("Cross-object path must not be blank");
        }
        if (!hasText(config.getRelationCode())) {
            throw new IllegalArgumentException("Cross-object relationCode must not be blank");
        }
        if (!hasText(config.getTargetObjectCode())) {
            throw new IllegalArgumentException("Cross-object targetObjectCode must not be blank");
        }
        if (!hasText(config.getReturnField())) {
            throw new IllegalArgumentException("Cross-object returnField must not be blank");
        }
        if (containsSqlFragment(config.getPath())
                || containsSqlFragment(config.getRelationCode())
                || containsSqlFragment(config.getTargetObjectCode())
                || containsSqlFragment(config.getReturnField())) {
            throw new IllegalArgumentException("Cross-object config must not contain SQL fragments");
        }
        if (!isSafeFieldCode(config.getReturnField())) {
            throw new IllegalArgumentException("Cross-object returnField contains illegal field code");
        }
    }

    private void validatePath(CrossObjectPath path) {
        if (!isSafeFieldCode(path.getRelationAlias()) || !isSafeFieldCode(path.getFieldCode())) {
            throw new IllegalArgumentException("Cross-object path contains illegal field code: " + path.asExpressionPath());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsSqlFragment(String value) {
        return value != null && SQL_FRAGMENT_PATTERN.matcher(value).find();
    }

    private boolean isSafeFieldCode(String value) {
        return value != null && SAFE_FIELD_CODE.matcher(value).matches();
    }

    private Long parseLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
