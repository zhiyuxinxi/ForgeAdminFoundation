package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single formula execution step for debugger and execution trace.
 */
public class FormulaExecutionStep {

    private final String fieldCode;
    private final String formulaType;
    private final String expression;
    private final Map<String, Object> input;
    private final Object output;
    private final long elapsedMs;
    private final boolean success;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    private FormulaExecutionStep(Builder builder) {
        this.fieldCode = builder.fieldCode;
        this.formulaType = builder.formulaType;
        this.expression = builder.expression;
        this.input = Collections.unmodifiableMap(new LinkedHashMap<>(builder.input));
        this.output = builder.output;
        this.elapsedMs = builder.elapsedMs;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public String getFieldCode() { return fieldCode; }

    public String getFormulaType() { return formulaType; }

    public String getExpression() { return expression; }

    public Map<String, Object> getInput() { return input; }

    public Object getOutput() { return output; }

    public long getElapsedMs() { return elapsedMs; }

    public boolean isSuccess() { return success; }

    public String getErrorMessage() { return errorMessage; }

    public Map<String, Object> getMetadata() { return metadata; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String fieldCode;
        private String formulaType;
        private String expression;
        private Map<String, Object> input = new LinkedHashMap<>();
        private Object output;
        private long elapsedMs;
        private boolean success = true;
        private String errorMessage;
        private Map<String, Object> metadata = new LinkedHashMap<>();

        public Builder fieldCode(String fieldCode) {
            this.fieldCode = fieldCode;
            return this;
        }

        public Builder formulaType(String formulaType) {
            this.formulaType = formulaType;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder input(Map<String, Object> input) {
            this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
            return this;
        }

        public Builder output(Object output) {
            this.output = output;
            return this;
        }

        public Builder elapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
            return this;
        }

        public Builder putMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public FormulaExecutionStep build() {
            return new FormulaExecutionStep(this);
        }
    }
}
