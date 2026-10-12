package com.mdframe.forge.plugin.generator.dto.formula;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 条件规则编译响应。
 */
@Data
@Builder
public class ConditionRuleCompileResponse {

    private boolean valid;

    private String expression;

    private List<String> dependencies;

    private List<String> errors;
}
