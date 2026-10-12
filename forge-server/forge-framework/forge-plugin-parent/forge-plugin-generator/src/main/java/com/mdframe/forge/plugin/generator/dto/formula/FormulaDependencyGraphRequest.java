package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 公式依赖图请求。
 */
@Data
public class FormulaDependencyGraphRequest {

    private String objectCode;

    @NotNull
    private List<FormulaFieldConfig> formulas;

    @Data
    public static class FormulaFieldConfig {
        private String fieldCode;
        private String fieldName;
        private String type;
        private String mode;
        private String expression;
        private List<String> dependsOn;
        private AggregateGraph aggregate;
        private ConditionGraph condition;
        private LookupGraph lookup;
        private CrossObjectGraph crossObject;

        public String resolvedFieldCode() {
            if (fieldCode != null && !fieldCode.isBlank()) {
                return fieldCode;
            }
            return fieldName;
        }
    }

    @Data
    public static class AggregateGraph {
        private String function;
        private String relationCode;
        private String targetField;
        private String filter;
    }

    @Data
    public static class ConditionGraph {
        private String expression;
        private Object trueValue;
        private Object falseValue;
    }

    @Data
    public static class LookupGraph {
        private String relationCode;
        private String targetObjectCode;
        private String sourceField;
        private String targetField;
        private String returnField;
        private Object notFoundValue;
    }

    @Data
    public static class CrossObjectGraph {
        private String path;
        private String relationCode;
        private String targetObjectCode;
        private String returnField;
        private String recomputeMode;
    }
}
