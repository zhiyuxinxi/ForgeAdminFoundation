package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isImmutableWriteField;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.IMMUTABLE_WRITE_FIELDS;

/** Resolves the published command field contract into validated writable columns. */
@lombok.RequiredArgsConstructor
public final class RuntimeCommandFields {
    private final DynamicCrudRepository repository;
    private final ObjectMapper objectMapper;
    private final RuntimeFieldMetadata fieldMetadata;
    private final RuntimeWriteFieldMapping writeMapping;

    public Map<String, Object> write(
            LowcodeRuntimeConfig config,
            String tableName,
            Map<String, ?> data) {
        if (data == null || data.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> columnMapping = writeMapping.columns(config, tableName);
        Set<String> tableColumns = repository.getTableColumns(tableName);
        Set<String> allowedFields = published(config);
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            String key = StringUtils.trimToNull(entry.getKey());
            if (key == null || isImmutableWriteField(key)) {
                throw new BusinessException(UiMessages.translate("事务命令字段不可写: ") + StringUtils.defaultString(key));
            }
            if (!allowedFields.contains(key)
                    && !allowedFields.contains(DynamicQueryGenerator.snakeToCamel(key))
                    && !allowedFields.contains(DynamicQueryGenerator.camelToSnake(key))) {
                throw new BusinessException(UiMessages.translate("事务命令字段不在已发布模型中: ") + key);
            }
            String column = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
            repository.validateIdentifier(column);
            if (!tableColumns.contains(column) || isImmutableWriteField(column)) {
                throw new BusinessException(UiMessages.translate("事务命令字段不在已发布模型中: ") + key);
            }
            if (filtered.containsKey(column)) {
                throw new BusinessException(UiMessages.translate("事务命令字段重复映射: ") + key);
            }
            filtered.put(column, entry.getValue());
        }
        return filtered;
    }

    public Set<String> published(LowcodeRuntimeConfig config) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null
                        || Boolean.TRUE.equals(field.getSystemField())
                        || Boolean.TRUE.equals(field.getPrimaryKey())
                        || "DISABLED".equalsIgnoreCase(field.getFieldStatus())) {
                    continue;
                }
                fields.addAll(RuntimeWriteFieldMapping.aliases(field.getField()));
                fields.addAll(RuntimeWriteFieldMapping.aliases(field.getColumnName()));
            }
        }
        fields.removeAll(IMMUTABLE_WRITE_FIELDS);
        return fields;
    }

    public Map<String, BigDecimal> decimals(
            LowcodeRuntimeConfig config,
            String tableName,
            Map<String, BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> mapped = write(config, tableName, values);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        mapped.forEach((field, value) -> {
            if (!(value instanceof BigDecimal decimal)) {
                throw new BusinessException("数值调整量格式不正确");
            }
            result.put(field, decimal);
        });
        return result;
    }
}
