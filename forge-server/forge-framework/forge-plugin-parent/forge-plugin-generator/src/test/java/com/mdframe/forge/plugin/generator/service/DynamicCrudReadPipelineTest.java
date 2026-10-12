package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public reads prove stage ordering and the retained fail-closed/degraded boundaries. */
class DynamicCrudReadPipelineTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService dataScope = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final PersistentCryptoService crypto = mock(PersistentCryptoService.class);
    private final VirtualFormulaRuntime virtual = mock(VirtualFormulaRuntime.class);
    private final DictValueProvider dictionaries = mock(DictValueProvider.class);
    private final DesensitizeStrategyFactory masks = mock(DesensitizeStrategyFactory.class);
    private final DesensitizeStrategy mask = mock(DesensitizeStrategy.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope =
            new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("suite_records");
        config.setObjectCode("records");
        config.setTableName("records");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setModelSchema("""
                {"fields":[
                  {"field":"secret","columnName":"secret_value"},
                  {"field":"amount","columnName":"cash_amount","dataType":"bigint","componentType":"money","precision":2},
                  {"field":"tags","columnName":"tag_values","componentType":"checkbox"}
                ]}
                """);
        config.setEncryptConfig("{\"secret\":{\"algorithm\":\"AES\"}}");
        config.setTransConfig("{\"state\":{\"dictType\":\"old\"}}");
        config.setColumnsSchema("[{\"field\":\"state\",\"dictType\":\"status\"}]");
        config.setDesensitizeConfig("{\"secret\":{\"type\":\"CUSTOM\"}}");
        when(configs.getByConfigKey("suite_records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("records"));
        when(dataScope.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("records");
            return expected.equals(actual);
        }), eq("records"), isNull())).thenReturn(scope);
        when(repository.selectById("records", "id", 1L, scope))
                .thenReturn(Map.of("id", 1L, "secret_value", "cipher", "cash_amount", 1230L, "tag_values", "[\"a\",\"b\"]"));
        when(crypto.decrypt("cipher", "AES")).thenReturn("clear");
        when(masks.getStrategy(DesensitizeType.CUSTOM)).thenReturn(mask);
        when(mask.desensitize("clear")).thenReturn("***");
        when(dictionaries.getLabel("status", "active")).thenReturn("Active");
        doAnswer(invocation -> {
            List<Map<String, Object>> rows = invocation.getArgument(0);
            Map<String, Object> row = rows.get(0);
            assertEquals("clear", row.get("secret"));
            assertEquals(new BigDecimal("12.30"), row.get("amount"));
            assertEquals(List.of("a", "b"), row.get("tags"));
            row.put("state", "active");
            return null;
        }).when(virtual).calculate(anyList(), any(), any());
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, dictionaries, masks, crypto,
                new LowcodeEncryptConfigParser(mapper), dataScope, null, null, null, null, virtual, sources);
    }

    @Test
    void scopedReadAliasesThenDecryptsConvertsCalculatesTranslatesAndMasks() {
        Map<String, Object> result = service.selectById("suite_records", 1L);
        assertEquals("***", result.get("secret"));
        assertEquals("Active", result.get("stateName"));
        assertEquals(1230L, result.get("cashAmount"));
        var order = inOrder(repository, crypto, virtual, dictionaries, mask);
        order.verify(repository).selectById("records", "id", 1L, scope);
        order.verify(crypto).decrypt("cipher", "AES");
        order.verify(virtual).calculate(anyList(), any(), any());
        order.verify(dictionaries).getLabel("status", "active");
        order.verify(mask).desensitize("clear");
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void decryptFailureStopsFormulaTranslationAndMaskAndReleasesScope() {
        when(crypto.decrypt("cipher", "AES")).thenThrow(new IllegalStateException("invalid ciphertext"));
        BusinessException error = assertThrows(BusinessException.class, () -> service.selectById("suite_records", 1L));
        assertTrue(error.getMessage().contains("低代码加密字段读取失败"));
        verifyNoInteractions(virtual, dictionaries, mask);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void translationFailureKeepsOriginalValueAndStillMasks() {
        when(dictionaries.getLabel("status", "active")).thenThrow(new IllegalStateException("dictionary unavailable"));
        Map<String, Object> result = service.selectById("suite_records", 1L);
        assertEquals("active", result.get("state"));
        assertFalse(result.containsKey("stateName"));
        assertEquals("***", result.get("secret"));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void inaccessibleRecordDoesNotRunAnyProjection() {
        when(repository.selectById("records", "id", 1L, scope)).thenReturn(null);
        assertNull(service.selectById("suite_records", 1L));
        verifyNoInteractions(crypto, virtual, dictionaries, mask);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void batchLabelsDeduplicateContinueAfterProviderFailureAndIgnoreUnknownTypes() {
        config.setModelSchema(null);
        config.setEncryptConfig(null);
        config.setColumnsSchema(null);
        config.setDesensitizeConfig(null);
        config.setTransConfig("""
                {"organization":{"type":"orgName"},"owner":{"type":"userName"},
                 "region":{"type":"regionName"},"attachment":{"type":"imageUpload"},
                 "unsupported":{"type":"unknown"}}
                """);
        Object unsupported = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("unknown translation must not evaluate this value");
            }
        };
        when(repository.selectListByColumnIn("records", "id", List.of(1L, 2L), scope)).thenReturn(List.of(
                Map.of("id", 1L, "organization", "o", "owner", "u", "region", "r", "attachment", "f", "unsupported", unsupported),
                Map.of("id", 2L, "organization", "o", "owner", "u", "region", "r", "attachment", "f")));
        when(dictionaries.batchGetOrgNames(List.of("o"))).thenThrow(new IllegalStateException("organization lookup unavailable"));
        when(dictionaries.batchGetUserNames(List.of("u"))).thenReturn(Map.of("u", "User"));
        when(dictionaries.batchGetRegionNames(List.of("r"))).thenReturn(Map.of("r", "Region"));
        when(dictionaries.batchGetFileNames(List.of("f"))).thenReturn(Map.of("f", "Image"));
        Map<Object, Map<String, Object>> result = service.selectByIds("suite_records", List.of(1L, 2L));
        assertEquals(2, result.size());
        for (Map<String, Object> row : result.values()) {
            assertFalse(row.containsKey("organizationName"));
            assertEquals("User", row.get("ownerName"));
            assertEquals("Region", row.get("regionName"));
            assertEquals("Image", row.get("attachmentName"));
        }
        assertSame(unsupported, result.get(1L).get("unsupported"));
        var order = inOrder(dictionaries);
        order.verify(dictionaries).batchGetOrgNames(List.of("o"));
        order.verify(dictionaries).batchGetUserNames(List.of("u"));
        order.verify(dictionaries).batchGetRegionNames(List.of("r"));
        order.verify(dictionaries).batchGetFileNames(List.of("f"));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }
}
