package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.Locale;

/**
 * Operators supported by the formula condition rule designer.
 */
public enum ConditionRuleOperator {
    AND,
    OR,
    EQ,
    NE,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    NOT_IN,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    IS_NULL,
    NOT_NULL;

    public static ConditionRuleOperator parse(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("规则操作符不能为空");
        }
        String value = rawValue.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (value) {
            case "=", "==" -> EQ;
            case "!=", "<>" -> NE;
            case ">" -> GT;
            case ">=" -> GTE;
            case "<" -> LT;
            case "<=" -> LTE;
            default -> ConditionRuleOperator.valueOf(value);
        };
    }

    public boolean isGroupOperator() {
        return this == AND || this == OR;
    }

    public boolean requiresValue() {
        return this != IS_NULL && this != NOT_NULL && !isGroupOperator();
    }

    public boolean requiresCollectionValue() {
        return this == IN || this == NOT_IN;
    }

    public boolean isStringOperator() {
        return this == CONTAINS || this == STARTS_WITH || this == ENDS_WITH;
    }
}
