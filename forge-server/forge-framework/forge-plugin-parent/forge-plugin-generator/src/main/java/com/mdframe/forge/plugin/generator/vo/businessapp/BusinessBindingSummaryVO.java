package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 流程模型反查业务对象绑定摘要。
 */
@Data
public class BusinessBindingSummaryVO {

    private Long bindingId;

    private String flowModelKey;

    private String bindingName;

    private String objectCode;

    private String objectName;

    private String suiteName;

    private Boolean codeApp;

    private String entryRoute;
}
