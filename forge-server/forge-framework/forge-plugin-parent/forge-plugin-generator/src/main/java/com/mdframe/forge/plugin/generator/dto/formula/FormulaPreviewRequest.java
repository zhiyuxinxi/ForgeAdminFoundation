package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FormulaPreviewRequest {
    @NotBlank
    private String expression;
    private String type;
    private List<String> dependsOn;
    private Map<String, Object> sampleValues;
    private ConditionPreview condition;

    @Data
    public static class ConditionPreview {
        private String expression;
        private Object trueValue;
        private Object falseValue;
    }
}