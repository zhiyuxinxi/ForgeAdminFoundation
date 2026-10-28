package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.support.FlowStartRecoveryQuery;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlowStartRecoveryControllerTest {
    @Test
    void exactRoundIsOwnedAndTenantBoundEvenWhenLaterRoundExists() throws Exception {
        try (var auth = new FlowEventAckFixture()) {
            var controller = prepare(auth);
            auth.use(auth.token("501", 97));
            TenantContextHolder.executeWithTenant(88L, () -> {
                var first = controller.getStartOutcome("business", "request-1").getData();
                assertEquals("original", first.processInstanceId());
                assertEquals(1, first.roundNo());
                assertEquals("rejected", first.processStatus());
                assertEquals("later", controller.getStartOutcome("business", "request-2").getData().processInstanceId());
                assertEquals(FlowStartOutcome.State.NOT_FOUND,
                        controller.getStartOutcome("other-business", "request-1").getData().state());
                assertEquals(FlowStartOutcome.State.NOT_FOUND,
                        controller.getStartOutcome("business", "missing").getData().state());
                assertEquals(88L, TenantContextHolder.getTenantId());
            });
            auth.use(auth.token("501", 96));
            assertEquals(FlowStartOutcome.State.NOT_FOUND,
                    controller.getStartOutcome("business", "request-1").getData().state());
            auth.use(auth.token("502", 97));
            assertEquals(403, assertThrows(BusinessException.class,
                    () -> controller.getStartOutcome("business", "request-1")).getCode());
            String token = auth.token("501", 97);
            auth.use(token);
            auth.logic.logoutByTokenValue(token);
            assertThrows(cn.dev33.satoken.exception.NotLoginException.class,
                    () -> controller.getStartOutcome("business", "request-1"));
        }
    }

    @Test
    void requestParameterSelectsRecoveryHandlerWhileOldStatusRouteRemains() throws Exception {
        try (var auth = new FlowEventAckFixture()) {
            var controller = prepare(auth);
            auth.use(auth.token("501", 97));
            var legacyService = mock(FlowInstanceService.class);
            var legacy = new FlowInstanceController(legacyService, null, null, null);
            var mvc = MockMvcBuilders.standaloneSetup(legacy, controller).build();
            mvc.perform(get("/api/flow/instance/status/business").param("startRequestId", "request-1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.processInstanceId").value("original"));
            mvc.perform(get("/api/flow/instance/status/business"))
                    .andExpect(status().isOk());
            verify(legacyService).getProcessStatus("business");
            auth.jdbc.update("UPDATE sys_flow_business SET process_instance_id=NULL WHERE id='first'");
            assertEquals(FlowStartOutcome.State.IN_PROGRESS,
                    controller.getStartOutcome("business", "request-1").getData().state());
        }
    }

    @Test
    void recoveryReturnsOriginalRecordedEventWithoutAcknowledgingItOrReadingLaterRound() throws Exception {
        try (var auth = new FlowEventAckFixture()) {
            var controller = prepare(auth);
            auth.use(auth.token("501", 97));
            record(auth, "original", "business", "97");
            var result = controller.getRecovery("business", "request-1").getData();
            assertEquals("original", result.outcome().processInstanceId());
            assertEquals("event-original", result.terminalEvent().getEventId());
            assertEquals("original-result", result.terminalEvent().getVariables().get("decision"));
            assertNull(controller.getRecovery("business", "request-2").getData().terminalEvent());
            assertEquals("RECORDED", auth.jdbc.queryForObject(
                    "SELECT delivery_status FROM sys_flow_event_outbox", String.class));
            var mvc = MockMvcBuilders.standaloneSetup(controller).build();
            mvc.perform(get("/api/flow/instance/recovery/business").param("startRequestId", "request-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.terminalEvent.eventId").value("event-original"));
            auth.use(auth.token("502", 97));
            assertEquals(403, assertThrows(BusinessException.class,
                    () -> controller.getRecovery("business", "request-1")).getCode());
            auth.use(auth.token("501", 96));
            assertEquals(FlowStartOutcome.State.NOT_FOUND,
                    controller.getRecovery("business", "request-1").getData().outcome().state());
        }
    }

    @Test
    void unavailableOrMismatchedTerminalEvidenceNeverBecomesAFabricatedResult() throws Exception {
        try (var auth = new FlowEventAckFixture()) {
            var controller = prepare(auth);
            auth.use(auth.token("501", 97));
            assertEquals("FLOW_START_TERMINAL_EVENT_UNAVAILABLE", assertThrows(BusinessException.class,
                    () -> controller.getRecovery("business", "request-1")).getMessage());
            assertEquals("rejected", controller.getStartOutcome("business", "request-1").getData().processStatus());
            record(auth, "original", "another-business", "97");
            assertEquals("FLOW_START_TERMINAL_EVENT_INVALID", assertThrows(BusinessException.class,
                    () -> controller.getRecovery("business", "request-1")).getMessage());
            auth.jdbc.update("UPDATE sys_flow_event_outbox SET payload='not-json'");
            assertEquals("FLOW_START_TERMINAL_EVENT_INVALID", assertThrows(BusinessException.class,
                    () -> controller.getRecovery("business", "request-1")).getMessage());
        }
    }

    private void record(FlowEventAckFixture fixture, String instance, String businessKey, String tenant) {
        String payload = """
                {"eventId":"event-original","tenantId":"%s","processInstanceId":"%s",
                "businessKey":"%s","eventType":"PROCESS_REJECTED","eventTime":"2026-10-16T02:00:00",
                "variables":{"decision":"original-result"}}
                """.formatted(tenant, instance, businessKey);
        fixture.jdbc.update("INSERT INTO sys_flow_event_outbox(event_id,tenant_id,process_instance_id,event_type,"
                + "process_def_key,destination_type,destination,payload,created_at,delivery_status)"
                + " VALUES('event-original',97,?,'PROCESS_REJECTED','approval','redis','flow:event:all',?,?,'RECORDED')",
                instance, payload, LocalDateTime.now());
    }

    private FlowStartRecoveryController prepare(FlowEventAckFixture fixture) throws Exception {
        try (var input = new ClassPathResource("db/migration/V1.0.2026100909__flow_terminal_event_outbox.sql")
                .getInputStream()) {
            fixture.jdbc.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        }
        fixture.jdbc.execute("""
                CREATE TABLE sys_flow_business (
                id VARCHAR(64) PRIMARY KEY, tenant_id BIGINT, business_key VARCHAR(100), round_no INT,
                start_request_id VARCHAR(64), previous_process_instance_id VARCHAR(64), start_request_digest CHAR(64),
                business_type VARCHAR(50), process_instance_id VARCHAR(64), process_def_id VARCHAR(64),
                process_def_key VARCHAR(100), title VARCHAR(200), status VARCHAR(50), apply_user_id VARCHAR(64),
                apply_user_name VARCHAR(100), apply_dept_id VARCHAR(64), apply_dept_name VARCHAR(100),
                apply_time TIMESTAMP, end_time TIMESTAMP, duration BIGINT, create_time TIMESTAMP, update_time TIMESTAMP)
                """);
        fixture.jdbc.update("""
                INSERT INTO sys_flow_business(id,tenant_id,business_key,round_no,start_request_id,
                process_instance_id,status,apply_user_id) VALUES
                ('first',97,'business',1,'request-1','original','rejected','501'),
                ('second',97,'business',2,'request-2','later','running','501')
                """);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(fixture.source);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setMapperLocations(new ClassPathResource("mapper/FlowBusinessMapper.xml"),
                new ClassPathResource("mapper/FlowEventOutboxMapper.xml"));
        var session = new SqlSessionTemplate(factory.getObject());
        return new FlowStartRecoveryController(new FlowStartRecoveryQuery(session.getMapper(FlowBusinessMapper.class),
                session.getMapper(FlowEventOutboxMapper.class)));
    }
}
