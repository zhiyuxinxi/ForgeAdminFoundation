package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public binding API contract; the same cases run before and after responsibility extraction. */
class BusinessFlowBindingConfigurationTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private BusinessFlowService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        service = new BusinessFlowService(bindings, mock(BusinessFlowInstanceLinkMapper.class), configs,
                mock(BusinessObjectMapper.class), documents, mock(BusinessDocumentRuntimeService.class),
                mock(DynamicCrudService.class), mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class), mock(BusinessCodeFormProviderRegistry.class),
                mock(ApplicationEventPublisher.class), mock(ObjectProvider.class), mock(ObjectProvider.class));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void savesNormalizedDtoWithTrustedTenantAndDefaultsBeforeDocumentSync() {
        AiCrudConfig runtime = new AiCrudConfig();
        runtime.setObjectCode("generic");
        runtime.setRuntimeTableName("runtime_table");
        runtime.setPrimaryKeyColumn("record_id");
        when(configs.selectPublishedByObjectCodeOrConfigKey(7L, "generic")).thenReturn(runtime);
        BusinessFlowBindingDTO dto = new BusinessFlowBindingDTO();
        dto.setFlowModelKey("  model  ");
        dto.setFlowModelName("  Model  ");
        dto.setStartMode(" manual_and_trigger ");
        service.saveFlowBinding("generic", dto);
        var capture = ArgumentCaptor.forClass(AiBusinessBinding.class);
        var order = inOrder(bindings, documents);
        order.verify(bindings).insert(capture.capture());
        order.verify(documents).syncDefaultFlowKeyByObjectCode(7L, "generic", "model");
        AiBusinessBinding saved = capture.getValue();
        assertEquals(7L, saved.getTenantId());
        assertEquals("generic", saved.getTargetCode());
        assertEquals("Model", saved.getBindingName());
        assertEquals(1, saved.getStatus());
        JSONObject config = JSON.parseObject(saved.getBindingConfig());
        assertEquals("BOTH", config.getString("startMode"));
        assertTrue(config.getJSONArray("variableMapping").isEmpty());
        assertTrue(config.getJSONArray("nodeForms").isEmpty());
        assertTrue(config.getJSONObject("options").isEmpty());
        JSONObject schema = config.getJSONObject("businessBinding");
        assertEquals("runtime_table", schema.getString("tableName"));
        assertEquals("record_id", schema.getString("primaryKeyField"));
        assertEquals("tenant_id", schema.getString("tenantField"));
    }

    @Test
    void legacyJsonAliasesNormalizeAndUpdateExistingWithoutReplacingIdentity() {
        AiBusinessBinding existing = binding("{}");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW")).thenReturn(existing);
        JSONObject input = JSON.parseObject("""
                {"processDefinitionKey":"  model  ","startMode":"automatic",
                 "variableMapping":[null,{"field":" amount ","variable":" total ","label":" Amount "},
                                    {"field":"missingVariable"}],
                 "businessBinding":{"mode":" adapter ","tableName":" custom ","tenantField":" company_id "},
                 "conditionFlows":[{"key":"condition"}],"options":{"flag":true}}
                """);
        service.saveFlowBinding("generic", input);
        verify(bindings).updateById(existing);
        verify(bindings, never()).insert(any(AiBusinessBinding.class));
        assertEquals(91L, existing.getId());
        assertEquals("model 流程", existing.getBindingName());
        JSONObject saved = JSON.parseObject(existing.getBindingConfig());
        assertEquals("TRIGGER", saved.getString("startMode"));
        assertEquals(1, saved.getJSONArray("variableMapping").size());
        JSONObject variable = saved.getJSONArray("variableMapping").getJSONObject(0);
        assertEquals("amount", variable.getString("formField"));
        assertEquals("total", variable.getString("flowVariable"));
        assertFalse(variable.containsKey("field"));
        assertEquals("ADAPTER", saved.getJSONObject("businessBinding").getString("mode"));
        assertEquals("custom", saved.getJSONObject("businessBinding").getString("tableName"));
        assertEquals("company_id", saved.getJSONObject("businessBinding").getString("tenantField"));
        assertEquals("id", saved.getJSONObject("businessBinding").getString("primaryKeyField"));
        assertEquals("condition", saved.getJSONArray("conditionFlows").getJSONObject(0).getString("key"));
        assertTrue(saved.getJSONObject("options").getBooleanValue("flag"));
    }

    @Test
    void readsBindingSummaryAndLegacyProjectionWithStoredValuesTakingPrecedence() {
        AiBusinessBinding existing = binding("""
                {"flowModelKey":"chosen","flowModelName":"Chosen","startMode":"both",
                 "variableMapping":[{"field":"amount","variable":"total"}],
                 "businessBinding":{"mode":"BUSINESS_TABLE","tableName":"explicit"},
                 "options":{"flag":true}}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW")).thenReturn(existing);
        var view = service.getFlowBinding("generic");
        assertEquals("chosen", view.getFlowModelKey());
        assertEquals("BOTH", view.getStartMode());
        assertTrue(view.getComplete());
        assertTrue(view.getGaps().isEmpty());
        assertEquals("AI_BUSINESS_BINDING", view.getCompatibilitySource());
        assertEquals(1, view.getMainFlowSummary().get("variableMappingCount"));
        assertEquals("explicit", view.getBusinessBinding().getTableName());
        assertEquals("tenant_id", view.getBusinessBinding().getTenantField());
        JSONObject legacy = service.getFlowBindingLegacy("generic");
        assertEquals(91L, legacy.getLong("bindingId"));
        assertEquals("Chosen", legacy.getString("bindingName"));
        assertEquals("chosen", legacy.getJSONObject("config").getString("flowModelKey"));
        assertEquals("total", legacy.getJSONObject("config").getJSONArray("variableMapping")
                .getJSONObject(0).getString("flowVariable"));
        verify(bindings, never()).updateById(any(AiBusinessBinding.class));
    }

    @Test
    void missingAndMalformedConfigurationKeepLegacyFallbackAndGapSemantics() {
        assertNull(service.getFlowBinding("generic"));
        AiBusinessBinding existing = binding("{broken");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW")).thenReturn(existing);
        var view = service.getFlowBinding("generic");
        assertEquals("fallback-model", view.getFlowModelKey());
        assertEquals("MANUAL", view.getStartMode());
        assertFalse(view.getComplete());
        assertEquals(java.util.List.of("变量映射缺失"), view.getGaps());
    }

    @Test
    void documentDefaultFlowRemainsExplicitlyIncompleteCompatibilityView() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setObjectCode("generic");
        document.setDefaultFlowKey("legacy-model");
        document.setStatusField("state");
        document.setOwnerField("creator");
        when(documents.selectEnabledByObjectCode(7L, "generic")).thenReturn(document);
        var view = service.getFlowBinding("generic");
        assertEquals("legacy-model", view.getFlowModelKey());
        assertEquals("DOCUMENT_DEFAULT_FLOW", view.getCompatibilitySource());
        assertFalse(view.getComplete());
        assertEquals("state", view.getBusinessBinding().getStatusField());
        assertEquals("creator", view.getBusinessBinding().getOwnerField());
        assertEquals(0, view.getMainFlowSummary().get("variableMappingCount"));
        assertEquals(java.util.List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"), view.getGaps());
    }

    @Test
    void invalidInputAndPersistenceFailureNeverSyncDocumentOrManufactureSuccess() {
        assertThrows(BusinessException.class, () -> service.saveFlowBinding("generic", (BusinessFlowBindingDTO) null));
        assertThrows(BusinessException.class, () -> service.saveFlowBinding("generic", new BusinessFlowBindingDTO()));
        verify(bindings, never()).insert(any(AiBusinessBinding.class));
        BusinessFlowBindingDTO dto = new BusinessFlowBindingDTO();
        dto.setFlowModelKey("model");
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        doThrow(failure).when(bindings).insert(any(AiBusinessBinding.class));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.saveFlowBinding("generic", dto)));
        verify(documents, never()).syncDefaultFlowKeyByObjectCode(any(), any(), any());
    }

    private AiBusinessBinding binding(String config) {
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setId(91L);
        binding.setTenantId(7L);
        binding.setBindingKey("fallback-model");
        binding.setBindingName("Fallback name");
        binding.setBindingConfig(config);
        binding.setStatus(1);
        return binding;
    }
}
