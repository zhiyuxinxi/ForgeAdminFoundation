package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.StoredAggregateRefreshService;
import com.mdframe.forge.plugin.generator.service.formula.StoredFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Keeps form, internal and physical-column write contracts distinct through public entries. */
class DynamicCrudWriteFieldsTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService scopes = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final StoredFormulaRuntime formulas = mock(StoredFormulaRuntime.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("records");
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("owner_id=:owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("records");
        config.setObjectCode("RECORD");
        config.setTableName("records");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("""
                {"fields":[{"field":"newField","columnName":"new_col"},
                {"field":"hiddenField","columnName":"hidden_col","fieldStatus":"HIDDEN"},
                {"field":"disabledField","columnName":"disabled_col","fieldStatus":"DISABLED"},
                {"field":"readonlyField","columnName":"readonly_col","readonly":true},
                {"field":"invisibleField","columnName":"invisible_col","formVisible":false},
                {"field":"systemValue","columnName":"system_value","systemField":true},
                {"field":"recordKey","columnName":"record_pk","primaryKey":true},
                {"field":"sequenceValue","columnName":"sequence_value","autoIncrement":true}]}
                """);
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(scopes.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("records")).thenReturn(Map.of("physicalOnly", "physical_only"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("id", "new_col", "hidden_col", "disabled_col",
                "readonly_col", "invisible_col", "system_value", "record_pk", "sequence_value", "physical_only"));
        when(repository.selectById(eq("records"), anyString(), eq(4L), same(scope))).thenReturn(Map.of("id", 4L, "new_col", "before"));
        when(repository.updateById(eq("records"), anyString(), eq(4L), anyMap(), same(scope))).thenReturn(1);
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), scopes, mock(BusinessDocumentConfigService.class), null,
                mock(StoredAggregateRefreshService.class), formulas, null, sources);
    }

    @Test
    void assignedPrimaryKeyIsWritableOnlyWhenCreatingTheConfiguredRecord() {
        LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
        primaryKey.setField("recordKey");
        primaryKey.setColumnName("record_pk");
        primaryKey.setAutoIncrement(false);
        runtime.setPrimaryKey(primaryKey);
        service.insert("records", new LinkedHashMap<>(Map.of("recordKey", "manual-1", "newField", "value")));
        verify(repository).insertReturningKey("records", Map.of("record_pk", "manual-1", "new_col", "value"),
                "record_pk", false);
        service.updateInternalFieldsById("records", 4L, new LinkedHashMap<>(Map.of("recordKey", "replacement", "newField", "changed")));
        verify(repository).updateById("records", "record_pk", 4L, Map.of("new_col", "changed"), scope);
    }

    @Test
    void formVisibilityAndInternalModelPermissionsStayDifferent() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (String key : List.of("newField", "hiddenField", "disabledField", "readonlyField", "invisibleField",
                "systemValue", "recordKey", "sequenceValue", "physicalOnly", "tenantId")) {
            data.put(key, "x");
        }
        service.insert("records", new LinkedHashMap<>(data));
        verify(repository).insertReturningKey("records", Map.of("new_col", "x"), "id", true);
        service.insertInternal("records", data);
        verify(repository).insertReturningKey("records", Map.of("new_col", "x", "hidden_col", "x", "disabled_col", "x",
                "readonly_col", "x", "invisible_col", "x", "system_value", "x", "record_pk", "x",
                "sequence_value", "x", "physical_only", "x"), "id", true);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void explicitEditContractIsRetainedWhileUpdateExcludesCustomPrimaryKeyAliases() {
        customPrimaryKey();
        config.setEditSchema("[{\"field\":\"hiddenField\"},{\"field\":\"recordKey\"},{\"field\":\"record_pk\"}]");
        service.updateById("records", new LinkedHashMap<>(Map.of("recordKey", 4L, "record_pk", 4L,
                "hiddenField", "per edit contract", "newField", "changed")));
        verify(repository).updateById("records", "record_pk", 4L,
                Map.of("hidden_col", "per edit contract", "new_col", "changed"), scope);
    }

    @Test
    void internalMissingColumnAndUnknownFieldFailBeforePersistenceAndRestoreScope() {
        config.setModelSchema("{\"fields\":[{\"field\":\"ghost\",\"columnName\":\"missing_col\"}]}");
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            BusinessException missing = assertThrows(BusinessException.class,
                    () -> service.insertInternal("records", new LinkedHashMap<>(Map.of("ghost", "x"))));
            assertTrue(missing.getMessage().contains("missing_col"));
            BusinessException unknown = assertThrows(BusinessException.class,
                    () -> service.insertInternal("records", new LinkedHashMap<>(Map.of("unknown", "x"))));
            assertTrue(unknown.getMessage().contains("字段不在模型中"));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
    }

    @Test
    void bothInternalUpdatePathsKeepPhysicalColumnsAndExcludeCustomPrimaryKeys() {
        customPrimaryKey();
        Map<String, Object> data = new LinkedHashMap<>(Map.of("recordKey", 4L, "record_pk", 4L,
                "hiddenField", "internal", "physicalOnly", "raw", "tenantId", 99L));
        service.updateFieldsInternal("records", 4L, new LinkedHashMap<>(data));
        service.updateInternalFieldsById("records", 4L, data);
        verify(repository, times(2)).updateById("records", "record_pk", 4L,
                Map.of("hidden_col", "internal", "physical_only", "raw"), scope);
        BusinessException missing = assertThrows(BusinessException.class,
                () -> service.updateInternalFieldsById("records", 4L, new LinkedHashMap<>(Map.of("unknown", "x"))));
        assertTrue(missing.getMessage().contains("unknown"));
    }

    @Test
    void storedFormulaOutputRemainsWritableWithoutMakingVirtualFieldsWritable() {
        config.setModelSchema("""
                {"fields":[{"field":"storedValue","columnName":"stored_value","readonly":true,
                 "formVisible":false,"formulaConfig":{"mode":"STORED"}},
                 {"field":"virtualValue","columnName":"virtual_value","readonly":true,
                 "formVisible":false,"formulaConfig":{"mode":"VIRTUAL"}}]}
                """);
        doAnswer(call -> {
            List<Map<String, Object>> rows = call.getArgument(0);
            rows.get(0).put("storedValue", 99);
            return null;
        }).when(formulas).calculate(anyList(), any(), any());
        service.insert("records", new LinkedHashMap<>(Map.of("virtualValue", 55)));
        verify(repository).insertReturningKey("records", Map.of("stored_value", 99), "id", true);
    }

    private void customPrimaryKey() {
        LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
        primaryKey.setField("recordKey");
        primaryKey.setColumnName("record_pk");
        primaryKey.setAutoIncrement(false);
        runtime.setPrimaryKey(primaryKey);
    }
}
