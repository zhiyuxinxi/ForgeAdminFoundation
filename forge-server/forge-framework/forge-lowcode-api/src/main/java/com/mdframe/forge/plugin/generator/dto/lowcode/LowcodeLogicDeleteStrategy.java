package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码运行表逻辑删除策略。
 */
@Data
public class LowcodeLogicDeleteStrategy {

    /** NONE-物理删除，DEL_FLAG-使用删除标志字段。 */
    private String mode;

    private String columnName;

    private String activeValue;

    private String deletedValue;

    /** VALUE (default): bind deletedValue; PRIMARY_KEY: atomically copy the validated row primary key. */
    private String deletedValueMode;
}
