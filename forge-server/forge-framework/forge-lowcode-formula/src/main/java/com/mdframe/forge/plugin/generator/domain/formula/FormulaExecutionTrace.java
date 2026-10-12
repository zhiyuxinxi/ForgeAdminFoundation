package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Formula execution trace with plan, steps, elapsed time and errors.
 */
public class FormulaExecutionTrace {

    private final String traceId;
    private final List<String> executionPlan;
    private final List<FormulaExecutionStep> steps;
    private final long elapsedMs;
    private final List<String> errors;

    private FormulaExecutionTrace(Builder builder) {
        this.traceId = builder.traceId;
        this.executionPlan = Collections.unmodifiableList(new ArrayList<>(builder.executionPlan));
        this.steps = Collections.unmodifiableList(new ArrayList<>(builder.steps));
        this.elapsedMs = builder.elapsedMs;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    }

    public String getTraceId() { return traceId; }

    public List<String> getExecutionPlan() { return executionPlan; }

    public List<FormulaExecutionStep> getSteps() { return steps; }

    public long getElapsedMs() { return elapsedMs; }

    public List<String> getErrors() { return errors; }

    public boolean isSuccess() {
        return errors.isEmpty() && steps.stream().allMatch(FormulaExecutionStep::isSuccess);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String traceId;
        private List<String> executionPlan = new ArrayList<>();
        private List<FormulaExecutionStep> steps = new ArrayList<>();
        private long elapsedMs;
        private List<String> errors = new ArrayList<>();

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder executionPlan(List<String> executionPlan) {
            this.executionPlan = executionPlan == null ? new ArrayList<>() : new ArrayList<>(executionPlan);
            return this;
        }

        public Builder steps(List<FormulaExecutionStep> steps) {
            this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
            return this;
        }

        public Builder addStep(FormulaExecutionStep step) {
            if (step != null) {
                this.steps.add(step);
            }
            return this;
        }

        public Builder elapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
            return this;
        }

        public Builder addError(String error) {
            if (error != null && !error.isBlank()) {
                this.errors.add(error);
            }
            return this;
        }

        public FormulaExecutionTrace build() {
            return new FormulaExecutionTrace(this);
        }
    }
}
