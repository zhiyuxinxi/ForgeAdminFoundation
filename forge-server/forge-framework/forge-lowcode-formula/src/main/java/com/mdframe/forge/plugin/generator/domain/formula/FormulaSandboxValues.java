package com.mdframe.forge.plugin.generator.domain.formula;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Bounded data copies shared by expression inputs and registered function results. */
public final class FormulaSandboxValues {
    private FormulaSandboxValues() {
    }

    /** Copy only bounded data values, never expose a bean, Class or reflection object. */
    public static Object copy(Object value) {
        return copy(value, 0, new int[]{0});
    }

    private static Object copy(Object value, int depth, int[] count) {
        if (depth > 16 || ++count[0] > 10000) {
            throw new IllegalArgumentException(UiMessages.label("公式数据超过上限", "Formula data exceeds the limit"));
        }
        if (value instanceof java.math.BigInteger integer && integer.bitLength() > 256) throw resourceLimit();
        if (value instanceof java.math.BigDecimal decimal && (decimal.precision() > 128 || Math.abs((long) decimal.scale()) > 128)) throw resourceLimit();
        if (value == null || value instanceof Boolean || value instanceof Character
            || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
            || value instanceof Float || value instanceof Double || value instanceof java.math.BigDecimal
            || value instanceof java.math.BigInteger || value instanceof java.time.LocalDate
            || value instanceof java.time.LocalDateTime || value instanceof java.time.LocalTime
            || value instanceof java.time.Instant || value instanceof java.time.OffsetDateTime
            || value instanceof java.time.ZonedDateTime) return value;
        if (value instanceof String text && text.length() <= 65536) return text;
        if (value instanceof java.util.Date date) return new java.util.Date(date.getTime());
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key) || key.startsWith("__") || key.length() > 256) {
                    throw new IllegalArgumentException(UiMessages.label("公式数据键不合法", "Invalid formula data key"));
                }
                copy.put(key, copy(entry.getValue(), depth + 1, count));
            }
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            Collection<Object> copy = value instanceof Set<?> ? new LinkedHashSet<>() : new ArrayList<>();
            for (Object item : collection) copy.add(copy(item, depth + 1, count));
            return copy;
        }
        throw new IllegalArgumentException(UiMessages.label("公式不允许访问宿主对象", "Host objects are not allowed in formulas"));
    }

    private static IllegalArgumentException resourceLimit() {
        return new IllegalArgumentException(UiMessages.label("公式计算超出受限表达式范围", "Formula exceeds the restricted expression limits"));
    }
}
