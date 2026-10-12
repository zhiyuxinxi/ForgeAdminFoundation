package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Owns published field/column mappings and the writable aliases of generated fields. */
@lombok.RequiredArgsConstructor
public final class RuntimeWriteFieldMapping {
    private final com.mdframe.forge.plugin.generator.service.DynamicCrudRepository repository;
    private final RuntimeFieldMetadata fieldMetadata;

    public Map<String, String> columns(LowcodeRuntimeConfig config, String tableName) {
        Map<String, String> result = new LinkedHashMap<>(repository.getColumnMapping(tableName));
        if (config == null || !StringUtils.equals(tableName, config.getTableName())) {
            return result;
        }
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return result;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null) {
                continue;
            }
            String columnName = StringUtils.trimToNull(StringUtils.defaultIfBlank(
                    field.getColumnName(), DynamicQueryGenerator.camelToSnake(field.getField())));
            if (columnName == null) {
                continue;
            }
            String normalizedColumn = columnName.toLowerCase(Locale.ROOT);
            putRuntimeColumnAlias(result, field.getField(), normalizedColumn);
            putRuntimeColumnAlias(result, field.getColumnName(), normalizedColumn);
        }
        return result;
    }

    private void putRuntimeColumnAlias(Map<String, String> mapping, String alias, String columnName) {
        if (StringUtils.isBlank(alias) || StringUtils.isBlank(columnName)) {
            return;
        }
        mapping.put(alias, columnName);
        mapping.put(DynamicQueryGenerator.snakeToCamel(alias), columnName);
        mapping.put(DynamicQueryGenerator.camelToSnake(alias), columnName);
    }

    public Set<String> generatedAliases(LowcodeRuntimeConfig config, String documentNoField) {
        Set<String> aliases = new LinkedHashSet<>();
        if (config == null || StringUtils.isBlank(config.getTableName()) || StringUtils.isBlank(documentNoField)) {
            return aliases;
        }
        Map<String, String> columnMapping = columns(config, config.getTableName());
        addDocumentNoAliasIfWritable(aliases, documentNoField, columnMapping);
        if (StringUtils.isNotBlank(config.getModelSchema())) {
            LowcodeModelSchema modelSchema = fieldMetadata.model(config);
            if (modelSchema != null && modelSchema.getFields() != null) {
                Set<String> requestedAliases = aliases(documentNoField);
                for (LowcodeFieldSchema field : modelSchema.getFields()) {
                    if (field == null) {
                        continue;
                    }
                    if (matches(field.getField(), requestedAliases)
                            || matches(field.getColumnName(), requestedAliases)) {
                        addDocumentNoAliasIfWritable(aliases, field.getField(), columnMapping);
                        addDocumentNoAliasIfWritable(aliases, field.getColumnName(), columnMapping);
                    }
                }
            }
        }
        return aliases;
    }

    public static Set<String> aliases(String field) {
        Set<String> aliases = new LinkedHashSet<>();
        if (StringUtils.isBlank(field)) {
            return aliases;
        }
        aliases.add(field);
        aliases.add(DynamicQueryGenerator.snakeToCamel(field));
        aliases.add(DynamicQueryGenerator.camelToSnake(field));
        return aliases;
    }

    public static boolean matches(String field, Set<String> aliases) {
        if (StringUtils.isBlank(field) || aliases == null || aliases.isEmpty()) {
            return false;
        }
        return aliases.contains(field)
                || aliases.contains(DynamicQueryGenerator.snakeToCamel(field))
                || aliases.contains(DynamicQueryGenerator.camelToSnake(field));
    }

    private void addDocumentNoAliasIfWritable(Set<String> aliases,
                                              String field,
                                              Map<String, String> columnMapping) {
        if (StringUtils.isBlank(field) || columnMapping == null || columnMapping.isEmpty()) {
            return;
        }
        for (String alias : aliases(field)) {
            if (columnMapping.containsKey(alias)) {
                aliases.add(alias);
            }
        }
    }
    public static boolean isPrimaryKeyAlias(String fieldName, String columnName, LowcodePrimaryKeyStrategy primaryKey) {
        String primaryField = StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getField(), "id");
        String primaryColumn = StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getColumnName(), "id");
        return equalsFieldAlias(fieldName, primaryField)
                || equalsFieldAlias(fieldName, primaryColumn)
                || equalsFieldAlias(columnName, primaryColumn);
    }

    private static boolean equalsFieldAlias(String actual, String expected) {
        if (StringUtils.isBlank(actual) || StringUtils.isBlank(expected)) {
            return false;
        }
        return actual.equals(expected)
                || actual.equals(DynamicQueryGenerator.snakeToCamel(expected))
                || actual.equals(DynamicQueryGenerator.camelToSnake(expected));
    }
}
