package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeAppMetadataStoreBoundaryTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
    private final BusinessApplicationService applications = mock(BusinessApplicationService.class);
    private BusinessFlowService service;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(7L);
        service = new BusinessFlowService(bindings, mock(BusinessFlowInstanceLinkMapper.class), configs, objects,
                mock(BusinessDocumentConfigService.class), mock(BusinessDocumentRuntimeService.class),
                mock(DynamicCrudService.class), mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class), mock(BusinessCodeFormProviderRegistry.class),
                mock(ApplicationEventPublisher.class), mock(ObjectProvider.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "businessApplicationService", applications);
    }

    @AfterEach
    void cleanTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void enabledFlowThenEnabledApprovalThenFirstDisabledOwnTheMetadata() {
        AiBusinessBinding flow = binding(0, "flow");
        AiBusinessBinding approval = binding(1, "approval");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(flow);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "APPROVAL")).thenReturn(approval);
        assertEquals("approval", service.getCodeAppMetadata("order").get("owner"));
        assertTrue(service.saveCodeAppMetadata("order", Map.of("owner", "edited")));
        verify(bindings).updateById(approval);
        verify(bindings, never()).updateById(flow);
        assertEquals(1, approval.getStatus());
        assertEquals("approval-model", JSON.parseObject(approval.getBindingConfig()).getString("flowModelKey"));
        approval.setStatus(0);
        assertEquals("flow", service.getCodeAppMetadata("order").get("owner"));
        flow.setStatus(1);
        approval.setStatus(1);
        clearInvocations(bindings);
        assertEquals("flow", service.getCodeAppMetadata("order").get("owner"));
        verify(bindings, never()).selectBindingByTypeAndCode(7L, "OBJECT", "order", "APPROVAL");
    }

    @Test
    void catalogAndMetadataUseAuthenticatedTenantBeforeAmbientTenant() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(9L);
            when(bindings.selectBindingByTypeAndCode(9L, "OBJECT", "order", "FLOW"))
                    .thenReturn(binding(1, "tenant-nine"));
            assertEquals("tenant-nine", service.getCodeAppMetadata("order").get("owner"));
            Map<String, Object> catalog = service.getFormAssets("order", false);
            assertEquals("tenant-nine", ((Map<?, ?>) catalog.get("codeAppMetadata")).get("owner"));
            verify(objects).selectObjectList(eq(9L), any());
            verify(bindings, never()).selectBindingByTypeAndCode(eq(7L), anyString(), anyString(), anyString());
            assertEquals(7L, TenantContextHolder.getTenantId());
        }
    }

    @Test
    void absentInvalidAndFailedStorageKeepExistingReturnAndFailureContracts() {
        assertTrue(service.getCodeAppMetadata(" ").isEmpty());
        assertFalse(service.saveCodeAppMetadata("order", "invalid"));
        assertFalse(service.saveCodeAppMetadata(" ", Map.of()));
        verifyNoInteractions(bindings);
        assertTrue(service.getCodeAppMetadata("order").isEmpty());
        AiBusinessBinding malformed = binding(1, "original");
        malformed.setBindingConfig("broken-json");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(malformed);
        assertTrue(service.getCodeAppMetadata("order").isEmpty());
        when(bindings.updateById(malformed)).thenReturn(0);
        assertTrue(service.saveCodeAppMetadata("order", Map.of("owner", "replacement")));
        IllegalStateException failure = new IllegalStateException("storage unavailable");
        doThrow(failure).when(bindings).updateById(malformed);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.saveCodeAppMetadata("order", Map.of("owner", "retry"))));
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getCodeAppMetadata("order")));
    }

    @Test
    void applicationPageAndEmptyCatalogReturnWithoutMetadataOrObjectQueries() {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(10L);
        application.setOptions("""
                {"inAppBuilder":{"nodes":[{"id":"page_order","type":"page","pageType":"form",
                "objectRef":{"objectCode":"order","configKey":"order"}}],
                "pages":{"page_order":{"children":[{"type":"form","props":{"formAssetId":"asset"}}]}},
                "formAssets":[{"id":"asset","formDesignerSchema":{"components":[
                {"type":"input","fieldBinding":{"fieldCode":"title"},"props":{"label":"Title"}}]}}]}}
                """);
        when(applications.detail(10L)).thenReturn(application);
        Map<String, Object> catalog = service.getFormAssets("order", true, 10L);
        List<?> assets = (List<?>) catalog.get("formAssets");
        assertEquals(1, assets.size());
        assertEquals("page_order", ((Map<?, ?>) assets.get(0)).get("pageId"));
        application.setOptions("{}");
        Map<String, Object> empty = service.getFormAssets("order", true, 10L);
        assertEquals(List.of(), empty.get("formAssets"));
        assertFalse(((List<?>) empty.get("warnings")).isEmpty());
        assertEquals(List.of(), service.getFormAssets(" ", true).get("formAssets"));
        verifyNoInteractions(bindings, configs, objects);
    }

    private AiBusinessBinding binding(int status, String owner) {
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setStatus(status);
        binding.setTenantId(7L);
        JSONObject config = new JSONObject();
        config.put("flowModelKey", owner + "-model");
        config.put("options", Map.of("codeAppMetadata", Map.of("owner", owner)));
        binding.setBindingConfig(config.toJSONString());
        return binding;
    }
}
