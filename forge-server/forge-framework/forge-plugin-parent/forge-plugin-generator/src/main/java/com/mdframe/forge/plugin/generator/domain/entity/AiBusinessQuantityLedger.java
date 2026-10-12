package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-通用数量流水。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_quantity_ledger")
public class AiBusinessQuantityLedger extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String operationType;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long quantityDelta;

    private Long balanceQuantity;

    private Long lockedQuantity;

    private String targetAccountCode;

    private String targetItemCode;

    private String targetDimensionKey;

    private String sourceObjectCode;

    private String sourceRecordId;

    private String sourceDetailId;

    private Long lockId;

    private String correlationId;

    private String idempotencyKey;

    private String remark;

    private String extraData;
}
