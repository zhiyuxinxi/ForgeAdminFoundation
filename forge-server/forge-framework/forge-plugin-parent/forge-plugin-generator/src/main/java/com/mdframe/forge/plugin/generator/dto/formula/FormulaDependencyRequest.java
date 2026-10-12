package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FormulaDependencyRequest {
    @NotNull
    private List<FormulaFieldConfig> formulas;

    @Data
    public static class FormulaFieldConfig {
        private String fieldName;
        private String expression;
        private String type;
        private List<String> dependsOn;
    }
}