package com.mdframe.forge.business.core.purchase.vo;

import lombok.Data;

/**
 * 采购单测试流程初始化结果。
 */
@Data
public class SamplePurchaseOrderFlowInitVO {

    private String modelKey;

    private String modelId;

    private String deploymentId;

    private Integer status;

    private String message;
}
