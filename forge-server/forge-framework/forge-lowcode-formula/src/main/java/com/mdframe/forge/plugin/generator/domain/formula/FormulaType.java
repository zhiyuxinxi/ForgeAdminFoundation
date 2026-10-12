package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * 公式类型枚举
 */
public enum FormulaType {
    /** 计算公式：同对象字段间运算，如 total = price * qty */
    CALC,
    /** 聚合公式：主表字段由从表记录聚合计算，如 total = SUM(detail.amount) */
    AGGREGATE,
    /** 条件公式：根据条件表达式动态赋值，如 discount = IF(amount > 1000, 0.1, 0) */
    CONDITIONAL,
    /** LOOKUP公式：根据对象关系读取关联对象字段值，如 ownerName = LOOKUP(owner.realName) */
    LOOKUP
}
