package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 通用业务动作执行日志查询条件。
 */
@Data
public class BusinessActionLogQueryDTO {

    private String suiteCode;

    private String objectCode;

    private String recordId;

    private String actionCode;

    private String executeStatus;

    private String correlationId;
}
