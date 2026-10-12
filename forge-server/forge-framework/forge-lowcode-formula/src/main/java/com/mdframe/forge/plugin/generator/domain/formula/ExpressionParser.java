package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Expression parser that bridges Aviator compilation with dependency extraction.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Parse an expression string → extract variable (dependency) names</li>
 *   <li>Validate expression syntax via {@link AviatorAdapter}</li>
 *   <li>Cross-check declared dependencies vs actual expression variables</li>
 * </ul>
 * <p>
 * This is a pure domain service with no Spring dependencies.
 */
public class ExpressionParser {

    private final AviatorAdapter aviator;

    public ExpressionParser() {
        this(new AviatorAdapter());
    }

    public ExpressionParser(AviatorAdapter aviator) {
        this.aviator = Objects.requireNonNull(aviator, "aviator must not be null");
    }

    public void useFunctionRegistry(com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry registry) {
        aviator.useFunctionRegistry(registry);
    }

    /**
     * Parse an expression and return a structured parse result.
     *
     * @param expression the Aviator expression string
     * @return parse result with validity, variables, and any errors
     */
    public ExpressionParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return ExpressionParseResult.error("Expression must not be blank");
        }

        AviatorAdapter.SyntaxValidationResult syntax = aviator.validate(expression);
        if (!syntax.isValid()) {
            return ExpressionParseResult.fromSyntaxError(syntax);
        }

        return ExpressionParseResult.success(syntax.getVariables());
    }

    /**
     * Extract dependency field names from an expression.
     * <p>
     * Returns the list of variable names referenced in the expression,
     * as determined by Aviator's compilation. Empty list if expression is blank.
     *
     * @param expression raw Aviator expression
     * @return sorted list of referenced variable names
     */
    public List<String> extractDependencies(String expression) {
        return aviator.extractVariables(expression);
    }

    /**
     * Cross-check declared dependencies against actual expression variables.
     * <p>
     * Returns warnings for:
     * <ul>
     *   <li>Variables in expression but not declared in dependsOn (potential missing dep)</li>
     *   <li>Variables declared in dependsOn but not found in expression (extraneous dep)</li>
     * </ul>
     *
     * @param declaredDeps dependencies declared in FormulaConfig
     * @param actualVars variables found in expression by Aviator
     * @return list of warning messages (empty if consistent)
     */
    public List<String> crossCheckDependencies(List<String> declaredDeps, List<String> actualVars) {
        List<String> warnings = new ArrayList<>();

        Set<String> declared = new HashSet<>(declaredDeps != null ? declaredDeps : Collections.emptyList());
        Set<String> actual = new HashSet<>(actualVars != null ? actualVars : Collections.emptyList());

        // Variables in expression but not declared
        Set<String> undeclared = new HashSet<>(actual);
        undeclared.removeAll(declared);
        for (String var : undeclared) {
            warnings.add("Variable '" + var + "' found in expression but not declared in dependsOn");
        }

        // Variables declared but not in expression
        Set<String> unused = new HashSet<>(declared);
        unused.removeAll(actual);
        for (String var : unused) {
            warnings.add("Variable '" + var + "' declared in dependsOn but not found in expression");
        }

        return warnings;
    }

    // ─── inner types ───

    /**
     * Result of parsing a single formula expression.
     */
    public static class ExpressionParseResult {
        private final boolean valid;
        private final List<String> variables;
        private final String errorMessage;
        private final Integer errorLine;
        private final Integer errorColumn;

        private ExpressionParseResult(boolean valid, List<String> variables,
                                       String errorMessage, Integer errorLine, Integer errorColumn) {
            this.valid = valid;
            this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
            this.errorMessage = errorMessage;
            this.errorLine = errorLine;
            this.errorColumn = errorColumn;
        }

        static ExpressionParseResult success(List<String> variables) {
            return new ExpressionParseResult(true, variables, null, null, null);
        }

        static ExpressionParseResult error(String message) {
            return new ExpressionParseResult(false, Collections.emptyList(), message, null, null);
        }

        static ExpressionParseResult fromSyntaxError(AviatorAdapter.SyntaxValidationResult syntax) {
            return new ExpressionParseResult(false, Collections.emptyList(),
                syntax.getErrorMessage(),
                syntax.getErrorLine() >= 0 ? syntax.getErrorLine() : null,
                syntax.getErrorColumn() >= 0 ? syntax.getErrorColumn() : null);
        }

        public boolean isValid() { return valid; }
        public List<String> getVariables() { return variables; }
        public String getErrorMessage() { return errorMessage; }
        public Integer getErrorLine() { return errorLine; }
        public Integer getErrorColumn() { return errorColumn; }
        public boolean hasPosition() { return errorLine != null && errorColumn != null; }

        @Override
        public String toString() {
            if (valid) return "ExpressionParseResult{valid=true, vars=" + variables + "}";
            return "ExpressionParseResult{valid=false, error='" + errorMessage + "'}";
        }
    }
}
