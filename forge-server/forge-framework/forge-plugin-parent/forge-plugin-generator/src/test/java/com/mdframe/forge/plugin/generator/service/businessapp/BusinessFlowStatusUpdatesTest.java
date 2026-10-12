package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessFlowStatusUpdatesTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessFlowInstanceLinkMapper links = mock(BusinessFlowInstanceLinkMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
    private final AiCrudConfig config = new AiCrudConfig();
    private final BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
    private BusinessFlowService service;

    @BeforeEach
    void setup() {
        service = new BusinessFlowService(bindings, links, configs, mock(BusinessObjectMapper.class),
                documents, mock(BusinessDocumentRuntimeService.class), data,
                mock(BusinessFieldDesignService.class), mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class), events, mock(ObjectProvider.class), mock(ObjectProvider.class));
        link.setTenantId(7L);
        link.setObjectCode("order");
        link.setRecordId(19L);
        link.setProcessInstanceId("process");
        link.setBusinessKey("order:19");
        link.setVariablesSnapshot("{}");
        config.setConfigKey("order-config");
        config.setObjectCode("order");
        config.setRuntimeTableName("orders");
        callback.setTenantId(7L);
        callback.setProcessInstanceId("process");
        callback.setResult("APPROVED");
        when(links.selectByProcessInstanceId(7L, "process")).thenReturn(link);
        when(configs.selectPublishedByObjectCodeOrConfigKey(7L, "order")).thenReturn(config);
        when(data.selectById("order-config", 19L)).thenReturn(Map.of("id", 19L));
    }

    @Test
    void documentMappingWritesBeforeLinkCompletionAndRestoresTenant() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setConfigKey("order-config");
        document.setStatusField("state");
        when(documents.selectEnabledByObjectCode(7L, "order")).thenReturn(document);
        BusinessDocumentConfigVO view = new BusinessDocumentConfigVO();
        view.setStatusMapping(Map.of("APPROVED", "accepted"));
        when(documents.toVO(document)).thenReturn(view);
        TenantContextHolder.executeWithTenant(91L, () -> {
            service.handleFlowCallback(callback);
            assertEquals(91L, TenantContextHolder.getTenantId());
        });
        var ordered = inOrder(data, links);
        ordered.verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "accepted"));
        ordered.verify(links).updateById(link);
        assertEquals("APPROVED", link.getResult());
        assertNotNull(link.getEndTime());
    }

    @Test
    void tableBindingMapsStatusButAdapterDoesNotWrite() {
        binding("LOWCODE_OBJECT", "ORDERS");
        service.handleFlowCallback(callback);
        verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "accepted"));
        reset(data, links);
        link.setEndTime(null);
        link.setResult(null);
        when(links.selectByProcessInstanceId(7L, "process")).thenReturn(link);
        binding("ADAPTER", "other");
        service.handleFlowCallback(callback);
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
        verify(links).updateById(link);
    }

    @Test
    void mismatchedTableAndMissingDocumentStatusRejectBeforeCompletion() {
        binding("LOWCODE_OBJECT", "unrelated");
        assertThrows(BusinessException.class, () -> service.handleFlowCallback(callback));
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setConfigKey("order-config");
        when(documents.selectEnabledByObjectCode(7L, "order")).thenReturn(document);
        assertThrows(BusinessException.class, () -> service.handleFlowCallback(callback));
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
    }

    @Test
    void managedStatusTakesPriorityAndKeepsItsExistingFailureDegradation() {
        binding("LOWCODE_OBJECT", "orders");
        link.setVariablesSnapshot("{\"flowStatusField\":\"flowStatus\",\"statusField\":\"state\"}");
        doThrow(new IllegalStateException("storage unavailable")).when(data)
                .updateInternalFieldsByIdAllowDraft("order-config", 19L, Map.of("flowStatus", "APPROVED"));
        service.handleFlowCallback(callback);
        verify(data).updateInternalFieldsByIdAllowDraft("order-config", 19L, Map.of("flowStatus", "APPROVED"));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
        verify(links).updateById(link);
        assertEquals("APPROVED", link.getResult());
    }

    @Test
    void invalidManagedFieldRejectsWithoutMutation() {
        link.setVariablesSnapshot("{\"flowStatusField\":\"customer_status\"}");
        assertThrows(BusinessException.class, () -> service.handleFlowCallback(callback));
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsByIdAllowDraft(anyString(), anyLong(), anyMap());
    }

    @Test
    void documentWriteFailurePropagatesAndDuplicateCompletionDoesNotWrite() {
        binding("LOWCODE_OBJECT", "orders");
        doThrow(new IllegalStateException("storage unavailable")).when(data)
                .updateInternalFieldsById(anyString(), anyLong(), anyMap());
        assertThrows(IllegalStateException.class, () -> service.handleFlowCallback(callback));
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
        clearInvocations(data);
        link.setResult("APPROVED");
        service.handleFlowCallback(callback);
        verifyNoInteractions(data);
    }

    private void binding(String mode, String table) {
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setStatus(1);
        binding.setBindingConfig("{\"businessBinding\":{\"mode\":\"" + mode + "\",\"tableName\":\"" + table
                + "\",\"statusField\":\"state\"},\"document\":{\"statusMapping\":{\"APPROVED\":\"accepted\"}}}");
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(binding);
    }
}
