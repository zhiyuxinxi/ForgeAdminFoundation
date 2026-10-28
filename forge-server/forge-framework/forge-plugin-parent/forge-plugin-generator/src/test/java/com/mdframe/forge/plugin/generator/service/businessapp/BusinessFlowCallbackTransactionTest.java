package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.domain.FlowStartRecoverySnapshot;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessApprovalResultEvent;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BusinessFlowCallbackTransactionTest {
    private final DynamicCrudService data = mock(DynamicCrudService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AiCrudConfigMapper configs = mock(AiCrudConfigMapper.class);
    private final BusinessBindingMapper bindings = mock(BusinessBindingMapper.class);
    private final FlowClient flow = mock(FlowClient.class);
    private final BusinessActionExecutionService actions = mock(BusinessActionExecutionService.class);
    private JdbcTemplate jdbc;
    private BusinessFlowService first;
    private BusinessFlowService second;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:flow_callback_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE ai_business_flow_instance_link(id BIGINT PRIMARY KEY,tenant_id BIGINT,"
                + "object_code VARCHAR,record_id BIGINT,business_key VARCHAR,flow_model_key VARCHAR,"
                + "process_instance_id VARCHAR,start_request_id VARCHAR(64),start_request_digest CHAR(64),"
                + "flow_status VARCHAR,start_user_id BIGINT,start_time TIMESTAMP,"
                + "end_time TIMESTAMP,result VARCHAR,variables_snapshot VARCHAR,create_by BIGINT,"
                + "create_time TIMESTAMP,create_dept BIGINT,update_by BIGINT,update_time TIMESTAMP,"
                + "UNIQUE(tenant_id,business_key,start_request_id))");
        jdbc.update("INSERT INTO ai_business_flow_instance_link(id,tenant_id,object_code,record_id,business_key,"
                + "process_instance_id,flow_status,variables_snapshot) VALUES(1,7,'order',19,'order:19','process','RUNNING','{}')");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getTenantId());
            }
        }));
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new ClassPathResource("mapper/BusinessFlowInstanceLinkMapper.xml"));
        BusinessFlowInstanceLinkMapper mapper = new SqlSessionTemplate(factory.getObject())
                .getMapper(BusinessFlowInstanceLinkMapper.class);
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(source);
        first = service(mapper, transactions);
        second = service(mapper, transactions);
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("order-config");
        config.setObjectCode("order");
        config.setRuntimeTableName("orders");
        when(configs.selectPublishedByObjectCodeOrConfigKey(7L, "order")).thenReturn(config);
        when(data.selectById("order-config", 19L)).thenReturn(Map.of("id", 19L));
        TenantContextHolder.setTenantId(7L);
    }

    @SuppressWarnings("unchecked")
    private BusinessFlowService service(BusinessFlowInstanceLinkMapper mapper,
                                        DataSourceTransactionManager transactions) {
        ObjectProvider<BusinessActionExecutionService> actionProvider = mock(ObjectProvider.class);
        when(actionProvider.getIfAvailable()).thenReturn(actions);
        BusinessFlowService target = new BusinessFlowService(bindings, mapper,
                configs, mock(BusinessObjectMapper.class), mock(BusinessDocumentConfigService.class),
                mock(BusinessDocumentRuntimeService.class), data, mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class), mock(BusinessCodeFormProviderRegistry.class),
                events, mock(ObjectProvider.class), actionProvider);
        ReflectionTestUtils.setField(target, "flowClient", flow);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (BusinessFlowService) proxy.getProxy();
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP ALL OBJECTS");
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentCallbacksAcrossServicesApplyDocumentEventOnce(boolean legacyBusinessKeyOnly) throws Exception {
        CountDownLatch insideFirst = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        AtomicBoolean blockOnce = new AtomicBoolean(true);
        when(data.selectById("order-config", 19L)).thenAnswer(invocation -> {
            if (blockOnce.compareAndSet(true, false)) {
                insideFirst.countDown();
                assertTrue(releaseFirst.await(3, TimeUnit.SECONDS));
            }
            return Map.of("id", 19L);
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var a = workers.submit(() -> call(first, legacyBusinessKeyOnly));
            assertTrue(insideFirst.await(3, TimeUnit.SECONDS));
            var b = workers.submit(() -> {
                secondEntered.countDown();
                call(second, legacyBusinessKeyOnly);
            });
            assertTrue(secondEntered.await(3, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> b.get(200, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            a.get(3, TimeUnit.SECONDS);
            b.get(3, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals("APPROVED", status());
        verify(events, times(1)).publishEvent(any(BusinessEvent.class));
        // The existing exact-instance approval-result replay remains available for downstream recovery.
        verify(events, times(2)).publishEvent(new BusinessProcessApprovalResultEvent(7L, "process", "APPROVED"));
    }

    @Test
    void failedListenerRollsBackLinkAndRetryCanComplete() {
        doThrow(new IllegalStateException("listener failed")).when(events).publishEvent(any(BusinessEvent.class));
        assertThrows(IllegalStateException.class, () -> call(first, false));
        assertEquals("RUNNING", status());
        assertNull(jdbc.queryForObject("SELECT result FROM ai_business_flow_instance_link WHERE id=1", String.class));
        reset(events);
        call(second, false);
        assertEquals("APPROVED", status());
        verify(events).publishEvent(any(BusinessEvent.class));
    }

    @Test
    void businessKeyOnlyCallbackCannotChooseBetweenDifferentRounds() {
        jdbc.update("INSERT INTO ai_business_flow_instance_link(id,tenant_id,object_code,record_id,business_key,"
                + "process_instance_id,flow_status,variables_snapshot) VALUES(2,7,'order',19,'order:19','new-process','RUNNING','{}')");

        assertThrows(BusinessException.class, () -> call(first, true));

        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_flow_instance_link WHERE flow_status='RUNNING'", Integer.class));
        verifyNoInteractions(data, events);
    }

    @Test
    void anotherTenantCannotLockOrCompleteTheLink() {
        BusinessFlowCallbackDTO request = request(false);
        request.setTenantId(8L);
        assertThrows(BusinessException.class, () -> TenantContextHolder.executeWithTenant(8L,
                () -> first.handleFlowCallback(request)));
        assertEquals("RUNNING", status());
        verifyNoInteractions(data, events);
    }

    @Test
    void terminalRecoveryRollsBackAssociationAndPostingThenReplaysOnce() {
        jdbc.update("DELETE FROM ai_business_flow_instance_link WHERE id=1");
        jdbc.execute("CREATE TABLE recovery_document(id BIGINT PRIMARY KEY,state VARCHAR(32))");
        jdbc.execute("CREATE TABLE recovery_posting(id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO recovery_document VALUES(19,'DRAFT')");
        when(data.selectById("order-config", 19L)).thenAnswer(call -> Map.of("id", 19L, "state",
                jdbc.queryForObject("SELECT state FROM recovery_document WHERE id=19", String.class)));
        var binding = new AiBusinessBinding();
        binding.setStatus(1);
        binding.setBindingType("FLOW");
        binding.setBindingConfig("""
                {"flowModelKey":"approval","businessBinding":{"mode":"TABLE","statusField":"state"},
                "callbackActions":{"APPROVED":"record-result"}}
                """);
        when(bindings.selectBindingByTypeAndCode(7L, "OBJECT", "order", "FLOW")).thenReturn(binding);
        doAnswer(call -> {
            Map<String, Object> values = call.getArgument(2);
            jdbc.update("UPDATE recovery_document SET state=? WHERE id=19", values.get("state"));
            return null;
        }).when(data).updateInternalFieldsById(eq("order-config"), eq(19L), anyMap());
        when(actions.execute(any())).thenAnswer(call -> {
            jdbc.update("INSERT INTO recovery_posting VALUES(19)");
            return null;
        });
        var event = FlowEventMessage.builder().eventId("original-event").tenantId("7")
                .businessKey("order:19").processInstanceId("original-instance")
                .eventType(FlowEventMessage.PROCESS_COMPLETED).variables(Map.of("approvedAmount", 17)).build();
        when(flow.startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(), anyMap(),
                eq("original-request"), isNull())).thenAnswer(call -> {
                    Map<String, Object> variables = call.getArgument(4);
                    assertEquals("DRAFT", variables.get("state"), "A repeated remote start must keep original variables");
                    return FlowResult.success("original-instance");
                });
        when(flow.getProcessStartRecovery("order:19", "original-request")).thenReturn(FlowResult.success(
                new FlowStartRecoverySnapshot(new FlowStartOutcome(FlowStartOutcome.State.STARTED,
                        "order:19", "original-request", "original-instance", "approved", 1), event)));
        BusinessFlowStartDTO request = new BusinessFlowStartDTO();
        request.setObjectCode("order");
        request.setRecordId(19L);
        request.setStartRequestId("original-request");
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            session.when(SessionHelper::getUserId).thenReturn(42L);
            session.when(SessionHelper::getUsername).thenReturn("Owner");
            doThrow(new IllegalStateException("listener failed")).when(events).publishEvent(any(BusinessEvent.class));
            assertThrows(IllegalStateException.class, () -> first.startDocumentFlowForCapability(request));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_flow_instance_link", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM recovery_posting", Integer.class));
            assertEquals("DRAFT", jdbc.queryForObject("SELECT state FROM recovery_document", String.class));

            reset(events);
            assertEquals("APPROVED", second.startDocumentFlowForCapability(request).getFlowStatus());
            assertEquals("APPROVED", first.startDocumentFlowForCapability(request).getFlowStatus());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_flow_instance_link", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recovery_posting", Integer.class));
            assertEquals("APPROVED", jdbc.queryForObject("SELECT state FROM recovery_document", String.class));
            assertEquals("17", com.alibaba.fastjson2.JSON.parseObject(jdbc.queryForObject(
                    "SELECT variables_snapshot FROM ai_business_flow_instance_link", String.class))
                    .getString("approvedAmount"));
            verify(events).publishEvent(any(BusinessEvent.class));
            verify(flow, times(2)).startProcessForDelegatedUser(anyString(), anyString(), anyString(), anyString(),
                    anyMap(), eq("original-request"), isNull());
            assertEquals("original-request", jdbc.queryForObject(
                    "SELECT start_request_id FROM ai_business_flow_instance_link", String.class));
            assertEquals(64, jdbc.queryForObject(
                    "SELECT start_request_digest FROM ai_business_flow_instance_link", String.class).length());
            request.setTitle("changed caller input");
            assertEquals(409, assertThrows(BusinessException.class,
                    () -> first.startDocumentFlowForCapability(request)).getCode());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recovery_posting", Integer.class));
        }
    }

    private void call(BusinessFlowService service, boolean legacyBusinessKeyOnly) {
        TenantContextHolder.executeWithTenant(7L, () -> service.handleFlowCallback(request(legacyBusinessKeyOnly)));
    }

    private BusinessFlowCallbackDTO request(boolean legacyBusinessKeyOnly) {
        BusinessFlowCallbackDTO request = new BusinessFlowCallbackDTO();
        request.setTenantId(7L);
        request.setProcessInstanceId(legacyBusinessKeyOnly ? null : "process");
        request.setBusinessKey("order:19");
        request.setResult("APPROVED");
        return request;
    }

    private String status() {
        return jdbc.queryForObject("SELECT flow_status FROM ai_business_flow_instance_link WHERE id=1", String.class);
    }
}
