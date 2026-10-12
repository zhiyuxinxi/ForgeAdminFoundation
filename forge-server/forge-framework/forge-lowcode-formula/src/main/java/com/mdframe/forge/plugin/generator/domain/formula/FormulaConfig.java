package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Formula configuration domain model.
 * <p>
 * Describes a field's formula calculation rule, including formula type,
 * calculation mode, expression, dependencies, and extended config metadata.
 * Pure domain object — no persistence annotations, no framework dependencies.
 */
public class FormulaConfig {
    /** Formula type */
    private final FormulaType type;
    /** Calculation mode */
    private final FormulaMode mode;
    /** Aviator expression (Phase 2 execution) */
    private final String expression;
    /** Dependent field names (for DAG and cycle detection) */
    private final List<String> dependsOn;
    /** Aggregate config (required when type=AGGREGATE) */
    private final AggregateConfig aggregate;
    /** Condition config (required when type=CONDITIONAL) */
    private final ConditionConfig condition;
    /** LOOKUP config (required when type=LOOKUP) */
    private final LookupConfig lookup;
    /** Cross-object path metadata */
    private final CrossObjectConfig crossObject;
    /** Conditional rule designer AST metadata */
    private final Map<String, Object> rule;
    /** Referenced function codes */
    private final List<String> functionRefs;

    @JsonCreator
    public FormulaConfig(@JsonProperty("type") FormulaType type,
                         @JsonProperty("mode") FormulaMode mode,
                         @JsonProperty("expression") String expression,
                         @JsonProperty("dependsOn") List<String> dependsOn,
                         @JsonProperty("aggregate") AggregateConfig aggregate,
                         @JsonProperty("condition") ConditionConfig condition,
                         @JsonProperty("lookup") LookupConfig lookup,
                         @JsonProperty("crossObject") CrossObjectConfig crossObject,
                         @JsonProperty("rule") Map<String, Object> rule,
                         @JsonProperty("functionRefs") List<String> functionRefs) {
        this(builder()
            .type(type == null ? FormulaType.CALC : type)
            .mode(mode == null ? FormulaMode.STORED : mode)
            .expression(expression)
            .dependsOn(dependsOn)
            .aggregate(aggregate)
            .condition(condition)
            .lookup(lookup)
            .crossObject(crossObject)
            .rule(rule)
            .functionRefs(functionRefs));
    }

    private FormulaConfig(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "type must not be null");
        this.mode = Objects.requireNonNull(builder.mode, "mode must not be null");
        this.expression = builder.expression;
        this.dependsOn = builder.dependsOn != null
            ? Collections.unmodifiableList(new ArrayList<>(builder.dependsOn))
            : Collections.emptyList();
        this.aggregate = builder.aggregate;
        this.condition = builder.condition;
        this.lookup = builder.lookup;
        this.crossObject = builder.crossObject;
        this.rule = builder.rule != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.rule))
            : Collections.emptyMap();
        this.functionRefs = builder.functionRefs != null
            ? Collections.unmodifiableList(new ArrayList<>(builder.functionRefs))
            : Collections.emptyList();
        validate();
    }

    private void validate() {
        if (type != FormulaType.AGGREGATE && aggregate != null) {
            throw new IllegalArgumentException("aggregate config only valid for AGGREGATE type");
        }
        if (type == FormulaType.AGGREGATE && aggregate == null) {
            throw new IllegalArgumentException("aggregate config required for AGGREGATE type");
        }
        if (type != FormulaType.CONDITIONAL && condition != null) {
            throw new IllegalArgumentException("condition config only valid for CONDITIONAL type");
        }
        if (type == FormulaType.CONDITIONAL && condition == null) {
            throw new IllegalArgumentException("condition config required for CONDITIONAL type");
        }
        if (type != FormulaType.LOOKUP && lookup != null) {
            throw new IllegalArgumentException("lookup config only valid for LOOKUP type");
        }
        if (type == FormulaType.LOOKUP && lookup == null) {
            throw new IllegalArgumentException("lookup config required for LOOKUP type");
        }
    }

    // ——— typed enum accessors ———

    public FormulaType getType() { return type; }
    public FormulaMode getMode() { return mode; }
    public String getExpression() { return expression; }
    public List<String> getDependsOn() { return dependsOn; }

    /** Aggregate config, non-null only when type=AGGREGATE */
    public AggregateConfig getAggregate() { return aggregate; }

    /** Condition config, non-null only when type=CONDITIONAL */
    public ConditionConfig getCondition() { return condition; }

    /** LOOKUP config, non-null only when type=LOOKUP */
    public LookupConfig getLookup() { return lookup; }

    /** Cross-object metadata, optional */
    public CrossObjectConfig getCrossObject() { return crossObject; }

    /** Conditional rule AST metadata, optional */
    public Map<String, Object> getRule() { return rule; }

    /** Referenced function codes */
    public List<String> getFunctionRefs() { return functionRefs; }

    public boolean isVirtual() { return mode == FormulaMode.VIRTUAL; }
    public boolean isStored() { return mode == FormulaMode.STORED; }
    public boolean isCalc() { return type == FormulaType.CALC; }
    public boolean isAggregate() { return type == FormulaType.AGGREGATE; }
    public boolean isConditional() { return type == FormulaType.CONDITIONAL; }
    public boolean isLookup() { return type == FormulaType.LOOKUP; }
    public boolean hasCrossObject() { return crossObject != null; }
    public boolean hasRule() { return !rule.isEmpty(); }
    public boolean hasFunctionRefs() { return !functionRefs.isEmpty(); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private FormulaType type;
        private FormulaMode mode;
        private String expression;
        private List<String> dependsOn;
        private AggregateConfig aggregate;
        private ConditionConfig condition;
        private LookupConfig lookup;
        private CrossObjectConfig crossObject;
        private Map<String, Object> rule;
        private List<String> functionRefs;

        public Builder type(FormulaType type) {
            this.type = type;
            return this;
        }
        public Builder mode(FormulaMode mode) {
            this.mode = mode;
            return this;
        }
        public Builder expression(String expr) {
            this.expression = expr;
            return this;
        }
        public Builder dependsOn(List<String> deps) {
            this.dependsOn = deps;
            return this;
        }
        public Builder aggregate(AggregateConfig ac) {
            this.aggregate = ac;
            return this;
        }
        public Builder condition(ConditionConfig cc) {
            this.condition = cc;
            return this;
        }
        public Builder lookup(LookupConfig lc) {
            this.lookup = lc;
            return this;
        }
        public Builder crossObject(CrossObjectConfig cc) {
            this.crossObject = cc;
            return this;
        }
        public Builder rule(Map<String, Object> rule) {
            this.rule = rule;
            return this;
        }
        public Builder functionRefs(List<String> refs) {
            this.functionRefs = refs;
            return this;
        }

        public FormulaConfig build() { return new FormulaConfig(this); }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormulaConfig that)) return false;
        return type == that.type && mode == that.mode
            && Objects.equals(expression, that.expression)
            && dependsOn.equals(that.dependsOn)
            && Objects.equals(aggregate, that.aggregate)
            && Objects.equals(condition, that.condition)
            && Objects.equals(lookup, that.lookup)
            && Objects.equals(crossObject, that.crossObject)
            && rule.equals(that.rule)
            && functionRefs.equals(that.functionRefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, mode, expression, dependsOn, aggregate, condition,
            lookup, crossObject, rule, functionRefs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FormulaConfig{")
            .append("type=").append(type)
            .append(", mode=").append(mode);
        if (expression != null) sb.append(", expr='").append(expression).append("'");
        if (!dependsOn.isEmpty()) sb.append(", dependsOn=").append(dependsOn);
        if (aggregate != null) sb.append(", aggregate=").append(aggregate);
        if (condition != null) sb.append(", condition=").append(condition);
        if (lookup != null) sb.append(", lookup=").append(lookup);
        if (crossObject != null) sb.append(", crossObject=").append(crossObject);
        if (!rule.isEmpty()) sb.append(", rule=").append(rule);
        if (!functionRefs.isEmpty()) sb.append(", functionRefs=").append(functionRefs);
        return sb.append("}").toString();
    }
}
