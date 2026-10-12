package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeUniqueConstraintSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;

/** Checks current unique values before persistence; the caller owns authorization and transaction scope. */
@lombok.RequiredArgsConstructor
public final class RuntimeUniqueValidator {
    private final DynamicCrudRepository repository;
    private final RuntimeFieldMetadata metadata;
    private final RuntimeUniqueRules rules;
    private final RuntimeUniqueFields fields;

    public void validate(LowcodeRuntimeConfig config, String tableName, Map<String, Object> data,
                         Map<String, Object> beforeRecord, Object excludeId, String primaryKeyColumn) {
        LowcodeModelSchema model = metadata.model(config);
        List<LowcodeUniqueConstraintSchema> constraints = rules.resolve(config, model);
        if (constraints.isEmpty()) {
            return;
        }
        Map<String, LowcodeFieldSchema> fieldMap = fields.resolve(config, tableName, model);
        Set<String> tableColumns = repository.getTableColumns(tableName);
        for (LowcodeUniqueConstraintSchema constraint : constraints) {
            if (shouldCheck(constraint, fieldMap, data, excludeId)
                    && isDuplicate(constraint, fieldMap, tableColumns, tableName, data,
                            beforeRecord, excludeId, primaryKeyColumn)) {
                throw new BusinessException(rules.message(constraint, fieldMap));
            }
        }
    }

    private boolean shouldCheck(LowcodeUniqueConstraintSchema constraint,
                                Map<String, LowcodeFieldSchema> fieldMap,
                                Map<String, Object> data, Object excludeId) {
        if (constraint == null || constraint.getFields() == null || constraint.getFields().isEmpty()) {
            return false;
        }
        return excludeId == null || constraint.getFields().stream()
                .map(fieldMap::get)
                .filter(Objects::nonNull)
                .anyMatch(field -> fields.containsInput(data, field));
    }

    private boolean isDuplicate(LowcodeUniqueConstraintSchema constraint,
                                Map<String, LowcodeFieldSchema> fieldMap, Set<String> tableColumns,
                                String tableName, Map<String, Object> data, Map<String, Object> beforeRecord,
                                Object excludeId, String primaryKeyColumn) {
        Map<String, Object> columnValues = new LinkedHashMap<>();
        for (String fieldName : constraint.getFields()) {
            LowcodeFieldSchema field = fieldMap.get(fieldName);
            if (field == null) {
                throw new BusinessException(UiMessages.translate("唯一校验字段不存在: ") + fieldName);
            }
            String columnName = StringUtils.defaultIfBlank(field.getColumnName(),
                    DynamicQueryGenerator.camelToSnake(field.getField()));
            repository.validateIdentifier(columnName);
            if (!tableColumns.contains(columnName)) {
                throw new BusinessException(UiMessages.translate("唯一校验字段未同步到数据表: ") + field.getField());
            }
            Object value = fields.value(field, data, beforeRecord, constraint);
            if (Boolean.TRUE.equals(constraint.getIgnoreBlank()) && fields.isBlank(value)) {
                return false;
            }
            columnValues.put(columnName, value);
        }
        return repository.existsByColumns(tableName, columnValues, primaryKeyColumn, excludeId, null);
    }
}
