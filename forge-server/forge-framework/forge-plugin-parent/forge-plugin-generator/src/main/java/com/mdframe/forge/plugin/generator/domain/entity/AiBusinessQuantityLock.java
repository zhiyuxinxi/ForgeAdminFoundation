package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-通用数量锁定。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_quantity_lock")
public class AiBusinessQuantityLock extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String lockCode;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long lockQuantity;

    private Long releasedQuantity;

    private Long committedQuantity;

    private String lockStatus;

    private String sourceObjectCode;

    private String sourceRecordId;

    private String sourceDetailId;

    private String correlationId;

    private String idempotencyKey;

    private String remark;
}
