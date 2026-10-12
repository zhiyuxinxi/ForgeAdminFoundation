package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of an aggregate computation over detail records.
 * <p>
 * Contains the computed aggregate value, row counts, and any per-row errors.
 */
public class AggregateResult {
    private final Object value;
    private final int totalRowCount;
    private final int matchedRowCount;
    private final List<String> errors;

    private AggregateResult(Builder builder) {
        this.value = builder.value;
        this.totalRowCount = builder.totalRowCount;
        this.matchedRowCount = builder.matchedRowCount;
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    }

    /** The computed aggregate value (may be null for empty sets with no default). */
    public Object getValue() { return value; }

    /** Total number of detail records before filtering. */
    public int getTotalRowCount() { return totalRowCount; }

    /** Number of detail records that matched the filter (if any). */
    public int getMatchedRowCount() { return matchedRowCount; }

    /** Per-row or per-computation error messages. */
    public List<String> getErrors() { return errors; }

    /** Whether the computation succeeded without errors. */
    public boolean isSuccess() { return errors.isEmpty(); }

    /** Convenience: return value as long, or 0 if null. */
    public long longValue() {
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    /** Convenience: return value as double, or 0.0 if null. */
    public double doubleValue() {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    @Override
    public String toString() {
        return "AggregateResult{value=" + value
            + ", totalRows=" + totalRowCount
            + ", matchedRows=" + matchedRowCount
            + ", errors=" + errors.size() + "}";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        Object value;
        int totalRowCount;
        int matchedRowCount;
        List<String> errors = new ArrayList<>();

        public Builder value(Object v) {
            this.value = v;
            return this;
        }
        public Builder totalRowCount(int c) {
            this.totalRowCount = c;
            return this;
        }
        public Builder matchedRowCount(int c) {
            this.matchedRowCount = c;
            return this;
        }
        public Builder addError(String e) {
            this.errors.add(e);
            return this;
        }

        public AggregateResult build() { return new AggregateResult(this); }
    }
}
