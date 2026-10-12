package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 通用数量台账操作结果。
 */
@Data
public class BusinessQuantityOperationResultVO {

    private String operationType;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long quantity;

    private Long balanceQuantity;

    private Long lockedQuantity;

    private Long availableQuantity;

    private String targetAccountCode;

    private String targetItemCode;

    private String targetDimensionKey;

    private Long targetBalanceQuantity;

    private Long targetLockedQuantity;

    private Long targetAvailableQuantity;

    private Long ledgerId;

    private Long targetLedgerId;

    private Long lockId;

    private Boolean idempotentHit;

    private String message;
}
