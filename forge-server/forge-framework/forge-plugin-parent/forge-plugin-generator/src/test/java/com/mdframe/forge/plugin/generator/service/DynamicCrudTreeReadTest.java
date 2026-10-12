package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.StoredAggregateRefreshService;
import com.mdframe.forge.plugin.generator.service.formula.StoredFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Identical public tree behavior before and after responsibility extraction. */
class DynamicCrudTreeReadTest {
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
        config.setConfigKey("navigation");
        config.setObjectCode("navigation");
        config.setTableName("navigation");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setLayoutType("tree-crud");
        when(configs.getByConfigKey("navigation")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("navigation"));
        when(dataScope.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("navigation");
            return expected.equals(actual);
        }), eq("navigation"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("navigation"))
                .thenReturn(Map.of("id", "id", "parentId", "parent_id", "name", "name"));
        when(repository.getTableColumns("navigation")).thenReturn(Set.of("id", "parent_id", "name"));
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), dataScope, mock(BusinessDocumentConfigService.class),
                null, mock(StoredAggregateRefreshService.class), mock(StoredFormulaRuntime.class),
                mock(VirtualFormulaRuntime.class), sources);
    }

    @Test
    void fullTreeKeepsOrderingOrphansSelfRootsAndLeafShape() {
        fullRows(List.of(row(1, 0, "root"), row(2, 1, "child"), row(3, 3, "self"), row(4, 99, "orphan")));
        List<Map<String, Object>> result = service.selectTree("navigation");
        assertEquals(List.of(1, 3, 4), result.stream().map(x -> x.get("id")).toList());
        assertEquals(false, result.get(0).get("isLeaf"));
        assertEquals("root", result.get(0).get("label"));
        assertEquals(1, result.get(0).get("targetValue"));
        List<?> children = (List<?>) result.get(0).get("children");
        assertEquals(2, ((Map<?, ?>) children.get(0)).get("id"));
        assertEquals(true, ((Map<?, ?>) children.get(0)).get("isLeaf"));
        assertFalse(((Map<?, ?>) children.get(0)).containsKey("children"));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void fullTreeRestoresAncestorsAsReadOnlyWithoutChangingScopedSelection() {
        fullRows(List.of(row(2, 1, "visible")));
        when(repository.selectListByColumn("navigation", "id", 1)).thenReturn(List.of(row(1, 0, "ancestor")));
        List<Map<String, Object>> result = service.selectTree("navigation");
        Map<String, Object> ancestor = result.get(0);
        assertEquals(true, ancestor.get("_scopeAncestor"));
        assertEquals(false, ancestor.get("_dataScopeWritable"));
        Map<?, ?> child = (Map<?, ?>) ((List<?>) ancestor.get("children")).get(0);
        assertEquals(false, child.get("_scopeAncestor"));
        assertEquals(true, child.get("_dataScopeWritable"));
        verify(repository).selectList(eq("navigation"), isNull(), anySet(), anyMap(), anyMap(), eq("id ASC"), eq(5000), same(scope));
    }

    @Test
    void lazyTreeKeepsParentFallbackAndScopedLeafProbe() {
        when(repository.selectTreeChildren("navigation", "parent_id", "7", "id ASC", 5000, scope))
                .thenReturn(List.of(row(8, 7, "branch"), row(9, 7, "leaf")));
        when(repository.existsByColumn("navigation", "parent_id", 8, scope)).thenReturn(true);
        List<Map<String, Object>> result = service.selectTree("navigation", " ", "7", "lazy");
        assertEquals(List.of(false, true), result.stream().map(x -> x.get("isLeaf")).toList());
        assertTrue(result.stream().noneMatch(x -> x.containsKey("children")));
        verify(repository).existsByColumn("navigation", "parent_id", 9, scope);
        verify(repository, never()).selectListByColumn(anyString(), anyString(), any());
    }

    @Test
    void optionsOverrideModelTreeFieldsAndExplicitSortIsMapped() {
        config.setModelSchema("{\"treeConfig\":{\"labelField\":\"missing\",\"childrenField\":\"modelNodes\",\"loadMode\":\"lazy\"}}");
        config.setOptions("{\"treeConfig\":{\"labelField\":\"name\",\"childrenField\":\"nodes\",\"loadMode\":\"full\"}}");
        when(repository.selectList(eq("navigation"), isNull(), anySet(), anyMap(), anyMap(), eq("name DESC"), eq(5000), same(scope)))
                .thenReturn(List.of(row(1, 0, "root"), row(2, 1, "child")));
        List<Map<String, Object>> result = service.selectTree("navigation", null, null, null, "name", "desc");
        assertTrue(result.get(0).containsKey("nodes"));
        assertFalse(result.get(0).containsKey("children"));
        assertEquals("root", result.get(0).get("label"));
    }

    @Test
    void unknownKeyAndParentFailBeforeReadAndReleaseRuntimeScope() {
        for (String field : List.of("keyField", "parentField")) {
            config.setOptions("{\"treeConfig\":{\"" + field + "\":\"unknown\"}}");
            assertThrows(BusinessException.class, () -> service.selectTree("navigation"));
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(dataScope, never()).buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("navigation");
            return expected.equals(actual);
        }), eq("navigation"), isNull());
        verify(repository, never()).selectList(anyString(), any(), anySet(), anyMap(), anyMap(), anyString(), anyInt(), any());
    }

    @Test
    void repositoryFailurePropagatesAndReleasesRuntimeScope() {
        when(repository.selectList(eq("navigation"), isNull(), anySet(), anyMap(), anyMap(), anyString(), anyInt(), same(scope)))
                .thenThrow(new IllegalStateException("query failed"));
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.selectTree("navigation"));
        assertEquals("query failed", failure.getMessage());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    private void fullRows(List<Map<String, Object>> rows) {
        when(repository.selectList(eq("navigation"), isNull(), anySet(), anyMap(), anyMap(), eq("id ASC"), eq(5000), same(scope)))
                .thenReturn(rows);
    }

    private Map<String, Object> row(int id, int parent, String name) {
        return Map.of("id", id, "parent_id", parent, "name", name);
    }
}
