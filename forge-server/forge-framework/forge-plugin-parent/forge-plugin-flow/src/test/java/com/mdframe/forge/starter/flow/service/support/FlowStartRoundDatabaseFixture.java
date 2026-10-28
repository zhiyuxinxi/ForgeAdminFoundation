package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.flow.dto.FlowStartRoundRequest;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.listener.FlowTaskEventListener;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.impl.FlowInstanceServiceImpl;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real shared Spring transaction, MyBatis mapper and Flowable engine; notification/task sinks are mocked. */
final class FlowStartRoundDatabaseFixture implements AutoCloseable {
    final JdbcTemplate jdbc;
    final TransactionTemplate transactions;
    final FlowBusinessMapper mapper;
    final ProcessEngine engine;
    final List<FlowTask> firstTasks = new CopyOnWriteArrayList<>();

    FlowStartRoundDatabaseFixture() throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:b07_rounds_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        var manager = new DataSourceTransactionManager(dataSource);
        transactions = new TransactionTemplate(manager);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE sys_flow_business (
                  id VARCHAR(64) PRIMARY KEY, tenant_id BIGINT NOT NULL, business_key VARCHAR(100) NOT NULL,
                  business_type VARCHAR(50), process_instance_id VARCHAR(64), process_def_id VARCHAR(64),
                  process_def_key VARCHAR(100), title VARCHAR(200), status VARCHAR(50),
                  apply_user_id VARCHAR(64), apply_user_name VARCHAR(100), apply_dept_id VARCHAR(64),
                  apply_dept_name VARCHAR(100), apply_time TIMESTAMP, end_time TIMESTAMP, duration BIGINT,
                  create_time TIMESTAMP, update_time TIMESTAMP, round_no INT DEFAULT 1 NOT NULL,
                  start_request_id VARCHAR(64), previous_process_instance_id VARCHAR(64), start_request_digest CHAR(64),
                  CONSTRAINT uk_flow_business_round UNIQUE (tenant_id,business_key,round_no),
                  CONSTRAINT uk_flow_business_start_request UNIQUE (tenant_id,business_key,start_request_id))
                """);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContextHolder.getTenantId());
            }
        }));
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new ClassPathResource("mapper/FlowBusinessMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FlowBusinessMapper.class);
        var flow = new SpringProcessEngineConfiguration();
        flow.setDataSource(dataSource);
        flow.setTransactionManager(manager);
        flow.setDatabaseSchemaUpdate("true");
        flow.setHistory("full");
        flow.setAsyncExecutorActivate(false);
        flow.setDisableIdmEngine(true);
        flow.setDisableEventRegistry(true);
        engine = flow.buildProcessEngine();
        registerFirstTaskListener();
        deploy();
    }

    FlowInstanceServiceImpl service() {
        var service = new FlowInstanceServiceImpl();
        ReflectionTestUtils.setField(service, "processEngine", engine);
        ReflectionTestUtils.setField(service, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(service, "historyService", engine.getHistoryService());
        ReflectionTestUtils.setField(service, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(service, "flowBusinessMapper", mapper);
        ReflectionTestUtils.setField(service, "flowErrorLogService", mock(FlowErrorLogService.class));
        ReflectionTestUtils.setField(service, "startRounds", new FlowStartRoundCoordinator(mapper, engine.getRuntimeService()));
        return service;
    }

    String start(FlowInstanceServiceImpl service, long tenant, String key, String request, String previous,
                 String actor, Map<String, Object> variables) {
        return TenantContextHolder.executeWithTenant(tenant,
                () -> transactions.execute(status -> service.startProcess("approval", key, "test", key, variables,
                    actor, "Applicant", "dept", "Department",
                    request == null ? null : new FlowStartRoundRequest(request, previous))));
    }

    void finish(String instance, String status) {
        transactions.executeWithoutResult(ignored -> {
            var task = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
            engine.getTaskService().complete(task.getId());
            jdbc.update("UPDATE sys_flow_business SET status=?, end_time=CURRENT_TIMESTAMP WHERE process_instance_id=?",
                    status, instance);
        });
    }

    int count(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_flow_business WHERE business_key=?", Integer.class, key);
    }

    private void registerFirstTaskListener() {
        var listener = new FlowTaskEventListener();
        var tasks = mock(FlowTaskMapper.class);
        when(tasks.insert(any(FlowTask.class))).thenAnswer(call -> {
            firstTasks.add(call.getArgument(0));
            return 1;
        });
        ReflectionTestUtils.setField(listener, "flowTaskMapper", tasks);
        ReflectionTestUtils.setField(listener, "flowBusinessMapper", mapper);
        ReflectionTestUtils.setField(listener, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(listener, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(listener, "historyService", engine.getHistoryService());
        ReflectionTestUtils.setField(listener, "eventPublisher", mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(listener, "flowErrorLogService", mock(FlowErrorLogService.class));
        engine.getRuntimeService().addEventListener(listener, FlowableEngineEventType.TASK_CREATED);
    }

    private void deploy() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:flowable="http://flowable.org/bpmn" targetNamespace="b07">
                  <process id="approval" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="a" sourceRef="start" targetRef="review"/>
                    <userTask id="review" name="Review" flowable:assignee="${reviewer}"/>
                    <sequenceFlow id="b" sourceRef="review" targetRef="end"/><endEvent id="end"/>
                  </process>
                </definitions>
                """;
        engine.getRepositoryService().createDeployment().addString("b07.bpmn20.xml", xml).deploy();
    }

    @Override
    public void close() {
        try {
            engine.close();
        } finally {
            jdbc.execute("SHUTDOWN");
        }
    }
}
