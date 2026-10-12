package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Formula validation service — integrates Phase 1 DependencyAnalyzer
 * with Phase 2A ExpressionParser for end-to-end formula validation.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse each formula's expression via {@link ExpressionParser}</li>
 *   <li>Cross-check declared dependencies against actual expression variables</li>
 *   <li>Run {@link FormulaDependencyAnalyzer} for cycle detection and depth validation</li>
 *   <li>Aggregate all errors into a single {@link FormulaValidationResult}</li>
 * </ul>
 * <p>
 * This is a pure domain service. No Spring, no DB, no API.
 * Designed for direct injection into {@code BusinessObjectPublishService} in Phase 3.
 */
@Component
public class FormulaValidationService {

    private static final Pattern SAFE_FIELD_CODE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");

    private static final Pattern SQL_FRAGMENT_PATTERN = Pattern.compile(
        "(?i)(;|--|/\\*|\\*/|\\bselect\\b|\\bfrom\\b|\\bwhere\\b|\\bjoin\\b|\\bunion\\b|\\binsert\\b|\\bupdate\\b|\\bdelete\\b|\\bdrop\\b|\\balter\\b)");

    private final ExpressionParser parser;
    private final FormulaDependencyAnalyzer dependencyAnalyzer;
    private final FormulaFunctionRegistry functionRegistry;
    private final FormulaFunctionMarketService functionMarketService;

    public FormulaValidationService() {
        this(new ExpressionParser(), new FormulaDependencyAnalyzer(), FormulaFunctionRegistry.builtin());
    }

    @Autowired
    public FormulaValidationService(FormulaFunctionRegistry functionRegistry,
                                    FormulaFunctionMarketService functionMarketService) {
        this(new ExpressionParser(), new FormulaDependencyAnalyzer(), functionRegistry, functionMarketService);
    }

    public FormulaValidationService(ExpressionParser parser, FormulaDependencyAnalyzer dependencyAnalyzer) {
        this(parser, dependencyAnalyzer, FormulaFunctionRegistry.builtin());
    }

    public FormulaValidationService(ExpressionParser parser,
                                    FormulaDependencyAnalyzer dependencyAnalyzer,
                                    FormulaFunctionRegistry functionRegistry) {
        this(parser, dependencyAnalyzer, functionRegistry, null);
    }

    public FormulaValidationService(ExpressionParser parser,
                                    FormulaDependencyAnalyzer dependencyAnalyzer,
                                    FormulaFunctionRegistry functionRegistry,
                                    FormulaFunctionMarketService functionMarketService) {
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.dependencyAnalyzer = Objects.requireNonNull(dependencyAnalyzer, "dependencyAnalyzer must not be null");
        this.functionRegistry = Objects.requireNonNull(functionRegistry, "functionRegistry must not be null");
        this.parser.useFunctionRegistry(functionRegistry);
        this.functionMarketService = functionMarketService;
    }

    /**
     * Validate a complete set of formula configurations for a business object.
     * <p>
     * Performs three levels of validation:
     * <ol>
     *   <li><b>Syntax</b> — each expression compiles without errors</li>
     *   <li><b>Dependency consistency</b> — declared dependsOn matches actual expression variables</li>
     *   <li><b>Structural</b> — no cycles, depth within limit (via FormulaDependencyAnalyzer)</li>
     * </ol>
     *
     * @param formulaMap field name → formula config mapping (one object's formulas)
     * @return aggregated validation result
     */
    public FormulaValidationResult validate(Map<String, FormulaConfig> formulaMap) {
        Objects.requireNonNull(formulaMap, "formulaMap must not be null");

        FormulaValidationResult.Builder result = FormulaValidationResult.builder();

        if (formulaMap.isEmpty()) {
            return result.valid(true).build();
        }


        boolean allSyntaxValid = true;
        List<FormulaValidationResult.FormulaError> formulaErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Level 1 & 2: Syntax + dependency consistency per formula
        for (Map.Entry<String, FormulaConfig> entry : formulaMap.entrySet()) {
            String fieldName = entry.getKey();
            FormulaConfig config = entry.getValue();
            validateCrossObjectConfig(fieldName, config, formulaErrors);

            // Skip formulas without expressions (aggregate formulas may use implicit expressions)
            String expression = config.getExpression();
            if (expression == null || expression.isBlank()) {
                if (config.isLookup()) {
                    validateLookupConfig(fieldName, config, formulaErrors, warnings);
                } else if (!config.isAggregate()) {
                    formulaErrors.add(new FormulaValidationResult.FormulaError(
                        fieldName, "SYNTAX", "Expression must not be blank for non-aggregate formula"));
                    allSyntaxValid = false;
                }
                continue;
            }

            if (config.isLookup()) {
                validateLookupConfig(fieldName, config, formulaErrors, warnings);
                continue;
            }

            List<String> functionErrors = validateFunctionReferences(expression, config.getFunctionRefs());
            if (!functionErrors.isEmpty()) {
                for (String error : functionErrors) {
                    formulaErrors.add(new FormulaValidationResult.FormulaError(
                        fieldName, "FUNCTION", error));
                }
                allSyntaxValid = false;
                continue;
            }

            // Keep managed-function errors classified as FUNCTION before strict compilation.
            ExpressionParser.ExpressionParseResult parseResult = parser.parse(expression);
            if (!parseResult.isValid()) {
                formulaErrors.add(new FormulaValidationResult.FormulaError(
                    fieldName, "SYNTAX", parseResult.getErrorMessage()));
                allSyntaxValid = false;
                continue;
            }

            // Cross-check dependencies
            List<String> actualVars = parseResult.getVariables();
            List<String> declaredDeps = config.getDependsOn();
            List<String> depWarnings = parser.crossCheckDependencies(declaredDeps, actualVars);
            for (String w : depWarnings) {
                warnings.add("[" + fieldName + "] " + w);
            }
        }

        // Level 3: Structural analysis via DependencyAnalyzer
        DependencyAnalysisResult depResult = dependencyAnalyzer.analyze(formulaMap);
        if (depResult.hasCycle()) {
            for (String err : depResult.getErrors()) {
                formulaErrors.add(new FormulaValidationResult.FormulaError(
                    "DAG", "CYCLE", err));
            }
        }
        if (!depResult.isValid() && !depResult.getErrors().isEmpty()) {
            for (String err : depResult.getErrors()) {
                if (!err.contains("循环")) { // avoid duplicating cycle errors
                    formulaErrors.add(new FormulaValidationResult.FormulaError(
                        "DAG", "DEPTH", err));
                }
            }
        }

        boolean valid = allSyntaxValid && depResult.isValid() && formulaErrors.isEmpty();

        return result.valid(valid)
            .dependencyAnalysis(depResult)
            .errors(formulaErrors)
            .warnings(warnings)
            .build();
    }

    private void validateLookupConfig(String fieldName,
                                      FormulaConfig config,
                                      List<FormulaValidationResult.FormulaError> errors,
                                      List<String> warnings) {
        LookupConfig lookup = config.getLookup();
        if (lookup == null) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP formula missing lookup config"));
            return;
        }
        if (!hasText(lookup.getRelationCode())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP relationCode must not be blank"));
        }
        if (!hasText(lookup.getTargetObjectCode())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP targetObjectCode must not be blank"));
        }
        if (!hasText(lookup.getSourceField()) || !hasText(lookup.getTargetField()) || !hasText(lookup.getReturnField())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP sourceField, targetField and returnField must not be blank"));
        }
        if (hasText(lookup.getRelationCode()) && lookup.getRelationCode().contains(".")) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP relationCode must reference one configured relation, not a path"));
        }
        if (containsSqlFragment(lookup.getRelationCode())
            || containsSqlFragment(lookup.getTargetObjectCode())
            || containsSqlFragment(lookup.getSourceField())
            || containsSqlFragment(lookup.getTargetField())
            || containsSqlFragment(lookup.getReturnField())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP config must not contain SQL fragments"));
        }
        if ((hasText(lookup.getSourceField()) && !isSafeFieldCode(lookup.getSourceField()))
            || (hasText(lookup.getTargetField()) && !isSafeFieldCode(lookup.getTargetField()))
            || (hasText(lookup.getReturnField()) && !isSafeFieldCode(lookup.getReturnField()))) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "LOOKUP field config contains illegal field code"));
        }
        if (config.getDependsOn() == null || !config.getDependsOn().contains(lookup.getSourceField())) {
            warnings.add("[" + fieldName + "] LOOKUP sourceField [" + lookup.getSourceField()
                + "] is not declared in dependsOn");
        }
    }

    private void validateCrossObjectConfig(String fieldName,
                                           FormulaConfig config,
                                           List<FormulaValidationResult.FormulaError> errors) {
        if (config == null || !config.hasCrossObject()) {
            return;
        }
        CrossObjectConfig crossObject = config.getCrossObject();
        if (!hasText(crossObject.getPath())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object path must not be blank"));
            return;
        }
        if (!hasText(crossObject.getRelationCode())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object relationCode must not be blank"));
        }
        if (!hasText(crossObject.getTargetObjectCode())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object targetObjectCode must not be blank"));
        }
        if (!hasText(crossObject.getReturnField())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object returnField must not be blank"));
        }
        try {
            CrossObjectPath path = CrossObjectPath.parse(crossObject.getPath());
            if (!isSafeFieldCode(path.getRelationAlias()) || !isSafeFieldCode(path.getFieldCode())) {
                errors.add(new FormulaValidationResult.FormulaError(
                    fieldName, "CONFIG", "Cross-object path contains illegal field code"));
            }
            if (hasText(crossObject.getReturnField()) && !Objects.equals(path.getFieldCode(), crossObject.getReturnField())) {
                errors.add(new FormulaValidationResult.FormulaError(
                    fieldName, "CONFIG", "Cross-object path field must match returnField"));
            }
        } catch (IllegalArgumentException e) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", e.getMessage()));
        }
        if (containsSqlFragment(crossObject.getPath())
            || containsSqlFragment(crossObject.getRelationCode())
            || containsSqlFragment(crossObject.getTargetObjectCode())
            || containsSqlFragment(crossObject.getReturnField())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object config must not contain SQL fragments"));
        }
        if (hasText(crossObject.getReturnField()) && !isSafeFieldCode(crossObject.getReturnField())) {
            errors.add(new FormulaValidationResult.FormulaError(
                fieldName, "CONFIG", "Cross-object returnField contains illegal field code"));
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

    private List<String> validateFunctionReferences(String expression, Collection<String> functionRefs) {
        if (functionMarketService != null) {
            return functionMarketService.validateFunctionReferences(expression, functionRefs);
        }
        return functionRegistry.validateFunctionReferences(expression, functionRefs);
    }

    /**
     * Quick syntax check on a single expression — useful for real-time validation in UI.
     */
    public ExpressionParser.ExpressionParseResult validateExpression(String expression) {
        return parser.parse(expression);
    }
}
