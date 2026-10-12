package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用业务动作步骤配置。
 */
@Data
public class BusinessActionStepDTO {

    private String stepCode;

    private String stepName;

    private String stepType;

    private Map<String, Object> stepConfig = new LinkedHashMap<>();

    private Integer sortOrder;

    private Boolean rollbackOnFailure;
}
