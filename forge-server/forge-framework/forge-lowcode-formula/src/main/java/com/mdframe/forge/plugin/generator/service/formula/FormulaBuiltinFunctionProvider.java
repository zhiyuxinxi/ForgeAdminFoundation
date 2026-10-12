package com.mdframe.forge.plugin.generator.service.formula;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Built-in Java Bean functions exposed to Aviator through the formula registry.
 */
@Component("formulaBuiltinFunctionProvider")
public class FormulaBuiltinFunctionProvider {

    public Number mathAbs(Object value) {
        Number number = toNumber(value);
        if (isIntegral(number)) {
            return Math.abs(number.longValue());
        }
        return Math.abs(number.doubleValue());
    }

    public Number mathRound(Object value) {
        return Math.round(toNumber(value).doubleValue());
    }

    public Number mathFloor(Object value) {
        return Math.floor(toNumber(value).doubleValue());
    }

    public Number mathCeil(Object value) {
        return Math.ceil(toNumber(value).doubleValue());
    }

    public Number mathMax(Object left, Object right) {
        Number leftNumber = toNumber(left);
        Number rightNumber = toNumber(right);
        if (isIntegral(leftNumber) && isIntegral(rightNumber)) {
            return Math.max(leftNumber.longValue(), rightNumber.longValue());
        }
        return Math.max(leftNumber.doubleValue(), rightNumber.doubleValue());
    }

    public Number mathMin(Object left, Object right) {
        Number leftNumber = toNumber(left);
        Number rightNumber = toNumber(right);
        if (isIntegral(leftNumber) && isIntegral(rightNumber)) {
            return Math.min(leftNumber.longValue(), rightNumber.longValue());
        }
        return Math.min(leftNumber.doubleValue(), rightNumber.doubleValue());
    }

    public Number mathPow(Object left, Object right) {
        return Math.pow(toNumber(left).doubleValue(), toNumber(right).doubleValue());
    }

    public Number mathSqrt(Object value) {
        return Math.sqrt(toNumber(value).doubleValue());
    }

    public Number stringLength(Object value) {
        return String.valueOf(value).length();
    }

    public Boolean stringContains(Object value, Object keyword) {
        return String.valueOf(value).contains(String.valueOf(keyword));
    }

    public Boolean stringStartsWith(Object value, Object prefix) {
        return String.valueOf(value).startsWith(String.valueOf(prefix));
    }

    public Boolean stringEndsWith(Object value, Object suffix) {
        return String.valueOf(value).endsWith(String.valueOf(suffix));
    }

    public Number stringIndexOf(Object value, Object keyword) {
        return String.valueOf(value).indexOf(String.valueOf(keyword));
    }

    public String stringReplace(Object value, Object target, Object replacement) {
        return String.valueOf(value).replace(String.valueOf(target), String.valueOf(replacement));
    }

    public List<Object> seqList(Object... values) {
        return List.of(values);
    }

    public LinkedHashSet<Object> seqSet(Object... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    public Map<Object, Object> seqMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("seq.map requires key/value pairs");
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    public String dateToString(Object value, Object pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(String.valueOf(pattern));
        if (value instanceof LocalDateTime localDateTime) {
            return formatter.format(localDateTime);
        }
        if (value instanceof TemporalAccessor temporalAccessor) {
            return formatter.format(temporalAccessor);
        }
        if (value instanceof Date date) {
            return formatter.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }
        if (value instanceof Number epochMillis) {
            return formatter.format(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis.longValue()), ZoneId.systemDefault()));
        }
        return formatter.format(LocalDateTime.parse(String.valueOf(value)));
    }

    private Number toNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value == null) {
            throw new IllegalArgumentException("number value must not be null");
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private boolean isIntegral(Number value) {
        return value instanceof Byte
            || value instanceof Short
            || value instanceof Integer
            || value instanceof Long;
    }
}
