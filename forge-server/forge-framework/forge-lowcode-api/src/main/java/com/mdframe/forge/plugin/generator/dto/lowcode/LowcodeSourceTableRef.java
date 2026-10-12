package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码模型绑定的代码生成表模型引用。
 */
@Data
public class LowcodeSourceTableRef {

    private Long tableId;

    private Long datasourceId;

    private String datasourceCode;

    private String datasourceName;

    private String dbType;

    private String tableName;

    private String tableComment;
}
