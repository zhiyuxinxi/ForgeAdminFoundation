package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 生成决策摘要。只暴露结构化依据和结论，避免把内部推理过程作为业务协议。
 */
@Data
public class LowcodeAiDecisionDTO {

    private String decisionType;

    private String title;

    private String target;

    private String value;

    private String reason;

    private Map<String, Object> meta = new LinkedHashMap<>();
}
