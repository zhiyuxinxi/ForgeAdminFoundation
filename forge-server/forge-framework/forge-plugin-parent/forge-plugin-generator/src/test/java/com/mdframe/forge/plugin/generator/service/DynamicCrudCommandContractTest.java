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
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Published command contracts through the original transactional application entry points. */
class DynamicCrudCommandContractTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService scopes = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final StoredAggregateRefreshService aggregates = mock(StoredAggregateRefreshService.class);
    private final PersistentCryptoService crypto = mock(PersistentCryptoService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final LowcodeRuntimeDataSourceContext runtime = LowcodeRuntimeDataSourceContext.master("records");
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 7L));
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
                {"fields":[{"field":"quantity","columnName":"qty_value"},
                {"field":"state","columnName":"state_value"},{"field":"secret","columnName":"secret_value"},
                {"field":"disabled","fieldStatus":"DISABLED"},{"field":"systemOnly","systemField":true}]}
                """);
        config.setEncryptConfig("{\"secret\":{\"algorithm\":\"AES\",\"columnName\":\"secret_value\"}}");
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(runtime);
        when(scopes.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("records")).thenReturn(Map.of("qtyValue", "qty_value", "stateValue", "state_value", "secretValue", "secret_value"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("id", "qty_value", "state_value", "secret_value", "disabled", "system_only", "unknown"));
        when(repository.selectById("records", "id", 1L, scope)).thenReturn(Map.of("id", 1L, "qty_value", 2));
        when(repository.selectById("records", "id", 1L, null)).thenReturn(Map.of("id", 1L, "qty_value", 3));
        when(repository.updateById(eq("records"), eq("id"), eq(1L), anyMap(), any())).thenReturn(1);
        when(repository.adjustNumbersById(eq("records"), eq("id"), eq(1L), anyMap(), anyMap(), anyMap(), any())).thenReturn(1);
        when(repository.selectByIdForUpdate(eq("records"), eq("id"), eq(1L), any())).thenReturn(Map.of("id", 1L));
        when(crypto.encrypt("before", "AES")).thenReturn("encrypted-before");
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, crypto,
                new LowcodeEncryptConfigParser(mapper), scopes, mock(BusinessDocumentConfigService.class),
                null, aggregates, mock(StoredFormulaRuntime.class), mock(VirtualFormulaRuntime.class), sources);
    }

    @Test
    void insertMapsOnlyPublishedFieldsBeforeWriteAndPreservesReadbackRefresh() {
        when(repository.insertReturningKey(eq("records"), anyMap(), eq("id"), eq(true))).thenReturn(1L);
        Map<String, Object> result = service.insertCommandRecord("records", new LinkedHashMap<>(Map.of("quantity", new BigDecimal("2.50"))));
        verify(repository).insertReturningKey("records", Map.of("qty_value", new BigDecimal("2.50")), "id", true);
        assertEquals(1L, result.get("id"));
        verify(aggregates).refreshRuntimeAfterChildInsert(RuntimeCrudConfigAdapter.fromDesign(config), result);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void updateCombinesDataScopeEncryptedExpectedAndNullCondition() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("secret", "before");
        expected.put("state", null);
        service.updateCommandFields("records", 1L, new LinkedHashMap<>(Map.of("quantity", BigDecimal.TEN)), expected);
        ArgumentCaptor<DynamicCrudRepository.SqlCondition> capture = ArgumentCaptor.forClass(DynamicCrudRepository.SqlCondition.class);
        verify(repository).updateById(eq("records"), eq("id"), eq(1L), eq(Map.of("qty_value", BigDecimal.TEN)), capture.capture());
        assertEquals("(owner_id = :owner) AND (secret_value = :commandExpected0 AND state_value IS NULL)", capture.getValue().sql());
        assertEquals(Map.of("owner", 7L, "commandExpected0", "encrypted-before"), capture.getValue().params());
        verify(aggregates).refreshRuntimeAfterChildUpdate(eq(RuntimeCrudConfigAdapter.fromDesign(config)), anyMap(), anyMap());
    }

    @Test
    void numericAdjustmentMapsDeltasBoundsAndExpectedBeforeAtomicMutation() {
        service.adjustCommandNumbers("records", 1L, Map.of("quantity", new BigDecimal("3")),
                Map.of("quantity", BigDecimal.ZERO), Map.of("quantity", BigDecimal.TEN), Map.of("state", "OPEN"));
        ArgumentCaptor<DynamicCrudRepository.SqlCondition> capture = ArgumentCaptor.forClass(DynamicCrudRepository.SqlCondition.class);
        verify(repository).adjustNumbersById(eq("records"), eq("id"), eq(1L),
                eq(Map.of("qty_value", new BigDecimal("3"))), eq(Map.of("qty_value", BigDecimal.ZERO)),
                eq(Map.of("qty_value", BigDecimal.TEN)), capture.capture());
        assertEquals("(owner_id = :owner) AND (state_value = :commandExpected0)", capture.getValue().sql());
        assertEquals("OPEN", capture.getValue().params().get("commandExpected0"));
        var order = inOrder(repository, aggregates);
        order.verify(repository).selectById("records", "id", 1L, scope);
        order.verify(repository).adjustNumbersById(anyString(), anyString(), any(), anyMap(), anyMap(), anyMap(), any());
        order.verify(repository).selectById("records", "id", 1L, null);
        order.verify(aggregates).refreshRuntimeAfterChildUpdate(eq(RuntimeCrudConfigAdapter.fromDesign(config)), anyMap(), anyMap());
    }

    @Test
    void lockedGuardCompilesAllAllowedNumericOperatorsAndSkipsEmptyConstraints() {
        List<Map<String, Object>> constraints = new java.util.ArrayList<>();
        constraints.add(null);
        constraints.add(Map.of());
        for (String op : List.of("gt", "gte", "lt", "lte", "eq", "neq")) {
            constraints.add(Map.of("field", "quantity", "operator", op, "value", "2.50"));
        }
        service.assertCommandRecord("records", 1L, Map.of("state", "OPEN"), constraints);
        ArgumentCaptor<DynamicCrudRepository.SqlCondition> capture = ArgumentCaptor.forClass(DynamicCrudRepository.SqlCondition.class);
        verify(repository).selectByIdForUpdate(eq("records"), eq("id"), eq(1L), capture.capture());
        assertEquals("((owner_id = :owner) AND (state_value = :commandExpected0)) AND (qty_value > :commandNumeric0 AND qty_value >= :commandNumeric1 AND qty_value < :commandNumeric2 AND qty_value <= :commandNumeric3 AND qty_value = :commandNumeric4 AND qty_value <> :commandNumeric5)", capture.getValue().sql());
        assertEquals(new BigDecimal("2.50"), capture.getValue().params().get("commandNumeric5"));
    }

    @Test
    void immutableUnknownDisabledAndDuplicateFieldsStopBeforePersistence() {
        for (String field : List.of("tenantId", "unknown", "disabled", "systemOnly")) {
            assertThrows(BusinessException.class, () -> service.insertCommandRecord("records", new LinkedHashMap<>(Map.of(field, "x"))));
        }
        Map<String, Object> aliases = new LinkedHashMap<>();
        aliases.put("quantity", BigDecimal.ONE);
        aliases.put("qty_value", BigDecimal.TEN);
        assertThrows(BusinessException.class, () -> service.updateCommandFields("records", 1L, aliases, null));
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(aggregates);
    }

    @Test
    void nullFirstAliasDuplicateStopsBeforePersistence() {
        Map<String, Object> aliases = new LinkedHashMap<>();
        aliases.put("quantity", null);
        aliases.put("qty_value", BigDecimal.TEN);
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.updateCommandFields("records", 1L, aliases, null));
        assertTrue(failure.getMessage().contains("事务命令字段重复映射"));
        verify(repository, never()).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(aggregates);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void invalidNumericRulesAndParameterCollisionStopBeforeRowLock() {
        assertThrows(BusinessException.class, () -> service.assertCommandRecord("records", 1L, null,
                List.of(Map.of("field", "quantity", "operator", "sql", "value", 1))));
        assertThrows(BusinessException.class, () -> service.assertCommandRecord("records", 1L, null,
                List.of(Map.of("field", "quantity", "operator", "gte", "value", "not-number"))));
        when(scopes.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(
                new DynamicCrudRepository.SqlCondition("owner_id=:commandExpected0", Map.of("commandExpected0", 7L)));
        assertThrows(BusinessException.class, () -> service.assertCommandRecord("records", 1L, Map.of("state", "OPEN")));
        verify(repository, never()).selectByIdForUpdate(anyString(), anyString(), any(), any());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void zeroAffectedRowsRejectMutationAndRestoreScopeWithoutAggregateRefresh() {
        when(repository.updateById(anyString(), anyString(), any(), anyMap(), any())).thenReturn(0);
        when(repository.adjustNumbersById(anyString(), anyString(), any(), anyMap(), anyMap(), anyMap(), any())).thenReturn(0);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertThrows(BusinessException.class, () -> service.updateCommandFields("records", 1L,
                    new LinkedHashMap<>(Map.of("quantity", BigDecimal.ONE)), Map.of()));
            assertThrows(BusinessException.class, () -> service.adjustCommandNumbers("records", 1L,
                    Map.of("quantity", BigDecimal.ONE), null, null, Map.of()));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verifyNoInteractions(aggregates);
    }

    @Test
    void publicationAndReadonlyAdmissionStopBeforeCommandMetadata() {
        config.setPublishStatus("DRAFT");
        assertThrows(BusinessException.class, () -> service.assertCommandRecord("records", 1L, Map.of()));
        config.setPublishStatus("PUBLISHED");
        runtime.setReadonly(true);
        assertThrows(BusinessException.class, () -> service.updateCommandFields("records", 1L, Map.of("quantity", BigDecimal.ONE), Map.of()));
        verifyNoInteractions(repository, aggregates);
    }
}
