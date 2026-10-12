package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates formula configurations during business object publishing.
 * <p>
 * Designed for injection into {@code BusinessObjectPublishService.publishCheck()}.
 * Uses LowcodeFieldSchema.formulaConfig (Phase 4B first-class field) to extract
 * formula configs, then runs the full validation pipeline.
 */
@Component
public class FormulaPublishValidator {

    private final FormulaValidationService validationService;
    private final ObjectMapper objectMapper;
    private final FormulaObjectDependencyAnalyzer objectDependencyAnalyzer;
    private final AggregateValidation aggregateValidation = new AggregateValidation();

    @Autowired
    public FormulaPublishValidator(FormulaValidationService validationService,
                                    ObjectMapper objectMapper,
                                    FormulaObjectDependencyAnalyzer objectDependencyAnalyzer) {
        this.validationService = Objects.requireNonNull(validationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.objectDependencyAnalyzer = Objects.requireNonNull(objectDependencyAnalyzer);
    }

    public FormulaPublishValidator(FormulaValidationService validationService,
                                    ObjectMapper objectMapper) {
        this(validationService, objectMapper, new FormulaObjectDependencyAnalyzer());
    }

    /**
     * Extract formula configs from a model schema and validate them.
     *
     * @param modelSchema the published model schema containing field definitions
     * @return validation result (caller checks {@link FormulaValidationResult#isValid()})
     */
    public FormulaValidationResult validate(LowcodeModelSchema modelSchema) {
        Map<String, FormulaConfig> formulaMap = extractFormulas(modelSchema);
        List<FormulaValidationResult.FormulaError> configErrors = collectFormulaConfigErrors(modelSchema);
        if (formulaMap.isEmpty()) {
            FormulaValidationResult result = validationService.validate(formulaMap);
            return configErrors.isEmpty() ? result : mergeErrors(result, configErrors);
        }

        // UAT-05-GAP: validate dependency fields exist in schema
        Set<String> schemaFieldNames = collectFieldNames(modelSchema);
        List<String> depErrors = FormulaDependencyAnalyzer.validateDependencyFields(formulaMap, schemaFieldNames);

        FormulaValidationResult result = validationService.validate(formulaMap);
        if (!configErrors.isEmpty()) {
            return mergeErrors(result, configErrors);
        }

        // Merge dependency errors
        if (!depErrors.isEmpty()) {
            List<FormulaValidationResult.FormulaError> errors = new ArrayList<>();
            for (String depErr : depErrors) {
                errors.add(new FormulaValidationResult.FormulaError("DEPENDENCY", "MISSING_FIELD", depErr));
            }
            return mergeErrors(result, errors);
        }

        // R1: validate AGGREGATE formula configurations
        List<String> aggErrors = validateAggregateFormulas(formulaMap);
        if (!aggErrors.isEmpty()) {
            List<FormulaValidationResult.FormulaError> errors = new ArrayList<>();
            for (String aggErr : aggErrors) {
                errors.add(new FormulaValidationResult.FormulaError("AGGREGATE", "CONFIG", aggErr));
            }
            return mergeErrors(result, errors);
        }

        return result;
    }

    public FormulaValidationResult validate(LowcodeModelSchema modelSchema,
                                            Collection<FormulaObjectDependencyAnalyzer.ObjectContext> objectContexts) {
        FormulaValidationResult result = validate(modelSchema);
        if (objectContexts == null || objectContexts.isEmpty()) {
            return result;
        }

        FormulaObjectDependencyAnalyzer.ObjectDependencyAnalysisResult objectResult =
                objectDependencyAnalyzer.analyze(objectContexts);
        if (objectResult.isValid()) {
            return result;
        }

        List<FormulaValidationResult.FormulaError> errors = new ArrayList<>();
        for (FormulaObjectDependencyAnalyzer.ObjectDependencyError error : objectResult.getErrors()) {
            errors.add(new FormulaValidationResult.FormulaError(
                    error.getFieldName(),
                    error.getCategory(),
                    error.getMessage()));
        }
        return mergeErrors(result, errors);
    }

    public FormulaObjectDependencyAnalyzer.ObjectContext buildObjectContext(
            String objectCode,
            LowcodeModelSchema modelSchema,
            Collection<FormulaObjectDependencyAnalyzer.ObjectRelation> relations) {
        return new FormulaObjectDependencyAnalyzer.ObjectContext(
                objectCode,
                collectFieldNames(modelSchema),
                extractFormulas(modelSchema),
                relations);
    }

    /**
     * Collect all field names from the model schema (for dependency validation).
     */
    private Set<String> collectFieldNames(LowcodeModelSchema modelSchema) {
        Set<String> names = new LinkedHashSet<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema f : modelSchema.getFields()) {
                if (f.getField() != null) {
                    names.add(f.getField());
                }
                if (f.getColumnName() != null) {
                    names.add(f.getColumnName());
                }
            }
        }
        return names;
    }

    /**
     * Validate AGGREGATE formula configurations using {@link AggregateValidation}.
     * Returns error messages for any invalid AGGREGATE configs.
     */
    private List<String> validateAggregateFormulas(Map<String, FormulaConfig> formulaMap) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, FormulaConfig> entry : formulaMap.entrySet()) {
            String fieldName = entry.getKey();
            FormulaConfig config = entry.getValue();
            if (!config.isAggregate()) continue;
            List<String> fieldErrors = aggregateValidation.validateFormula(config);
            for (String err : fieldErrors) {
                errors.add("Aggregate field [" + fieldName + "] " + err);
            }
        }
        return errors;
    }

    /**
     * Extract formula configurations from model schema fields.
     * <p>
     * Uses LowcodeFieldSchema.formulaConfig - the Phase 4B first-class field.
     */
    Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return Collections.emptyMap();
        }

        Map<String, FormulaConfig> formulaMap = new LinkedHashMap<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            FormulaConfig config = parseFormulaConfig(field);
            if (config != null) {
                formulaMap.put(field.getField(), config);
            }
        }
        return formulaMap;
    }

    private List<FormulaValidationResult.FormulaError> collectFormulaConfigErrors(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return List.of();
        }
        List<FormulaValidationResult.FormulaError> errors = new ArrayList<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || field.getFormulaConfig() == null || field.getFormulaConfig().isEmpty()) {
                continue;
            }
            if (parseFormulaConfig(field) == null) {
                errors.add(new FormulaValidationResult.FormulaError(
                        field.getField(), "CONFIG", "Invalid formulaConfig, check type/mode/aggregate/condition settings"));
                continue;
            }
            if (field.getFormulaConfig().containsKey("crossObject")
                    && field.getFormulaConfig().get("crossObject") != null
                    && parseCrossObjectConfig(field.getFormulaConfig().get("crossObject")) == null) {
                errors.add(new FormulaValidationResult.FormulaError(
                        field.getField(), "CONFIG", "Invalid crossObject config, check path/relationCode/targetObjectCode/returnField settings"));
            }
        }
        return errors;
    }

    private FormulaValidationResult mergeErrors(FormulaValidationResult result,
                                                List<FormulaValidationResult.FormulaError> errors) {
        FormulaValidationResult.Builder builder = FormulaValidationResult.builder()
                .valid(false)
                .dependencyAnalysis(result == null ? null : result.getDependencyAnalysis());
        if (result != null) {
            for (FormulaValidationResult.FormulaError error : result.getErrors()) {
                builder.addError(error);
            }
            for (String warning : result.getWarnings()) {
                builder.addWarning(warning);
            }
        }
        for (FormulaValidationResult.FormulaError error : errors) {
            builder.addError(error);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    FormulaConfig parseFormulaConfig(LowcodeFieldSchema field) {
        Map<String, Object> fc = field.getFormulaConfig();
        if (fc == null || fc.isEmpty()) return null;

        try {
            FormulaType type = FormulaType.valueOf(text(fc.get("type"), "CALC"));
            FormulaMode mode = FormulaMode.valueOf(text(fc.get("mode"), "STORED"));
            String expression = text(fc.get("expression"), null);
            List<String> dependsOn = stringList(fc.get("dependsOn"));
            AggregateConfig aggregate = parseAggregateConfig(fc.get("aggregate"));

            FormulaConfig.Builder builder = FormulaConfig.builder()
                .type(type)
                .mode(mode)
                .expression(expression)
                .dependsOn(dependsOn)
                .crossObject(parseCrossObjectConfig(fc.get("crossObject")))
                .rule(objectMap(fc.get("rule")))
                .functionRefs(stringList(fc.get("functionRefs")));

            if (type == FormulaType.AGGREGATE && aggregate != null) {
                builder.aggregate(aggregate);
            }
            if (type == FormulaType.CONDITIONAL) {
                ConditionConfig cc = parseConditionConfig(fc.get("condition"));
                if (cc != null) builder.condition(cc);
            }
            if (type == FormulaType.LOOKUP) {
                LookupConfig lookup = parseLookupConfig(fc.get("lookup"));
                if (lookup != null) builder.lookup(lookup);
            }

            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ConditionConfig parseConditionConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            String expression = text(map.get("expression"), null);
            if (expression == null || expression.isBlank()) return null;
            Object trueValue = map.get("trueValue");
            Object falseValue = map.get("falseValue");
            return new ConditionConfig(expression, trueValue, falseValue);
        } catch (Exception e) {
            return null;
        }
    }

    private AggregateConfig parseAggregateConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            AggregateFunction function = AggregateFunction.valueOf(
                text(map.get("function"), "SUM"));
            String relationCode = text(map.get("relationCode"), null);
            String targetField = text(map.get("targetField"), null);
            String filter = text(map.get("filter"), null);

            if (relationCode == null || targetField == null) return null;
            return new AggregateConfig(function, relationCode, targetField, filter);
        } catch (Exception e) {
            return null;
        }
    }

    private LookupConfig parseLookupConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            return new LookupConfig(
                text(map.get("relationCode"), null),
                text(map.get("targetObjectCode"), null),
                text(map.get("sourceField"), null),
                text(map.get("targetField"), null),
                text(map.get("returnField"), null),
                map.get("notFoundValue"));
        } catch (Exception e) {
            return null;
        }
    }

    private CrossObjectConfig parseCrossObjectConfig(Object raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> map = objectMapper.convertValue(raw,
                new TypeReference<Map<String, Object>>() {});
            CrossObjectRecomputeMode recomputeMode = parseEnum(
                CrossObjectRecomputeMode.class,
                text(map.get("recomputeMode"), null),
                CrossObjectRecomputeMode.ASYNC);
            return new CrossObjectConfig(
                text(map.get("path"), null),
                text(map.get("relationCode"), null),
                text(map.get("targetObjectCode"), null),
                text(map.get("returnField"), null),
                recomputeMode);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> objectMap(Object val) {
        if (val == null) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.convertValue(val, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object val) {
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    private String text(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }
}
