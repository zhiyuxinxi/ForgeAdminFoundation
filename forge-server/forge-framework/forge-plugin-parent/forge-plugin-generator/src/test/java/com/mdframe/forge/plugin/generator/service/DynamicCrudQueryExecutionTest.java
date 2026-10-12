package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DynamicCrudQueryExecutionTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService permissions = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("runtime_orders");
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("orders");
        config.setObjectCode("orders");
        config.setTableName("logical_orders");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("{\"fields\":[{\"field\":\"dueAt\",\"columnName\":\"due_at\"}]}");
        config.setColumnsSchema("[{\"field\":\"dueAt\"}]");
        config.setSearchSchema("[{\"field\":\"dueAt\"}]");
        when(configs.getByConfigKey("orders")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(repository.getColumnMapping(anyString())).thenReturn(Map.of("id", "id", "dueAt", "due_at"));
        when(repository.getTableColumns(anyString())).thenReturn(Set.of("id", "due_at"));
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), permissions, null, null, null, null, null, sources);
    }

    @Test
    void exportPlanningUsesTargetScopeAndResolvedTableThenRestoresCaller() {
        DataScopeContext explicit = new DataScopeContext();
        when(repository.getColumnMapping(anyString())).thenAnswer(invocation -> {
            assertSame(runtime, LowcodeRuntimeDataSourceContextHolder.get(), "Export metadata must use the target datasource scope");
            assertEquals("runtime_orders", invocation.getArgument(0));
            return Map.of("id", "id", "dueAt", "due_at");
        });
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertEquals(0L, service.countExportRows("orders", null, explicit));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(permissions).buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("runtime_orders");
            return expected.equals(actual);
        }), eq("runtime_orders"), isNull(), eq(explicit));
        verify(repository).countList(eq("runtime_orders"), isNull(), anySet(), anyMap(), anyMap(), isNull());
    }

    @Test
    void exportFailureRestoresScopeAndKeepsOriginalException() {
        IllegalStateException failure = new IllegalStateException("query unavailable");
        when(repository.selectPageRecords(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any()))
                .thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> service.selectExportPageRows("orders", null, 0, 9000, null)));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository).selectPageRecords(eq("runtime_orders"), eq(1), eq(5000), isNull(), anySet(), anyMap(), anyMap(), eq("id DESC"), isNull());
    }

    @Test
    void scheduledListKeepsWhitelistedRangePrimarySortAndBatchCap() {
        LowcodePrimaryKeyStrategy key = new LowcodePrimaryKeyStrategy();
        key.setField("recordId");
        key.setColumnName("record_id");
        runtime.setPrimaryKey(key);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = start.plusDays(1);
        assertTrue(service.selectScheduledCandidateRows("orders", "dueAt", start, end, 1000).isEmpty());
        verify(repository).selectList(eq("runtime_orders"), eq(Map.of("dueAt", List.of(start, end))),
                argThat(fields -> fields.contains("dueAt")), eq(Map.of("dueAt", "between")), anyMap(), eq("record_id ASC"), eq(200), isNull());
        clearInvocations(repository);
        assertThrows(BusinessException.class, () -> service.selectScheduledCandidateRows("orders", "unpublishedField", start, end, 1));
        verifyNoInteractions(repository);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void exportShortcutKeepsEffectiveLimitAndRejectsDisabledConfigBeforeDatasourceWork() {
        assertTrue(service.selectExportRows("orders", null, Integer.MAX_VALUE).isEmpty());
        verify(repository).selectPageRecords(eq("runtime_orders"), eq(1), eq(5000), isNull(), anySet(), anyMap(), anyMap(), eq("id DESC"), isNull());
        config.setStatus("1");
        clearInvocations(repository, sources);
        assertThrows(BusinessException.class, () -> service.countExportRows("orders", null, null));
        verifyNoInteractions(repository, sources);
    }
}
