package com.mdframe.forge.plugin.generator.service.runtime.value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import java.util.*;

import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeComponentCatalog;

/** Owns structured field recognition, storage serialization and JSON/legacy list restoration. */
@RequiredArgsConstructor
public final class RuntimeStructuredValues {
    private final ObjectMapper objectMapper;
    private final RuntimeFieldMetadata metadata;

    public String key(Map<String, Object> data, StructuredFieldContract field) {
        String camelColumn = DynamicQueryGenerator.snakeToCamel(field.columnName());
        for (String candidate : List.of(field.fieldName(), field.columnName(), camelColumn)) {
            if (StringUtils.isNotBlank(candidate) && data.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public List<StructuredFieldContract> fields(LowcodeRuntimeConfig config) {
        LowcodeModelSchema modelSchema = metadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return List.of();
        }
        List<StructuredFieldContract> result = new ArrayList<>();
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null) {
                continue;
            }
            String componentType = StringUtils.trimToNull(field.getComponentType());
            String businessType = StringUtils.upperCase(StringUtils.trimToEmpty(field.getBusinessFieldType()));
            if (componentType == null && "CHECKBOX".equals(businessType)) {
                componentType = "checkbox";
            } else if (componentType == null && "MULTI_SELECT".equals(businessType)) {
                componentType = "transfer";
            }
            if (!LowcodeComponentCatalog.isStructuredValueComponent(componentType)
                    || StringUtils.isBlank(field.getField())) {
                continue;
            }
            String columnName = StringUtils.defaultIfBlank(field.getColumnName(),
                    DynamicQueryGenerator.camelToSnake(field.getField()));
            result.add(new StructuredFieldContract(field.getField(), columnName, componentType));
        }
        return result;
    }

    public record StructuredFieldContract(String fieldName, String columnName, String componentType) {
    }

    public void structured(List<Map<String, Object>> rows, LowcodeRuntimeConfig config) {
        if (rows == null || rows.isEmpty() || config == null) {
            return;
        }
        List<StructuredFieldContract> fields = fields(config);
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            for (StructuredFieldContract field : fields) {
                String key = key(row, field);
                Object value = key == null ? null : row.get(key);
                if (value == null || value instanceof Collection<?>) {
                    continue;
                }
                row.put(key, parseStructuredValue(value));
            }
        }
    }

    private Object parseStructuredValue(Object rawValue) {
        if (rawValue instanceof String value) {
            String text = value.trim();
            if (text.isEmpty()) {
                return new ArrayList<>();
            }
            try {
                JsonNode node = objectMapper.readTree(text);
                if (node != null && node.isArray()) {
                    return objectMapper.convertValue(node, new TypeReference<List<Object>>() { });
                }
            } catch (Exception ignored) {
                // 兼容早期以逗号分隔保存的历史值。
            }
            if (text.contains(",")) {
                return Arrays.stream(text.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .toList();
            }
            return List.of(value);
        }
        if (rawValue instanceof JsonNode node && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<List<Object>>() { });
        }
        return rawValue;
    }

    public void store(Map<String, Object> data, LowcodeRuntimeConfig config) {
        if (data == null || data.isEmpty() || config == null) {
            return;
        }
        for (StructuredFieldContract field : fields(config)) {
            String key = key(data, field);
            Object value = key == null ? null : data.get(key);
            if (value == null || value instanceof String) {
                continue;
            }
            try {
                data.put(key, objectMapper.writeValueAsString(value));
            } catch (Exception e) {
                throw new BusinessException(UiMessages.translate("字段值序列化失败: ") + field.fieldName());
            }
        }
    }
}
