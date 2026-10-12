package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * Options controlling formula trace collection.
 */
public class FormulaTraceOptions {

    private static final FormulaTraceOptions DISABLED = builder().build();

    private final boolean enabled;
    private final boolean includeInputSnapshot;
    private final boolean includeOutputValue;
    private final boolean debugMode;

    private FormulaTraceOptions(Builder builder) {
        this.debugMode = builder.debugMode;
        this.enabled = builder.enabled || builder.debugMode;
        this.includeInputSnapshot = builder.includeInputSnapshot || builder.debugMode;
        this.includeOutputValue = builder.includeOutputValue || builder.debugMode;
    }

    public boolean isEnabled() { return enabled; }

    public boolean isIncludeInputSnapshot() { return includeInputSnapshot; }

    public boolean isIncludeOutputValue() { return includeOutputValue; }

    public boolean isDebugMode() { return debugMode; }

    public static FormulaTraceOptions disabled() { return DISABLED; }

    public static FormulaTraceOptions debug() {
        return builder()
                .enabled(true)
                .includeInputSnapshot(true)
                .includeOutputValue(true)
                .debugMode(true)
                .build();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean enabled;
        private boolean includeInputSnapshot;
        private boolean includeOutputValue;
        private boolean debugMode;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder includeInputSnapshot(boolean includeInputSnapshot) {
            this.includeInputSnapshot = includeInputSnapshot;
            return this;
        }

        public Builder includeOutputValue(boolean includeOutputValue) {
            this.includeOutputValue = includeOutputValue;
            return this;
        }

        public Builder debugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        public FormulaTraceOptions build() {
            return new FormulaTraceOptions(this);
        }
    }
}
