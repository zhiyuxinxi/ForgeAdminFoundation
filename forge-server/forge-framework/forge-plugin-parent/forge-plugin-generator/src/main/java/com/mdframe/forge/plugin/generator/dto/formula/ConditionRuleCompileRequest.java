package com.mdframe.forge.plugin.generator.dto.formula;

import com.mdframe.forge.plugin.generator.domain.formula.ConditionRuleNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 条件规则编译请求。
 */
@Data
public class ConditionRuleCompileRequest {

    @NotNull
    private ConditionRuleNode rule;

    /** 当前对象字段元数据；为空时只校验 AST 结构和操作符。 */
    private List<FieldDefinition> fields;

    @Data
    public static class FieldDefinition {
        private String fieldCode;
        private String fieldName;
        private String dataType;
        private String valueType;

        public String resolvedFieldCode() {
            if (fieldCode != null && !fieldCode.isBlank()) {
                return fieldCode.trim();
            }
            return fieldName == null ? null : fieldName.trim();
        }

        public String resolvedType() {
            if (dataType != null && !dataType.isBlank()) {
                return dataType.trim();
            }
            return valueType == null ? null : valueType.trim();
        }
    }
}
