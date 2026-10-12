package com.mdframe.forge.plugin.ai.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 大屏生成记录保存请求。
 */
@Data
public class AiDashboardGenerateRecordSaveDTO {

    private String sessionId;

    private Long projectId;

    private String projectName;

    private Long businessDefinitionId;

    private String businessName;

    private Long providerId;

    private String providerName;

    private String modelName;

    private String style;

    private Integer canvasWidth;

    private Integer canvasHeight;

    private String prompt;

    private String requestJson;

    private String generatedTitle;

    private String responseJson;

    private String validationSummaryJson;

    private String status;

    private Integer componentCount;

    private Integer boundCount;

    private Integer staticCount;

    private Integer staticFallbackCount;

    private Integer repairedCount;

    private Long elapsedMs;

    private String errorMessage;

    private List<AiDashboardComponentLineageDTO> lineageItems;
}
