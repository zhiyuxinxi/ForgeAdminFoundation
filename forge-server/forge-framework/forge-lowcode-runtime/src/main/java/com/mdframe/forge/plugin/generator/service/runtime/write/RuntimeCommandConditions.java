package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.util.*;

import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeMoneyValues;

/** Compiles command state guards and numeric comparisons into parameterized predicates. */
@lombok.RequiredArgsConstructor
public final class RuntimeCommandConditions {
    private final RuntimeCommandFields fields;
    private final RuntimePrivacyWrite privacyWrite;

    public DynamicCrudRepository.SqlCondition expected(
            LowcodeRuntimeConfig config,
            String tableName,
            Map<String, Object> expectedFields) {
        if (expectedFields == null || expectedFields.isEmpty()) {
            return null;
        }
        Map<String, Object> mapped = fields.write(config, tableName, expectedFields);
        privacyWrite.encrypt(mapped, config.getEncryptConfig());
        List<String> expressions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, Object> entry : mapped.entrySet()) {
            String param = "commandExpected" + index++;
            if (entry.getValue() == null) {
                expressions.add(entry.getKey() + " IS NULL");
            } else {
                expressions.add(entry.getKey() + " = :" + param);
                params.put(param, entry.getValue());
            }
        }
        return expressions.isEmpty() ? null
                : new DynamicCrudRepository.SqlCondition(String.join(" AND ", expressions), params);
    }

    public DynamicCrudRepository.SqlCondition numeric(
            LowcodeRuntimeConfig config,
            String tableName,
            List<Map<String, Object>> numericConstraints) {
        if (numericConstraints == null || numericConstraints.isEmpty()) {
            return null;
        }
        List<String> expressions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        int index = 0;
        for (Map<String, Object> constraint : numericConstraints) {
            if (constraint == null || constraint.isEmpty()) {
                continue;
            }
            String field = StringUtils.trimToNull(String.valueOf(constraint.get("field")));
            String operator = StringUtils.trimToNull(String.valueOf(constraint.get("operator")));
            Object value = constraint.get("value");
            if (field == null || operator == null || value == null) {
                throw new BusinessException("数值比较配置不完整");
            }
            Map<String, Object> mapped = fields.write(
                    config, tableName, Map.of(field, value));
            String column = mapped.keySet().stream().findFirst()
                    .orElseThrow(() -> new BusinessException(UiMessages.translate("数值比较字段无效: ") + field));
            BigDecimal number = RuntimeMoneyValues.decimal(value);
            if (number == null) {
                throw new BusinessException(UiMessages.translate("数值比较值必须为有效数字: ") + field);
            }
            String normalizedOperator = operator.toLowerCase(Locale.ROOT);
            String sqlOperator = switch (normalizedOperator) {
                case "gt" -> ">";
                case "gte" -> ">=";
                case "lt" -> "<";
                case "lte" -> "<=";
                case "eq" -> "=";
                case "neq" -> "<>";
                default -> throw new BusinessException(UiMessages.translate("不支持的数值比较操作符: ") + operator);
            };
            String parameter = "commandNumeric" + index++;
            expressions.add(column + " " + sqlOperator + " :" + parameter);
            params.put(parameter, number);
        }
        return expressions.isEmpty() ? null
                : new DynamicCrudRepository.SqlCondition(String.join(" AND ", expressions), params);
    }

    public DynamicCrudRepository.SqlCondition combine(
            DynamicCrudRepository.SqlCondition first,
            DynamicCrudRepository.SqlCondition second) {
        if (first == null || StringUtils.isBlank(first.sql())) {
            return second;
        }
        if (second == null || StringUtils.isBlank(second.sql())) {
            return first;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        if (first.params() != null) {
            params.putAll(first.params());
        }
        if (second.params() != null) {
            for (Map.Entry<String, Object> entry : second.params().entrySet()) {
                if (params.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                    throw new BusinessException("事务命令条件参数冲突");
                }
            }
        }
        return new DynamicCrudRepository.SqlCondition(
                "(" + first.sql() + ") AND (" + second.sql() + ")", params);
    }
}
