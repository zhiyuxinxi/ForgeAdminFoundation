package com.mdframe.forge.business.core.purchase.dto;

import lombok.Data;

/**
 * 采购单查询参数。
 */
@Data
public class SamplePurchaseOrderQuery {

    private String orderNo;

    private String title;

    private String supplierName;

    private String status;
}
