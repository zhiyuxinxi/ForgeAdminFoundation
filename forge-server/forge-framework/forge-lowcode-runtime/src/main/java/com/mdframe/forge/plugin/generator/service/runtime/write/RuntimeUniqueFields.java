package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeUniqueConstraintSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Owns unique-field aliases and values; preserves explicit-null fallback to before/default values. */
@lombok.RequiredArgsConstructor
public final class RuntimeUniqueFields {
    private final RuntimeEditFieldReader editReader;
    private final RuntimeWriteFieldMapping writeMapping;

    Map<String, LowcodeFieldSchema> resolve(LowcodeRuntimeConfig config, String tableName, LowcodeModelSchema model) {
        Map<String, LowcodeFieldSchema> result = buildModelFieldAliasMap(model);
        appendEditSchemaFieldAliases(config, tableName, result);
        return result;
    }

    Object value(LowcodeFieldSchema field, Map<String, Object> data, Map<String, Object> before,
                 LowcodeUniqueConstraintSchema constraint) {
        return normalizeUniqueValue(resolveFieldValue(field, data, before), constraint);
    }

    private Map<String, LowcodeFieldSchema> buildModelFieldAliasMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> result = new LinkedHashMap<>();
        if (modelSchema == null || modelSchema.getFields() == null) {
            return result;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null) {
                continue;
            }
            putFieldAlias(result, field.getField(), field);
            putFieldAlias(result, field.getColumnName(), field);
        }
        return result;
    }

    private void putFieldAlias(Map<String, LowcodeFieldSchema> fields, String alias, LowcodeFieldSchema field) {
        if (StringUtils.isBlank(alias)) {
            return;
        }
        fields.putIfAbsent(alias, field);
        fields.putIfAbsent(DynamicQueryGenerator.snakeToCamel(alias), field);
        fields.putIfAbsent(DynamicQueryGenerator.camelToSnake(alias), field);
    }

    private void appendEditSchemaFieldAliases(LowcodeRuntimeConfig config,
                                              String tableName,
                                              Map<String, LowcodeFieldSchema> fieldMap) {
        List<Map<String, Object>> editFields = editReader.read(config);
        if (editFields.isEmpty()) {
            return;
        }
        Map<String, String> columnMapping = writeMapping.columns(config, tableName);
        for (Map<String, Object> editField : editFields) {
            String fieldName = text(editField.get("field"));
            if (StringUtils.isBlank(fieldName) || fieldMap.containsKey(fieldName)) {
                continue;
            }
            LowcodeFieldSchema field = new LowcodeFieldSchema();
            field.setField(fieldName);
            field.setColumnName(columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName)));
            field.setLabel(text(editField.get("label")));
            field.setAdvancedProps(new LinkedHashMap<>(mapFrom(editField.get("advancedProps"))));
            putFieldAlias(fieldMap, field.getField(), field);
            putFieldAlias(fieldMap, field.getColumnName(), field);
        }
    }

    boolean containsInput(Map<String, Object> data, LowcodeFieldSchema field) {
        if (data == null || field == null) {
            return false;
        }
        return data.containsKey(field.getField())
                || data.containsKey(field.getColumnName())
                || data.containsKey(DynamicQueryGenerator.camelToSnake(field.getField()))
                || data.containsKey(DynamicQueryGenerator.snakeToCamel(field.getColumnName()));
    }

    private Object resolveFieldValue(LowcodeFieldSchema field,
                                           Map<String, Object> data,
                                           Map<String, Object> beforeRecord) {
        Object value = firstPresentValue(data,
                field.getField(),
                field.getColumnName(),
                DynamicQueryGenerator.camelToSnake(field.getField()),
                DynamicQueryGenerator.snakeToCamel(field.getColumnName()));
        if (value != null) {
            return value;
        }
        value = firstPresentValue(beforeRecord,
                field.getColumnName(),
                field.getField(),
                DynamicQueryGenerator.camelToSnake(field.getField()),
                DynamicQueryGenerator.snakeToCamel(field.getColumnName()));
        return value != null ? value : field.getDefaultValue();
    }

    private Object normalizeUniqueValue(Object value, LowcodeUniqueConstraintSchema constraint) {
        Object result = value;
        List<String> normalizers = constraint.getNormalize() == null ? List.of() : constraint.getNormalize();
        for (String normalizer : normalizers) {
            if (result instanceof String textValue && "trim".equalsIgnoreCase(normalizer)) {
                result = textValue.trim();
            } else if (result instanceof String textValue
                    && ("lower".equalsIgnoreCase(normalizer) || "lowercase".equalsIgnoreCase(normalizer))) {
                result = textValue.toLowerCase(Locale.ROOT);
            }
        }
        return result;
    }

    boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String textValue) {
            return StringUtils.isBlank(textValue);
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private Object firstPresentValue(Map<String, Object> data, String... keys) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapFrom(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
