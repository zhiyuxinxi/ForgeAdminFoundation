package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用数量台账操作请求。
 */
@Data
public class BusinessQuantityOperationDTO {

    private String operationType;

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private Long quantity;

    private String targetAccountCode;

    private String targetItemCode;

    private String targetDimensionKey;

    private Long lockId;

    private String lockCode;

    private String sourceObjectCode;

    private String sourceRecordId;

    private String sourceDetailId;

    private String correlationId;

    private String idempotencyKey;

    private String remark;

    private Map<String, Object> extraData = new LinkedHashMap<>();
}
