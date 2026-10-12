package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON AST node for condition rule designer.
 * <p>
 * Group example: {@code {"operator":"AND","children":[...]}}
 * Condition example: {@code {"field":"amount","op":"GT","value":1000}}
 */
public class ConditionRuleNode {

    private final String operator;
    private final List<ConditionRuleNode> children;
    private final String field;
    private final String op;
    private final Object value;

    @JsonCreator
    public ConditionRuleNode(@JsonProperty("operator") String operator,
                             @JsonProperty("children") List<ConditionRuleNode> children,
                             @JsonProperty("field") String field,
                             @JsonProperty("op") String op,
                             @JsonProperty("value") Object value) {
        this.operator = operator;
        this.children = children == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(children));
        this.field = field;
        this.op = op;
        this.value = value;
    }

    public static ConditionRuleNode group(String operator, List<ConditionRuleNode> children) {
        return new ConditionRuleNode(operator, children, null, null, null);
    }

    public static ConditionRuleNode condition(String field, String op, Object value) {
        return new ConditionRuleNode(null, List.of(), field, op, value);
    }

    public String getOperator() {
        return operator;
    }

    public List<ConditionRuleNode> getChildren() {
        return children;
    }

    public String getField() {
        return field;
    }

    public String getOp() {
        return op;
    }

    public Object getValue() {
        return value;
    }

    public boolean isGroup() {
        if (!children.isEmpty()) {
            return true;
        }
        try {
            return operator != null && ConditionRuleOperator.parse(operator).isGroupOperator();
        } catch (Exception ignored) {
            return false;
        }
    }

    public String resolvedConditionOperator() {
        if (op != null && !op.isBlank()) {
            return op;
        }
        return operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConditionRuleNode that)) return false;
        return Objects.equals(operator, that.operator)
            && Objects.equals(children, that.children)
            && Objects.equals(field, that.field)
            && Objects.equals(op, that.op)
            && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, children, field, op, value);
    }
}
