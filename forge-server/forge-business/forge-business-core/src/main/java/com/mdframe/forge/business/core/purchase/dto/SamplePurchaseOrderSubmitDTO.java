package com.mdframe.forge.business.core.purchase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购单提交流程参数。
 */
@Data
public class SamplePurchaseOrderSubmitDTO {

    private Long deptLeaderId;

    private Long engineeringManagerId;

    private List<Long> countersignUserIds = new ArrayList<>();

    /**
     * 流程完成后抄送的角色编码，默认由前端给出 admin，测试时可改为 general_manager。
     */
    private List<String> ccRoleKeys = new ArrayList<>();
}
