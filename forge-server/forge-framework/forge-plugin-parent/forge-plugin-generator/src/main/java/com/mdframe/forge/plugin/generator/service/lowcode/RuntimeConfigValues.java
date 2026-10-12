package com.mdframe.forge.plugin.generator.service.lowcode;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Protocol scalar conversion and ordered value copying. */
final class RuntimeConfigValues {

    private RuntimeConfigValues() {
    }

    static final String MASTER_DETAIL_LAYOUT = "master-detail-crud";
    static final Set<String> SYSTEM_FIELD_NAMES = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    static final Set<String> SYSTEM_COLUMN_NAMES = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    static Object firstPresent(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static Object firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (StringUtils.isNotBlank(text(value))) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> copyRuleList(Object source) {
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    static boolean isRequiredSwitchEnabled(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)
                && !"false".equalsIgnoreCase(text) && !"0".equals(text)) {
            return true;
        }
        return booleanWithDefault(value, false);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    static Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static boolean booleanWithDefault(Object value, boolean defaultValue) {
        Boolean bool = booleanValue(value);
        return bool == null ? defaultValue : bool;
    }

    static Integer integerValue(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int intValue(Object value, int defaultValue) {
        Integer parsed = integerValue(value);
        return parsed == null ? defaultValue : parsed;
    }

    static String safeKey(String value) {
        String key = StringUtils.defaultIfBlank(value, "model").replaceAll("[^A-Za-z0-9_]", "_");
        return StringUtils.defaultIfBlank(key, "model");
    }

    static String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    static String snakeToCamel(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    static void copyOption(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }
}
