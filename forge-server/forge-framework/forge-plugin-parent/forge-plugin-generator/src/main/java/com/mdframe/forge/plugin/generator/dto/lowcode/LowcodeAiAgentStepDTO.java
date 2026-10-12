package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

/**
 * AI 业务系统生成过程步骤。用于前端展示可解释的生成进度，不保存为业务数据。
 */
@Data
public class LowcodeAiAgentStepDTO {

    private Integer orderNo;

    private String stepKey;

    private String title;

    /** pending/running/completed/error */
    private String status;

    private String message;

    private String summary;
}
