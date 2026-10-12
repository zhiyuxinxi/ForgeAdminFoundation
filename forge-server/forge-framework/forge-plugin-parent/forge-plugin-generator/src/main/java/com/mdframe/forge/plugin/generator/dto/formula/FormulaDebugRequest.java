package com.mdframe.forge.plugin.generator.dto.formula;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 公式调试请求。
 */
@Data
public class FormulaDebugRequest {

    private String objectCode;

    private String recordId;

    /** 为空时调试全部公式；有值时只调试该字段及其公式依赖闭包。 */
    private String fieldCode;

    private Map<String, Object> sampleValues;

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
        private AggregateDebug aggregate;
        private ConditionDebug condition;
        private LookupDebug lookup;
        private CrossObjectDebug crossObject;

        public String resolvedFieldCode() {
            if (fieldCode != null && !fieldCode.isBlank()) {
                return fieldCode;
            }
            return fieldName;
        }
    }

    @Data
    public static class AggregateDebug {
        private String function;
        private String relationCode;
        private String targetField;
        private String filter;
    }

    @Data
    public static class ConditionDebug {
        private String expression;
        private Object trueValue;
        private Object falseValue;
    }

    @Data
    public static class LookupDebug {
        private String relationCode;
        private String targetObjectCode;
        private String sourceField;
        private String targetField;
        private String returnField;
        private Object notFoundValue;
    }

    @Data
    public static class CrossObjectDebug {
        private String path;
        private String relationCode;
        private String targetObjectCode;
        private String returnField;
        private String recomputeMode;
    }
}
