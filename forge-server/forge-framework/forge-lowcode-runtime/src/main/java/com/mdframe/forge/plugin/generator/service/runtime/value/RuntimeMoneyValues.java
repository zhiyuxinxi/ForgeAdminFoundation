package com.mdframe.forge.plugin.generator.service.runtime.value;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.util.*;

/** Owns integer-backed money field rules and exact display/storage conversions. */
@RequiredArgsConstructor
public final class RuntimeMoneyValues {
    private final RuntimeFieldMetadata metadata;

    public String key(Map<String, Object> data, MoneyFieldContract field) {
        String camelColumn = DynamicQueryGenerator.snakeToCamel(field.columnName());
        for (String candidate : List.of(field.fieldName(), field.columnName(), camelColumn)) {
            if (StringUtils.isNotBlank(candidate) && data.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public List<MoneyFieldContract> fields(LowcodeRuntimeConfig config) {
        LowcodeModelSchema modelSchema = metadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return List.of();
        }
        List<MoneyFieldContract> result = new ArrayList<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || !isMinorMoneyField(field)) {
                continue;
            }
            String fieldName = StringUtils.trimToNull(field.getField());
            if (fieldName == null) {
                continue;
            }
            String columnName = StringUtils.defaultIfBlank(
                    StringUtils.trimToNull(field.getColumnName()),
                    DynamicQueryGenerator.camelToSnake(fieldName));
            if (columnName == null) {
                continue;
            }
            int scale = field.getPrecision() == null ? 2 : field.getPrecision();
            if (scale < 0 || scale > 6) {
                throw new BusinessException(UiMessages.translate("MONEY 字段小数位配置不正确: ") + fieldName);
            }
            Map<String, Object> basicProps = field.getBasicProps() == null ? Map.of() : field.getBasicProps();
            result.add(new MoneyFieldContract(
                    fieldName,
                    columnName,
                    scale,
                    decimal(basicProps.get("min")),
                    decimal(basicProps.get("max"))));
        }
        return result;
    }

    private boolean isMinorMoneyField(LowcodeFieldSchema field) {
        String businessType = StringUtils.upperCase(StringUtils.trimToEmpty(field.getBusinessFieldType()));
        String componentType = StringUtils.lowerCase(StringUtils.trimToEmpty(field.getComponentType()));
        String dataType = StringUtils.lowerCase(StringUtils.trimToEmpty(field.getDataType()));
        boolean money = "MONEY".equals(businessType) || "money".equals(componentType);
        return money && Set.of("tinyint", "smallint", "mediumint", "int", "integer", "bigint", "long").contains(dataType);
    }

    public static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public record MoneyFieldContract(String fieldName,
                                      String columnName,
                                      int scale,
                                      BigDecimal minValue,
                                      BigDecimal maxValue) {
    }

    public void money(List<Map<String, Object>> rows, LowcodeRuntimeConfig config) {
        if (rows == null || rows.isEmpty() || config == null) {
            return;
        }
        List<MoneyFieldContract> fields = fields(config);
        if (fields.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (MoneyFieldContract field : fields) {
                String key = key(row, field);
                if (key == null || row.get(key) == null) {
                    continue;
                }
                row.put(key, fromMinorMoney(field, row.get(key)));
            }
        }
    }

    private BigDecimal fromMinorMoney(MoneyFieldContract field, Object rawValue) {
        try {
            return new BigDecimal(String.valueOf(rawValue)).movePointLeft(field.scale());
        } catch (NumberFormatException e) {
            throw new BusinessException(UiMessages.translate("金额存储值格式不正确: ") + field.fieldName());
        }
    }

    public void store(Map<String, Object> data, LowcodeRuntimeConfig config) {
        if (data == null || data.isEmpty() || config == null) {
            return;
        }
        for (MoneyFieldContract field : fields(config)) {
            String key = key(data, field);
            if (key == null || data.get(key) == null) {
                continue;
            }
            data.put(key, toMinorMoney(field, data.get(key)));
        }
    }

    private Long toMinorMoney(MoneyFieldContract field, Object rawValue) {
        try {
            String text = String.valueOf(rawValue).trim();
            if (StringUtils.isBlank(text)) {
                return null;
            }
            BigDecimal amount = new BigDecimal(text).stripTrailingZeros();
            if (amount.scale() > field.scale()) {
                throw new BusinessException(UiMessages.translate("金额输入超过允许的小数位，禁止静默舍入: ") + field.fieldName());
            }
            if (field.minValue() != null && amount.compareTo(field.minValue()) < 0) {
                throw new BusinessException(UiMessages.translate("金额输入小于最小值: ") + field.fieldName());
            }
            if (field.maxValue() != null && amount.compareTo(field.maxValue()) > 0) {
                throw new BusinessException(UiMessages.translate("金额输入大于最大值: ") + field.fieldName());
            }
            return amount.movePointRight(field.scale()).longValueExact();
        } catch (BusinessException e) {
            throw e;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new BusinessException(UiMessages.translate("金额输入格式不正确: ") + field.fieldName());
        }
    }
}
