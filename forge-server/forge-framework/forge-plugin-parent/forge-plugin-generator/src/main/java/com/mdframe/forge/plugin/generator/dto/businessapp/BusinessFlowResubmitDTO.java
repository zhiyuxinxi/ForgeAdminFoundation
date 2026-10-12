package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 驳回修改后重提参数。
 */
@Data
public class BusinessFlowResubmitDTO {

    private String taskId;

    private String businessKey;

    private String processInstanceId;

    private String processDefKey;

    private String taskDefKey;

    private String comment;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
