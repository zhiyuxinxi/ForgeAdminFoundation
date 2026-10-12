package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.DependencyAnalysisResult;

import java.util.*;

/**
 * Aggregated result of formula validation for a business object.
 * <p>
 * Contains:
 * <ul>
 *   <li>Overall validity flag</li>
 *   <li>Per-formula errors (syntax, dependency, cycle, depth)</li>
 *   <li>Dependency analysis result (topological order, depth map, cycle info)</li>
 *   <li>Warnings (e.g., dependency mismatch between declared and actual)</li>
 * </ul>
 */
public class FormulaValidationResult {
    private final boolean valid;
    private final List<FormulaError> errors;
    private final List<String> warnings;
    private final DependencyAnalysisResult dependencyAnalysis;

    private FormulaValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
        this.dependencyAnalysis = builder.dependencyAnalysis;
    }

    public boolean isValid() { return valid; }
    public List<FormulaError> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public DependencyAnalysisResult getDependencyAnalysis() { return dependencyAnalysis; }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }

    @Override
    public String toString() {
        return "FormulaValidationResult{valid=" + valid
            + ", errors=" + errors.size()
            + ", warnings=" + warnings.size()
            + ", depAnalysis=" + dependencyAnalysis + "}";
    }

    public static Builder builder() { return new Builder(); }

    // ─── inner types ───

    /**
     * A single formula validation error.
     */
    public static class FormulaError {
        private final String fieldName;
        private final String category;   // SYNTAX, CYCLE, DEPTH, DEPENDENCY
        private final String message;

        public FormulaError(String fieldName, String category, String message) {
            this.fieldName = fieldName;
            this.category = category;
            this.message = message;
        }

        public String getFieldName() { return fieldName; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "[" + category + "] " + fieldName + ": " + message;
        }
    }

    public static class Builder {
        boolean valid;
        List<FormulaError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        DependencyAnalysisResult dependencyAnalysis;

        public Builder valid(boolean v) { this.valid = v; return this; }
        public Builder errors(List<FormulaError> e) { this.errors = e; return this; }
        public Builder addError(FormulaError e) { this.errors.add(e); return this; }
        public Builder warnings(List<String> w) { this.warnings = w; return this; }
        public Builder addWarning(String w) { this.warnings.add(w); return this; }
        public Builder dependencyAnalysis(DependencyAnalysisResult d) { this.dependencyAnalysis = d; return this; }

        public FormulaValidationResult build() { return new FormulaValidationResult(this); }
    }
}