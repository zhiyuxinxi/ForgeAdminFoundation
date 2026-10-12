package com.mdframe.forge.plugin.ai.dashboard.dto;

import lombok.Data;

/**
 * AI 大屏生成记录稽核查询条件。
 */
@Data
public class AiDashboardGenerateRecordQuery {

    private String projectName;

    private String businessName;

    private String userKeyword;

    private String providerName;

    private String modelName;

    private String status;

    private String startTime;

    private String endTime;
}
