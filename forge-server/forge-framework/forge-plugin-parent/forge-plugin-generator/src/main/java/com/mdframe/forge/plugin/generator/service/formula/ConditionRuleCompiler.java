package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.mdframe.forge.plugin.generator.domain.formula.ConditionRuleNode;
import com.mdframe.forge.plugin.generator.domain.formula.ConditionRuleOperator;
import com.mdframe.forge.plugin.generator.domain.formula.ExpressionParser;
import com.mdframe.forge.plugin.generator.dto.formula.ConditionRuleCompileRequest;
import com.mdframe.forge.plugin.generator.dto.formula.ConditionRuleCompileResponse;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compiles condition rule AST into a restricted Aviator expression.
 */
@Service
public class ConditionRuleCompiler {

    private static final Pattern FIELD_PATTERN =
        Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final ExpressionParser expressionParser = new ExpressionParser();

    public ConditionRuleCompileResponse compile(ConditionRuleCompileRequest request) {
        CompileContext context = new CompileContext(buildFieldMap(request));
        String expression = "";

        if (request == null || request.getRule() == null) {
            context.errors.add(UiMessages.translate("条件规则不能为空"));
        } else {
            expression = compileNode(request.getRule(), "$", context);
        }

        if (context.errors.isEmpty()) {
            var parseResult = expressionParser.parse(expression);
            if (!parseResult.isValid()) {
                context.errors.add(UiMessages.translate("条件规则表达式语法错误: ") + parseResult.getErrorMessage());
            }
        }

        boolean valid = context.errors.isEmpty();
        return ConditionRuleCompileResponse.builder()
            .valid(valid)
            .expression(valid ? expression : null)
            .dependencies(new ArrayList<>(context.dependencies))
            .errors(new ArrayList<>(context.errors))
            .build();
    }

    public ConditionRuleCompileResponse validate(ConditionRuleCompileRequest request) {
        return compile(request);
    }

    private String compileNode(ConditionRuleNode node, String path, CompileContext context) {
        if (node == null) {
            context.errors.add(path + UiMessages.translate(" 节点不能为空"));
            return "";
        }
        if (node.isGroup()) {
            return compileGroup(node, path, context);
        }
        return compileCondition(node, path, context);
    }

    private String compileGroup(ConditionRuleNode node, String path, CompileContext context) {
        ConditionRuleOperator operator;
        try {
            operator = ConditionRuleOperator.parse(node.getOperator());
        } catch (Exception e) {
            context.errors.add(path + UiMessages.translate(" 分组操作符仅支持 AND / OR"));
            return "";
        }
        if (!operator.isGroupOperator()) {
            context.errors.add(path + UiMessages.translate(" 分组操作符仅支持 AND / OR"));
            return "";
        }
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            context.errors.add(path + UiMessages.translate(" 分组不能为空"));
            return "";
        }

        List<String> compiledChildren = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            String compiled = compileNode(node.getChildren().get(i), path + ".children[" + i + "]", context);
            if (!compiled.isBlank()) {
                compiledChildren.add(compiled);
            }
        }
        if (compiledChildren.isEmpty()) {
            return "";
        }
        String joiner = operator == ConditionRuleOperator.AND ? " && " : " || ";
        return "(" + String.join(joiner, compiledChildren) + ")";
    }

    private String compileCondition(ConditionRuleNode node, String path, CompileContext context) {
        String field = trimToNull(node.getField());
        if (field == null) {
            context.errors.add(path + UiMessages.translate(" 条件字段不能为空"));
            return "";
        }
        if (!FIELD_PATTERN.matcher(field).matches()) {
            context.errors.add(path + UiMessages.translate(" 条件字段格式非法: ") + field);
            return "";
        }

        ConditionRuleOperator operator;
        try {
            operator = ConditionRuleOperator.parse(node.resolvedConditionOperator());
        } catch (Exception e) {
            context.errors.add(path + UiMessages.translate(" 不支持的条件操作符: ") + node.resolvedConditionOperator());
            return "";
        }
        if (operator.isGroupOperator()) {
            context.errors.add(path + UiMessages.translate(" 条件节点不能使用分组操作符: ") + operator.name());
            return "";
        }

        FieldMeta fieldMeta = context.fields.get(field);
        if (!context.fields.isEmpty() && fieldMeta == null) {
            context.errors.add(path + UiMessages.translate(" 条件字段不存在: ") + field);
            return "";
        }

        validateValue(path, field, operator, node.getValue(), fieldMeta, context);
        context.dependencies.add(field);
        return compileConditionExpression(field, operator, node.getValue());
    }

    private String compileConditionExpression(String field, ConditionRuleOperator operator, Object value) {
        return switch (operator) {
            case EQ -> field + " == " + literal(value);
            case NE -> field + " != " + literal(value);
            case GT -> field + " > " + literal(value);
            case GTE -> field + " >= " + literal(value);
            case LT -> field + " < " + literal(value);
            case LTE -> field + " <= " + literal(value);
            case IN -> compileIn(field, normalizeValues(value), false);
            case NOT_IN -> compileIn(field, normalizeValues(value), true);
            case CONTAINS -> "string.contains(" + field + ", " + literal(value) + ")";
            case STARTS_WITH -> "string.startsWith(" + field + ", " + literal(value) + ")";
            case ENDS_WITH -> "string.endsWith(" + field + ", " + literal(value) + ")";
            case IS_NULL -> field + " == nil";
            case NOT_NULL -> field + " != nil";
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    private String compileIn(String field, List<Object> values, boolean notIn) {
        List<String> parts = values.stream()
            .map(value -> field + (notIn ? " != " : " == ") + literal(value))
            .toList();
        String joiner = notIn ? " && " : " || ";
        return "(" + String.join(joiner, parts) + ")";
    }

    private void validateValue(String path,
                               String field,
                               ConditionRuleOperator operator,
                               Object value,
                               FieldMeta fieldMeta,
                               CompileContext context) {
        if (!operator.requiresValue()) {
            return;
        }
        if (operator.requiresCollectionValue()) {
            if (!isCollectionLike(value)) {
                context.errors.add(path + UiMessages.translate(" 操作符 ") + operator.name() + UiMessages.translate(" 需要集合类型的条件值"));
                return;
            }
            List<Object> values = normalizeValues(value);
            if (values.isEmpty()) {
                context.errors.add(path + UiMessages.translate(" 操作符 ") + operator.name() + UiMessages.translate(" 的值不能为空集合"));
            }
            for (Object item : values) {
                validateSingleValue(path, field, operator, item, fieldMeta, context);
            }
            return;
        }
        if (value == null) {
            context.errors.add(path + UiMessages.translate(" 操作符 ") + operator.name() + UiMessages.translate(" 的值不能为空"));
            return;
        }
        validateSingleValue(path, field, operator, value, fieldMeta, context);
    }

    private void validateSingleValue(String path,
                                     String field,
                                     ConditionRuleOperator operator,
                                     Object value,
                                     FieldMeta fieldMeta,
                                     CompileContext context) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?>) {
            context.errors.add(path + UiMessages.translate(" 条件值不支持对象类型"));
            return;
        }
        String type = fieldMeta == null ? null : normalizeType(fieldMeta.type());
        if (type == null) {
            return;
        }
        if (isNumericType(type) && !(value instanceof Number)) {
            context.errors.add(path + UiMessages.translate(" 字段 ") + field + UiMessages.translate(" 需要数字类型的条件值"));
        }
        if (isBooleanType(type) && !(value instanceof Boolean)) {
            context.errors.add(path + UiMessages.translate(" 字段 ") + field + UiMessages.translate(" 需要布尔类型的条件值"));
        }
        if (operator.isStringOperator() && !isTextType(type)) {
            context.errors.add(path + UiMessages.translate(" 操作符 ") + operator.name() + UiMessages.translate(" 仅支持文本字段"));
        }
    }

    private String literal(Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Boolean bool) {
            return Boolean.TRUE.equals(bool) ? "true" : "false";
        }
        if (value instanceof Number number) {
            return normalizeNumber(number);
        }
        return "'" + escapeString(String.valueOf(value)) + "'";
    }

    private String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String normalizeNumber(Number number) {
        if (number instanceof Float || number instanceof Double || number instanceof BigDecimal) {
            return new BigDecimal(String.valueOf(number)).stripTrailingZeros().toPlainString();
        }
        return number.toString();
    }

    private List<Object> normalizeValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(Array.get(value, i));
            }
            return result;
        }
        return List.of(value);
    }

    private boolean isCollectionLike(Object value) {
        return value instanceof Collection<?> || (value != null && value.getClass().isArray());
    }

    private Map<String, FieldMeta> buildFieldMap(ConditionRuleCompileRequest request) {
        Map<String, FieldMeta> result = new LinkedHashMap<>();
        if (request == null || request.getFields() == null) {
            return result;
        }
        for (ConditionRuleCompileRequest.FieldDefinition field : request.getFields()) {
            String fieldCode = trimToNull(field == null ? null : field.resolvedFieldCode());
            if (fieldCode == null) {
                continue;
            }
            result.putIfAbsent(fieldCode, new FieldMeta(fieldCode, trimToNull(field.resolvedType())));
        }
        return result;
    }

    private boolean isNumericType(String type) {
        return type.contains("int")
            || type.contains("long")
            || type.contains("double")
            || type.contains("float")
            || type.contains("decimal")
            || type.contains("number")
            || type.contains("money")
            || type.contains("amount");
    }

    private boolean isBooleanType(String type) {
        return type.contains("bool");
    }

    private boolean isTextType(String type) {
        return type.contains("char")
            || type.contains("text")
            || type.contains("string")
            || type.contains("varchar");
    }

    private String normalizeType(String type) {
        String value = trimToNull(type);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record FieldMeta(String fieldCode, String type) {
    }

    private static class CompileContext {
        private final Map<String, FieldMeta> fields;
        private final Set<String> dependencies = new LinkedHashSet<>();
        private final List<String> errors = new ArrayList<>();

        private CompileContext(Map<String, FieldMeta> fields) {
            this.fields = fields;
        }
    }
}
