package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
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
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public update entries preserve the formula writeback boundary. */
class DynamicCrudFormulaWriteTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService scopes = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final StoredFormulaRuntime formulas = mock(StoredFormulaRuntime.class);
    private final StoredAggregateRefreshService aggregates = mock(StoredAggregateRefreshService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope =
            new DynamicCrudRepository.SqlCondition("owner_id=:owner", Map.of("owner", 8L));
    private final Map<String, Object> existing = new LinkedHashMap<>();
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("suite_records");
        config.setObjectCode("RECORD");
        config.setTenantId(7L);
        config.setTableName("records");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("""
                {"fields":[{"field":"unitPrice","columnName":"unit_price"},
                {"field":"quantity","columnName":"quantity"},
                {"field":"storedValue","columnName":"stored_value","readonly":true,"formVisible":false,
                 "formulaConfig":{"expression":"quantity"}},
                {"field":"lowerValue","columnName":"lower_value","readonly":true,"formVisible":false,
                 "formulaConfig":{"mode":"stored"}},
                {"field":"nullValue","columnName":"null_value","readonly":true,"formVisible":false,
                 "formulaConfig":{"mode":"STORED"}},
                {"field":"virtualValue","columnName":"virtual_value","readonly":true,"formVisible":false,
                 "formulaConfig":{"mode":"VIRTUAL"}}]}
                """);
        LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("records");
        LowcodePrimaryKeyStrategy primary = new LowcodePrimaryKeyStrategy();
        primary.setField("recordKey");
        primary.setColumnName("record_pk");
        primary.setAutoIncrement(false);
        runtime.setPrimaryKey(primary);
        when(configs.getByConfigKey("suite_records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(scopes.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("records")).thenReturn(Map.of(
                "unitPrice", "unit_price", "quantity", "quantity", "storedValue", "stored_value",
                "lowerValue", "lower_value", "nullValue", "null_value", "virtualValue", "virtual_value"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("record_pk", "unit_price", "quantity",
                "stored_value", "lower_value", "null_value", "virtual_value"));
        existing.put("record_pk", 99L);
        existing.put("unit_price", 12);
        existing.put("quantity", 3);
        existing.put("stored_value", 36);
        when(repository.selectById("records", "record_pk", 4L, scope)).thenReturn(existing);
        when(repository.updateById(eq("records"), eq("record_pk"), eq(4L), anyMap(), same(scope))).thenReturn(1);
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), scopes, mock(BusinessDocumentConfigService.class), null,
                aggregates, formulas, mock(VirtualFormulaRuntime.class), sources);
    }

    @Test
    void publicUpdateChecksReferencesAgainstTheScopedExistingRecordBeforeWriting() {
        config.setModelSchema("""
                {"fields":[{"field":"orgId","columnName":"org_id","dataType":"bigint","componentType":"orgTreeSelect"}]}
                """);
        existing.put("org_id", 10L);
        RuntimeIdentityReferenceService references = mock(RuntimeIdentityReferenceService.class);
        service.setIdentityReferences(references);
        Map<String, Object> patch = new LinkedHashMap<>(Map.of("recordKey", 4L, "orgId", 20L));
        doThrow(new BusinessException(400, "Invalid identity reference"))
                .when(references).validateWrite(any(), same(patch), anyMap(), eq("edit"));
        assertThrows(BusinessException.class, () -> service.updateById("suite_records", patch));
        verify(references).validateWrite(argThat(actual -> "suite_records".equals(actual.getConfigKey())),
                same(patch), argThat(before -> Long.valueOf(10L).equals(before.get("orgId"))), eq("edit"));
        verify(repository).selectById("records", "record_pk", 4L, scope);
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(formulas);
    }

    @Test
    void publicUpdateRejectsForgedTaskLinksWithoutAnIdentityField() {
        config.setOptions("""
                {"publicTaskBindings":[{"spec":{"taskIdField":"taskId"}}]}
                """);
        existing.put("task_id", 100L);
        Map<String, Object> patch = new LinkedHashMap<>(Map.of("recordKey", 4L, "taskId", 200L));
        assertThrows(BusinessException.class, () -> service.updateById("suite_records", patch));
        verify(repository).selectById("records", "record_pk", 4L, scope);
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(formulas);
    }

    @Test
    void patchAliasesAndNullOverrideOldRecordWhileTrustedContextAndPrimaryKeyRemain() {
        Map<String, Object> before = new LinkedHashMap<>(existing);
        doAnswer(call -> {
            List<Map<String, Object>> rows = call.getArgument(0);
            Map<String, Object> row = rows.get(0);
            FormulaRuntimeContext context = call.getArgument(2);
            assertEquals(7L, context.getTenantId());
            assertEquals("suite", context.getSuiteCode());
            assertEquals("RECORD", context.getSourceObjectCode());
            assertEquals(4L, row.get("id"));
            assertEquals(4L, row.get("recordKey"));
            assertEquals(4L, row.get("record_pk"));
            assertNull(row.get("unitPrice"));
            assertTrue(row.containsKey("unit_price"));
            assertNull(row.get("unit_price"));
            assertEquals(5, row.get("quantity"));
            assertEquals(36, row.get("storedValue"));
            assertEquals(row, context.getCurrentRow());
            assertNotSame(row, context.getCurrentRow());
            row.put("storedValue", 50);
            return rows;
        }).when(formulas).calculate(anyList(), any(), any());
        Map<String, Object> patch = new LinkedHashMap<>();
        patch.put("recordKey", 4L);
        patch.put("unitPrice", 20);
        patch.put("unit_price", null);
        patch.put("quantity", 5);
        service.updateById("suite_records", patch);
        assertEquals(50, patch.get("storedValue"));
        assertEquals(before, existing);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        verify(repository).updateById(eq("records"), eq("record_pk"), eq(4L), argThat(values ->
                values.containsKey("unit_price") && values.get("unit_price") == null
                        && Integer.valueOf(50).equals(values.get("stored_value"))), same(scope));
    }

    @Test
    void onlyStoredOutputsAreCopiedAndNullComputedValueStaysWritable() {
        doAnswer(call -> {
            List<Map<String, Object>> rows = call.getArgument(0);
            rows.get(0).put("storedValue", 51);
            rows.get(0).put("lowerValue", 52);
            rows.get(0).put("nullValue", null);
            rows.get(0).put("virtualValue", 53);
            rows.get(0).put("unrelated", 54);
            return rows;
        }).when(formulas).calculate(anyList(), any(), any());
        Map<String, Object> patch = new LinkedHashMap<>(Map.of("recordKey", 4L, "quantity", 6));
        service.updateById("suite_records", patch);
        assertEquals(51, patch.get("storedValue"));
        assertEquals(52, patch.get("lowerValue"));
        assertTrue(patch.containsKey("nullValue"));
        assertNull(patch.get("nullValue"));
        assertFalse(patch.containsKey("virtualValue"));
        assertFalse(patch.containsKey("unrelated"));
        Map<String, Object> expected = new LinkedHashMap<>(Map.of("quantity", 6, "stored_value", 51, "lower_value", 52));
        expected.put("null_value", null);
        verify(repository).updateById("records", "record_pk", 4L, expected, scope);
    }

    @Test
    void absentAuthorizedRecordFailsBeforeFormulaOrWriteAndRestoresOuterScope() {
        when(repository.selectById("records", "record_pk", 4L, scope)).thenReturn(null);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.updateFieldsInternal("suite_records", 4L, new LinkedHashMap<>(Map.of("quantity", 2))));
            assertEquals("无权限更新该数据或数据不存在", error.getMessage());
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verifyNoInteractions(formulas);
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
    }

    @Test
    void formulaExceptionPropagatesWithoutCopyingPartialResultsOrPersisting() {
        IllegalStateException failure = new IllegalStateException("formula failed");
        doAnswer(call -> {
            List<Map<String, Object>> rows = call.getArgument(0);
            rows.get(0).put("storedValue", 999);
            throw failure;
        }).when(formulas).calculate(anyList(), any(), any());
        Map<String, Object> patch = new LinkedHashMap<>(Map.of("quantity", 2));
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> service.updateFieldsInternal("suite_records", 4L, patch)));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        assertEquals(Map.of("quantity", 2), patch);
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(aggregates);
    }
}
