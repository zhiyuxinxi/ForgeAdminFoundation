package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.event.FlowTerminalEventOutbox;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryRecorder;
import com.mdframe.forge.starter.flow.listener.FlowTaskEventListener;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reuses the established real Flowable/Spring database fixture, without running its prior tests. */
final class FlowTerminalOutboxFixture implements AutoCloseable {
    static final long TENANT = 97L;
    final FlowStartRoundDatabaseFixture runtime;
    final AnnotationConfigApplicationContext context;
    final FlowModelMapper models = mock(FlowModelMapper.class);
    final FlowModel model = new FlowModel();
    final FlowEventOutboxMapper outbox;

    FlowTerminalOutboxFixture() throws Exception {
        this(null);
    }

    FlowTerminalOutboxFixture(Function<DataSource, FlowEventDeliveryRecorder> recorderFactory) throws Exception {
        runtime = new FlowStartRoundDatabaseFixture();
        String migration = Files.readString(Path.of("forge-server/db/migration/"
                + "V1.0.2026100909__flow_terminal_event_outbox.sql"), StandardCharsets.UTF_8);
        runtime.jdbc.execute(migration.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        outbox = newReader();
        model.setNotifyType("redis");
        when(models.selectByModelKey(anyString())).thenReturn(model);
        context = new AnnotationConfigApplicationContext();
        FlowEventDeliveryRecorder recorder = recorderFactory == null ? null : recorderFactory.apply(runtime.jdbc.getDataSource());
        context.registerBean(FlowTerminalEventOutbox.class, () -> new FlowTerminalEventOutbox(models, outbox, recorder));
        context.refresh();
        registerTerminalListener();
    }

    FlowEventOutboxMapper newReader() throws Exception {
        var configuration = new MybatisConfiguration();
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(runtime.jdbc.getDataSource());
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new FileSystemResource("forge-server/forge-framework/forge-plugin-parent/"
                + "forge-plugin-flow/src/main/resources/mapper/FlowEventOutboxMapper.xml"));
        return new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventOutboxMapper.class);
    }

    String start(String key, String previous) {
        return runtime.start(runtime.service(), TENANT, key, UUID.randomUUID().toString(), previous,
                "123", Map.of("reviewer", "123"));
    }

    void terminate(String instance, String outcome) {
        TenantContextHolder.executeWithTenant(TENANT, () -> runtime.transactions.executeWithoutResult(status -> {
            if ("canceled".equals(outcome)) {
                runtime.engine.getRuntimeService().deleteProcessInstance(instance, "Applicant canceled");
            } else {
                runtime.engine.getRuntimeService().setVariable(instance, "approvalResult", outcome);
                var task = runtime.engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
                runtime.engine.getTaskService().complete(task.getId());
            }
        }));
    }

    String recordedId(String instance) {
        return runtime.jdbc.queryForObject("SELECT event_id FROM sys_flow_event_outbox WHERE process_instance_id=?",
                String.class, instance);
    }

    private void registerTerminalListener() {
        var listener = new FlowTaskEventListener();
        ReflectionTestUtils.setField(listener, "flowTaskMapper", mock(FlowTaskMapper.class));
        ReflectionTestUtils.setField(listener, "flowBusinessMapper", runtime.mapper);
        ReflectionTestUtils.setField(listener, "taskService", runtime.engine.getTaskService());
        ReflectionTestUtils.setField(listener, "runtimeService", runtime.engine.getRuntimeService());
        ReflectionTestUtils.setField(listener, "historyService", runtime.engine.getHistoryService());
        ReflectionTestUtils.setField(listener, "eventPublisher", context);
        ReflectionTestUtils.setField(listener, "flowErrorLogService", mock(FlowErrorLogService.class));
        runtime.engine.getRuntimeService().addEventListener(listener,
                FlowableEngineEventType.PROCESS_COMPLETED, FlowableEngineEventType.PROCESS_CANCELLED);
    }

    @Override
    public void close() {
        context.close();
        runtime.close();
    }
}
