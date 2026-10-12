package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessFlowCodeTaskContextTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessCodeFormProviderRegistry registry = mock(BusinessCodeFormProviderRegistry.class);
    private final BusinessCodeFormProvider provider = mock(BusinessCodeFormProvider.class);
    private final FlowClient flow = mock(FlowClient.class);
    private final AiBusinessBinding binding = new AiBusinessBinding();
    private BusinessFlowService service;
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getUserId).thenReturn(42L);
        session.when(SessionHelper::getTenantId).thenReturn(7L);
        service = new BusinessFlowService(bindings, mock(BusinessFlowInstanceLinkMapper.class),
                mock(AiCrudConfigMapper.class), mock(BusinessObjectMapper.class),
                mock(BusinessDocumentConfigService.class), mock(BusinessDocumentRuntimeService.class),
                mock(DynamicCrudService.class), mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class), registry, mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "flowClient", flow);
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of(
                "status", 1, "assignee", "42", "taskDefKey", "review")));
        binding.setStatus(1);
        binding.setBindingConfig("""
                {"flowModelKey":"approval","nodeForms":[{"taskDefKey":"review",
                "formMode":"BUSINESS_CODE_FORM","providerKey":"generic","formKey":"code-form",
                "formName":"Review","editMode":"EDITABLE","allowApprove":false,
                "formRef":{"providerKey":"generic","formUrl":"/generic"},
                "fieldPermissions":[{"field":"amount","readable":true,"writable":true,"required":true},
                {"field":"secret","readable":false,"writable":false}]}],
                "options":{"codeAppMetadata":{"fields":[{"field":"amount","label":"Configured"},
                {"field":"secret","label":"Hidden"}]}}}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW")).thenReturn(binding);
        when(registry.find("generic")).thenReturn(Optional.of(provider));
        when(registry.require("generic")).thenReturn(provider);
    }

    @AfterEach
    void cleanUp() {
        session.close();
        TenantContextHolder.clear();
    }

    @Test
    void nullProviderContextUsesCanonicalBaseAndFalseApprovalDefault() {
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertEquals("business-code", result.getFormType());
        assertEquals("generic", result.getObjectCode());
        assertEquals(19L, result.getRecordId());
        assertEquals("code-form", result.getFormKey());
        assertEquals("/generic", result.getFormUrl());
        assertEquals(false, result.getAllowApprove());
        assertTrue(result.getConfigured());
        ArgumentCaptor<BusinessTaskFormContextQueryDTO> capture = ArgumentCaptor.forClass(BusinessTaskFormContextQueryDTO.class);
        verify(provider).buildContext(capture.capture(), anyMap(), anyList());
        assertEquals("review", capture.getValue().getTaskDefKey());
        assertEquals("code-form", capture.getValue().getFormKey());
        assertNotNull(capture.getValue().getBusinessKey());
    }

    @Test
    void providerResultKeepsFalseAndMetadataThenPermissionsFilterPrivateData() {
        BusinessTaskFormContextVO source = context();
        source.setConfigured(false);
        when(provider.buildContext(any(), anyMap(), anyList())).thenReturn(source);
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertSame(source, result);
        assertFalse(result.getConfigured());
        assertEquals(1, result.getFields().size());
        assertEquals("Configured", result.getFields().get(0).get("label"));
        assertEquals(true, result.getFields().get(0).get("writable"));
        assertEquals(Map.of("amount", 5), result.getRecordData());
    }

    @Test
    void unregisteredAndBlankProvidersReturnExistingWarningsWithoutInvocation() {
        when(registry.find("generic")).thenReturn(Optional.empty());
        assertTrue(service.getTaskFormContext(query()).getWarnings().contains("代码表单Provider未注册: generic"));
        JSONObject config = JSON.parseObject(binding.getBindingConfig());
        JSONObject node = config.getJSONArray("nodeForms").getJSONObject(0);
        node.remove("providerKey");
        node.put("formRef", Map.of());
        binding.setBindingConfig(config.toJSONString());
        assertTrue(service.getTaskFormContext(query()).getWarnings().contains("当前代码表单缺少 providerKey，无法加载业务表单"));
        verifyNoInteractions(provider);
    }

    @Test
    void savePassesOnlyWritableFieldsAndProjectsProviderResponse() {
        when(provider.saveContext(any(), anyMap(), anyList())).thenReturn(context());
        BusinessTaskFormContextVO result = service.saveTaskFormContext(save(Map.of("amount", 9, "secret", "hidden")));
        ArgumentCaptor<BusinessTaskFormSaveDTO> capture = ArgumentCaptor.forClass(BusinessTaskFormSaveDTO.class);
        verify(provider).saveContext(capture.capture(), anyMap(), anyList());
        assertEquals(Map.of("amount", 9), capture.getValue().getData());
        assertEquals("task", capture.getValue().getTaskId());
        assertEquals(Map.of("amount", 5), result.getRecordData());
        verify(bindings, atLeastOnce()).selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW");
    }

    @Test
    void missingRequiredAndUnauthorizedActorsStopBeforeProvider() {
        assertThrows(BusinessException.class, () -> service.saveTaskFormContext(save(Map.of("secret", "hidden"))));
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of("status", 1, "assignee", "other")));
        assertThrows(BusinessException.class, () -> service.getTaskFormContext(query()));
        assertThrows(BusinessException.class, () -> service.saveTaskFormContext(save(Map.of("amount", 9))));
        verifyNoInteractions(provider);
    }

    @Test
    void providerFailuresPropagateUnchangedFromReadAndSave() {
        IllegalStateException failure = new IllegalStateException("provider failed");
        when(provider.buildContext(any(), anyMap(), anyList())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getTaskFormContext(query())));
        when(provider.saveContext(any(), anyMap(), anyList())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.saveTaskFormContext(save(Map.of("amount", 9)))));
    }

    @Test
    void nullSaveResultRemainsNullWithoutFallback() {
        assertNull(service.saveTaskFormContext(save(Map.of("amount", 9))));
        verify(provider).saveContext(any(), anyMap(), anyList());
    }

    private BusinessTaskFormContextQueryDTO query() {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId("task");
        query.setObjectCode("generic");
        query.setRecordId(19L);
        return query;
    }

    private BusinessTaskFormSaveDTO save(Map<String, Object> data) {
        BusinessTaskFormSaveDTO dto = new BusinessTaskFormSaveDTO();
        dto.setTaskId("task");
        dto.setObjectCode("generic");
        dto.setRecordId(19L);
        dto.setData(data);
        return dto;
    }

    private BusinessTaskFormContextVO context() {
        BusinessTaskFormContextVO context = new BusinessTaskFormContextVO();
        context.setObjectCode("generic");
        context.setFields(new ArrayList<>(List.of(new LinkedHashMap<>(Map.of("field", "amount", "label", "Provider")),
                new LinkedHashMap<>(Map.of("field", "secret")), new LinkedHashMap<>(Map.of("field", "internal", "internal", true)))));
        context.setRecordData(new LinkedHashMap<>(Map.of("amount", 5, "secret", "hidden", "internal", "internal")));
        return context;
    }
}
