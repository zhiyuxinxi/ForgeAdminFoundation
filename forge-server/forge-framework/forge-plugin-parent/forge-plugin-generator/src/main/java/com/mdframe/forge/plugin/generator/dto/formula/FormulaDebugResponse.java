package com.mdframe.forge.plugin.generator.dto.formula;

import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionStep;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 公式调试响应。
 */
@Data
@Builder
public class FormulaDebugResponse {

    private boolean success;

    private String traceId;

    private List<String> executionPlan;

    private List<FormulaExecutionStep> steps;

    private Map<String, Object> contextBefore;

    private Map<String, Object> contextAfter;

    private Map<String, Object> result;

    private Map<String, List<String>> errors;

    private long elapsedMs;
}
