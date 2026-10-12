package com.mdframe.forge.plugin.generator.service.runtime.child;

import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/** Resolved runtime ChildFieldRule metadata. */
public record RuntimeChildFieldRule(String fieldName,
                                     String sourceField,
                                     String columnName,
                                     String label,
                                     boolean required,
                                     BigDecimal minValue,
                                     BigDecimal maxValue) {
    public static RuntimeChildFieldRule from(Map<String, Object> source, RuntimeFieldRef fieldRef) {
        Map<String, Object> props = firstMap(source.get("props"), source.get("basicProps"));
        return new RuntimeChildFieldRule(
                fieldRef.fieldName(),
                fieldRef.sourceField(),
                fieldRef.columnName(),
                StringUtils.defaultIfBlank(text(source.get("label")), text(source.get("rawLabel"))),
                readBoolean(source.get("required"), false),
                firstDecimal(source.get("min"), source.get("minimum"), props.get("min")),
                firstDecimal(source.get("max"), source.get("maximum"), props.get("max"))
        );
    }

    private static BigDecimal firstDecimal(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            BigDecimal decimal = toBigDecimal(value);
            if (decimal != null) {
                return decimal;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstMap(Object... values) {
        if (values == null) {
            return Map.of();
        }
        for (Object value : values) {
            if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                return (Map<String, Object>) map;
            }
        }
        return Map.of();
    }

    private static boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String textValue = String.valueOf(value).trim();
        if (StringUtils.isBlank(textValue)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(textValue)
                || "1".equals(textValue)
                || "yes".equalsIgnoreCase(textValue)
                || "Y".equalsIgnoreCase(textValue);
    }

    public static BigDecimal toBigDecimal(Object value) {
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

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
