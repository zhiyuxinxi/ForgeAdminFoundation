package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 聚合公式配置领域模型
 * <p>
 * 描述主表聚合从表字段时的参数：聚合函数、对象关系、目标字段、过滤条件。
 */
public class AggregateConfig {
    /** 聚合函数 */
    private final AggregateFunction function;
    /** 对象关系编码（定位从表） */
    private final String relationCode;
    /** 从表中需要聚合的字段名 */
    private final String targetField;
    /** 可选的过滤条件（Aviator 表达式，Phase 2 执行） */
    private final String filter;

    @JsonCreator
    public AggregateConfig(@JsonProperty("function") AggregateFunction function,
                           @JsonProperty("relationCode") String relationCode,
                           @JsonProperty("targetField") String targetField,
                           @JsonProperty("filter") String filter) {
        this.function = Objects.requireNonNull(function, "function must not be null");
        this.relationCode = Objects.requireNonNull(relationCode, "relationCode must not be null");
        this.targetField = Objects.requireNonNull(targetField, "targetField must not be null");
        this.filter = filter;
    }

    public AggregateFunction getFunction() { return function; }
    public String getRelationCode() { return relationCode; }
    public String getTargetField() { return targetField; }

    /** 可选的过滤条件，可能为 null */
    public String getFilter() { return filter; }

    /** 聚合函数名（小写），如 "sum", "count" */
    public String getFunctionName() { return function.name().toLowerCase(); }

    /** 是否配置了过滤条件 */
    public boolean hasFilter() { return filter != null && !filter.isBlank(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregateConfig that)) return false;
        return function == that.function
            && relationCode.equals(that.relationCode)
            && targetField.equals(that.targetField)
            && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, relationCode, targetField, filter);
    }

    @Override
    public String toString() {
        return function.name() + "(" + relationCode + "." + targetField + ")"
            + (hasFilter() ? " WHERE " + filter : "");
    }
}
