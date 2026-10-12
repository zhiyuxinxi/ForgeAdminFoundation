package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowRuntimeMetadata.readJsonObject;

/** Renders configured business record summaries and field aliases for read-only views. */
final class BusinessFlowDisplayText {
    private BusinessFlowDisplayText() { }

    static String resolveBusinessSummary(BusinessObjectVO object,
                                          JSONObject bindingConfig,
                                          Map<String, Object> recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return null;
        }
        JSONObject objectOptions = object == null ? new JSONObject() : readJsonObject(object.getOptions());
        JSONObject designerOptions = object == null ? new JSONObject() : readJsonObject(object.getDesignerOptions());
        String template = StringUtils.firstNonBlank(
                StringUtils.trimToNull(objectOptions.getString("summaryExpression")),
                StringUtils.trimToNull(designerOptions.getString("summaryExpression")),
                bindingConfig == null ? null : StringUtils.trimToNull(bindingConfig.getString("titleTemplate")));
        if (StringUtils.isNotBlank(template)) {
            String resolved = template;
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                resolved = replaceTemplateValue(resolved, entry.getKey(), entry.getValue());
            }
            resolved = StringUtils.trimToNull(resolved);
            if (resolved != null) {
                return resolved;
            }
        }
        String displayField = object == null ? null : StringUtils.trimToNull(object.getDisplayField());
        Object displayValue = displayField == null ? null : readRecordValue(recordData, displayField);
        if (displayValue != null && StringUtils.isNotBlank(String.valueOf(displayValue))) {
            return String.valueOf(displayValue);
        }
        for (String field : List.of("orderNo", "businessNo", "title", "name", "code")) {
            Object value = readRecordValue(recordData, field);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    static String replaceTemplateValue(String template, String key, Object value) {
        if (StringUtils.isBlank(template) || StringUtils.isBlank(key)) {
            return template;
        }
        String text = value != null ? String.valueOf(value) : "";
        String result = replaceTemplateToken(template, key, text);
        result = replaceTemplateToken(result, snakeToCamel(key), text);
        return replaceTemplateToken(result, camelToSnake(key), text);
    }

    static String replaceTemplateToken(String template, String key, String value) {
        if (StringUtils.isBlank(key)) {
            return template;
        }
        return template.replace("${" + key + "}", value)
                .replace("{" + key + "}", value);
    }

    static Object readRecordValue(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return null;
        }
        Object value = readRecordValueFromFlatMap(recordData, field);
        if (value != null || containsRecordField(recordData, field)) {
            return value;
        }
        Object main = recordData.get("main");
        if (main instanceof Map<?, ?> mainMap) {
            Map<String, Object> mainRecord = new LinkedHashMap<>();
            mainMap.forEach((key, item) -> {
                if (key != null) {
                    mainRecord.put(String.valueOf(key), item);
                }
            });
            return readRecordValueFromFlatMap(mainRecord, field);
        }
        return null;
    }

    static Object readRecordValueFromFlatMap(Map<String, Object> recordData, String field) {
        if (recordData.containsKey(field)) {
            return recordData.get(field);
        }
        String camelField = snakeToCamel(field);
        if (recordData.containsKey(camelField)) {
            return recordData.get(camelField);
        }
        String snakeField = camelToSnake(field);
        if (recordData.containsKey(snakeField)) {
            return recordData.get(snakeField);
        }
        return null;
    }

    static boolean containsRecordField(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return false;
        }
        return recordData.containsKey(field)
                || recordData.containsKey(snakeToCamel(field))
                || recordData.containsKey(camelToSnake(field));
    }

    static String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
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

    static String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
