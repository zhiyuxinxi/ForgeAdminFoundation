package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/**
 * 流程入口提交结果。
 */
@Data
public class FlowStartResultVO {

    private Long formInstanceId;

    private String businessKey;

    private String processInstanceId;

    private String dataMode;

    private String objectCode;

    private Long recordId;
}
