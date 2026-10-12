package com.mdframe.forge.flow.client.spi;

import lombok.Data;

/**
 * 流程列表业务展示项。
 */
@Data
public class FlowBusinessListDisplayItem {

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String processName;

    private String processDefinitionName;

    private String taskId;

    private String taskName;

    private String title;

    private String objectCode;

    private Long recordId;

    private String businessObjectName;

    private String businessSummary;
}
