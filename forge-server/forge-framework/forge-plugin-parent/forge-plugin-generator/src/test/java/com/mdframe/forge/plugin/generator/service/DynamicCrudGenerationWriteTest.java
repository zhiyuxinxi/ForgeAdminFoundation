package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.lowcode.api.code.RuntimeDocumentNumber;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.businessapp.CodeRuleService;
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

/** Public creation entries preserve automatic numbering rules and write failures. */
class DynamicCrudGenerationWriteTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final CodeRuleService codes = mock(CodeRuleService.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private DynamicCrudService service;

    @BeforeEach
    void setUp() throws Exception {
        config.setConfigKey("records");
        config.setTableName("records");
        config.setObjectCode("RECORD");
        config.setDomainCode("SUITE");
        config.setTenantId(7L);
        config.setMode("CONFIG");
        config.setPublishStatus("PUBLISHED");
        config.setEditSchema("[{\"field\":\"recordCode\"},{\"field\":\"title\"}]");
        model(Map.of());
        when(configs.getByConfigKey("records")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("records"));
        when(repository.getColumnMapping("records"))
                .thenReturn(Map.of("recordCode", "record_code", "record_code", "record_code", "title", "title"));
        when(repository.getTableColumns("records")).thenReturn(Set.of("record_code", "title"));
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), null, documents, codes,
                mock(StoredAggregateRefreshService.class), mock(StoredFormulaRuntime.class), null, sources);
    }

    @Test
    void explicitModelRuleWinsAndReceivesTrustedConfigWithinWriteScope() throws Exception {
        model(Map.of("basicProps", Map.of("generation", rule("MODEL"))));
        config.setEditSchema("[{\"field\":\"recordCode\",\"props\":{\"generation\":{\"enabled\":true,\"ruleCode\":\"EDIT\"}}}]");
        when(codes.generate(eq("MODEL"), anyMap())).thenAnswer(call -> {
            assertEquals("records", LowcodeRuntimeDataSourceContextHolder.get().getTableName());
            Map<?, ?> context = call.getArgument(1);
            Map<?, ?> fields = (Map<?, ?>) context.get("fields");
            assertEquals("RECORD", fields.get("objectCode"));
            assertEquals("SUITE", fields.get("suiteCode"));
            assertEquals("Record", fields.get("fieldName"));
            assertEquals("sample", fields.get("title"));
            assertNull(context.get("tenantId"));
            return "R-1";
        });
        Map<String, Object> data = payload();
        service.insert("records", data);
        assertEquals("R-1", data.get("recordCode"));
        assertEquals("R-1", data.get("record_code"));
        verify(repository).insertReturningKey("records", Map.of("record_code", "R-1", "title", "sample"), "id", true);
        verify(codes, never()).generate(eq("EDIT"), anyMap());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void nestedEditRuleHonorsOverwritePolicyForInternalCreation() {
        config.setEditSchema("{\"components\":[{\"items\":[{\"field\":\"recordCode\",\"generation\":{\"enabled\":\"yes\",\"generatorCode\":\"EDIT\",\"type\":\"AUTO_CODE\",\"fillPolicy\":\"ALWAYS\"}}]}]}");
        when(codes.generate(eq("EDIT"), anyMap())).thenReturn("E-1");
        Map<String, Object> data = payload();
        data.put("recordCode", "manual");
        service.insertInternal("records", data);
        assertEquals("E-1", data.get("recordCode"));
        verify(repository).insertReturningKey("records", Map.of("record_code", "E-1", "title", "sample"), "id", true);
    }

    @Test
    void emptyOnlyKeepsExistingAliasAndConfiguredRuleSuppressesDocumentNumber() throws Exception {
        model(Map.of("advancedProps", Map.of("generation", rule("EXPLICIT"))));
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        when(documents.selectEnabledByConfigKey(7L, "records")).thenReturn(document);
        when(documents.resolveRuntimeDocumentNoField(document, RuntimeCrudConfigAdapter.fromDesign(config))).thenReturn("record_code");
        Map<String, Object> data = payload();
        data.put("record_code", "MANUAL");
        service.insertCommandRecord("records", data);
        verifyNoInteractions(codes);
        verify(documents, never()).generateRuntimeDocumentNo(any(), anyMap());
        assertEquals("MANUAL", data.get("record_code"));
    }

    @Test
    void disabledExplicitRulePreventsConventionalFallback() throws Exception {
        model(Map.of("basicProps", Map.of("generation", Map.of("enabled", false, "ruleCode", "DISABLED"))));
        service.insert("records", payload());
        verifyNoInteractions(codes);
        verify(repository).insertReturningKey("records", Map.of("title", "sample"), "id", true);
    }

    @Test
    void conventionalRuleBusinessFailureFallsBackToDocumentNumber() {
        BusinessException unavailable = new BusinessException("rule unavailable");
        when(codes.generate(eq("record_code"), anyMap())).thenThrow(unavailable);
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        when(documents.selectEnabledByConfigKey(7L, "records")).thenReturn(document);
        when(documents.resolveRuntimeDocumentNoField(document, RuntimeCrudConfigAdapter.fromDesign(config))).thenReturn("recordCode");
        when(documents.generateRuntimeDocumentNo(eq(number(document, "recordCode")), anyMap())).thenReturn("D-1");
        Map<String, Object> data = payload();
        service.insert("records", data);
        assertEquals("D-1", data.get("recordCode"));
        verify(repository).insertReturningKey("records", Map.of("record_code", "D-1", "title", "sample"), "id", true);
    }

    @Test
    void internalCreationFallsBackToObjectBindingWithTrustedTenantAndSamePayload() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        when(documents.selectEnabledByObjectCode(7L, "RECORD")).thenReturn(document);
        when(documents.resolveRuntimeDocumentNoField(document, RuntimeCrudConfigAdapter.fromDesign(config))).thenReturn("recordCode");
        Map<String, Object> data = payload();
        data.put("tenantId", 99L);
        when(documents.generateRuntimeDocumentNo(eq(number(document, "recordCode")), same(data))).thenAnswer(call -> {
            assertEquals("records", LowcodeRuntimeDataSourceContextHolder.get().getTableName());
            assertEquals("sample", data.get("title"));
            return "D-TENANT-7";
        });
        service.insertInternal("records", data);
        var order = inOrder(documents, repository);
        order.verify(documents).selectEnabledByConfigKey(7L, "records");
        order.verify(documents).selectEnabledByObjectCode(7L, "RECORD");
        order.verify(documents).generateRuntimeDocumentNo(number(document, "recordCode"), data);
        order.verify(repository).insertReturningKey("records",
                Map.of("record_code", "D-TENANT-7", "title", "sample"), "id", true);
        verify(documents, never()).selectEnabledByObjectCode(anyString());
        assertEquals("D-TENANT-7", data.get("recordCode"));
        assertEquals("D-TENANT-7", data.get("record_code"));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void explicitFailureAbortsWriteAndRestoresOuterDataSourceScope() throws Exception {
        model(Map.of("basicProps", Map.of("generation", rule("EXPLICIT"))));
        BusinessException failure = new BusinessException("required rule failed");
        when(codes.generate(eq("EXPLICIT"), anyMap())).thenThrow(failure);
        LowcodeRuntimeDataSourceContext outer = LowcodeRuntimeDataSourceContext.master("outer");
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(outer)) {
            assertSame(failure, assertThrows(BusinessException.class, () -> service.insert("records", payload())));
            assertSame(outer, LowcodeRuntimeDataSourceContextHolder.get());
        }
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        verify(documents, never()).generateRuntimeDocumentNo(any(), anyMap());
    }

    @Test
    void conventionalInfrastructureFailureIsNotSwallowed() {
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        when(codes.generate(eq("record_code"), anyMap())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.insertInternal("records", payload())));
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    private RuntimeDocumentNumber number(AiBusinessDocumentConfig document, String field) {
        return new RuntimeDocumentNumber(document.getTenantId(), document.getSuiteCode(), document.getObjectCode(),
                document.getDocumentEnabled(), document.getDocumentNoRule(), field);
    }

    private void model(Map<String, Object> options) throws Exception {
        Map<String, Object> field = new LinkedHashMap<>(Map.of("field", "recordCode", "columnName", "record_code", "label", "Record"));
        field.putAll(options);
        config.setModelSchema(mapper.writeValueAsString(Map.of("fields", List.of(field, Map.of("field", "title", "columnName", "title")))));
    }

    private Map<String, Object> rule(String code) {
        return Map.of("enabled", true, "ruleCode", code);
    }

    private Map<String, Object> payload() {
        return new LinkedHashMap<>(Map.of("title", "sample"));
    }
}
