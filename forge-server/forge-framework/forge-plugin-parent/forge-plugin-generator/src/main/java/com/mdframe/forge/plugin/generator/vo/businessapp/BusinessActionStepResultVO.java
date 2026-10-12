package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用业务动作步骤执行结果。
 */
@Data
public class BusinessActionStepResultVO {

    private String stepCode;

    private String stepName;

    private String stepType;

    private String status;

    private String message;

    private Map<String, Object> result = new LinkedHashMap<>();

    private String errorMessage;

    private Long durationMs;
}
