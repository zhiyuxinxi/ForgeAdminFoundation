package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.support.FlowStartRecoveryQuery;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private FlowStartRecoveryController prepare(FlowEventAckFixture fixture) throws Exception {
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
        factory.setMapperLocations(new FileSystemResource("forge-server/forge-framework/forge-plugin-parent/"
                + "forge-plugin-flow/src/main/resources/mapper/FlowBusinessMapper.xml"));
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FlowBusinessMapper.class);
        return new FlowStartRecoveryController(new FlowStartRecoveryQuery(mapper));
    }
}
