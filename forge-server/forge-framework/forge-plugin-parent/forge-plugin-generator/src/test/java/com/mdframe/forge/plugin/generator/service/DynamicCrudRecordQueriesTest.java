package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Public record entry points preserve admission, runtime identity and scoped result assembly. */
class DynamicCrudRecordQueriesTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService permissions = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final VirtualFormulaRuntime formulas = mock(VirtualFormulaRuntime.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("runtime_records");
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("owner_id=:owner", Map.of("owner", 7L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("records");
        config.setObjectCode("records");
        config.setTableName("logical_records");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(permissions.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("runtime_records");
            return expected.equals(actual);
        }), eq("runtime_records"), isNull())).thenReturn(scope);
        when(permissions.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("runtime_records");
            return expected.equals(actual);
        }), eq("runtime_records"), eq("t0"))).thenReturn(scope);
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), permissions, null, null, null, null, formulas, sources);
        verify(sources).getResolution();
        clearInvocations(sources);
    }

    @Test
    void emptyAndBlankBatchSkipsConfigurationAndRuntimeAccess() {
        assertTrue(service.selectByIds("missing", null).isEmpty());
        assertTrue(service.selectByIds("missing", Arrays.asList(null, "", "  ")).isEmpty());
        verifyNoInteractions(configs, sources, repository, formulas);
    }

    @Test
    void normalizedBatchUsesRuntimePrimaryKeyAndPreservesFirstPresentNullSemantics() {
        config.setModelSchema("{\"fields\":[{\"field\":\"displayName\"}]}");
        LowcodePrimaryKeyStrategy key = new LowcodePrimaryKeyStrategy();
        key.setField("recordKey");
        key.setColumnName("record_key");
        runtime.setPrimaryKey(key);
        Map<String, Object> missingKey = new LinkedHashMap<>();
        missingKey.put("record_key", null);
        missingKey.put("id", "must-not-fallback");
        when(repository.selectListByColumnIn("runtime_records", "record_key", List.of("a", " a ", 8L), scope))
                .thenAnswer(invocation -> {
                    assertSame(runtime, LowcodeRuntimeDataSourceContextHolder.get());
                    return List.of(Map.of("record_key", "a", "display_name", "first"),
                            Map.of("record_key", "a", "display_name", "last"),
                            Map.of("id", 8L, "display_name", "fallback"), missingKey);
                });
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            Map<Object, Map<String, Object>> result = service.selectByIds("records", Arrays.asList(null, "", " ", "a", "a", " a ", 8L));
            assertEquals(List.of("a", 8L), new ArrayList<>(result.keySet()));
            assertEquals("last", result.get("a").get("displayName"));
            assertEquals("fallback", result.get(8L).get("displayName"));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(formulas, times(4)).calculate(anyList(), any(), any());
    }

    @Test
    void draftReadBypassesPublicationOnlyAndStillRejectsDisabledOrWrongMode() {
        config.setPublishStatus("DRAFT");
        assertThrows(BusinessException.class, () -> service.selectById("records", 1L));
        verifyNoInteractions(sources, repository);
        when(repository.selectById("runtime_records", "id", 1L, scope)).thenReturn(Map.of("id", 1L, "display_name", "draft"));
        assertEquals("draft", service.selectByIdAllowDraft("records", 1L).get("displayName"));
        verify(configs, never()).resolvePublishedRuntimeConfig(any());
        var draftRuntime = RuntimeCrudConfigAdapter.fromDesign(config);
        draftRuntime.setTableName("runtime_records");
        config.setStatus("1");
        assertThrows(BusinessException.class, () -> service.selectByIdAllowDraft("records", 1L));
        config.setStatus("0");
        config.setMode("CODE");
        assertThrows(BusinessException.class, () -> service.selectByIdAllowDraft("records", 1L));
        verify(runtimeSources, times(1)).resolveRuntime(argThat(draftRuntime::equals));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void joinedIdReadUsesScopedPlanAndPreservesJoinedKeys() {
        config.setModelSchema("""
                {"object":{"code":"records"},"fields":[{"field":"title"}],
                "relations":[{"targetObjectCode":"owners","sourceField":"owner_id","targetField":"id","relationType":"REFERENCE"}]}
                """);
        config.setPageSchema("""
                {"primaryModelCode":"records","modelRefs":[{"modelCode":"records","tableName":"runtime_records","primary":true},
                {"modelCode":"owners","tableName":"owners","fields":[{"field":"id"}]}]}
                """);
        when(repository.tableExists("owners")).thenReturn(true);
        when(repository.getColumnMapping("owners")).thenReturn(Map.of("id", "id"));
        when(repository.getColumnMapping("runtime_records")).thenReturn(Map.of("id", "id", "title", "title", "ownerId", "owner_id"));
        when(repository.getTableColumns("runtime_records")).thenReturn(Set.of("id", "title", "owner_id"));
        Map<String, Object> row = new LinkedHashMap<>(Map.of("id", 1L, "joined_key", "kept"));
        when(repository.selectJoinedById(eq("runtime_records"), eq(1L), anyList(), anyList(), same(scope)))
                .thenAnswer(invocation -> {
                    assertSame(runtime, LowcodeRuntimeDataSourceContextHolder.get());
                    return row;
                });
        assertSame(row, service.selectById("records", 1L));
        assertTrue(row.containsKey("joined_key"));
        assertFalse(row.containsKey("joinedKey"));
        verify(formulas).calculate(anyList(), any(), any());
        verify(repository, never()).selectById(anyString(), anyString(), any(), any());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void batchRepositoryFailurePropagatesAndRestoresCallerScope() {
        IllegalStateException failure = new IllegalStateException("unavailable");
        when(repository.selectListByColumnIn("runtime_records", "id", List.of(1L), scope)).thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> service.selectByIds("records", List.of(1L))));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verifyNoInteractions(formulas);
    }
}
