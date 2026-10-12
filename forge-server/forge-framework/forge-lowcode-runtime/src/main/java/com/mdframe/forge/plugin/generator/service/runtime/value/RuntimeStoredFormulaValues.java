package com.mdframe.forge.plugin.generator.service.runtime.value;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.lowcode.formula.runtime.port.StoredFormulaCalculation;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns stored-formula input merging, writable fields and calculated value writeback. */
public final class RuntimeStoredFormulaValues {
    private final DynamicCrudRepository repository;
    private final RuntimeFieldMetadata fieldMetadata;
    private final StoredFormulaCalculation storedFormulaRuntime;

    public RuntimeStoredFormulaValues(DynamicCrudRepository repository,
                                      RuntimeFieldMetadata fieldMetadata,
                                      StoredFormulaCalculation storedFormulaRuntime) {
        this.repository = repository;
        this.fieldMetadata = fieldMetadata;
        this.storedFormulaRuntime = storedFormulaRuntime;
    }

    public Map<String, Object> applyUpdate(LowcodeRuntimeConfig config,
                                                             String tableName,
                                                             Object id,
                                                             Map<String, Object> data,
                                                             DynamicCrudRepository.SqlCondition dataScopeCondition,
                                                             LowcodePrimaryKeyStrategy primaryKey) {
        Map<String, Object> existingRecord = repository.selectById(
                tableName, StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getColumnName(), "id"), id, dataScopeCondition);
        return applyUpdate(config, tableName, id, data, dataScopeCondition, existingRecord, primaryKey);
    }

    public Map<String, Object> applyUpdate(LowcodeRuntimeConfig config,
                                                             String tableName,
                                                             Object id,
                                                             Map<String, Object> data,
                                                             DynamicCrudRepository.SqlCondition dataScopeCondition,
                                                             Map<String, Object> existingRecord,
                                                             LowcodePrimaryKeyStrategy primaryKey) {
        if (existingRecord == null) {
            throw new BusinessException("无权限更新该数据或数据不存在");
        }
        Map<String, Object> formulaContext = DynamicQueryGenerator.convertMapToCamelCase(existingRecord);
        formulaContext.put("id", id);
        formulaContext.put(StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getField(), "id"), id);
        formulaContext.put(StringUtils.defaultIfBlank(primaryKey == null ? null : primaryKey.getColumnName(), "id"), id);
        mergeWriteDataForFormula(formulaContext, data);
        apply(config, formulaContext);
        copyStoredFormulaValues(config, formulaContext, data);
        return existingRecord;
    }

    private void mergeWriteDataForFormula(Map<String, Object> formulaContext, Map<String, Object> data) {
        if (formulaContext == null || data == null || data.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            formulaContext.put(key, entry.getValue());
            formulaContext.put(DynamicQueryGenerator.snakeToCamel(key), entry.getValue());
            formulaContext.put(DynamicQueryGenerator.camelToSnake(key), entry.getValue());
        }
    }

    public void addWriteFields(Set<String> allowedFields, LowcodeRuntimeConfig config) {
        if (allowedFields == null) {
            return;
        }
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (isStoredFormulaField(field)) {
                addFieldAlias(allowedFields, field.getField());
                addFieldAlias(allowedFields, field.getColumnName());
            }
        }
    }

    private void copyStoredFormulaValues(LowcodeRuntimeConfig config,
                                         Map<String, Object> source,
                                         Map<String, Object> target) {
        if (source == null || target == null) {
            return;
        }
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema == null || modelSchema.getFields() == null) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (!isStoredFormulaField(field) || StringUtils.isBlank(field.getField())) {
                continue;
            }
            if (source.containsKey(field.getField())) {
                target.put(field.getField(), source.get(field.getField()));
            }
        }
    }

    private boolean isStoredFormulaField(LowcodeFieldSchema field) {
        if (field == null || field.getFormulaConfig() == null || field.getFormulaConfig().isEmpty()) {
            return false;
        }
        Object mode = field.getFormulaConfig().get("mode");
        return mode == null || "STORED".equalsIgnoreCase(String.valueOf(mode));
    }

    public void apply(LowcodeRuntimeConfig config, Map<String, Object> data) {
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema == null) return;
        FormulaRuntimeContext ctx = RuntimeFormulaContexts.create(config, data);
        storedFormulaRuntime.calculate(List.of(data), modelSchema, ctx);
    }

    private void addFieldAlias(Set<String> fields, String field) {
        if (StringUtils.isBlank(field)) {
            return;
        }
        fields.add(field);
        fields.add(DynamicQueryGenerator.snakeToCamel(field));
        fields.add(DynamicQueryGenerator.camelToSnake(field));
    }
}
