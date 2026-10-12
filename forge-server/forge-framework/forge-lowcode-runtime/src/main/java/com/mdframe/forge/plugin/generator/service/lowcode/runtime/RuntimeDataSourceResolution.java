package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeSourceTableRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

/** Resolves runtime model/config values without persistence entities, credentials or pool ownership. */
public final class RuntimeDataSourceResolution {
    private static final String DEFAULT_PRIMARY_KEY = "id";
    private static final String TENANT_MODE_FORGE = "FORGE_TENANT_ID";
    private static final String AUDIT_MODE_FORGE_COLUMNS = "FORGE_COLUMNS";
    private static final String LOGIC_MODE_DEL_FLAG = "DEL_FLAG";
    private final ObjectMapper objectMapper;
    private final RuntimeDataSourceAccess datasourceAccess;

    public RuntimeDataSourceResolution(ObjectMapper objectMapper, RuntimeDataSourceAccess datasourceAccess) {
        this.objectMapper = objectMapper;
        this.datasourceAccess = datasourceAccess;
    }

    public LowcodeRuntimeDataSourceContext resolve(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            throw new BusinessException("低代码模型协议不能为空");
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = resolveModelSnapshot(modelSchema);
        String tableName = StringUtils.firstNonBlank(
            snapshot == null ? null : snapshot.getTableName(),
            modelSchema.getTableName(),
            modelSchema.getSourceTable() == null ? null : modelSchema.getSourceTable().getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName, modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(modelSchema.getPrimaryKey()));
        context.setTenantStrategy(defaultTenantStrategy(modelSchema.getTenantStrategy()));
        context.setAuditStrategy(defaultAuditStrategy(modelSchema.getAuditStrategy()));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(modelSchema.getLogicDeleteStrategy()));
        return context;
    }

    public LowcodeRuntimeDataSourceContext resolveRuntime(LowcodeRuntimeConfig config) {
        if (config == null) {
            throw new BusinessException("低代码应用配置不能为空");
        }
        LowcodeModelSchema modelSchema = readModelSchema(config);
        LowcodeRuntimeDatasourceSnapshot snapshot = readJson(
            config.getRuntimeDatasourceSnapshot(), LowcodeRuntimeDatasourceSnapshot.class, "runtimeDatasourceSnapshot");
        if (snapshot == null && modelSchema != null) {
            snapshot = resolveModelSnapshot(modelSchema);
        }
        if (snapshot == null && (config.getRuntimeDatasourceId() != null
            || StringUtils.isNotBlank(config.getRuntimeDatasourceCode()))) {
            snapshot = new LowcodeRuntimeDatasourceSnapshot();
            snapshot.setDatasourceId(config.getRuntimeDatasourceId());
            snapshot.setDatasourceCode(config.getRuntimeDatasourceCode());
        }
        String tableName = StringUtils.firstNonBlank(
            config.getRuntimeTableName(),
            snapshot == null ? null : snapshot.getTableName(),
            config.getTableName(),
            modelSchema == null ? null : modelSchema.getTableName()
        );
        LowcodeRuntimeDataSourceContext context = resolveBySnapshot(snapshot, tableName,
            modelSchema == null ? null : modelSchema.getTableMode());
        context.setPrimaryKey(defaultPrimaryKey(resolveConfigPrimaryKey(config, modelSchema)));
        context.setTenantStrategy(defaultTenantStrategy(readJson(
            config.getTenantStrategy(), LowcodeTenantStrategy.class, "tenantStrategy",
            modelSchema == null ? null : modelSchema.getTenantStrategy())));
        context.setAuditStrategy(defaultAuditStrategy(readJson(
            config.getAuditStrategy(), LowcodeAuditStrategy.class, "auditStrategy",
            modelSchema == null ? null : modelSchema.getAuditStrategy())));
        context.setLogicDeleteStrategy(defaultLogicDeleteStrategy(readJson(
            config.getLogicDeleteStrategy(), LowcodeLogicDeleteStrategy.class, "logicDeleteStrategy",
            modelSchema == null ? null : modelSchema.getLogicDeleteStrategy())));
        return context;
    }

    private LowcodeRuntimeDatasourceSnapshot resolveModelSnapshot(LowcodeModelSchema modelSchema) {
        if (modelSchema.getRuntimeDatasource() != null) {
            return modelSchema.getRuntimeDatasource();
        }
        LowcodeSourceTableRef sourceTable = modelSchema.getSourceTable();
        if (sourceTable == null || (sourceTable.getDatasourceId() == null
            && StringUtils.isBlank(sourceTable.getDatasourceCode()))) {
            return null;
        }
        LowcodeRuntimeDatasourceSnapshot snapshot = new LowcodeRuntimeDatasourceSnapshot();
        snapshot.setDatasourceId(sourceTable.getDatasourceId());
        snapshot.setDatasourceCode(sourceTable.getDatasourceCode());
        snapshot.setDatasourceName(sourceTable.getDatasourceName());
        snapshot.setDbType(sourceTable.getDbType());
        snapshot.setTableName(StringUtils.firstNonBlank(sourceTable.getTableName(), modelSchema.getTableName()));
        snapshot.setTableMode(StringUtils.defaultIfBlank(modelSchema.getTableMode(), "EXISTING"));
        return snapshot;
    }

    private LowcodeRuntimeDataSourceContext resolveBySnapshot(LowcodeRuntimeDatasourceSnapshot snapshot,
                                                             String tableName,
                                                             String tableMode) {
        if (snapshot == null || (snapshot.getDatasourceId() == null
            && StringUtils.isBlank(snapshot.getDatasourceCode()))) {
            LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master(tableName);
            context.setTableMode(tableMode);
            context.setSnapshot(null);
            return context;
        }
        String effectiveTableName = StringUtils.firstNonBlank(tableName, snapshot.getTableName());
        LowcodeRuntimeDatasourceSnapshot freshSnapshot = datasourceAccess.resolveSnapshot(
            snapshot.getDatasourceId(), snapshot.getDatasourceCode(), effectiveTableName,
            StringUtils.firstNonBlank(tableMode, snapshot.getTableMode()));

        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setMaster(false);
        context.setDatasourceId(freshSnapshot.getDatasourceId());
        context.setDatasourceCode(freshSnapshot.getDatasourceCode());
        context.setDatasourceName(freshSnapshot.getDatasourceName());
        context.setDbType(freshSnapshot.getDbType());
        context.setTableName(effectiveTableName);
        context.setTableMode(freshSnapshot.getTableMode());
        context.setAllowWrite(Boolean.TRUE.equals(freshSnapshot.getAllowWrite()));
        context.setAllowDdl(Boolean.TRUE.equals(freshSnapshot.getAllowDdl()));
        context.setReadonly(Boolean.TRUE.equals(freshSnapshot.getReadonly()));
        context.setRiskLevel(freshSnapshot.getRiskLevel());
        context.setSnapshot(freshSnapshot);
        return context;
    }

    private LowcodeModelSchema readModelSchema(LowcodeRuntimeConfig config) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        return readJson(config.getModelSchema(), LowcodeModelSchema.class, "modelSchema");
    }

    private LowcodePrimaryKeyStrategy resolveConfigPrimaryKey(LowcodeRuntimeConfig config, LowcodeModelSchema modelSchema) {
        if (StringUtils.isNotBlank(config.getPrimaryKeyField()) || StringUtils.isNotBlank(config.getPrimaryKeyColumn())) {
            LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
            primaryKey.setField(config.getPrimaryKeyField());
            primaryKey.setColumnName(config.getPrimaryKeyColumn());
            primaryKey.setDataType(config.getPrimaryKeyType());
            primaryKey.setAutoIncrement(modelSchema == null || modelSchema.getPrimaryKey() == null
                ? null : modelSchema.getPrimaryKey().getAutoIncrement());
            return primaryKey;
        }
        return modelSchema == null ? null : modelSchema.getPrimaryKey();
    }

    private LowcodePrimaryKeyStrategy defaultPrimaryKey(LowcodePrimaryKeyStrategy primaryKey) {
        LowcodePrimaryKeyStrategy value = primaryKey == null ? new LowcodePrimaryKeyStrategy() : primaryKey;
        value.setField(StringUtils.defaultIfBlank(value.getField(), DEFAULT_PRIMARY_KEY));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), DEFAULT_PRIMARY_KEY));
        value.setDataType(StringUtils.defaultIfBlank(value.getDataType(), "bigint"));
        value.setAutoIncrement(value.getAutoIncrement() == null || value.getAutoIncrement());
        return value;
    }

    private LowcodeTenantStrategy defaultTenantStrategy(LowcodeTenantStrategy tenantStrategy) {
        LowcodeTenantStrategy value = tenantStrategy == null ? new LowcodeTenantStrategy() : tenantStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), TENANT_MODE_FORGE));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "tenant_id"));
        return value;
    }

    private LowcodeAuditStrategy defaultAuditStrategy(LowcodeAuditStrategy auditStrategy) {
        LowcodeAuditStrategy value = auditStrategy == null ? new LowcodeAuditStrategy() : auditStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), AUDIT_MODE_FORGE_COLUMNS));
        value.setCreateByColumn(StringUtils.defaultIfBlank(value.getCreateByColumn(), "create_by"));
        value.setCreateTimeColumn(StringUtils.defaultIfBlank(value.getCreateTimeColumn(), "create_time"));
        value.setCreateDeptColumn(StringUtils.defaultIfBlank(value.getCreateDeptColumn(), "create_dept"));
        value.setUpdateByColumn(StringUtils.defaultIfBlank(value.getUpdateByColumn(), "update_by"));
        value.setUpdateTimeColumn(StringUtils.defaultIfBlank(value.getUpdateTimeColumn(), "update_time"));
        return value;
    }

    private LowcodeLogicDeleteStrategy defaultLogicDeleteStrategy(LowcodeLogicDeleteStrategy logicDeleteStrategy) {
        LowcodeLogicDeleteStrategy value = logicDeleteStrategy == null ? new LowcodeLogicDeleteStrategy() : logicDeleteStrategy;
        value.setMode(StringUtils.defaultIfBlank(value.getMode(), LOGIC_MODE_DEL_FLAG));
        value.setColumnName(StringUtils.defaultIfBlank(value.getColumnName(), "del_flag"));
        value.setActiveValue(StringUtils.defaultIfBlank(value.getActiveValue(), "0"));
        value.setDeletedValue(StringUtils.defaultIfBlank(value.getDeletedValue(), "1"));
        return value;
    }

    private <T> T readJson(String json, Class<T> type, String fieldName) {
        return readJson(json, type, fieldName, null);
    }

    private <T> T readJson(String json, Class<T> type, String fieldName, T defaultValue) {
        if (StringUtils.isBlank(json)) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException(fieldName + UiMessages.translate("格式不正确"));
        }
    }

}
