package com.mdframe.forge.plugin.ai.prompt.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词模板列表视图。
 */
@Data
public class AiPromptTemplateVO {

    private Long id;

    private Long tenantId;

    private String templateName;

    private String templateCode;

    private String usageScene;

    private String businessCategory;

    private String domainCategory;

    private String templateTags;

    private String description;

    private String contentSummary;

    private String exampleInput;

    private String status;

    private String isRecommended;

    private Integer sortOrder;

    private Integer useCount;

    private Integer testCount;

    private Integer downloadCount;

    private String remark;

    private Long createBy;

    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Long updateBy;

    private String updaterName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
