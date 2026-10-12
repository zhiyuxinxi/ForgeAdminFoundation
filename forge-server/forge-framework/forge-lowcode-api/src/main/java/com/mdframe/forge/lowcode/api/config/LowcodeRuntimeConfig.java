package com.mdframe.forge.lowcode.api.config;

import lombok.Data;

/** Per-request low-code execution values. JSON remains opaque until its existing consumer parses it. */
@Data
public final class LowcodeRuntimeConfig {
    private Long tenantId;
    private String configKey;
    private String tableName;
    private String status;
    private String mode;
    private String buildMode;
    private String publishStatus;
    private String layoutType;
    private String domainCode;
    private String objectCode;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String options;
    private String modelSchema;
    private String pageSchema;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
    private Long runtimeDatasourceId;
    private String runtimeDatasourceCode;
    private String runtimeDatasourceSnapshot;
    private String runtimeTableName;
    private String primaryKeyField;
    private String primaryKeyColumn;
    private String primaryKeyType;
    private String tenantStrategy;
    private String auditStrategy;
    private String logicDeleteStrategy;
    private String appName;
    private String tableComment;
}
