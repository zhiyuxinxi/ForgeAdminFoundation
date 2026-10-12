package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用数量余额查询结果。
 */
@Data
public class BusinessQuantityBalanceVO {

    private Long id;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long quantity;

    private Long lockedQuantity;

    private Long availableQuantity;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
