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

import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeFieldRef;
import com.mdframe.forge.plugin.generator.service.runtime.query.RuntimeJoinContext;

/** Owns editable form/model fields and their ordinary or joined-primary column projection. */
@lombok.RequiredArgsConstructor
public final class RuntimeFormWriteFields {
    private final DynamicCrudRepository repository;
    private final ObjectMapper objectMapper;
    private final RuntimeFieldMetadata fieldMetadata;

    public Set<String> allowed(LowcodeRuntimeConfig config, String tableName) {
        Set<String> fields = new LinkedHashSet<>(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        addWritableModelFields(fields, config, tableName);
        return fields;
    }

    private void addWritableModelFields(Set<String> fields, LowcodeRuntimeConfig config, String tableName) {
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null || modelSchema.getFields().isEmpty()) {
            return;
        }
        Set<String> tableColumns = repository.getTableColumns(tableName);
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (!isWritableModelField(field, tableColumns)) {
                continue;
            }
            fields.addAll(RuntimeWriteFieldMapping.aliases(field.getField()));
            fields.addAll(RuntimeWriteFieldMapping.aliases(field.getColumnName()));
        }
    }

    private boolean isWritableModelField(LowcodeFieldSchema field, Set<String> tableColumns) {
        if (field == null || StringUtils.isBlank(field.getField())) {
            return false;
        }
        String fieldStatus = StringUtils.defaultString(field.getFieldStatus());
        if ("DISABLED".equalsIgnoreCase(fieldStatus) || "HIDDEN".equalsIgnoreCase(fieldStatus)) {
            return false;
        }
        if (Boolean.TRUE.equals(field.getSystemField())
                || Boolean.TRUE.equals(field.getPrimaryKey())
                || Boolean.TRUE.equals(field.getAutoIncrement())
                || Boolean.TRUE.equals(field.getReadonly())
                || Boolean.FALSE.equals(field.getFormVisible())) {
            return false;
        }
        String columnName = StringUtils.defaultIfBlank(field.getColumnName(), DynamicQueryGenerator.camelToSnake(field.getField()));
        return StringUtils.isNotBlank(columnName) && tableColumns.contains(columnName);
    }

    /** A null primary key keeps insertion columns; updates pass the key to exclude its aliases. */
    public Map<String, Object> columns(Map<String, Object> data, Set<String> allowedFields,
                                       Map<String, String> columnMapping, LowcodePrimaryKeyStrategy excludedPrimaryKey) {
        Map<String, Object> filteredData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key)) {
                continue;
            }
            if (allowedFields.contains(key)) {
                String columnName = columnMapping.getOrDefault(key, DynamicQueryGenerator.camelToSnake(key));
                if (excludedPrimaryKey == null || !RuntimeWriteFieldMapping.isPrimaryKeyAlias(key, columnName, excludedPrimaryKey)) {
                    filteredData.put(columnName, entry.getValue());
                }
            }
        }
        return filteredData;
    }

    public Map<String, Object> primaryColumns(Map<String, Object> data,
                                                       Set<String> allowedFields,
                                                       RuntimeJoinContext joinContext) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return primaryData;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = joinContext.fields().get(key);
            if (fieldRef == null || !fieldRef.primary()) {
                continue;
            }
            primaryData.put(fieldRef.columnName(), entry.getValue());
        }
        return primaryData;
    }
}
