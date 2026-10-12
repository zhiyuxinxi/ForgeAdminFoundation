package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * 公式计算模式枚举
 */
public enum FormulaMode {
    /** 虚拟模式：读取时实时计算，不存储到数据库 */
    VIRTUAL,
    /** 存储模式：保存时计算并持久化到数据库 */
    STORED
}
