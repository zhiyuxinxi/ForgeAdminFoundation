package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Formula dependency analysis result.
 * <p>
 * Contains complete DAG analysis information including topological order,
 * depth information, cycle detection, and error messages.
 * <p>
 * Designed for direct consumption by {@code BusinessObjectPublishService}
 * and other service-layer callers.
 */
public class DependencyAnalysisResult {
    private final boolean valid;
    private final boolean hasCycle;
    private final Integer maxDepth;
    private final List<String> topologicalOrder;
    private final Map<String, Integer> depthMap;
    private final List<String> cyclePath;
    private final List<List<String>> cycles;
    private final List<String> errors;

    private DependencyAnalysisResult(Builder builder) {
        this.valid = builder.valid;
        this.hasCycle = !builder.cycles.isEmpty();
        this.maxDepth = builder.depthMap.isEmpty() ? null
            : Collections.max(builder.depthMap.values());
        this.topologicalOrder = Collections.unmodifiableList(new ArrayList<>(builder.topologicalOrder));
        this.depthMap = Collections.unmodifiableMap(new LinkedHashMap<>(builder.depthMap));
        this.cycles = builder.cycles.stream()
            .map(c -> Collections.unmodifiableList(new ArrayList<>(c)))
            .collect(Collectors.toUnmodifiableList());
        // Flatten first cycle (or all cycles) into a single path for service consumption
        this.cyclePath = builder.cycles.isEmpty() ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.cycles.get(0)));
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    }

    /** Whether the analysis is valid (no cycles, no depth violations). */
    public boolean isValid() { return valid; }

    /** Whether any cyclic dependency was detected. */
    public boolean isHasCycle() { return hasCycle; }
    // Convenience alias matching user-requested field name
    public boolean hasCycle() { return hasCycle; }

    /** Maximum nesting depth across all formulas, or null if empty. */
    public Integer getMaxDepth() { return maxDepth; }

    /** Topological order: dependencies first, dependents later. */
    public List<String> getTopologicalOrder() { return topologicalOrder; }

    /** Depth map: field name → nesting depth. */
    public Map<String, Integer> getDepthMap() { return depthMap; }

    /** Flattened cycle path (first detected cycle), empty if no cycle. */
    public List<String> getCyclePath() { return cyclePath; }

    /** All detected cycles. */
    public List<List<String>> getCycles() { return cycles; }

    /** Error messages. */
    public List<String> getErrors() { return errors; }

    /** @deprecated use {@link #hasCycle()} instead */
    @Deprecated
    public boolean hasCycles() { return hasCycle; }

    @Override
    public String toString() {
        return "DependencyAnalysisResult{valid=" + valid
            + ", hasCycle=" + hasCycle
            + ", maxDepth=" + maxDepth
            + ", order=" + topologicalOrder
            + ", depth=" + depthMap
            + ", cyclePath=" + cyclePath
            + ", cycles=" + cycles
            + ", errors=" + errors + "}";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        boolean valid;
        List<String> topologicalOrder = new ArrayList<>();
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        List<List<String>> cycles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        public Builder valid(boolean v) {
            this.valid = v;
            return this;
        }
        public Builder topologicalOrder(List<String> o) {
            this.topologicalOrder = o;
            return this;
        }
        public Builder depthMap(Map<String, Integer> d) {
            this.depthMap = d;
            return this;
        }
        public Builder addCycle(List<String> cycle) {
            this.cycles.add(cycle);
            return this;
        }
        public Builder addError(String err) {
            this.errors.add(err);
            return this;
        }

        public DependencyAnalysisResult build() { return new DependencyAnalysisResult(this); }
    }
}
