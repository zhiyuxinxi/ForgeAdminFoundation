package com.mdframe.forge.plugin.ai.dashboard.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 大屏生成记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_dashboard_generate_record")
public class AiDashboardGenerateRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

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
}
