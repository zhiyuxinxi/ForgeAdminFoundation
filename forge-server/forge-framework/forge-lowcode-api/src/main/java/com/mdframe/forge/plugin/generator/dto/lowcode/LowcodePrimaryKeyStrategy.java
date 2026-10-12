package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码运行表主键策略。首期只支持单字段主键。
 */
@Data
public class LowcodePrimaryKeyStrategy {

    private String field;

    private String columnName;

    private String dataType;

    private Boolean autoIncrement;
}
