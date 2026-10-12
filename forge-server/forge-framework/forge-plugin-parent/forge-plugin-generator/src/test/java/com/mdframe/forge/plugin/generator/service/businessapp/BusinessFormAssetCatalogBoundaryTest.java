package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BusinessFormAssetCatalogBoundaryTest {
    private BusinessFlowService service;
    private BusinessBindingMapper bindings;
    private BusinessObjectMapper objects;
    private BusinessCodeFormProviderRegistry providers;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        bindings = mock(BusinessBindingMapper.class);
        objects = mock(BusinessObjectMapper.class);
        providers = mock(BusinessCodeFormProviderRegistry.class);
        service = new BusinessFlowService(bindings, null, mock(AiCrudConfigMapper.class), objects,
                null, null, null, null, null, providers, null, null, null);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void blankObjectHasAnEmptyCatalogWithoutReadingOtherSources() {
        Map<String, Object> result = service.getFormAssets(" ", false, 9L);
        assertEquals(List.of(), result.get("formAssets"));
        assertEquals(List.of(), result.get("providerCatalog"));
        assertEquals(List.of(), result.get("warnings"));
        verifyNoInteractions(objects, providers, bindings);
    }

    @Test
    void inaccessibleApplicationDoesNotFallBackToAnotherObjectsForms() {
        BusinessApplicationService applications = mock(BusinessApplicationService.class);
        ReflectionTestUtils.setField(service, "businessApplicationService", applications);
        when(applications.detail(9L)).thenThrow(new IllegalStateException("denied"));
        Map<String, Object> result = service.getFormAssets("invoice", false, 9L);
        assertEquals(List.of(), result.get("formAssets"));
        assertFalse(((List<?>) result.get("warnings")).isEmpty());
        verifyNoInteractions(objects, bindings);
    }

    @Test
    void configuredRemovalAndHiddenFieldsRespectProviderDataAndDisplayOverrides() {
        Map<String, Object> amount = Map.of("field", "amount", "label", "Amount");
        Map<String, Object> secret = Map.of("field", "secret", "label", "Secret");
        Map<String, Object> provider = Map.of("formKey", "kept", "providerKey", "invoice",
                "fields", List.of(amount, secret));
        when(providers.listAssets("invoice", false)).thenReturn(List.of(provider,
                Map.of("formKey", "removed", "fields", List.of(amount))));
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setBindingConfig(JSON.toJSONString(Map.of("options", Map.of("codeAppMetadata", Map.of(
                "removedFormAssetKeys", List.of("form:removed"), "fields", List.of(
                        Map.of("field", "amount", "label", "Configured"),
                        Map.of("field", "secret", "visible", false)))))));
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "invoice", "FLOW")).thenReturn(binding);
        Map<String, Object> result = service.getFormAssets("invoice");
        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.get("formAssets");
        assertEquals(1, assets.size());
        List<Map<String, Object>> fields = (List<Map<String, Object>>) assets.get(0).get("fields");
        assertEquals(1, fields.size());
        assertEquals("amount", fields.get(0).get("field"));
        assertEquals("Configured", fields.get(0).get("label"));
        assertEquals("Amount", amount.get("label"));
        assertTrue(((List<?>) provider.get("fields")).contains(secret));
    }
}
