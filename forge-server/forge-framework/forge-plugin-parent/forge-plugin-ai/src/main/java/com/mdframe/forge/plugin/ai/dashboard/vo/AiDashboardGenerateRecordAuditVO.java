package com.mdframe.forge.plugin.ai.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 大屏生成记录稽核视图。
 */
@Data
public class AiDashboardGenerateRecordAuditVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long tenantId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    private String username;

    private String realName;

    private String sessionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    private String projectName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long businessDefinitionId;

    private String businessName;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
