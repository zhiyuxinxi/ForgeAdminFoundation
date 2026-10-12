package com.mdframe.forge.business.core.purchase.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 待办节点保存采购单业务字段参数。
 */
@Data
public class SamplePurchaseOrderTaskSaveDTO {

    private Long id;

    private String businessKey;

    private String taskId;

    private String taskDefKey;

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
}
