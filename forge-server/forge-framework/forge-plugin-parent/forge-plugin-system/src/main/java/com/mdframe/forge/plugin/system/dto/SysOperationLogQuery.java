package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志查询条件
 */
@Data
public class SysOperationLogQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String operatorName;

    private String operationModule;

    private String operationType;

    private Integer operationStatus;

    private String operationIp;

    private String requestUrl;

    private String requestParams;

    private String operationPage;

    private String operationPageTitle;

    private String operationContent;

    private String startTime;

    private String endTime;
}
