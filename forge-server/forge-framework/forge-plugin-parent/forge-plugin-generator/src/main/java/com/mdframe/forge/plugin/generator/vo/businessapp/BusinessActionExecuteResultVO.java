package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用业务动作执行结果。
 */
@Data
public class BusinessActionExecuteResultVO {

    private Long logId;

    private String suiteCode;

    private String objectCode;

    private String recordId;

    private String actionCode;

    private String actionName;

    private String executeStatus;

    private String message;

    private String correlationId;

    private Long durationMs;

    private Boolean idempotentHit;

    private List<BusinessActionStepResultVO> stepResults = new ArrayList<>();
}
