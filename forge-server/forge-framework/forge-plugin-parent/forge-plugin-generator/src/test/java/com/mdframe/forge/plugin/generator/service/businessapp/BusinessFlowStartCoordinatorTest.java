package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.FlowStartOutcomeUnknownException;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.domain.FlowStartRecoverySnapshot;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessFlowStartCoordinatorTest {
    private final FlowClient flow = mock(FlowClient.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final BusinessFlowInstanceLinkMapper links = mock(BusinessFlowInstanceLinkMapper.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessDocumentConfigService documents = mock(BusinessDocumentConfigService.class);
    private final BusinessDocumentRuntimeService documentRuntime = mock(BusinessDocumentRuntimeService.class);
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final ObjectProvider<RedissonClient> redis = mock(ObjectProvider.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final AiBusinessBinding binding = new AiBusinessBinding();
    private BusinessFlowService service;
    private MockedStatic<SessionHelper> session;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(7L);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getTenantId).thenReturn(7L);
        session.when(SessionHelper::getUserId).thenReturn(42L);
        session.when(SessionHelper::getUsername).thenReturn("Tester");
        service = new BusinessFlowService(bindings, links, configs, mock(BusinessObjectMapper.class), documents,
                documentRuntime, data, mock(BusinessFieldDesignService.class), mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class), mock(ApplicationEventPublisher.class), redis,
                mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "flowClient", flow);
        config.setObjectCode("order");
        config.setConfigKey("order-config");
        when(configs.selectPublishedByObjectCodeOrConfigKey(eq(7L), anyString())).thenReturn(config);
        binding.setStatus(1);
        binding.setBindingType("FLOW");
        binding.setBindingConfig("""
                {"flowModelKey":"approval","titleTemplate":"Review ${orderNo}",
                "variableMapping":[{"formField":"order_no","flowVariable":"mapped"}],
                "businessBinding":{"mode":"TABLE","statusField":"state"}}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(binding);
        when(data.selectById("order-config", 19L)).thenAnswer(call -> {
            assertEquals(7L, TenantContextHolder.getTenantId());
            return Map.of("order_no", "A-19", "nested", Map.of("private", true));
        });
        when(flow.startProcess(anyString(), anyString(), anyString(), anyMap(), nullable(String.class),
                nullable(String.class), isNull(), isNull())).thenReturn(FlowResult.success("process-1"));
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(FlowResult.success("process-1"));
    }

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        Thread.interrupted();
        session.close();
        TenantContextHolder.clear();
    }

    @Test
    void ordinaryStartPreservesVariablesTitleNewRoundAndWriteOrder() {
        AiBusinessFlowInstanceLink latest = new AiBusinessFlowInstanceLink();
        latest.setId(9L);
        when(links.selectLatestByBusinessKey(7L, "order:19")).thenReturn(latest);
        BusinessFlowStartDTO dto = request();
        dto.setVariables(new LinkedHashMap<>(Map.of("mapped", "override")));

        assertEquals("process-1", service.startDocumentFlow(dto).getProcessInstanceId());

        ArgumentCaptor<Map> variables = ArgumentCaptor.forClass(Map.class);
        var order = inOrder(data, links, flow);
        order.verify(data).selectById("order-config", 19L);
        order.verify(links).selectRunningByBusinessKey(7L, "order:19");
        order.verify(links).selectLatestByBusinessKey(7L, "order:19");
        order.verify(flow).startProcess(eq("approval"), eq("order:19:R10"), eq("Review A-19"),
                variables.capture(), eq("42"), eq("Tester"), isNull(), isNull());
        ArgumentCaptor<AiBusinessFlowInstanceLink> saved = ArgumentCaptor.forClass(AiBusinessFlowInstanceLink.class);
        order.verify(links).insert(saved.capture());
        order.verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "IN_PROCESS"));
        assertEquals("override", variables.getValue().get("mapped"));
        assertEquals("A-19", variables.getValue().get("orderNo"));
        assertFalse(variables.getValue().containsKey("nested"));
        assertEquals("order:19", saved.getValue().getBusinessKey());
        assertEquals(7L, saved.getValue().getTenantId());
        assertEquals("order:19:R10", JSON.parseObject(saved.getValue().getVariablesSnapshot()).getString("flowBusinessKey"));
        assertEquals(Map.of("mapped", "override"), dto.getVariables());
    }

    @Test
    void existingProcessReturnsBeforeBindingRemoteStartAndWrites() {
        AiBusinessFlowInstanceLink running = new AiBusinessFlowInstanceLink();
        running.setProcessInstanceId("existing");
        when(links.selectRunningByBusinessKey(7L, "order:19")).thenReturn(running);
        assertEquals("existing", service.startDocumentFlow(request()).getProcessInstanceId());
        verifyNoInteractions(bindings, flow);
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
    }

    @Test
    void documentPermissionFailurePrecedesExistingProcessLookup() {
        AiBusinessDocumentConfig document = new AiBusinessDocumentConfig();
        document.setObjectCode("order");
        document.setConfigKey("order-config");
        when(documents.selectEnabledByObjectCode(7L, "order")).thenReturn(document);
        BusinessException denied = new BusinessException("denied");
        doThrow(denied).when(documentRuntime).validateStartAllowed("order", 19L, true);
        assertSame(denied, assertThrows(BusinessException.class, () -> service.startDocumentFlow(request())));
        verifyNoInteractions(links, flow);
    }

    @Test
    void stableCapabilityKeepsBusinessKeyAndLocalFailureDoesNotSwallowRemoteSuccess() {
        IllegalStateException failure = new IllegalStateException("link insert failed");
        doThrow(failure).when(links).insert(any(AiBusinessFlowInstanceLink.class));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.startDocumentFlowForCapability(request())));
        verify(flow).startProcessForDelegatedUser(eq("approval"), eq("order:19"), eq("order"),
                eq("Review A-19"), anyMap());
        verify(links, never()).selectLatestByBusinessKey(anyLong(), anyString());
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
    }

    @Test
    void explicitStartRetainsIdentityAfterUnknownOutcomeAndLocalAssociationFailure() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("flow-action-100");
        mockRecovery("flow-action-100", "original-process", "running", null);
        FlowStartOutcomeUnknownException unknown = new FlowStartOutcomeUnknownException(
                "order:19", dto.getStartRequestId(), new IllegalStateException("response lost"));
        when(flow.startProcessForDelegatedUser(eq("approval"), eq("order:19"), eq("order"),
                eq("Review A-19"), anyMap(), eq("flow-action-100"), isNull()))
                .thenThrow(unknown).thenReturn(FlowResult.success("original-process"));
        IllegalStateException writeFailure = new IllegalStateException("local insert failed");
        doThrow(writeFailure).doReturn(1).when(links).insert(any(AiBusinessFlowInstanceLink.class));

        assertSame(unknown, assertThrows(FlowStartOutcomeUnknownException.class,
                () -> service.startDocumentFlowForCapability(dto)));
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        assertSame(writeFailure, assertThrows(IllegalStateException.class,
                () -> service.startDocumentFlowForCapability(dto)));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
        assertEquals("original-process", service.startDocumentFlowForCapability(dto).getProcessInstanceId());

        verify(flow, times(3)).startProcessForDelegatedUser(eq("approval"), eq("order:19"), eq("order"),
                eq("Review A-19"), anyMap(), eq("flow-action-100"), isNull());
        verify(links, never()).selectLatestByBusinessKey(anyLong(), anyString());
        verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "IN_PROCESS"));
    }

    @Test
    void explicitReplayReturnsExactEndedLinkInsteadOfAnotherRunningRound() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("round-one");
        AiBusinessFlowInstanceLink newer = new AiBusinessFlowInstanceLink();
        newer.setProcessInstanceId("newer-process");
        when(links.selectRunningByBusinessKey(7L, "order:19")).thenReturn(newer);
        AiBusinessFlowInstanceLink original = new AiBusinessFlowInstanceLink();
        original.setBusinessKey("order:19");
        original.setProcessInstanceId("original-process");
        original.setFlowStatus("APPROVED");
        when(links.selectByProcessInstanceIdForUpdate(7L, "original-process")).thenReturn(original);
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq("round-one"), isNull())).thenReturn(FlowResult.success("original-process"));

        var result = service.startDocumentFlowForCapability(dto);

        assertEquals("original-process", result.getProcessInstanceId());
        assertEquals("APPROVED", result.getFlowStatus());
        verify(flow, never()).getProcessStartRecovery(anyString(), anyString());
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
    }

    @Test
    void explicitManualRoundForwardsItsPredecessorAndCanonicalBusinessKey() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("round-two");
        mockRecovery("round-two", "second-process", "running", null);
        dto.setPreviousProcessInstanceId("original-process");
        when(flow.startProcess(eq("approval"), eq("order:19"), eq("order"), eq("Review A-19"), anyMap(),
                eq("42"), eq("Tester"), isNull(), isNull(), eq("round-two"), eq("original-process")))
                .thenReturn(FlowResult.success("second-process"));

        assertEquals("second-process", service.startDocumentFlow(dto).getProcessInstanceId());
        verify(links, never()).selectLatestByBusinessKey(anyLong(), anyString());
    }

    @Test
    void explicitReturnedInstanceCannotBindAnotherBusinessRecord() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("round-one");
        AiBusinessFlowInstanceLink other = new AiBusinessFlowInstanceLink();
        other.setBusinessKey("other:20");
        when(links.selectByProcessInstanceIdForUpdate(7L, "original-process")).thenReturn(other);
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq("round-one"), isNull())).thenReturn(FlowResult.success("original-process"));

        assertThrows(BusinessException.class, () -> service.startDocumentFlowForCapability(dto));
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
    }

    @Test
    void predecessorWithoutRequestIdentityFailsBeforeRemoteOrRecordLookup() {
        BusinessFlowStartDTO dto = request();
        dto.setPreviousProcessInstanceId("previous");

        assertThrows(BusinessException.class, () -> service.startDocumentFlow(dto));
        verifyNoInteractions(flow, data, links);
    }

    @Test
    void distributedLockStaysUntilTransactionCompletionAndReleasesAfterRemoteFailure() throws Exception {
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redis.getIfAvailable()).thenReturn(client);
        when(client.getLock("forge:business-flow:start:7:order:19")).thenReturn(lock);
        when(lock.tryLock(5L, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        service.startDocumentFlow(request());
        verify(lock, never()).unlock();
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        verify(lock).unlock();

        when(flow.startProcess(anyString(), anyString(), anyString(), anyMap(), nullable(String.class),
                nullable(String.class), isNull(), isNull())).thenReturn(null);
        assertEquals("流程发起失败: 无返回结果", assertThrows(BusinessException.class,
                () -> service.startDocumentFlow(request())).getMessage());
        verify(lock, times(2)).unlock();
    }

    @Test
    void localLockSerializesAnotherCallUntilTransactionCompletes() throws Exception {
        CountDownLatch secondRecordRead = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        when(data.selectById("order-config", 19L)).thenAnswer(call -> {
            if (reads.incrementAndGet() == 2) secondRecordRead.countDown();
            return Map.of("order_no", "A-19");
        });
        TransactionSynchronizationManager.initSynchronization();
        service.startDocumentFlow(request());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var second = executor.submit(() -> service.startFlowFromTrigger(
                    "approval", "order:19", "Explicit", 42L, "Tester", 7L, new JSONObject()));
            assertTrue(secondRecordRead.await(2, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
            assertEquals("process-1", second.get(2, TimeUnit.SECONDS).getProcessInstanceId());
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void lockRejectionInterruptionAndInvalidInputKeepOriginalErrors() throws Exception {
        assertEquals("发起主流程参数不能为空", assertThrows(BusinessException.class,
                () -> service.startDocumentFlow(null)).getMessage());
        RedissonClient client = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redis.getIfAvailable()).thenReturn(client);
        when(client.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5L, TimeUnit.SECONDS)).thenReturn(false);
        assertEquals("流程正在发起，请勿重复提交", assertThrows(BusinessException.class,
                () -> service.startDocumentFlow(request())).getMessage());
        when(lock.tryLock(5L, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));
        assertEquals("流程发起锁等待被中断，请稍后重试", assertThrows(BusinessException.class,
                () -> service.startDocumentFlow(request())).getMessage());
        assertTrue(Thread.currentThread().isInterrupted());
        verifyNoInteractions(flow, links);
        verify(lock, never()).unlock();
    }

    @Test
    void endedRemoteRoundRestoresOriginalVariablesThroughExistingCallback() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("ended-request");
        var terminal = FlowEventMessage.builder().eventId("original-event").tenantId("7")
                .businessKey("order:19").processInstanceId("original-process")
                .eventType(FlowEventMessage.PROCESS_COMPLETED).variables(Map.of("decision", "original"))
                .build();
        mockRecovery(dto.getStartRequestId(), "original-process", "approved", terminal);
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq(dto.getStartRequestId()), isNull())).thenReturn(FlowResult.success("original-process"));

        var result = service.startDocumentFlowForCapability(dto);

        assertEquals("APPROVED", result.getFlowStatus());
        ArgumentCaptor<AiBusinessFlowInstanceLink> saved = ArgumentCaptor.forClass(AiBusinessFlowInstanceLink.class);
        verify(links).updateById(saved.capture());
        assertEquals("original", JSON.parseObject(saved.getValue().getVariablesSnapshot()).getString("decision"));
        verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "APPROVED"));
    }

    @Test
    void missingEventAndAnotherLocalRoundCannotBecomeRunningOrOverwriteNewerState() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("ended-request");
        mockRecovery(dto.getStartRequestId(), "original-process", "approved", null);
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq(dto.getStartRequestId()), isNull())).thenReturn(FlowResult.success("original-process"));
        assertEquals(503, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)).getCode());
        var newer = new AiBusinessFlowInstanceLink();
        newer.setProcessInstanceId("newer-instance");
        when(links.selectLatestByBusinessKeyForUpdate(7L, "order:19")).thenReturn(newer);
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)).getCode());
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        verify(data, never()).updateInternalFieldsById(anyString(), anyLong(), anyMap());
        verify(flow, times(1)).getProcessStartRecovery("order:19", "ended-request");
    }

    @Test
    void explicitRetryReconcilesExistingRunningLinkFromOriginalTerminalEvent() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("ended-request");
        var existing = new AiBusinessFlowInstanceLink();
        existing.setTenantId(7L);
        existing.setObjectCode("order");
        existing.setRecordId(19L);
        existing.setBusinessKey("order:19");
        existing.setProcessInstanceId("original-process");
        existing.setFlowStatus("RUNNING");
        when(links.selectByProcessInstanceIdForUpdate(7L, "original-process")).thenReturn(existing);
        when(links.selectLatestByBusinessKeyForUpdate(7L, "order:19")).thenReturn(existing);
        var event = FlowEventMessage.builder().eventId("original-event").tenantId("7")
                .businessKey("order:19").processInstanceId("original-process")
                .eventType(FlowEventMessage.PROCESS_REJECTED).variables(Map.of("reason", "original"))
                .build();
        mockRecovery(dto.getStartRequestId(), "original-process", "rejected", event);
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq(dto.getStartRequestId()), isNull())).thenReturn(FlowResult.success("original-process"));

        assertEquals("REJECTED", service.startDocumentFlowForCapability(dto).getFlowStatus());
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
        verify(data).updateInternalFieldsById("order-config", 19L, Map.of("state", "REJECTED"));
    }

    @Test
    void committedRequestIgnoresLaterRecordChangesButRejectsNewCallerInputAndActor() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("committed-request");
        AiBusinessFlowInstanceLink original = committedLink(dto);
        when(links.selectByStartRequestForUpdate(7L, "order:19", dto.getStartRequestId())).thenReturn(original);
        when(data.selectById("order-config", 19L)).thenReturn(Map.of("state", "APPROVED", "order_no", "changed"));

        assertEquals("original-process", service.startDocumentFlowForCapability(dto).getProcessInstanceId());
        verifyNoInteractions(flow, bindings);
        dto.setTitle("different caller title");
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)).getCode());
        dto.setTitle(null);
        session.when(SessionHelper::getUserId).thenReturn(43L);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)).getCode());
        verifyNoInteractions(flow);
        verify(links, never()).insert(any(AiBusinessFlowInstanceLink.class));
    }

    @Test
    void committedRequestStillChecksCurrentDocumentPermissionBeforeRemoteRecovery() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("committed-request");
        AiBusinessFlowInstanceLink original = committedLink(dto);
        original.setFlowStatus("RUNNING");
        when(links.selectByStartRequestForUpdate(7L, "order:19", dto.getStartRequestId())).thenReturn(original);
        var document = new AiBusinessDocumentConfig();
        document.setObjectCode("order");
        document.setConfigKey("order-config");
        when(documents.selectEnabledByObjectCode(7L, "order")).thenReturn(document);
        BusinessException denied = new BusinessException("current permission denied");
        doThrow(denied).when(documentRuntime).validateStartReplayAllowed("order", 19L, true);

        assertSame(denied, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)));
        verifyNoInteractions(flow);
        verify(documentRuntime, never()).validateStartAllowed(anyString(), anyLong(), anyBoolean());
    }

    @Test
    void requestMapOrderDoesNotChangeIdentityButVariableChangesDo() {
        BusinessFlowStartDTO dto = request();
        dto.setStartRequestId("committed-request");
        dto.setVariables(new LinkedHashMap<>(Map.of("first", 1, "second", 2)));
        AiBusinessFlowInstanceLink original = committedLink(dto);
        when(links.selectByStartRequestForUpdate(7L, "order:19", dto.getStartRequestId())).thenReturn(original);
        var reordered = new LinkedHashMap<String, Object>();
        reordered.put("second", 2);
        reordered.put("first", 1);
        dto.setVariables(reordered);
        assertEquals("APPROVED", service.startDocumentFlowForCapability(dto).getFlowStatus());
        reordered.put("first", 3);
        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.startDocumentFlowForCapability(dto)).getCode());
        verifyNoInteractions(flow);
    }

    private AiBusinessFlowInstanceLink committedLink(BusinessFlowStartDTO dto) {
        var link = new AiBusinessFlowInstanceLink();
        link.setTenantId(7L);
        link.setObjectCode("order");
        link.setRecordId(19L);
        link.setBusinessKey("order:19");
        link.setProcessInstanceId("original-process");
        link.setFlowStatus("APPROVED");
        link.setStartUserId(42L);
        BusinessFlowStartRequestIdentity.from(7L, "order:19", 42L, dto, true).bind(link);
        return link;
    }

    private void mockRecovery(String requestId, String instanceId, String status, FlowEventMessage event) {
        when(flow.getProcessStartRecovery("order:19", requestId)).thenReturn(FlowResult.success(
                new FlowStartRecoverySnapshot(new FlowStartOutcome(FlowStartOutcome.State.STARTED,
                        "order:19", requestId, instanceId, status, 1), event)));
    }

    private BusinessFlowStartDTO request() {
        BusinessFlowStartDTO dto = new BusinessFlowStartDTO();
        dto.setObjectCode("order");
        dto.setRecordId(19L);
        return dto;
    }

    private void completeTransaction(int status) {
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        try {
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
