package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.*;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusinessFlowNodeFormResolutionTest {
    private final FlowClient flow = mock(FlowClient.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessCodeFormProviderRegistry providers = mock(BusinessCodeFormProviderRegistry.class);
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
                mock(BusinessFlowVariableResolver.class), providers, mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "flowClient", flow);
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of(
                "status", 1, "assignee", "42", "taskDefKey", "review")));
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setStatus(1);
        binding.setBindingConfig("""
                {"flowModelKey":"approval","nodeForms":[{"taskDefKey":"review","formMode":"EXTERNAL",
                "formKey":"binding","formName":"Saved","formUrl":"/saved","allowApprove":false}]}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW")).thenReturn(binding);
        when(providers.find(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void cleanUp() {
        session.close();
        TenantContextHolder.clear();
    }

    @Test
    void completeTaskFormWinsAndDoesNotReadProcessForm() {
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(external("remote", "Remote")));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertEquals("remote", result.getFormKey());
        assertEquals("Remote", result.getFormName());
        assertEquals("EXTERNAL", result.getFormType());
        verify(flow, never()).getProcessFormInfo(any(), any(), any(), any(), any());
        verify(bindings, atLeastOnce()).selectBindingByTypeAndCode(7L, "OBJECT", "generic", "FLOW");
    }

    @Test
    void partialTaskOverlaysProcessWithFalseAndCollectionsButNotBlankText() {
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(Map.of(
                "formName", " ", "allowApprove", false, "formFieldPermissions", List.of())));
        Map<String, Object> process = external("process", "Process");
        process.put("allowApprove", true);
        process.put("formFieldPermissions", List.of(Map.of("field", "amount", "writable", true)));
        when(flow.getProcessFormInfo(any(), any(), any(), any(), any())).thenReturn(FlowResult.success(process));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertEquals("process", result.getFormKey());
        assertEquals("Process", result.getFormName());
        assertFalse(result.getAllowApprove());
        assertEquals("READONLY", result.getEditMode());
        verify(flow).getProcessFormInfo(eq(null), anyString(), eq("approval"), eq("task"), eq("review"));
    }

    @Test
    void remoteFailuresFallBackToSavedNodeWithoutHidingTaskAccessFailure() {
        when(flow.getTaskFormInfo("task")).thenThrow(new IllegalStateException("task unavailable"));
        when(flow.getProcessFormInfo(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("process unavailable"));
        BusinessTaskFormContextVO result = service.getTaskFormContext(query());
        assertEquals("binding", result.getFormKey());
        assertEquals("/saved", result.getFormUrl());
        assertFalse(result.getAllowApprove());
        when(flow.getTaskDetail("task")).thenReturn(FlowResult.success(Map.of("status", 1, "assignee", "other")));
        assertThrows(BusinessException.class, () -> service.getTaskFormContext(query()));
    }

    @Test
    void nullOrUnsuccessfulRemoteResultsKeepPartialTaskOrSavedFallback() {
        when(flow.getTaskFormInfo("task")).thenReturn(null);
        when(flow.getProcessFormInfo(any(), any(), any(), any(), any())).thenReturn(FlowResult.error("unavailable"));
        assertEquals("binding", service.getTaskFormContext(query()).getFormKey());
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(Map.of(
                "formKey", "partial", "formMode", "EXTERNAL")));
        assertEquals("partial", service.getTaskFormContext(query()).getFormKey());
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(Map.of("formType", "none")));
        clearInvocations(flow);
        assertEquals("binding", service.getTaskFormContext(query()).getFormKey());
        verify(flow, never()).getProcessFormInfo(any(), any(), any(), any(), any());
    }

    @Test
    void selectedRuntimePageOverridesConfiguredPageAndResetsForeignFieldPermissions() {
        when(providers.listAssets("generic")).thenReturn(List.of(
                Map.of("formKey", "model", "formMode", "BUSINESS_CODE_FORM", "providerKey", "generic"),
                Map.of("formKey", "runtime", "formMode", "BUSINESS_CODE_FORM", "providerKey", "generic")));
        for (Map<String, Object> variables : List.<Map<String, Object>>of(
                Map.of("businessFormKey", "runtime", "businessFormRef", Map.of("formKey", "model", "pageId", "p")),
                Map.of("formKey", "runtime"))) {
            Map<String, Object> info = new LinkedHashMap<>(Map.of("formType", "custom", "formKey", "model",
                    "formMode", "BUSINESS_CODE_FORM", "variables", variables,
                    "formFieldPermissions", List.of(Map.of("field", "foreign", "readable", true, "writable", true))));
            when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(info));
            BusinessTaskFormContextVO result = service.getTaskFormContext(query());
            assertEquals("runtime", result.getFormKey());
            assertTrue(result.getFieldPermissions().isEmpty());
            assertEquals("READONLY", result.getEditMode());
        }
    }

    @Test
    void missingRuntimeAssetKeepsConfiguredFormAndReadOnlyEntrySupportsAbsentClient() {
        Map<String, Object> info = external("remote", "Remote");
        info.put("variables", Map.of("businessFormKey", "missing"));
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(info));
        assertEquals("remote", service.getTaskFormContext(query()).getFormKey());
        ReflectionTestUtils.setField(service, "flowClient", null);
        BusinessTaskFormContextQueryDTO query = query();
        query.setTaskDefKey("review");
        assertEquals("binding", service.getTaskFormReadonlyContext(query).getFormKey());
    }

    @Test
    void strictRuntimeStillRejectsMissingPublishedConfigBeforeReturningExternalForm() {
        when(flow.getTaskFormInfo("task")).thenReturn(FlowResult.success(external("remote", "Remote")));
        assertThrows(BusinessException.class, () -> service.getActionableTaskFormContext(query()));
    }

    private BusinessTaskFormContextQueryDTO query() {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId("task");
        query.setObjectCode("generic");
        query.setRecordId(19L);
        return query;
    }

    private Map<String, Object> external(String key, String name) {
        return new LinkedHashMap<>(Map.of("formType", "custom", "formKey", key,
                "formName", name, "formMode", "EXTERNAL", "formUrl", "/" + key));
    }
}
