package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryProperties;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryRecorder;
import com.mdframe.forge.starter.flow.mapper.FlowEventDeliveryMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowEventDeliveryTransactionTest {
    @Test
    void boundDeliveryCommitsWithTheEventAndItsInsertFailureRollsBackFlowable() throws Exception {
        try (var fixture = new FlowTerminalOutboxFixture(this::recorder)) {
            String completed = fixture.start("delivery-commits", null);
            fixture.terminate(completed, "approve");
            assertEquals("business-admin", fixture.runtime.jdbc.queryForObject(
                    "SELECT consumer_id FROM sys_flow_event_delivery WHERE event_id=?", String.class, fixture.recordedId(completed)));
            String running = fixture.start("delivery-write-failure", null);
            fixture.runtime.jdbc.execute("ALTER TABLE sys_flow_event_delivery ADD CONSTRAINT b08_delivery_reject "
                    + "CHECK (status <> 'PENDING') NOCHECK");
            assertThrows(RuntimeException.class, () -> fixture.terminate(running, "approve"));
            assertEquals("running", fixture.runtime.jdbc.queryForObject(
                    "SELECT status FROM sys_flow_business WHERE process_instance_id=?", String.class, running));
            assertEquals(1, fixture.runtime.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(running).count());
            assertEquals(1, fixture.runtime.engine.getTaskService().createTaskQuery().processInstanceId(running).count());
            assertEquals(0, fixture.runtime.jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_flow_event_outbox WHERE process_instance_id=?", Integer.class, running));
        }
    }

    private FlowEventDeliveryRecorder recorder(DataSource source) {
        try {
            String migration = FlowTerminalOutboxFixture.readMigration(
                    "V1.0.2026100911__flow_event_delivery_confirmation.sql");
            new JdbcTemplate(source).execute(migration.substring(0, migration.indexOf(';'))
                    .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
                    .replace(",\n    KEY idx_flow_delivery_due (status, next_attempt_at, claimed_until)", ""));
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setConfiguration(new MybatisConfiguration());
            factory.setMapperLocations(new ClassPathResource("mapper/FlowEventDeliveryMapper.xml"));
            var properties = new FlowEventDeliveryProperties();
            properties.setEnabled(true);
            var consumer = new FlowEventDeliveryProperties.Consumer();
            consumer.setId("business-admin");
            consumer.setLoginId("501");
            consumer.setTenantId(97L);
            consumer.setModelKeys(List.of("approval"));
            consumer.setDestinationType("redis");
            consumer.setDestination("flow:event:all");
            properties.setConsumers(List.of(consumer));
            return new FlowEventDeliveryRecorder(properties,
                    new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventDeliveryMapper.class));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
