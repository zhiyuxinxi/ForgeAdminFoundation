package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 编码规则变量说明。
 */
@Data
public class CodeRuleTokenVO {

    private String token;

    private String insertText;

    private String label;

    private String groupName;

    private String description;

    private String example;

    private String sampleValue;
}
