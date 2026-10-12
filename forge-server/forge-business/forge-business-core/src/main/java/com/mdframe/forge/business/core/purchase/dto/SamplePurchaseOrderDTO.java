package com.mdframe.forge.business.core.purchase.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 采购单保存参数。
 */
@Data
public class SamplePurchaseOrderDTO {

    private Long id;

    private String orderNo;

    private String title;

    private String supplierName;

    private Long amountCent;

    private String purchaseItems;

    private LocalDate needDate;

    private String arrivalListFileIds;

    private String applicantModifyRemark;

    private String deptLeaderRemark;

    private String engineeringManagerRemark;

    private String countersignRemark;

    private String remark;
}
