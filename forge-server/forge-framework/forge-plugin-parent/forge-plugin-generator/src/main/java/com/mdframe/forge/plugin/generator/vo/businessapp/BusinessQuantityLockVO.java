package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用数量锁定查询结果。
 */
@Data
public class BusinessQuantityLockVO {

    private Long id;

    private String lockCode;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long lockQuantity;

    private Long releasedQuantity;

    private Long committedQuantity;

    private Long remainingQuantity;

    private String lockStatus;

    private String sourceObjectCode;

    private String sourceRecordId;

    private String sourceDetailId;

    private String correlationId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
