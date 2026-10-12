package com.mdframe.forge.plugin.ai.prompt.dto;

import lombok.Data;

/**
 * 提示词模板查询条件。
 */
@Data
public class AiPromptTemplateQuery {

    private String keyword;

    private String templateName;

    private String usageScene;

    private String businessCategory;

    private String domainCategory;

    private String status;

    private String isRecommended;

    private Integer limit;
}
