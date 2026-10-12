package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public writes retain unique constraint values, exclusions, permission ordering and failures. */
class DynamicCrudUniqueWriteTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService permissions = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope =
            new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() throws Exception {
        config.setConfigKey("records");
        config.setTableName("records");
        config.setObjectCode("RECORD");
        config.setMode("CONFIG");
        config.setPublishStatus("PUBLISHED");
        config.setTenantId(7L);
        config.setEditSchema("[{\"field\":\"email\"},{\"field\":\"groupKey\"},{\"field\":\"title\"}]");
        schema(List.of("email", "groupKey"), true, "duplicate");
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("records"));
        when(permissions.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("records"))
                .thenReturn(Map.of("email", "email", "groupKey", "group_key", "group_key", "group_key", "title", "title"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("email", "group_key", "title"));
        when(repository.selectById("records", "id", 4L, scope))
                .thenReturn(Map.of("id", 4L, "email", " OLD@EXAMPLE ", "group_key", "Team"));
        when(repository.updateById(eq("records"), eq("id"), eq(4L), anyMap(), same(scope))).thenReturn(1);
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), permissions, mock(BusinessDocumentConfigService.class), null,
                mock(StoredAggregateRefreshService.class), mock(StoredFormulaRuntime.class), null, sources);
    }

    @Test
    void createNormalizesCompositeValuesAndRejectsDuplicateBeforeWriting() {
        when(repository.existsByColumns("records", Map.of("email", "a@example", "group_key", "team"), "id", null, null))
                .thenAnswer(call -> {
                    assertEquals("records", LowcodeRuntimeDataSourceContextHolder.get().getTableName());
                    return true;
                });
        Map<String, Object> data = data(Map.of("email", " A@Example ", "groupKey", "Team"));
        assertEquals("duplicate", assertThrows(BusinessException.class, () -> service.insert("records", data)).getMessage());
        assertEquals(" A@Example ", data.get("email"));
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void blankCompositeIsSkippedWhileExplicitNullIsQueriedWhenNotIgnored() throws Exception {
        service.insert("records", data(Map.of("email", " ", "groupKey", "Team")));
        verify(repository, never()).existsByColumns(anyString(), anyMap(), anyString(), any(), any());
        schema(List.of("email"), false, "duplicate");
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("email", null);
        service.insertCommandRecord("records", data(Map.of("title", "sample")));
        verify(repository).existsByColumns("records", expected, "id", null, null);
    }

    @Test
    void editRuleUsesModelDefaultAndDoesNotDuplicateDeclaredConstraint() throws Exception {
        config.setModelSchema(mapper.writeValueAsString(Map.of("fields", List.of(
                Map.of("field", "email", "columnName", "email", "defaultValue", " DEFAULT@EXAMPLE "),
                Map.of("field", "title", "columnName", "title")),
                "validationRules", List.of(Map.of("type", "UNIQUE", "field", "email", "normalizers", List.of("trim", "lower"))))));
        config.setEditSchema("{\"components\":[{\"field\":\"email\",\"unique\":true},{\"field\":\"title\"}]}");
        service.insert("records", data(Map.of("title", "sample")));
        verify(repository, times(1)).existsByColumns("records", Map.of("email", "default@example"), "id", null, null);
    }

    @Test
    void partialUpdateCombinesBeforeValuesAndExcludesSelfIncludingExplicitNullFallback() {
        Map<String, Object> data = data(Map.of("id", 4L, "group_key", "New Team"));
        data.put("email", null);
        service.updateById("records", data);
        verify(repository).existsByColumns("records", Map.of("email", "old@example", "group_key", "new team"), "id", 4L, null);
        verify(repository).updateById(eq("records"), eq("id"), eq(4L), anyMap(), same(scope));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void unrelatedUpdateDoesNotQueryUniqueConstraints() {
        service.updateById("records", data(Map.of("id", 4L, "title", "new title")));
        verify(repository, never()).existsByColumns(anyString(), anyMap(), anyString(), any(), any());
        verify(repository).updateById("records", "id", 4L, Map.of("title", "new title"), scope);
    }

    @Test
    void invalidConstraintFieldOrUnsyncedColumnFailsBeforeDuplicateQuery() throws Exception {
        schema(List.of("missing"), true, "duplicate");
        assertTrue(assertThrows(BusinessException.class,
                () -> service.insert("records", data(Map.of("title", "sample")))).getMessage().contains("missing"));
        schema(List.of("email"), true, "duplicate");
        when(repository.getTableColumns("records")).thenReturn(Set.of("title"));
        assertTrue(assertThrows(BusinessException.class,
                () -> service.insert("records", data(Map.of("email", "a@example")))).getMessage().contains("email"));
        verify(repository, never()).existsByColumns(anyString(), anyMap(), anyString(), any(), any());
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
    }

    @Test
    void unauthorizedUpdateStopsBeforeUniqueProbe() {
        when(repository.selectById("records", "id", 4L, scope)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.updateById("records", data(Map.of("id", 4L, "email", "a@example"))));
        verify(repository, never()).existsByColumns(anyString(), anyMap(), anyString(), any(), any());
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
    }

    @Test
    void repositoryFailureIsPropagatedAndRestoresTheOuterDataSource() {
        IllegalStateException failure = new IllegalStateException("query unavailable");
        when(repository.existsByColumns(anyString(), anyMap(), anyString(), any(), any())).thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> service.insert("records", data(Map.of("email", "a@example", "groupKey", "Team")))));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
    }

    private void schema(List<String> fields, boolean ignoreBlank, String message) throws Exception {
        config.setModelSchema(mapper.writeValueAsString(Map.of("fields", List.of(
                Map.of("field", "email", "columnName", "email", "label", "Email"),
                Map.of("field", "groupKey", "columnName", "group_key"),
                Map.of("field", "title", "columnName", "title")),
                "uniqueConstraints", List.of(Map.of("fields", fields, "normalize", List.of("trim", "lowercase"),
                        "ignoreBlank", ignoreBlank, "message", message)))));
    }

    private Map<String, Object> data(Map<String, Object> values) {
        return new LinkedHashMap<>(values);
    }
}
