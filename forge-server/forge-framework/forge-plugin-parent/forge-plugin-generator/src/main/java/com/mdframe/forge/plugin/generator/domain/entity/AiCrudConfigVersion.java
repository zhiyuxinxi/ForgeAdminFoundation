package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI CRUD 低代码发布版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_crud_config_version")
public class AiCrudConfigVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long configId;

    private String configKey;

    private Long domainId;

    private String domainCode;

    private String objectCode;

    private String objectName;

    private Integer versionNo;

    /** 版本类型：publish 发布，rollback 回滚 */
    private String versionType;

    private String modelSchema;

    private String pageSchema;

    private String searchSchema;

    private String columnsSchema;

    private String editSchema;

    private String apiConfig;

    private String options;

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

    private String publishSnapshot;

    private String remark;
}
