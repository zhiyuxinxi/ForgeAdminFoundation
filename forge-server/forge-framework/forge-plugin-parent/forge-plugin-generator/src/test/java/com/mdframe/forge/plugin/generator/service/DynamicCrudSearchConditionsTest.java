package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public query calls retain their condition, permission, limit and input-ownership contracts. */
class DynamicCrudSearchConditionsTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService dataScope = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope =
            new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("records");
        config.setObjectCode("records");
        config.setTableName("records");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setLayoutType("tree-crud");
        config.setSearchSchema("[{\"field\":\"categoryId\",\"searchType\":\"eq\"},{\"field\":\"title\",\"searchType\":\"like\"}]");
        config.setColumnsSchema("[{\"field\":\"title\"}]");
        config.setOptions("{\"treeConfig\":{\"sourceTableName\":\"nodes\",\"filterField\":\"categoryId\"}}");
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("records"));
        when(dataScope.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("records"))
                .thenReturn(Map.of("id", "id", "title", "title", "categoryId", "category_id"));
        when(repository.tableExists("nodes")).thenReturn(true);
        when(repository.getColumnMapping("nodes")).thenReturn(Map.of("id", "id", "parentId", "parent_id"));
        when(repository.selectPage(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any()))
                .thenReturn(new Page<>(1, 10));
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), dataScope, null, null, null, null, null, sources);
    }

    @Test
    void pageOverridesOnlyDeclaredFieldsAndSupportedOperatorsWithoutChangingInput() {
        DynamicCrudQuery query = query(Map.of("title", "needle"));
        query.setSearchTypeMap(Map.of(" title ", " RIGHT_LIKE ", "unknown", "eq", "categoryId", "unsafe"));
        PageQuery page = new PageQuery();
        page.setPageNum(2);
        page.setPageSize(20);
        page.setOrderByColumn("title");
        page.setIsAsc("desc");
        service.selectPage("records", page, query);
        verify(repository).selectPage(eq("records"), eq(2), eq(20), eq(Map.of("title", "needle")),
                eq(Set.of("id", "title", "categoryId")), eq(Map.of("title", "right_like", "categoryId", "eq")),
                anyMap(), eq("title DESC"), same(scope));
        assertEquals(" RIGHT_LIKE ", query.getSearchTypeMap().get(" title "));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void scalarDescendantsNormalizeAllSuffixAndTerminateCyclesInStableOrder() {
        when(repository.selectListByColumn("nodes", "parent_id", "1")).thenReturn(List.of(Map.of("id", 2L)));
        when(repository.selectListByColumn("nodes", "parent_id", 2L)).thenReturn(List.of(Map.of("id", "1")));
        Map<String, Object> original = new LinkedHashMap<>(Map.of("categoryId", "1ALL", "categoryId_includeChildren", true));
        service.selectPage("records", new PageQuery(), query(original));
        verify(repository).selectPage(eq("records"), anyInt(), anyInt(), eq(Map.of("categoryId", List.of("1", 2L))),
                anySet(), eq(Map.of("title", "like", "categoryId", "in")), anyMap(), anyString(), same(scope));
        assertEquals("1ALL", original.get("categoryId"));
        assertEquals(true, original.get("categoryId_includeChildren"));
        verify(repository, times(1)).selectListByColumn("nodes", "parent_id", "1");
    }

    @Test
    void suppliedCollectionsDeduplicateWithoutTraversalAndUnknownFieldsRemainUnexpanded() {
        DynamicCrudQuery query = query(Map.of("categoryId", Arrays.asList(1, null, 1, " ", 2),
                "categoryId_includeChildren", "1", "unknown", 4, "unknown_includeChildren", true));
        service.selectPage("records", new PageQuery(), query);
        verify(repository).selectPage(eq("records"), anyInt(), anyInt(),
                eq(Map.of("categoryId", List.of(1, 2), "unknown", 4, "unknown_includeChildren", true)),
                anySet(), eq(Map.of("title", "like", "categoryId", "in")), anyMap(), anyString(), same(scope));
        verify(repository, never()).selectListByColumn(anyString(), anyString(), any());
    }

    @Test
    void customConditionsCopyOnlyExpandedEntriesAndKeepCallerValues() {
        CustomQueryConditionDTO condition = new CustomQueryConditionDTO();
        condition.setField("categoryId");
        condition.setValue("a,b,a");
        condition.setValueEnd("end");
        condition.setOperator("eq");
        condition.setRelation("OR");
        condition.setIncludeChildren(true);
        CustomQueryExecuteDTO request = new CustomQueryExecuteDTO();
        request.setConditions(List.of(condition));
        when(repository.selectCustomPage(eq("records"), anyInt(), anyInt(), any(), anyList(), anySet(), anyMap(), anyString(), same(scope)))
                .thenAnswer(call -> {
                    List<CustomQueryConditionDTO> actual = call.getArgument(4);
                    assertNotSame(condition, actual.get(0));
                    assertEquals("in", actual.get(0).getOperator());
                    assertEquals(List.of("a", "b"), actual.get(0).getValue());
                    assertEquals("OR", actual.get(0).getRelation());
                    assertNull(actual.get(0).getValueEnd());
                    return new Page<Map<String, Object>>(1, 10);
                });
        service.selectCustomPage("records", request);
        assertEquals("eq", condition.getOperator());
        assertEquals("a,b,a", condition.getValue());
        assertEquals("end", condition.getValueEnd());
    }

    @Test
    void exportKeepsExplicitDataScopeOrderingAndLimitWhileSharingPreparedConditions() {
        DataScopeContext context = mock(DataScopeContext.class);
        when(dataScope.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull(), eq(context))).thenReturn(scope);
        DynamicCrudQuery query = query(Map.of("categoryId", "1,2", "categoryId_includeChildren", true));
        when(repository.countList(eq("records"), any(), anySet(), anyMap(), anyMap(), same(scope))).thenReturn(7L);
        assertEquals(7L, service.countExportRows("records", query, context));
        service.selectExportPageRows("records", query, -1, 9000, context);
        verify(repository).selectPageRecords(eq("records"), eq(1), eq(5000), eq(Map.of("categoryId", List.of("1", "2"))),
                anySet(), eq(Map.of("title", "like", "categoryId", "in")), anyMap(), eq("id DESC"), same(scope));
        verify(dataScope, times(2)).buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull(), eq(context));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void descendantQueryFailurePropagatesBeforeMainQueryAndReleasesScope() {
        when(repository.selectListByColumn("nodes", "parent_id", 1L)).thenThrow(new IllegalStateException("descendants unavailable"));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.selectPage("records", new PageQuery(),
                query(Map.of("categoryId", 1L, "categoryId_includeChildren", true))));
        assertEquals("descendants unavailable", error.getMessage());
        verify(repository, never()).selectPage(anyString(), anyInt(), anyInt(), any(), anySet(), anyMap(), anyMap(), anyString(), any());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    private DynamicCrudQuery query(Map<String, Object> params) {
        DynamicCrudQuery query = new DynamicCrudQuery();
        query.setSearchParams(params);
        return query;
    }
}
