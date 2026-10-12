package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用数量流水查询结果。
 */
@Data
public class BusinessQuantityLedgerVO {

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

    private String remark;

    private String extraData;

    private LocalDateTime createTime;
}
