package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessActionExecuteDTO;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessApprovalResultEvent;
import org.mockito.ArgumentCaptor;
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

class BusinessFlowCallbackResultsTest {
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessFlowInstanceLinkMapper links = mock(BusinessFlowInstanceLinkMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AiBusinessFlowInstanceLink link = new AiBusinessFlowInstanceLink();
    private final AiCrudConfig config = new AiCrudConfig();
    private final BusinessFlowCallbackDTO callback = new BusinessFlowCallbackDTO();
    private final ObjectProvider<BusinessActionExecutionService> actionProvider = mock(ObjectProvider.class);
    private final BusinessActionExecutionService actions = mock(BusinessActionExecutionService.class);
    private BusinessFlowService service;

    @BeforeEach
    void setup() {
        service = new BusinessFlowService(bindings, links, configs, mock(BusinessObjectMapper.class),
                documents, mock(BusinessDocumentRuntimeService.class), data,
                mock(BusinessFieldDesignService.class), mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class), events, mock(ObjectProvider.class), actionProvider);
        when(actionProvider.getIfAvailable()).thenReturn(actions);
        link.setStartUserId(42L);
        link.setFlowModelKey("approval");
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
        when(links.selectByProcessInstanceIdForUpdate(7L, "process")).thenReturn(link);
        when(configs.selectPublishedByObjectCodeOrConfigKey(7L, "order")).thenReturn(config);
        when(data.selectById("order-config", 19L)).thenReturn(Map.of("id", 19L));
    }

    @Test
    void callbackActionAndBothEventsKeepDataRefreshAndSideEffectOrder() {
        configureAction();
        callback.setResult("completed");
        callback.setVariables(Map.of("input", "kept"));
        Map<String, Object> before = Map.of("state", "before");
        Map<String, Object> afterStatus = Map.of("state", "status-updated");
        Map<String, Object> afterAction = Map.of("state", "action-updated");
        when(data.selectById("order-config", 19L)).thenReturn(before, afterStatus, afterAction);
        TenantContextHolder.executeWithTenant(91L, () -> {
            service.handleFlowCallback(callback);
            assertEquals(91L, TenantContextHolder.getTenantId());
        });

        var order = inOrder(data, links, actions, events);
        order.verify(data).selectById("order-config", 19L);
        order.verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "APPROVED"));
        order.verify(links).updateById(link);
        order.verify(data).selectById("order-config", 19L);
        ArgumentCaptor<BusinessActionExecuteDTO> request = ArgumentCaptor.forClass(BusinessActionExecuteDTO.class);
        order.verify(actions).execute(request.capture());
        order.verify(data).selectById("order-config", 19L);
        ArgumentCaptor<BusinessEvent> businessEvent = ArgumentCaptor.forClass(BusinessEvent.class);
        order.verify(events).publishEvent(businessEvent.capture());
        order.verify(events).publishEvent(new BusinessProcessApprovalResultEvent(7L, "process", "APPROVED"));
        assertEquals("configured", request.getValue().getActionCode());
        assertEquals("flowCallback:process:APPROVED:configured", request.getValue().getIdempotencyKey());
        assertEquals("19", request.getValue().getRecordId());
        assertEquals(42L, request.getValue().getContext().get("operatorId"));
        assertEquals(Map.of("input", "kept"), request.getValue().getContext().get("variables"));
        assertEquals("FLOW_CALLBACK", request.getValue().getContext().get("source"));
        assertSame(before, businessEvent.getValue().getPreviousData());
        assertSame(afterAction, businessEvent.getValue().getRecordData());
        assertEquals(BusinessEvent.FLOW_APPROVED, businessEvent.getValue().getEventType());
        assertEquals(7L, businessEvent.getValue().getTenantId());
        assertNull(businessEvent.getValue().getSuiteCode());
        assertEquals(Map.of("input", "kept"), JSON.parseObject(link.getVariablesSnapshot()));
    }

    @Test
    void completedLinkOnlyRepublishesApprovalResultWithoutRepeatingSideEffects() {
        link.setResult("approved");
        configureAction();
        service.handleFlowCallback(callback);
        verify(events).publishEvent(new BusinessProcessApprovalResultEvent(7L, "process", "APPROVED"));
        verifyNoMoreInteractions(events);
        verifyNoInteractions(documents, configs, bindings, data, actions, actionProvider);
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
    }

    @Test
    void actionBusinessFailureWrapsButUnexpectedFailurePropagatesAndMissingProviderRejects() {
        configureAction();
        doThrow(new BusinessException("rejected by action")).when(actions).execute(any(BusinessActionExecuteDTO.class));
        assertEquals("流程回调动作执行失败: rejected by action", assertThrows(BusinessException.class,
                () -> service.handleFlowCallback(callback)).getMessage());
        verifyNoInteractions(events);
        link.setResult(null);
        link.setEndTime(null);
        IllegalStateException unexpected = new IllegalStateException("action unavailable");
        doThrow(unexpected).when(actions).execute(any(BusinessActionExecuteDTO.class));
        assertSame(unexpected, assertThrows(IllegalStateException.class, () -> service.handleFlowCallback(callback)));
        verifyNoInteractions(events);
        link.setResult(null);
        link.setEndTime(null);
        when(actionProvider.getIfAvailable()).thenReturn(null);
        assertEquals("动作执行服务未启用，无法执行流程回调动作", assertThrows(BusinessException.class,
                () -> service.handleFlowCallback(callback)).getMessage());
        verifyNoInteractions(events);
    }

    @Test
    void engineEventPreservesDocumentSuiteAndOperatorAndIgnoresUnboundEvents() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setConfigKey("order-config");
        document.setStatusField("state");
        document.setSuiteCode("suite");
        when(documents.selectEnabledByObjectCode(7L, "order")).thenReturn(document);
        BusinessDocumentConfigVO view = new BusinessDocumentConfigVO();
        view.setStatusMapping(Map.of("REJECTED", "returned"));
        when(documents.toVO(document)).thenReturn(view);
        FlowEventContext context = FlowEventContext.builder().tenantId(7L).processInstanceId("process")
                .businessKey("order:19").event(FlowCallback.ON_REJECTED).assigneeId("56")
                .variables(Map.of("decision", "return")).build();
        TenantContextHolder.executeWithTenant(91L, () -> {
            service.handleFlowEngineEvent(context);
            assertEquals(91L, TenantContextHolder.getTenantId());
        });
        ArgumentCaptor<BusinessEvent> event = ArgumentCaptor.forClass(BusinessEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("suite", event.getValue().getSuiteCode());
        assertEquals(BusinessEvent.FLOW_REJECTED, event.getValue().getEventType());
        assertEquals(56L, event.getValue().getOperatorId());
        assertEquals("REJECTED", link.getResult());
        verify(events).publishEvent(new BusinessProcessApprovalResultEvent(7L, "process", "REJECTED"));
        clearInvocations(events, data);
        context.setProcessInstanceId("unbound");
        context.setBusinessKey(null);
        service.handleFlowEngineEvent(context);
        verifyNoInteractions(events, data);
    }

    @Test
    void unknownExplicitInstanceCannotResolveToLatestDocumentRound() {
        callback.setProcessInstanceId("old-unbound-process");
        callback.setBusinessKey("order:19");
        when(links.selectLatestByBusinessKeyForUpdate(7L, "order:19")).thenReturn(link);

        assertThrows(BusinessException.class, () -> service.handleFlowCallback(callback));

        verify(links).selectByProcessInstanceIdForUpdate(7L, "old-unbound-process");
        verify(links, never()).selectLatestByBusinessKeyForUpdate(anyLong(), anyString());
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
        verifyNoInteractions(documents, configs, bindings, data, actions, events);
    }

    @Test
    void unknownEngineInstanceDoesNotCompleteLatestDocumentRound() {
        when(links.selectLatestByBusinessKeyForUpdate(7L, "order:19")).thenReturn(link);
        FlowEventContext context = FlowEventContext.builder().tenantId(7L)
                .processInstanceId("old-unbound-process").businessKey("order:19")
                .event(FlowCallback.ON_COMPLETED).build();

        service.handleFlowEngineEvent(context);

        verify(links).selectByProcessInstanceIdForUpdate(7L, "old-unbound-process");
        verify(links, never()).selectLatestByBusinessKeyForUpdate(anyLong(), anyString());
        verify(links, never()).updateById(any(AiBusinessFlowInstanceLink.class));
        verifyNoInteractions(documents, configs, bindings, data, actions, events);
    }

    @Test
    void businessEventFailurePropagatesBeforeApprovalResultPublication() {
        configureAction();
        IllegalStateException failure = new IllegalStateException("event listener failed");
        doThrow(failure).when(events).publishEvent(any(BusinessEvent.class));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.handleFlowCallback(callback)));
        verify(actions).execute(any(BusinessActionExecuteDTO.class));
        verify(events, never()).publishEvent(any(BusinessProcessApprovalResultEvent.class));
    }

    private void configureAction() {
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setStatus(1);
        binding.setBindingConfig("""
                {"businessBinding":{"mode":"LOWCODE_OBJECT","tableName":"orders","statusField":"state"},
                 "options":{"callbackActions":{"APPROVED":"configured"}},
                 "callbackActions":{"approvedActionCode":"legacy"}}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(binding);
    }
}
