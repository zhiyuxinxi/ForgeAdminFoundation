package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateDataException;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateDataProvider;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Database-backed implementation of {@link AggregateDataProvider}.
 * <p>
 * Resolves {@code relationCode} to an {@link AiBusinessObjectRelation} entity,
 * then fetches detail records via {@link DetailRecordFetcher}.
 * <p>
 * Mapping strategy:
 * <pre>
 *   relationCode (String) -> Long.parseLong -> relationId
 *     -> BusinessObjectRelationMapper.selectRelationById(tenantId, relationId)
 *       -> AiBusinessObjectRelation { sourceObjectCode, targetObjectCode,
 *           sourceFieldCode, targetFieldCode, suiteCode }
 *         -> the sourceFieldCode provides the join field into the child table
 *         -> DetailRecordFetcher queries child records matching the join value
 * </pre>
 * <p>
 * This design does NOT assume relationCode == targetObjectCode.
 * The relation is always resolved through AiBusinessObjectRelation.
 */
@Component
public class DbAggregateDataProvider implements AggregateDataProvider {

    private static final Logger log = LoggerFactory.getLogger(DbAggregateDataProvider.class);

    private final BusinessObjectRelationMapper relationMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final AiLowcodeModelMapper lowcodeModelMapper;
    private final DetailRecordFetcher detailRecordFetcher;
    private final ObjectMapper objectMapper;

    public DbAggregateDataProvider(BusinessObjectRelationMapper relationMapper,
                                    BusinessObjectMapper businessObjectMapper,
                                    AiLowcodeModelMapper lowcodeModelMapper,
                                    DetailRecordFetcher detailRecordFetcher,
                                    ObjectMapper objectMapper) {
        this.relationMapper = Objects.requireNonNull(relationMapper);
        this.businessObjectMapper = Objects.requireNonNull(businessObjectMapper);
        this.lowcodeModelMapper = Objects.requireNonNull(lowcodeModelMapper);
        this.detailRecordFetcher = Objects.requireNonNull(detailRecordFetcher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * Fetch detail records for an aggregate formula.
     *
     * @param relationCode identifies the relation (string form of AiBusinessObjectRelation.id)
     * @param context      the current row context (must contain sourceObjectCode, tenantId, etc.)
     * @return list of detail records
     * @throws AggregateDataException if relation cannot be resolved
     */
    @Override
    public List<Map<String, Object>> getDetailRecords(String relationCode,
                                                       Map<String, Object> context) {
        if (relationCode == null || relationCode.isBlank()) {
            throw new AggregateDataException("Aggregate relationCode must not be blank");
        }

        Objects.requireNonNull(context, "context must not be null");

        // 1. Extract runtime context
        FormulaRuntimeContext rtCtx = extractRuntimeContext(context);

        // 2. Look up AiBusinessObjectRelation
        AiBusinessObjectRelation relation = resolveRelation(rtCtx, relationCode);
        if (relation == null) {
            throw new AggregateDataException(
                "Relation not found: tenantId=" + rtCtx.getTenantId()
                    + ", relationCode=" + relationCode);
        }

        // 3. Verify the relation belongs to the current source object
        if (!Objects.equals(relation.getSourceObjectCode(), rtCtx.getSourceObjectCode())) {
            throw new AggregateDataException(
                "Relation " + relation.getId() + " sourceObjectCode mismatch: expected "
                    + rtCtx.getSourceObjectCode() + ", got " + relation.getSourceObjectCode());
        }

        // 4. Get target object -> model -> table name
        AiBusinessObject targetObject = businessObjectMapper.selectByObjectCode(
            rtCtx.getTenantId(), relation.getSuiteCode(), relation.getTargetObjectCode());
        if (targetObject == null) {
            throw new AggregateDataException(
                "Target object not found: " + relation.getTargetObjectCode());
        }

        LowcodeModelSchema targetSchema = resolveModelSchema(rtCtx.getTenantId(), targetObject);
        if (targetSchema == null || targetSchema.getTableName() == null || targetSchema.getTableName().isBlank()) {
            throw new AggregateDataException(
                "Cannot resolve table name for target object: "
                    + relation.getTargetObjectCode());
        }
        String targetTableName = targetSchema.getTableName();
        String targetJoinColumn = resolveColumnName(targetSchema, relation.getTargetFieldCode());

        // 5. Determine join value from current row
        Object joinValue = FormulaFieldAliases.readFieldValue(rtCtx.getCurrentRow(), relation.getSourceFieldCode());

        // 6. Fetch detail records
        log.debug("Fetching detail records: table={}, joinField={}, joinValue={}, tenant={}",
            targetTableName, targetJoinColumn, joinValue, rtCtx.getTenantId());

        List<Map<String, Object>> rows = detailRecordFetcher.fetchDetailRecords(
            targetTableName, targetJoinColumn, joinValue, rtCtx.getTenantId());
        return FormulaFieldAliases.appendFieldAliases(rows);
    }

    private AiBusinessObjectRelation resolveRelation(FormulaRuntimeContext rtCtx, String relationCode) {
        Long relationId = parseLong(relationCode);
        if (relationId != null) {
            return relationMapper.selectRelationById(rtCtx.getTenantId(), relationId);
        }
        List<AiBusinessObjectRelation> relations = relationMapper.selectRuntimeRelationsBySource(
            rtCtx.getTenantId(), rtCtx.getSuiteCode(), rtCtx.getSourceObjectCode());
        if (relations == null || relations.isEmpty()) {
            return null;
        }
        return relations.stream()
            .filter(relation -> relation != null
                && isAggregateRelation(relation)
                && (Objects.equals(relation.getTargetObjectCode(), relationCode)
                || Objects.equals(relation.getRelationName(), relationCode)))
            .findFirst()
            .orElse(null);
    }

    private boolean isAggregateRelation(AiBusinessObjectRelation relation) {
        String relationType = relation.getRelationType();
        return "DETAIL".equalsIgnoreCase(relationType) || "CHILD_LIST".equalsIgnoreCase(relationType);
    }

    /**
     * Resolve the low-code model schema for a business object.
     */
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
            throw new AggregateDataException("Invalid target model schema: " + e.getMessage());
        }
    }

    private String resolveColumnName(LowcodeModelSchema schema, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return fieldName;
        }
        if (schema.getFields() != null) {
            for (LowcodeFieldSchema field : schema.getFields()) {
                if (field != null && FormulaFieldAliases.matchesField(field, fieldName)) {
                    return firstNonBlank(field.getColumnName(), DynamicQueryGenerator.camelToSnake(field.getField()), fieldName);
                }
            }
        }
        return DynamicQueryGenerator.camelToSnake(fieldName);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Extract FormulaRuntimeContext from the context map.
     * The context map is the same one passed through FormulaExecutionEngine.
     */
    private FormulaRuntimeContext extractRuntimeContext(Map<String, Object> context) {
        // Context may carry FormulaRuntimeContext as a special key
        Object ctxObj = context.get("__formulaRuntimeContext__");
        if (ctxObj instanceof FormulaRuntimeContext fc) {
            return fc;
        }

        // Fallback: extract from individual keys
        Long tenantId = toLong(context.get("tenantId"), 1L);
        String suiteCode = Objects.toString(context.get("suiteCode"), "default");
        String sourceObjectCode = Objects.toString(context.get("sourceObjectCode"), "");

        return new FormulaRuntimeContext(tenantId, suiteCode, sourceObjectCode, context);
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
}
