package com.mdframe.forge.business.core.purchase.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购单展示对象。
 */
@Data
public class SamplePurchaseOrderVO {

    private Long id;

    private Long tenantId;

    private String orderNo;

    private String title;

    private String supplierName;

    private Long amountCent;

    private String purchaseItems;

    private LocalDate needDate;

    private String status;

    private Long applicantId;

    private String applicantName;

    private Long applicantDeptId;

    private String applicantDeptName;

    private String businessKey;

    private String processInstanceId;

    private Long deptLeaderId;

    private String deptLeaderName;

    private Long engineeringManagerId;

    private String engineeringManagerName;

    private String countersignUserIds;

    private String countersignUserNames;

    private String ccRoleKeys;

    private String arrivalListFileIds;

    private String applicantModifyRemark;

    private String deptLeaderRemark;

    private String engineeringManagerRemark;

    private String countersignRemark;

    private String rejectReason;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long createDept;

    private Long updateBy;

    private LocalDateTime updateTime;
}
