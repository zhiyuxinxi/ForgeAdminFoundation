package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Field-name aliases shared by formula relation consumers.
 * Keeps original keys and values; alias conversion does not validate or escape SQL identifiers.
 * Schema acceptance and unknown-field policies remain with each consumer.
 */
final class FormulaFieldAliases {

    private FormulaFieldAliases() {
    }

    static boolean matchesField(LowcodeFieldSchema field, String value) {
        String snakeValue = DynamicQueryGenerator.camelToSnake(value);
        String camelValue = DynamicQueryGenerator.snakeToCamel(value);
        return Objects.equals(field.getField(), value)
                || Objects.equals(field.getField(), camelValue)
                || Objects.equals(field.getColumnName(), value)
                || Objects.equals(field.getColumnName(), snakeValue);
    }

    static List<Map<String, Object>> appendFieldAliases(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> enriched = new LinkedHashMap<>(row);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    continue;
                }
                enriched.putIfAbsent(DynamicQueryGenerator.snakeToCamel(key), entry.getValue());
                enriched.putIfAbsent(DynamicQueryGenerator.camelToSnake(key), entry.getValue());
            }
            result.add(enriched);
        }
        return result;
    }

    static Object readFieldValue(Map<String, Object> row, String fieldName) {
        if (row == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        if (row.containsKey(fieldName)) {
            return row.get(fieldName);
        }
        String snake = DynamicQueryGenerator.camelToSnake(fieldName);
        if (row.containsKey(snake)) {
            return row.get(snake);
        }
        String camel = DynamicQueryGenerator.snakeToCamel(fieldName);
        if (row.containsKey(camel)) {
            return row.get(camel);
        }
        return null;
    }
}
