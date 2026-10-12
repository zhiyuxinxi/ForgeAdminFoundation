package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 低代码 AI 应用生成请求。
 */
@Data
public class LowcodeAiAppGenerateRequest {

    private Long domainId;

    private String description;

    private String layoutType;

    private Long providerId;

    private Long modelId;

    private BigDecimal temperature;

    private String sessionId;

    private Integer maxTokens;

    private Boolean autoCreateModel;

    private Boolean includeDdl;

    /** 当前目标领域已有模型上下文，用于让 AI 基于既有模型生成页面或补充模型。 */
    private List<LowcodeDataModelDTO> existingModels = new ArrayList<>();

    /** 当前已生成草稿上下文，用于追加需求时继续优化。 */
    private String draftContext;
}
