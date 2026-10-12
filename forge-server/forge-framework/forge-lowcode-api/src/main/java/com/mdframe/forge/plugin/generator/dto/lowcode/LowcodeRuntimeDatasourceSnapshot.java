package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * 低代码运行数据源快照，不包含密码等敏感连接信息。
 */
@Data
public class LowcodeRuntimeDatasourceSnapshot {

    private Long datasourceId;

    private String datasourceCode;

    private String datasourceName;

    private String dbType;

    private String tableName;

    private String tableMode;

    private String usageScope;

    private Boolean allowWrite;

    private Boolean allowDdl;

    private Boolean readonly;

    private String riskLevel;
}
