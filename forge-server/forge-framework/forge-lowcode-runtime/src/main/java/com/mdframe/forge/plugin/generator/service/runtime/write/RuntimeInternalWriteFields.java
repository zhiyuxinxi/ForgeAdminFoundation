package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.isImmutableWriteField;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.IMMUTABLE_WRITE_FIELDS;

/** Owns internal write field/physical-column validation, independently of form visibility. */
@lombok.RequiredArgsConstructor
public final class RuntimeInternalWriteFields {
    private final DynamicCrudRepository repository;
    private final ObjectMapper objectMapper;
    private final RuntimeFieldMetadata fieldMetadata;
    private final RuntimeWriteFieldMapping writeMapping;

    /** Captures the column metadata at the caller's existing point before authorization/formula reads. */
    public record ColumnMetadata(Map<String, String> mapping, Set<String> names) {
    }

    public ColumnMetadata columns(LowcodeRuntimeConfig config, String tableName) {
        return new ColumnMetadata(writeMapping.columns(config, tableName), repository.getTableColumns(tableName));
    }

    public Map<String, Object> modelColumns(LowcodeRuntimeConfig config, String tableName, Map<String, Object> data) {
        ColumnMetadata columns = columns(config, tableName);
        Set<String> allowedFields = allowed(config, tableName);
        return project(config, tableName, data, columns, allowedFields, null);
    }

    public Map<String, Object> physicalColumns(LowcodeRuntimeConfig config, String tableName, Map<String, Object> data,
                                               ColumnMetadata columns, LowcodePrimaryKeyStrategy primaryKey) {
        return project(config, tableName, data, columns, null, primaryKey);
    }

    private Map<String, Object> project(LowcodeRuntimeConfig config, String tableName, Map<String, Object> data,
                                        ColumnMetadata columns, Set<String> allowedFields,
                                        LowcodePrimaryKeyStrategy excludedPrimaryKey) {
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key)) {
                continue;
            }
            if (allowedFields != null && !allowedFields.contains(key)) {
                throw new BusinessException(UiMessages.translate("字段不在模型中: ") + key);
            }
            String columnName = columns.mapping().getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
            repository.validateIdentifier(columnName);
            if (!columns.names().contains(columnName)) {
                throw missingRuntimeColumn(config, tableName, key, columnName);
            }
            if (isImmutableWriteField(columnName)
                    || (excludedPrimaryKey != null && RuntimeWriteFieldMapping.isPrimaryKeyAlias(key, columnName, excludedPrimaryKey))) {
                continue;
            }
            filteredData.put(columnName, entry.getValue());
        }
        return filteredData;
    }

    private BusinessException missingRuntimeColumn(
            LowcodeRuntimeConfig config,
            String tableName,
            String fieldName,
            String columnName) {
        String configKey = config == null ? null : config.getConfigKey();
        return new BusinessException(UiMessages.translate("业务字段 ") + fieldName + UiMessages.translate(" 映射的数据库列 ") + columnName
                + UiMessages.translate(" 不存在（运行配置: ") + StringUtils.defaultIfBlank(configKey, "未知")
                + UiMessages.translate("，数据表: ") + StringUtils.defaultIfBlank(tableName, "未知")
                + UiMessages.translate("），请先同步低代码数据表结构后重新发布能力"));
    }

    public Set<String> allowed(LowcodeRuntimeConfig config, String tableName) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        LowcodeModelSchema modelSchema = StringUtils.isNotBlank(config.getModelSchema()) ? fieldMetadata.model(config) : null;
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                fields.addAll(RuntimeWriteFieldMapping.aliases(field.getField()));
                fields.addAll(RuntimeWriteFieldMapping.aliases(field.getColumnName()));
            }
        }
        for (String column : repository.getTableColumns(tableName)) {
            fields.addAll(RuntimeWriteFieldMapping.aliases(column));
        }
        fields.removeAll(IMMUTABLE_WRITE_FIELDS);
        return fields;
    }
}
