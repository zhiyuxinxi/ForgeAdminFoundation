package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClientProperties;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.event.FlowCallbackCatalog;
import com.mdframe.forge.flow.client.event.FlowDurableEventConsumer;
import com.mdframe.forge.flow.client.event.FlowEventAcknowledgementClient;
import com.mdframe.forge.flow.client.event.FlowEventConsumerProperties;
import com.mdframe.forge.flow.client.helper.FlowEventSubscriber;
import com.mdframe.forge.flow.client.mapper.FlowEventInboxMapper;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryDispatcher;
import com.mdframe.forge.starter.flow.event.FlowEventOutboxEntry;
import com.mdframe.forge.starter.flow.event.FlowRecordedEventSender;
import com.mdframe.forge.starter.flow.mapper.FlowEventOutboxMapper;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowDeliveryRoundTripTest {
    @Test
    void offlineAndFailedCallbackAndLostAckRecoverThroughRealInboxAndAuthenticatedAck() throws Exception {
        try (var flow = new FlowEventAckFixture(); var consumer = new ConsumerFixture()) {
            var outbox = prepareOutbox(flow.source);
            LocalDateTime now = LocalDateTime.now().plusSeconds(1);
            var entry = new FlowEventOutboxEntry("event-one", 97L, "instance-one", "PROCESS_COMPLETED",
                    "approval", "redis", "flow:event:all", "{\"eventId\":\"event-one\",\"tenantId\":97,"
                    + "\"processInstanceId\":\"instance-one\",\"processDefKey\":\"approval\","
                    + "\"eventType\":\"PROCESS_COMPLETED\",\"businessKey\":\"sample:one\"}", now);
            outbox.insert(entry);
            flow.pending(entry.eventId(), "business-admin");
            String token = flow.token("501", 97);
            var outbound = mock(SecureOutboundClient.class);
            var requests = new AtomicInteger();
            when(outbound.execute(any())).thenAnswer(call -> {
                if (requests.incrementAndGet() == 1) {
                    throw new IllegalStateException("fixture-request-lost-before-ACK");
                }
                flow.use(token);
                String acknowledged = flow.controller.acknowledge(
                        new FlowEventAcknowledgementController.Acknowledgement("event-one")).getData();
                return new OutboundResponse(200, Map.of(), ("{\"code\":200,\"data\":\""
                        + acknowledged + "\"}").getBytes(StandardCharsets.UTF_8));
            });
            consumer.attachAck(outbound, token);
            var redis = mock(StringRedisTemplate.class);
            AtomicInteger publications = new AtomicInteger();
            when(redis.convertAndSend(anyString(), any())).thenAnswer(call -> {
                assertEquals(entry.destination(), call.getArgument(0));
                assertEquals(entry.payload(), call.getArgument(1));
                if (publications.incrementAndGet() == 1) {
                    return 0L; // nobody online; publishing is not an ACK
                }
                consumer.subscriber.onMessage(call.getArgument(1));
                return 1L;
            });
            var beans = new StaticListableBeanFactory();
            beans.addBean("redis", redis);
            var sender = new FlowRecordedEventSender(beans.getBeanProvider(StringRedisTemplate.class), outbound);
            var dispatcher = new FlowEventDeliveryDispatcher(flow.mapper, outbox, flow.properties, sender);
            TenantContextHolder.executeWithTenant(98L, () -> {
                assertEquals(1, dispatcher.dispatchAt(now));
                assertEquals("PENDING", flow.status("event-one"));
                consumer.callback.fail = true;
                assertEquals(1, dispatcher.dispatchAt(now.plusSeconds(6)));
                assertEquals(0, consumer.effects());
                consumer.callback.fail = false;
                assertEquals(1, dispatcher.dispatchAt(now.plusSeconds(17)));
                assertEquals(1, consumer.effects());
                assertEquals("PENDING", flow.status("event-one"));
                assertEquals(1, dispatcher.dispatchAt(now.plusSeconds(38)));
                assertEquals("ACKNOWLEDGED", flow.status("event-one"));
                assertEquals(0, dispatcher.dispatchAt(now.plusMinutes(10)));
                assertEquals(98L, TenantContextHolder.getTenantId());
            });
            assertEquals(2, consumer.callback.calls);
            assertEquals(1, consumer.effects());
            assertEquals(2, requests.get());
            assertEquals(4, publications.get());
        }
    }

    static FlowEventOutboxMapper prepareOutbox(DataSource source) throws Exception {
        String sql = Files.readString(Path.of("forge-server/db/migration/V1.0.2026100909__flow_terminal_event_outbox.sql"));
        new JdbcTemplate(source).execute(sql.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setMapperLocations(new FileSystemResource("forge-server/forge-framework/forge-plugin-parent/"
                + "forge-plugin-flow/src/main/resources/mapper/FlowEventOutboxMapper.xml"));
        return new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventOutboxMapper.class);
    }

    static final class ConsumerFixture implements AutoCloseable {
        final DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:roundtrip;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        final JdbcTemplate jdbc = new JdbcTemplate(source);
        final GenericApplicationContext context = new GenericApplicationContext();
        final BusinessCallback callback = new BusinessCallback(jdbc);
        final FlowEventConsumerProperties properties = new FlowEventConsumerProperties();
        final FlowEventSubscriber subscriber;

        ConsumerFixture() throws Exception {
            String sql = Files.readString(Path.of("forge-server/db/migration/V1.0.2026100910__flow_event_consumer_inbox.sql"));
            jdbc.execute(sql.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
            jdbc.execute("CREATE TABLE callback_effect (event_id VARCHAR(64) PRIMARY KEY)");
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            factory.setConfiguration(new MybatisConfiguration());
            factory.setMapperLocations(new FileSystemResource("forge-server/forge-flow/forge-flow-client/"
                    + "src/main/resources/mapper/FlowEventInboxMapper.xml"));
            var inbox = new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventInboxMapper.class);
            context.registerBean("business", BusinessCallback.class, () -> callback);
            context.refresh();
            properties.setEnabled(true);
            properties.setAcknowledgementEnabled(true);
            properties.setTenantId(97L);
            properties.setConsumerId("business-admin");
            subscriber = new FlowEventSubscriber(context);
            subscriber.setDurableConsumer(new FlowDurableEventConsumer(properties, new FlowCallbackCatalog(context),
                    inbox, source, new DataSourceTransactionManager(source)));
        }

        void attachAck(SecureOutboundClient outbound, String token) {
            var client = new FlowClientProperties();
            client.setUrl("https://flow.example");
            properties.setAcknowledgementToken(token);
            subscriber.setAcknowledgementClient(new FlowEventAcknowledgementClient(properties, client, outbound));
        }

        int effects() {
            return jdbc.queryForObject("SELECT COUNT(*) FROM callback_effect", Integer.class);
        }

        public void close() {
            context.close();
            jdbc.execute("SHUTDOWN");
        }
    }

    @FlowBind(modelKey = "approval")
    public static class BusinessCallback {
        private final JdbcTemplate jdbc;
        boolean fail;
        int calls;

        BusinessCallback(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @FlowCallback
        public void completed(FlowEventContext event) {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            calls++;
            jdbc.update("INSERT INTO callback_effect VALUES (?)", event.getEventId());
            if (fail) {
                throw new IllegalStateException("fixture-callback-failure");
            }
        }
    }
}
