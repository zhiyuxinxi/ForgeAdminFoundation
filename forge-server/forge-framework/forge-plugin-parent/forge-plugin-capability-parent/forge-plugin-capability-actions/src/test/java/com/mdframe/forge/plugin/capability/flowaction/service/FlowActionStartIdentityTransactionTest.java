package com.mdframe.forge.plugin.capability.flowaction.service;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.execution.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlowActionStartIdentityTransactionTest {
    private JdbcTemplate jdbc;
    private FlowActionExecutionLogService service;
    private FlowActionExecutionLogMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:flow_start_identity_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE ai_capability_flow_action_log(id BIGINT PRIMARY KEY,tenant_id BIGINT,"
                + "request_id VARCHAR,flow_start_request_id VARCHAR(64),client_id BIGINT,capability_id BIGINT,"
                + "capability_code VARCHAR,capability_version VARCHAR,operation VARCHAR,object_code VARCHAR,"
                + "record_id VARCHAR,task_ref VARCHAR,idempotency_key VARCHAR,request_digest VARCHAR,"
                + "actor_type VARCHAR,actor_user_id BIGINT,service_user_id BIGINT,active_org_id BIGINT,"
                + "execute_status VARCHAR,result_code VARCHAR,result_snapshot VARCHAR,error_code VARCHAR,"
                + "duration_ms BIGINT,del_flag BIGINT DEFAULT 0,create_by BIGINT,create_dept BIGINT,"
                + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_by BIGINT,"
                + "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE association_probe(id BIGINT PRIMARY KEY, request_id VARCHAR)");
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
        factory.setMapperLocations(new ClassPathResource("mapper/FlowActionExecutionLogMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FlowActionExecutionLogMapper.class);
        service = new FlowActionExecutionLogService(mapper, json, new DataSourceTransactionManager(source));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
    }

    @Test
    void committedStartIdentitySurvivesLocalRollbackAndIsReadBackForRetry() {
        Map<String, Object> command = Map.of("recordId", "1001", "idempotencyKey", "same-start", "arguments", Map.of());
        List<String> requestIds = new ArrayList<>();
        assertThrows(BusinessException.class, () -> service.executeWithAttempt(
                descriptor(), identity(), command, "transport-one", attempt -> {
                    requestIds.add(attempt.startRequestId());
                    assertNotNull(attempt.startRequestId());
                    jdbc.update("INSERT INTO association_probe VALUES(1001, ?)", attempt.startRequestId());
                    throw new IllegalStateException("local association failed after remote success");
                }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM association_probe", Integer.class));
        assertEquals("FAILED", jdbc.queryForObject("SELECT execute_status FROM ai_capability_flow_action_log", String.class));
        assertEquals(requestIds.get(0), jdbc.queryForObject(
                "SELECT flow_start_request_id FROM ai_capability_flow_action_log", String.class));

        service.executeWithAttempt(descriptor(), identity(), command, "transport-two", attempt -> {
            requestIds.add(attempt.startRequestId());
            jdbc.update("INSERT INTO association_probe VALUES(1001, ?)", attempt.startRequestId());
            return Map.of("executeStatus", "SUCCESS");
        });

        assertEquals(requestIds.get(0), requestIds.get(1));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_capability_flow_action_log", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM association_probe", Integer.class));
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT execute_status FROM ai_capability_flow_action_log", String.class));
        TenantContextHolder.setTenantId(2L);
        assertNull(mapper.selectByIdempotency(1L, 301L, 10L, "START", "same-start"));
    }

    private SecureActionDescriptor descriptor() {
        return new SecureActionDescriptor(10L, "fixture.order.start", "Start", "Start", "1.0.0", "FLOW_ACTION",
                "fixture/order/START", "3", "FLOW", "MEDIUM", "fixture", "order", "START", 3,
                "ai:businessFlow:start", Set.of(), Set.of(), json.createObjectNode(), json.createObjectNode(),
                json.createObjectNode());
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        return new ExecutionIdentity(user, "USER", 101L, null, 301L, "fixture", "fixture-token", Set.of());
    }
}
