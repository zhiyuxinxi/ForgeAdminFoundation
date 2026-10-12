package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-通用数量余额。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_quantity_balance")
public class AiBusinessQuantityBalance extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long quantity;

    private Long lockedQuantity;

    private Integer status;

    private String remark;
}
