package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JSON and scalar values used only by the form asset catalog protocol. */
final class FormAssetValues {
    private FormAssetValues() {}

    static JSONObject readJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static JSONObject readNestedObject(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONObject() : JSON.parseObject(text);
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    static JSONArray readNestedArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof List<?> || value instanceof String) {
            try {
                String text = value instanceof String stringValue ? stringValue : JSON.toJSONString(value);
                return StringUtils.isBlank(text) ? new JSONArray() : JSON.parseArray(text);
            } catch (Exception e) {
                return new JSONArray();
            }
        }
        return new JSONArray();
    }

    static List<Map<String, Object>> readMapList(JSONArray array) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(new LinkedHashMap<>(item));
            }
        }
        return result;
    }

    static String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    static boolean readBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    static Boolean readNullableBooleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = StringUtils.trimToEmpty(String.valueOf(value));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
    }

    static Integer integerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static void putIfText(Map<String, Object> target, String key, Object value) {
        String text = StringUtils.trimToNull(value == null ? null : String.valueOf(value));
        if (text != null && !"null".equalsIgnoreCase(text)) {
            target.put(key, text);
        }
    }

    static void mergeNonNull(Map<String, Object> target, Map<String, Object> source) {
        if (target == null || source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (value != null) {
                target.put(key, value);
            }
        });
    }

    static Set<String> readStringSet(Object source) {
        Set<String> result = new LinkedHashSet<>();
        if (source == null) {
            return result;
        }
        if (source instanceof String text && !StringUtils.trimToEmpty(text).startsWith("[")) {
            for (String item : text.split(",")) {
                String value = StringUtils.trimToNull(item);
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        }
        JSONArray array = readNestedArray(source);
        for (int i = 0; i < array.size(); i++) {
            String value = StringUtils.trimToNull(textValue(array.get(i)));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
