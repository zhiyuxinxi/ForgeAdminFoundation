package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratorRuntimeDataSourceAccess;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.support.GeneratorJdbcDataSources;
import com.mdframe.forge.plugin.generator.util.GenDatasourcePasswordCodec;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public resolver and JDBC provider boundaries; no database connections or clock sleeps. */
class LowcodeRuntimeDataSourceBoundaryTest {
    private final GenDatasourceMapper mapper = mock(GenDatasourceMapper.class);
    private final GeneratorJdbcDataSources pools = mock(GeneratorJdbcDataSources.class);
    private final GeneratorRuntimeDataSourceAccess access = new GeneratorRuntimeDataSourceAccess(mapper, pools);
    private final LowcodeRuntimeDataSourceResolver resolver =
            new LowcodeRuntimeDataSourceResolver(access, new ObjectMapper());
    private final DataSource masterDataSource = mock(DataSource.class);
    private final JdbcTemplate masterJdbc = new JdbcTemplate(masterDataSource);
    private final NamedParameterJdbcTemplate masterNamed = new NamedParameterJdbcTemplate(masterDataSource);

    private RuntimeJdbcTemplateProvider provider() {
        return new RuntimeJdbcTemplateProvider(masterJdbc, masterNamed, access);
    }

    private AiCrudConfig config(Long id, String code) {
        AiCrudConfig value = new AiCrudConfig();
        value.setTableName("design_records");
        value.setRuntimeTableName("effective_records");
        value.setRuntimeDatasourceId(id);
        value.setRuntimeDatasourceCode(code);
        return value;
    }

    private GenDatasource datasource(long id, String code) {
        GenDatasource value = new GenDatasource();
        value.setDatasourceId(id);
        value.setDatasourceCode(code);
        value.setDatasourceName("Source " + id);
        value.setIsEnabled(1);
        value.setReadonly(0);
        value.setAllowRuntimeWrite(1);
        value.setAllowRuntimeDdl(1);
        value.setRiskLevel("LOW");
        value.setUsageScope("LOWCODE_RUNTIME");
        return value;
    }

    @Test
    void masterResolutionKeepsDefaultsAndExistingTemplatesWithoutLookingUpDatasource() {
        LowcodeRuntimeDataSourceContext context = resolver.resolve(config(null, null));
        assertTrue(context.isMaster());
        assertEquals("effective_records", context.getTableName());
        assertEquals("id", context.getPrimaryKey().getColumnName());
        assertEquals("FORGE_TENANT_ID", context.getTenantStrategy().getMode());
        assertEquals("FORGE_COLUMNS", context.getAuditStrategy().getMode());
        assertEquals("DEL_FLAG", context.getLogicDeleteStrategy().getMode());
        assertTrue(context.isAllowWrite());
        assertTrue(context.isAllowDdl());
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setTableName("model_table");
        assertEquals("model_table", resolver.resolve(model).getTableName());
        RuntimeJdbcTemplateProvider provider = provider();
        assertSame(masterJdbc, provider.jdbcTemplate(null));
        assertSame(masterJdbc, provider.jdbcTemplate(context));
        assertSame(masterNamed, provider.namedJdbcTemplate(context));
        assertSame(masterDataSource, provider.resolveDataSource(context));
        verifyNoInteractions(mapper, pools, masterDataSource);
    }

    @Test
    void publishedPrimaryKeyColumnsKeepImportedNonGeneratedStrategy() {
        AiCrudConfig value = config(null, null);
        value.setPrimaryKeyField("recordCode");
        value.setPrimaryKeyColumn("record_code");
        value.setPrimaryKeyType("varchar");
        value.setModelSchema("{\"primaryKey\":{\"field\":\"recordCode\",\"columnName\":\"record_code\",\"autoIncrement\":false}}");
        LowcodeRuntimeDataSourceContext context = resolver.resolve(value);
        assertFalse(context.getPrimaryKey().getAutoIncrement());
        assertEquals("record_code", context.getPrimaryKey().getColumnName());
        value.setModelSchema(null);
        assertTrue(resolver.resolve(value).getPrimaryKey().getAutoIncrement());
    }

    @Test
    void idTakesPriorityAndCodeLookupIsUsedOnlyWhenIdIsAbsent() {
        when(mapper.selectById(7L)).thenReturn(datasource(7L, "primary"));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(datasource(9L, "alternate"));
        LowcodeRuntimeDataSourceContext byId = resolver.resolve(config(7L, "alternate"));
        assertEquals(7L, byId.getDatasourceId());
        assertEquals("primary", byId.getDatasourceCode());
        verify(mapper, never()).selectOne(any(Wrapper.class));
        LowcodeRuntimeDataSourceContext byCode = resolver.resolve(config(null, "alternate"));
        assertEquals(9L, byCode.getDatasourceId());
        assertEquals("effective_records", byCode.getTableName());
        verify(mapper).selectOne(any(Wrapper.class));
    }

    @Test
    void missingIdDoesNotFallBackToCodeAndDisabledDatasourceKeepsOriginalFailure() {
        BusinessException missing = assertThrows(BusinessException.class,
                () -> resolver.resolve(config(7L, "alternate")));
        assertEquals("运行数据源不存在: alternate", missing.getMessage());
        verify(mapper, never()).selectOne(any(Wrapper.class));
        GenDatasource disabled = datasource(8L, "disabled");
        disabled.setIsEnabled(0);
        when(mapper.selectById(8L)).thenReturn(disabled);
        assertEquals("运行数据源已禁用: Source 8", assertThrows(BusinessException.class,
                () -> resolver.resolve(config(8L, null))).getMessage());
    }

    @Test
    void readonlyDatasourceOverridesStoredWriteAndDdlFlagsWithFreshSnapshot() {
        GenDatasource readonly = datasource(7L, "readonly");
        readonly.setReadonly(1);
        when(mapper.selectById(7L)).thenReturn(readonly);
        AiCrudConfig config = config(7L, null);
        config.setRuntimeDatasourceSnapshot("{\"datasourceId\":7,\"allowWrite\":true,\"allowDdl\":true}");
        LowcodeRuntimeDataSourceContext context = resolver.resolve(config);
        assertFalse(context.isMaster());
        assertTrue(context.isReadonly());
        assertFalse(context.isAllowWrite());
        assertFalse(context.isAllowDdl());
        assertEquals(Boolean.FALSE, context.getSnapshot().getAllowWrite());
        assertEquals(Boolean.FALSE, context.getSnapshot().getAllowDdl());
        assertEquals("LOW", context.getSnapshot().getRiskLevel());
        assertEquals("effective_records", context.getSnapshot().getTableName());
    }

    @Test
    void cacheReturnsCopiesAndIdCodeEvictionAndClearExposeFreshDefinitions() {
        GenDatasource stored = datasource(7L, "cache-code");
        when(mapper.selectById(7L)).thenReturn(stored);
        GenDatasource first = resolver.getDatasourceById(7L);
        first.setDatasourceName("caller mutation");
        stored.setDatasourceName("changed in store");
        GenDatasource cached = resolver.getDatasourceById(7L);
        assertNotSame(first, cached);
        assertEquals("Source 7", cached.getDatasourceName());
        assertEquals("Source 7", resolver.resolve(config(null, "cache-code")).getDatasourceName());
        verify(mapper, times(1)).selectById(7L);
        verify(mapper, never()).selectOne(any(Wrapper.class));
        resolver.evictDatasource(7L, " cache-code ");
        assertEquals("changed in store", resolver.getDatasourceById(7L).getDatasourceName());
        resolver.clearDatasourceCache();
        assertEquals("changed in store", resolver.getDatasourceById(7L).getDatasourceName());
        verify(mapper, times(3)).selectById(7L);
    }

    @Test
    void malformedJsonFailsAtItsOriginalFieldWithoutDatasourceLookup() {
        AiCrudConfig model = config(null, null);
        model.setModelSchema("{");
        assertEquals("modelSchema格式不正确", assertThrows(BusinessException.class,
                () -> resolver.resolve(model)).getMessage());
        AiCrudConfig snapshot = config(null, null);
        snapshot.setRuntimeDatasourceSnapshot("{");
        assertEquals("runtimeDatasourceSnapshot格式不正确", assertThrows(BusinessException.class,
                () -> resolver.resolve(snapshot)).getMessage());
        AiCrudConfig tenant = config(null, null);
        tenant.setTenantStrategy("{");
        assertEquals("tenantStrategy格式不正确", assertThrows(BusinessException.class,
                () -> resolver.resolve(tenant)).getMessage());
        verifyNoInteractions(mapper);
    }

    @Test
    void externalTemplatesResolveLazilyAndDecryptOnlyAnIndependentDefinitionCopy() throws Exception {
        GenDatasource stored = datasource(7L, "external");
        String encrypted = GenDatasourcePasswordCodec.encrypt("test-only-password");
        stored.setPassword(encrypted);
        when(mapper.selectById(7L)).thenReturn(stored);
        DataSource external = mock(DataSource.class);
        when(pools.getDataSource(any())).thenAnswer(call -> {
            GenDatasource supplied = call.getArgument(0);
            assertEquals(7L, supplied.getDatasourceId());
            assertEquals("test-only-password", supplied.getPassword());
            assertNotSame(stored, supplied);
            return external;
        });
        LowcodeRuntimeDataSourceContext context = resolver.resolve(config(7L, null));
        RuntimeJdbcTemplateProvider provider = provider();
        verifyNoInteractions(pools);
        assertSame(external, provider.resolveDataSource(context));
        assertSame(external, provider.jdbcTemplate(context).getDataSource());
        assertSame(external, provider.namedJdbcTemplate(context).getJdbcTemplate().getDataSource());
        assertEquals(encrypted, stored.getPassword());
        assertEquals(encrypted, resolver.getDatasourceById(7L).getPassword());
        verify(pools, times(3)).getDataSource(any());
        verify(external, never()).getConnection();
    }

    @Test
    void externalProviderRejectsMissingIdMissingDefinitionAndDisabledDefinitionBeforePoolAccess() {
        RuntimeJdbcTemplateProvider provider = provider();
        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        assertEquals("运行数据源ID不能为空", assertThrows(BusinessException.class,
                () -> provider.resolveDataSource(context)).getMessage());
        context.setDatasourceId(7L);
        assertEquals("运行数据源不存在: 7", assertThrows(BusinessException.class,
                () -> provider.resolveDataSource(context)).getMessage());
        GenDatasource disabled = datasource(7L, "disabled");
        disabled.setIsEnabled(0);
        when(mapper.selectById(7L)).thenReturn(disabled);
        assertEquals("运行数据源已禁用: Source 7", assertThrows(BusinessException.class,
                () -> provider.resolveDataSource(context)).getMessage());
        verifyNoInteractions(pools);
    }
}
