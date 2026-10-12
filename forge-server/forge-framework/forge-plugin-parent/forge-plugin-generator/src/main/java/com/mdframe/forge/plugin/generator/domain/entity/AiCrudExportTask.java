package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 通用 CRUD 异步导出任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_crud_export_task")
public class AiCrudExportTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;
    private String exportName;
    private String fileName;
    private String fileId;
    private Long fileSize;
    private String status;
    private Long totalCount;
    private Long exportedCount;
    private Integer progress;
    private String queryParams;
    private String errorMessage;
    private LocalDateTime finishTime;
    private LocalDateTime expireTime;
}
