package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public reads keep the metadata planning contract consumed by joined reads and writes. */
class DynamicCrudJoinPlanningTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService permissions = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final VirtualFormulaRuntime formulas = mock(VirtualFormulaRuntime.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("orders");
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("t0.owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("orders");
        config.setObjectCode("orders");
        config.setTableName("orders");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("""
                {"object":{"code":"orders"},"fields":[{"field":"ownerId","columnName":"owner_id"}],
                 "relations":[{"targetObjectCode":"owners","sourceField":"owner_id","targetField":"id","relationType":"REFERENCE","displayField":"full_name"}]}
                """);
        config.setPageSchema("""
                {"primaryModelCode":"orders","modelRefs":[{"modelCode":"orders","tableName":"orders","primary":true},
                 {"modelCode":"missing","tableName":"missing","fields":[{"field":"name"}]},
                 {"modelCode":"owners","tableName":"owners","fields":[{"field":"id"},{"field":"fullName","columnName":"full_name"}]}]}
                """);
        config.setColumnsSchema("[{\"field\":\"ownerId\"},{\"field\":\"title\"}]");
        config.setSearchSchema("[{\"field\":\"owners__fullName\"}]");
        when(configs.getByConfigKey("orders")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(permissions.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("orders");
            return expected.equals(actual);
        }), eq("orders"), eq("t0"))).thenReturn(scope);
        when(repository.getColumnMapping("orders")).thenReturn(Map.of("id", "id", "title", "title", "ownerId", "owner_id"));
        when(repository.getTableColumns("orders")).thenReturn(Set.of("id", "title", "owner_id"));
        when(repository.tableExists("owners")).thenReturn(true);
        when(repository.getColumnMapping("owners")).thenReturn(Map.of("id", "id", "fullName", "full_name"));
        when(repository.selectJoinedPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any()))
                .thenReturn(new Page<>(1, 10));
        when(repository.selectPage(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any()))
                .thenReturn(new Page<>(1, 10));
        when(repository.selectJoinedCustomPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyString(), any()))
                .thenReturn(new Page<>(1, 10));
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), permissions, null, null, null, null, formulas, sources);
    }

    @Test
    void relationDisplayProjectionKeepsStableAliasesScopeAndReadPipeline() {
        Map<String, Object> row = new LinkedHashMap<>(Map.of("id", 1L, "ownerId", 8L, "ownerIdName", "Alice"));
        Page<Map<String, Object>> page = new Page<>(1, 10);
        page.setRecords(List.of(row));
        when(repository.selectJoinedPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any())).thenReturn(page);
        PageQuery request = new PageQuery();
        request.setOrderByColumn("owners__fullName,unknown");
        request.setIsAsc("asc");
        assertSame(page, service.selectPage("orders", request, null));
        verify(repository).selectJoinedPage(eq("orders"), argThat(fields -> fields.contains(new DynamicCrudRepository.JoinField("ownerIdName", "t1", "full_name"))
                        && fields.get(0).fieldName().equals("id")),
                eq(List.of(new DynamicCrudRepository.JoinSpec("owners", "t1", "id", "owner_id"))),
                anyInt(), anyInt(), isNull(), anySet(), anyMap(), argThat(mapping -> "t1.full_name".equals(mapping.get("ownerIdName"))),
                eq("t1.full_name ASC"), same(scope));
        verify(formulas).calculate(anyList(), any(), any());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void blankChildSearchUsesMainQueryButActiveValueRequiresJoin() {
        config.setColumnsSchema("[{\"field\":\"title\"}]");
        DynamicCrudQuery query = new DynamicCrudQuery();
        query.setSearchParams(Map.of("owners__fullName", " "));
        service.selectPage("orders", new PageQuery(), query);
        verify(repository).selectPage(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any());
        query.setSearchParams(Map.of("owners__fullName", "Alice"));
        service.selectPage("orders", new PageQuery(), query);
        verify(repository).selectJoinedPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), eq(query.getSearchParams()), anySet(), anyMap(), anyMap(), eq("t0.id DESC"), same(scope));
    }

    @Test
    void customChildConditionUsesDefaultPrimaryProjectionAndCurrentPrimaryKeySort() {
        LowcodePrimaryKeyStrategy key = new LowcodePrimaryKeyStrategy();
        key.setField("recordId");
        key.setColumnName("record_id");
        runtime.setPrimaryKey(key);
        when(repository.getTableColumns("orders")).thenReturn(Set.of("record_id", "owner_id", "title"));
        CustomQueryConditionDTO condition = new CustomQueryConditionDTO();
        condition.setField("owners__fullName");
        condition.setOperator("eq");
        condition.setValue("Alice");
        CustomQueryExecuteDTO request = new CustomQueryExecuteDTO();
        request.setConditions(List.of(condition));
        request.setOrderByColumn("invalid");
        service.selectCustomPage("orders", request);
        verify(repository).selectJoinedCustomPage(eq("orders"), argThat(fields -> fields.get(0).fieldName().equals("recordId")
                        && fields.contains(new DynamicCrudRepository.JoinField("ownerIdName", "t1", "full_name"))),
                anyList(), eq(1), eq(10), eq(List.of(condition)), argThat(fields -> fields.contains("owners__fullName")), anyMap(),
                eq("t0.record_id DESC"), same(scope));
    }

    @Test
    void exportKeepsExplicitScopeJoinProjectionAndPageCap() {
        DataScopeContext explicit = new DataScopeContext();
        when(permissions.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("orders");
            return expected.equals(actual);
        }), eq("orders"), eq("t0"), eq(explicit))).thenReturn(scope);
        when(repository.countJoined(anyString(), anyList(), anyList(), any(), anySet(), anyMap(), anyMap(), same(scope))).thenReturn(42L);
        when(repository.selectJoinedPageRecords(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), same(scope))).thenReturn(List.of());
        assertEquals(42L, service.countExportRows("orders", null, explicit));
        assertTrue(service.selectExportPageRows("orders", null, -1, 9000, explicit).isEmpty());
        verify(repository).selectJoinedPageRecords(eq("orders"), argThat(fields -> fields.contains(new DynamicCrudRepository.JoinField("ownerIdName", "t1", "full_name"))),
                anyList(), eq(1), eq(5000), isNull(), anySet(), anyMap(), anyMap(), eq("t0.id DESC"), same(scope));
    }

    @Test
    void unavailableRelationAndMalformedPageFallBackToMainReads() {
        when(repository.tableExists("owners")).thenReturn(false);
        service.selectPage("orders", new PageQuery(), null);
        config.setPageSchema("{");
        service.selectPage("orders", new PageQuery(), null);
        verify(repository, times(2)).selectPage(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any());
        verify(repository, never()).selectJoinedPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any());
    }

    @Test
    void planningMetadataFailureRestoresCallerScopeBeforeAnyJoinedQuery() {
        IllegalStateException failure = new IllegalStateException("metadata unavailable");
        when(repository.getColumnMapping("owners")).thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> service.selectPage("orders", new PageQuery(), null)));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository, never()).selectJoinedPage(anyString(), anyList(), anyList(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any());
    }
}
