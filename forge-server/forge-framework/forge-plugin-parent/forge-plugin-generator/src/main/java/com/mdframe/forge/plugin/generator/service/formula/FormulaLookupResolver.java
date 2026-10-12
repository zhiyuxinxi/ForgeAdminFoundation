package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.port.FormulaLookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Resolves LOOKUP formula values through configured business object relations.
 * <p>
 * The resolver accepts only object metadata from {@link LookupConfig}; table
 * names and SQL are resolved internally from published object/model metadata.
 */
@Component
public class FormulaLookupResolver implements FormulaLookup {

    private static final Pattern SAFE_FIELD_CODE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");

    private static final Pattern SQL_FRAGMENT_PATTERN = Pattern.compile(
        "(?i)(;|--|/\\*|\\*/|\\bselect\\b|\\bfrom\\b|\\bwhere\\b|\\bjoin\\b|\\bunion\\b|\\binsert\\b|\\bupdate\\b|\\bdelete\\b|\\bdrop\\b|\\balter\\b)");

    private final BusinessObjectRelationMapper relationMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final DetailRecordFetcher detailRecordFetcher;
    private final ObjectMapper objectMapper;
    private final LookupRecordProvider recordProvider;

    @Autowired
    public FormulaLookupResolver(BusinessObjectRelationMapper relationMapper,
                                 BusinessObjectMapper businessObjectMapper,
                                 AiLowcodeModelMapper lowcodeModelMapper,
                                 DetailRecordFetcher detailRecordFetcher,
                                 ObjectMapper objectMapper) {
        this.relationMapper = Objects.requireNonNull(relationMapper);
        this.businessObjectMapper = Objects.requireNonNull(businessObjectMapper);
        this.lowcodeModelMapper = Objects.requireNonNull(lowcodeModelMapper);
        this.detailRecordFetcher = Objects.requireNonNull(detailRecordFetcher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.recordProvider = null;
    }

    public FormulaLookupResolver(LookupRecordProvider recordProvider) {
        this.relationMapper = null;
        this.businessObjectMapper = null;
        this.lowcodeModelMapper = null;
        this.detailRecordFetcher = null;
        this.objectMapper = null;
        this.recordProvider = Objects.requireNonNull(recordProvider);
    }

    public static FormulaLookupResolver unsupported() {
        return new FormulaLookupResolver((config, context) -> {
            throw new IllegalStateException("LOOKUP resolver not configured");
        });
    }

    public LookupResolveResult resolve(LookupConfig config, Map<String, Object> context) {
        Objects.requireNonNull(config, "lookup config must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Map<String, Object> metadata = baseMetadata(config);
        String validationError = validateConfig(config);
        if (validationError != null) {
            return LookupResolveResult.failure(validationError, metadata);
        }
        try {
            if (recordProvider != null) {
                return resolveRows(config, context, recordProvider.getTargetRecords(config, context), metadata);
            }
            return resolveFromDatabase(config, context, metadata);
        } catch (Exception e) {
            return LookupResolveResult.failure(e.getMessage(), metadata);
        }
    }

    private LookupResolveResult resolveFromDatabase(LookupConfig config,
                                                   Map<String, Object> context,
                                                   Map<String, Object> metadata) {
        FormulaRuntimeContext rtCtx = extractRuntimeContext(context);
        Map<String, Object> currentRow = effectiveCurrentRow(rtCtx, context);
        AiBusinessObjectRelation relation = resolveRelation(rtCtx, config);
        if (relation == null) {
            return LookupResolveResult.failure("Relation not found: " + config.getRelationCode(), metadata);
        }
        if (!Objects.equals(relation.getSourceObjectCode(), rtCtx.getSourceObjectCode())) {
            return LookupResolveResult.failure(
                "Relation sourceObjectCode mismatch: expected " + rtCtx.getSourceObjectCode()
                    + ", got " + relation.getSourceObjectCode(),
                metadata);
        }
        if (!Objects.equals(relation.getTargetObjectCode(), config.getTargetObjectCode())) {
            return LookupResolveResult.failure(
                "Relation targetObjectCode mismatch: expected " + config.getTargetObjectCode()
                    + ", got " + relation.getTargetObjectCode(),
                metadata);
        }
        if (hasText(relation.getSourceFieldCode())
            && !sameField(relation.getSourceFieldCode(), config.getSourceField())) {
            return LookupResolveResult.failure(
                "Relation sourceField mismatch: expected " + config.getSourceField()
                    + ", got " + relation.getSourceFieldCode(),
                metadata);
        }
        if (hasText(relation.getTargetFieldCode())
            && !sameField(relation.getTargetFieldCode(), config.getTargetField())) {
            return LookupResolveResult.failure(
                "Relation targetField mismatch: expected " + config.getTargetField()
                    + ", got " + relation.getTargetFieldCode(),
                metadata);
        }

        Object sourceValue = FormulaFieldAliases.readFieldValue(currentRow, config.getSourceField());
        metadata.put("sourceValue", sourceValue);
        if (sourceValue == null) {
            metadata.put("matchedRowCount", 0);
            return LookupResolveResult.notFound(config.getNotFoundValue(), metadata);
        }

        AiBusinessObject targetObject = businessObjectMapper.selectByObjectCode(
            rtCtx.getTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
        if (targetObject == null) {
            return LookupResolveResult.failure("Target object not found: " + relation.getTargetObjectCode(), metadata);
        }

        LowcodeModelSchema targetSchema = resolveModelSchema(rtCtx.getTenantId(), targetObject);
        if (targetSchema == null || !hasText(targetSchema.getTableName())) {
            return LookupResolveResult.failure(
                "Cannot resolve table name for target object: " + relation.getTargetObjectCode(),
                metadata);
        }

        String targetMatchColumn = resolveColumnNameStrict(targetSchema, config.getTargetField());
        resolveReturnFieldStrict(targetSchema, config.getReturnField());
        metadata.put("targetMatchColumn", targetMatchColumn);
        List<Map<String, Object>> rows = detailRecordFetcher.fetchDetailRecords(
            targetSchema.getTableName(), targetMatchColumn, sourceValue, rtCtx.getTenantId());
        return resolveRows(config, currentRow, FormulaFieldAliases.appendFieldAliases(rows), metadata);
    }

    private LookupResolveResult resolveRows(LookupConfig config,
                                            Map<String, Object> context,
                                            List<Map<String, Object>> rows,
                                            Map<String, Object> metadata) {
        Object sourceValue = FormulaFieldAliases.readFieldValue(context, config.getSourceField());
        metadata.put("sourceValue", sourceValue);
        if (sourceValue == null) {
            metadata.put("matchedRowCount", 0);
            return LookupResolveResult.notFound(config.getNotFoundValue(), metadata);
        }

        List<Map<String, Object>> candidates = rows == null ? List.of() : rows;
        int matchedCount = 0;
        Object firstValue = null;
        for (Map<String, Object> row : candidates) {
            Object targetValue = FormulaFieldAliases.readFieldValue(row, config.getTargetField());
            if (valuesEqual(sourceValue, targetValue)) {
                matchedCount++;
                if (firstValue == null) {
                    firstValue = FormulaFieldAliases.readFieldValue(row, config.getReturnField());
                }
            }
        }
        metadata.put("matchedRowCount", matchedCount);
        metadata.put("condition", config.getSourceField() + " == " + config.getTargetField());
        if (matchedCount == 0) {
            return LookupResolveResult.notFound(config.getNotFoundValue(), metadata);
        }
        return LookupResolveResult.matched(firstValue, metadata);
    }

    private AiBusinessObjectRelation resolveRelation(FormulaRuntimeContext rtCtx, LookupConfig config) {
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

    private boolean relationMatches(AiBusinessObjectRelation relation, LookupConfig config) {
        return Objects.equals(relation.getTargetObjectCode(), config.getTargetObjectCode())
            && (Objects.equals(relation.getTargetObjectCode(), config.getRelationCode())
            || Objects.equals(relation.getRelationName(), config.getRelationCode()));
    }

    private LowcodeModelSchema resolveModelSchema(Long tenantId, AiBusinessObject targetObject) {
        AiLowcodeModel model = null;
        if (targetObject.getModelCode() != null) {
            model = lowcodeModelMapper.selectByModelCode(tenantId, targetObject.getModelCode());
        }
        if (model == null && targetObject.getModelId() != null) {
            model = lowcodeModelMapper.selectById(targetObject.getModelId());
        }
        if (model == null) return null;
        return parseModelSchema(model.getModelSchema());
    }

    private LowcodeModelSchema parseModelSchema(String modelSchemaJson) {
        if (modelSchemaJson == null || modelSchemaJson.isBlank()) return null;
        try {
            return objectMapper.readValue(modelSchemaJson, LowcodeModelSchema.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid target model schema: " + e.getMessage(), e);
        }
    }

    private String resolveColumnNameStrict(LowcodeModelSchema schema, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Target field must not be blank");
        }
        if ("id".equals(fieldName)) {
            return "id";
        }
        if (schema.getFields() != null) {
            for (LowcodeFieldSchema field : schema.getFields()) {
                if (field != null && FormulaFieldAliases.matchesField(field, fieldName)) {
                    return firstNonBlank(field.getColumnName(), DynamicQueryGenerator.camelToSnake(field.getField()), fieldName);
                }
            }
        }
        throw new IllegalArgumentException("Target field not found in model schema: " + fieldName);
    }

    private void resolveReturnFieldStrict(LowcodeModelSchema schema, String fieldName) {
        resolveColumnNameStrict(schema, fieldName);
    }

    private boolean valuesEqual(Object left, Object right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(String.valueOf(left), String.valueOf(right));
    }

    private FormulaRuntimeContext extractRuntimeContext(Map<String, Object> context) {
        Object ctxObj = context.get("__formulaRuntimeContext__");
        if (ctxObj instanceof FormulaRuntimeContext fc) {
            return fc;
        }
        Long tenantId = toLong(context.get("tenantId"), 1L);
        String suiteCode = Objects.toString(context.get("suiteCode"), "default");
        String sourceObjectCode = Objects.toString(context.get("sourceObjectCode"), "");
        return new FormulaRuntimeContext(tenantId, suiteCode, sourceObjectCode, context);
    }

    private Map<String, Object> effectiveCurrentRow(FormulaRuntimeContext rtCtx, Map<String, Object> context) {
        Map<String, Object> currentRow = new LinkedHashMap<>();
        if (rtCtx != null && rtCtx.getCurrentRow() != null) {
            currentRow.putAll(rtCtx.getCurrentRow());
        }
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().startsWith("__")) {
                currentRow.put(entry.getKey(), entry.getValue());
            }
        }
        return currentRow;
    }

    private Map<String, Object> baseMetadata(LookupConfig config) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("relationCode", config.getRelationCode());
        metadata.put("targetObjectCode", config.getTargetObjectCode());
        metadata.put("sourceField", config.getSourceField());
        metadata.put("targetField", config.getTargetField());
        metadata.put("returnField", config.getReturnField());
        return metadata;
    }

    private String validateConfig(LookupConfig config) {
        if (!hasText(config.getRelationCode())) {
            return "LOOKUP relationCode must not be blank";
        }
        if (!hasText(config.getTargetObjectCode())) {
            return "LOOKUP targetObjectCode must not be blank";
        }
        if (!hasText(config.getSourceField())) {
            return "LOOKUP sourceField must not be blank";
        }
        if (!hasText(config.getTargetField())) {
            return "LOOKUP targetField must not be blank";
        }
        if (!hasText(config.getReturnField())) {
            return "LOOKUP returnField must not be blank";
        }
        if (config.getRelationCode().contains(".")) {
            return "LOOKUP relationCode must reference one configured relation, not a path";
        }
        if (containsSqlFragment(config.getRelationCode())
            || containsSqlFragment(config.getTargetObjectCode())
            || containsSqlFragment(config.getSourceField())
            || containsSqlFragment(config.getTargetField())
            || containsSqlFragment(config.getReturnField())) {
            return "LOOKUP config must not contain SQL fragments";
        }
        if (!isSafeFieldCode(config.getSourceField())
            || !isSafeFieldCode(config.getTargetField())
            || !isSafeFieldCode(config.getReturnField())) {
            return "LOOKUP field config contains illegal field code";
        }
        return null;
    }

    private boolean containsSqlFragment(String value) {
        return value != null && SQL_FRAGMENT_PATTERN.matcher(value).find();
    }

    private boolean isSafeFieldCode(String value) {
        return value != null && SAFE_FIELD_CODE.matcher(value).matches();
    }

    private boolean sameField(String left, String right) {
        return Objects.equals(left, right)
            || Objects.equals(DynamicQueryGenerator.camelToSnake(left), DynamicQueryGenerator.camelToSnake(right))
            || Objects.equals(DynamicQueryGenerator.snakeToCamel(left), DynamicQueryGenerator.snakeToCamel(right));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long parseLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Long toLong(Object val, Long fallback) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    @FunctionalInterface
    public interface LookupRecordProvider {
        List<Map<String, Object>> getTargetRecords(LookupConfig config, Map<String, Object> context);
    }
}
