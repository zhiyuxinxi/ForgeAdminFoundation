package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.Map;

/**
 * 流程入口填报提交请求。
 */
@Data
public class FlowEntrySubmitDTO {

    private Map<String, Object> formData;

    private Map<String, Object> variables;

    private String title;

    private Long batchItemId;

    private String startUserId;

    private String startUserName;

    private String startDeptId;

    private String startDeptName;
}
