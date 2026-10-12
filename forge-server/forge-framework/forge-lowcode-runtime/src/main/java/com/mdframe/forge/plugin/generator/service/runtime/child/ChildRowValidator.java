package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.firstPresent;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isImmutableWriteField;
import static com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeChildFieldRule.toBigDecimal;

/** Validates child field constraints without accessing storage or changing request values. */
public final class ChildRowValidator {
    private ChildRowValidator() {
    }

    public static void validateChildRow(RuntimeChildRelation relation, Map<String, Object> row, boolean create) {
        if (relation == null || row == null || relation.fieldRules() == null || relation.fieldRules().isEmpty()) {
            return;
        }
        for (RuntimeChildFieldRule rule : relation.fieldRules().values()) {
            if (isSystemChildValidationRule(relation, rule)) {
                continue;
            }
            Object value = firstPresent(row, rule.sourceField(), rule.fieldName(), rule.columnName());
            boolean provided = containsAnyKey(row, rule.sourceField(), rule.fieldName(), rule.columnName());
            if (rule.required() && (create || provided) && isBlankChildValue(value)) {
                throw new BusinessException(StringUtils.defaultIfBlank(rule.label(), rule.sourceField()) + UiMessages.translate("不能为空"));
            }
            if (!isBlankChildValue(value)) {
                validateChildNumberRange(rule, value);
            }
        }
    }

    private static boolean isSystemChildValidationRule(RuntimeChildRelation relation, RuntimeChildFieldRule rule) {
        if (rule == null) {
            return true;
        }
        return isImmutableWriteField(rule.fieldName())
                || isImmutableWriteField(rule.sourceField())
                || isImmutableWriteField(rule.columnName())
                || StringUtils.equals(rule.columnName(), relation.childFkColumn());
    }

    private static void validateChildNumberRange(RuntimeChildFieldRule rule, Object value) {
        if (rule.minValue() == null && rule.maxValue() == null) {
            return;
        }
        BigDecimal number = toBigDecimal(value);
        if (number == null) {
            throw new BusinessException(StringUtils.defaultIfBlank(rule.label(), rule.sourceField()) + UiMessages.translate("必须为有效数字"));
        }
        if (rule.minValue() != null && number.compareTo(rule.minValue()) < 0) {
            throw new BusinessException(StringUtils.defaultIfBlank(rule.label(), rule.sourceField()) + UiMessages.translate("不能小于") + rule.minValue());
        }
        if (rule.maxValue() != null && number.compareTo(rule.maxValue()) > 0) {
            throw new BusinessException(StringUtils.defaultIfBlank(rule.label(), rule.sourceField()) + UiMessages.translate("不能大于") + rule.maxValue());
        }
    }

    private static boolean containsAnyKey(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && row.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlankChildValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return StringUtils.isBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

}
