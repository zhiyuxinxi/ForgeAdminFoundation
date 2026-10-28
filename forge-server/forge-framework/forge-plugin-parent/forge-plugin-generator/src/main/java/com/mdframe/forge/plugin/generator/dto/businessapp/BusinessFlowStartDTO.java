package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务单据发起流程参数。
 */
@Data
public class BusinessFlowStartDTO {

    private String objectCode;

    private Long recordId;

    private String flowModelKey;

    private String title;

    /** Stable identity for one explicit start round; retries must preserve this value. */
    private String startRequestId;

    /** Required by the flow owner when intentionally starting after an ended round. */
    private String previousProcessInstanceId;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
