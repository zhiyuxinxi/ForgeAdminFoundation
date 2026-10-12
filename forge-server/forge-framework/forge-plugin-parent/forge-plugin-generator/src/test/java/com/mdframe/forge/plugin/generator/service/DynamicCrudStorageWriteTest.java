package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public write calls retain the order and failure policies of their value conversion steps. */
class DynamicCrudStorageWriteTest {
    private final ObjectMapper mapper = spy(new ObjectMapper());
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService permissions = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final PersistentCryptoService crypto = mock(PersistentCryptoService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("records");
        config.setObjectCode("RECORD");
        config.setTableName("records");
        config.setMode("CONFIG");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("""
                {"fields":[{"field":"amount","columnName":"amount","dataType":"bigint","businessFieldType":"MONEY","precision":2},
                 {"field":"tags","columnName":"tags","dataType":"text","componentType":"checkbox"},
                 {"field":"phoneNumber","columnName":"phone_value"},{"field":"title","columnName":"title"}]}
                """);
        config.setEditSchema("[{\"field\":\"amount\"},{\"field\":\"tags\"},{\"field\":\"phoneNumber\"},{\"field\":\"title\"}]");
        config.setEncryptConfig("{\"phoneNumber\":{\"algorithm\":\"SM4\",\"columnName\":\"phone_value\"},\"tags\":{\"algorithm\":\"SM4\"}}");
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
                .thenReturn(Map.of("amount", "amount", "tags", "tags", "phoneNumber", "phone_value", "title", "title"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("amount", "tags", "phone_value", "title"));
        when(repository.selectById("records", "id", 4L, scope)).thenReturn(Map.of("id", 4L, "phone_value", "old"));
        when(repository.updateById(eq("records"), eq("id"), eq(4L), anyMap(), same(scope))).thenReturn(1);
        when(crypto.encrypt(anyString(), eq("SM4"))).thenAnswer(call -> "cipher:" + call.getArgument(0));
        service = new DynamicCrudService(repository, configs, mapper, null, null, crypto,
                new LowcodeEncryptConfigParser(mapper), permissions, mock(BusinessDocumentConfigService.class), null,
                mock(StoredAggregateRefreshService.class), mock(StoredFormulaRuntime.class), null, sources);
    }

    @Test
    void createConvertsMoneyAndSerializesBeforeEncryptingMappedColumns() {
        Map<String, Object> data = new LinkedHashMap<>(Map.of("amount", "12.30", "tags", List.of("a", "b"), "phoneNumber", "123"));
        service.insert("records", data);
        verify(repository).insertReturningKey("records", Map.of("amount", 1230L, "tags", "cipher:[\"a\",\"b\"]", "phone_value", "cipher:123"), "id", true);
        verify(crypto).encrypt("[\"a\",\"b\"]", "SM4");
        verify(crypto).encrypt("123", "SM4");
        assertEquals("12.30", data.get("amount"));
        assertEquals(List.of("a", "b"), data.get("tags"));
    }

    @Test
    void invalidMoneyStopsBeforeEncryptionAndPersistence() {
        for (String value : List.of("12.345", "invalid")) {
            assertThrows(BusinessException.class, () -> service.insert("records", new LinkedHashMap<>(Map.of("amount", value, "phoneNumber", "123"))));
        }
        verifyNoInteractions(crypto);
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void updateDropsMaskedColumnsAndRejectsAnAllMaskedUpdate() {
        config.setDesensitizeConfig("{\"phoneNumber\":{\"strategy\":\"PHONE\"}}");
        service.updateById("records", new LinkedHashMap<>(Map.of("id", 4L, "phoneNumber", "12***89", "title", "updated")));
        verify(repository).updateById("records", "id", 4L, Map.of("title", "updated"), scope);
        assertThrows(BusinessException.class, () -> service.updateById("records", new LinkedHashMap<>(Map.of("id", 4L, "phoneNumber", "12***89"))));
        verify(repository, times(1)).updateById(anyString(), anyString(), any(), anyMap(), any());
        verifyNoInteractions(crypto);
    }

    @Test
    void internalCreateKeepsItsOriginalMoneyPolicyButStillSerializesAndEncrypts() {
        service.insertInternal("records", new LinkedHashMap<>(Map.of("amount", "12.30", "tags", List.of("a"))));
        verify(repository).insertReturningKey("records", Map.of("amount", "12.30", "tags", "cipher:[\"a\"]"), "id", true);
    }

    @Test
    void encryptionFailureKeepsCauseStopsWriteAndRestoresCallerScope() {
        IllegalStateException failure = new IllegalStateException("key unavailable");
        when(crypto.encrypt("123", "SM4")).thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            BusinessException wrapped = assertThrows(BusinessException.class,
                    () -> service.insert("records", new LinkedHashMap<>(Map.of("phoneNumber", "123"))));
            assertSame(failure, wrapped.getCause());
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
    }

    @Test
    void structuredSerializationFailureStopsBeforeEncryptionAndPersistence() throws Exception {
        List<String> tags = List.of("a");
        doThrow(new JsonProcessingException("serialization unavailable") { }).when(mapper).writeValueAsString(tags);
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.insert("records", new LinkedHashMap<>(Map.of("tags", tags))));
        assertTrue(failure.getMessage().contains("tags"));
        verifyNoInteractions(crypto);
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }
}
