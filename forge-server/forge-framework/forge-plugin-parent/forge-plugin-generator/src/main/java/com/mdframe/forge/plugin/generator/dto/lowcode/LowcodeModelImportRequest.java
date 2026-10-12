package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 从数据源表结构生成低代码模型的请求。
 */
@Data
public class LowcodeModelImportRequest {

    private Long datasourceId;

    private Long domainId;

    private String tableName;

    private String modelCode;

    private String modelName;

    private String modelDesc;

    private Boolean tenantEnabled;

    private Boolean masterData;
}

