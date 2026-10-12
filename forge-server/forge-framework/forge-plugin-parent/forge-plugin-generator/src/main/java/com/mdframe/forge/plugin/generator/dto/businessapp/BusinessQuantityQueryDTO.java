package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用数量台账只读查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessQuantityQueryDTO extends PageQuery {

    private String accountCode;

    private String itemCode;

    private String dimensionKey;

    private String sourceObjectCode;

    private String sourceRecordId;

    private String sourceDetailId;

    private String operationType;

    private String lockStatus;

    private Integer status;
}
