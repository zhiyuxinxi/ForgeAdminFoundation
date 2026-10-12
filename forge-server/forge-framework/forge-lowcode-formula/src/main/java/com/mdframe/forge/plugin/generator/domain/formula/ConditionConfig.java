package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Condition formula configuration — specifies a conditional expression
 * with true/false branch values.
 * <p>
 * Example:
 * <pre>
 *   expression: "amount > 1000"
 *   trueValue:  "VIP"
 *   falseValue: "NORMAL"
 * </pre>
 */
public class ConditionConfig {

    private final String expression;
    private final Object trueValue;
    private final Object falseValue;

    @JsonCreator
    public ConditionConfig(@JsonProperty("expression") String expression,
                           @JsonProperty("trueValue") Object trueValue,
                           @JsonProperty("falseValue") Object falseValue) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    public String getExpression() { return expression; }
    public Object getTrueValue() { return trueValue; }
    public Object getFalseValue() { return falseValue; }

    public boolean hasExpression() { return !expression.isBlank(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConditionConfig that)) return false;
        return expression.equals(that.expression)
            && Objects.equals(trueValue, that.trueValue)
            && Objects.equals(falseValue, that.falseValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, trueValue, falseValue);
    }

    @Override
    public String toString() {
        return "IF(" + expression + ") THEN " + trueValue + " ELSE " + falseValue;
    }
}
