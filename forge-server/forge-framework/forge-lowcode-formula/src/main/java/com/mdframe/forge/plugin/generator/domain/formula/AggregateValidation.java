package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;

/**
 * Validates aggregate formula configurations.
 * <p>
 * Checks:
 * <ul>
 *   <li>{@link AggregateFunction} is not null</li>
 *   <li>{@code relationCode} is not blank</li>
 *   <li>{@code targetField} is not blank (except for COUNT)</li>
 *   <li>{@code filter} syntax is valid Aviator (if present)</li>
 * </ul>
 * <p>
 * Phase 4 will extend to validate that the relation and target field
 * actually exist in the business object schema.
 */
public class AggregateValidation {

    private final ExpressionParser expressionParser;

    public AggregateValidation() {
        this(new ExpressionParser());
    }

    public AggregateValidation(ExpressionParser expressionParser) {
        this.expressionParser = Objects.requireNonNull(expressionParser, "expressionParser");
    }

    /**
     * Validate an aggregate configuration.
     *
     * @param config the aggregate config to validate
     * @return list of validation errors (empty = valid)
     */
    public List<String> validate(AggregateConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        List<String> errors = new ArrayList<>();

        if (config.getFunction() == null) {
            errors.add("Aggregate function must not be null");
        }
        if (config.getRelationCode() == null || config.getRelationCode().isBlank()) {
            errors.add("Relation code must not be blank");
        }
        if (config.getFunction() != AggregateFunction.COUNT) {
            if (config.getTargetField() == null || config.getTargetField().isBlank()) {
                errors.add("Target field must not be blank for " + config.getFunction());
            }
        }
        if (config.hasFilter()) {
            ExpressionParser.ExpressionParseResult parseResult =
                expressionParser.parse(config.getFilter());
            if (!parseResult.isValid()) {
                errors.add("Filter expression invalid: " + parseResult.getErrorMessage());
            }
        }

        return errors;
    }

    /**
     * Validate and throw if any errors.
     *
     * @throws AggregateValidationException if validation fails
     */
    public void validateOrThrow(AggregateConfig config) {
        List<String> errors = validate(config);
        if (!errors.isEmpty()) {
            throw new AggregateValidationException(
                "Aggregate config validation failed: " + String.join("; ", errors));
        }
    }

    /**
     * Validate an aggregate formula config holistically.
     * Checks both the formula-level config and the aggregate sub-config.
     */
    public List<String> validateFormula(FormulaConfig formulaConfig) {
        Objects.requireNonNull(formulaConfig, "formulaConfig must not be null");
        List<String> errors = new ArrayList<>();

        if (!formulaConfig.isAggregate()) {
            errors.add("Formula type must be AGGREGATE, got: " + formulaConfig.getType());
            return errors;
        }

        AggregateConfig aggConfig = formulaConfig.getAggregate();
        if (aggConfig == null) {
            errors.add("Aggregate config must not be null for AGGREGATE formula");
            return errors;
        }

        errors.addAll(validate(aggConfig));
        return errors;
    }

    // ─── inner types ───

    /**
     * Exception for aggregate config validation failures.
     */
    public static class AggregateValidationException extends RuntimeException {
        public AggregateValidationException(String message) {
            super(message);
        }
    }
}
