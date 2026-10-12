package com.mdframe.forge.flow.client.event;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.flow.client.annotation.FlowBind;
import com.mdframe.forge.flow.client.annotation.FlowCallback;
import com.mdframe.forge.flow.client.annotation.FlowComplete;
import com.mdframe.forge.flow.client.annotation.FlowEventContext;
import com.mdframe.forge.flow.client.mapper.FlowEventInboxMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class FlowInboxFixture implements AutoCloseable {
    final DriverManagerDataSource source = new DriverManagerDataSource(
            "jdbc:h2:mem:inbox" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
    final DataSourceTransactionManager transactions = new DataSourceTransactionManager(source);
    final JdbcTemplate jdbc = new JdbcTemplate(source);
    final GenericApplicationContext context = new GenericApplicationContext();
    final FlowEventConsumerProperties properties = properties();
    final FlowEventInboxMapper mapper;

    FlowInboxFixture() throws Exception {
        String migration = Files.readString(Path.of("forge-server/db/migration/"
                + "V1.0.2026100910__flow_event_consumer_inbox.sql"), StandardCharsets.UTF_8);
        jdbc.execute(migration.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", ""));
        jdbc.execute("CREATE TABLE callback_effect (callback_name VARCHAR(80), event_id VARCHAR(64), "
                + "PRIMARY KEY(callback_name,event_id))");
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(new MybatisConfiguration());
        factory.setMapperLocations(new FileSystemResource("forge-server/forge-flow/forge-flow-client/"
                + "src/main/resources/mapper/FlowEventInboxMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(FlowEventInboxMapper.class);
    }

    Callback add(String name) {
        Callback callback = new Callback(jdbc, name);
        addBean(name, callback);
        return callback;
    }

    void addBean(String name, Object callback) {
        ProxyFactory proxy = new ProxyFactory(callback);
        proxy.setInterfaces(CallbackContract.class);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        if (callback instanceof Callback tracked) {
            proxy.addAdvice((MethodInterceptor) invocation -> {
                tracked.advice.incrementAndGet();
                return invocation.proceed();
            });
        }
        Object bean = proxy.getProxy();
        context.registerBean(name, Object.class, () -> bean);
    }

    FlowDurableEventConsumer consumer() {
        context.refresh();
        return new FlowDurableEventConsumer(properties, new FlowCallbackCatalog(context), mapper, source, transactions);
    }

    int effects() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM callback_effect", Integer.class);
    }

    int completed() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_flow_event_inbox WHERE status='COMPLETED'", Integer.class);
    }

    static FlowEventContext event() {
        return FlowEventContext.builder().eventId("event-one").event(FlowCallback.ON_COMPLETED)
                .tenantId(97L).processInstanceId("instance-one").processDefKey("approval")
                .businessKey("sample:one").build();
    }

    private static FlowEventConsumerProperties properties() {
        var properties = new FlowEventConsumerProperties();
        properties.setEnabled(true);
        properties.setConsumerId("business-admin");
        properties.setTenantId(97L);
        return properties;
    }

    @Override
    public void close() {
        context.close();
        jdbc.execute("SHUTDOWN");
    }

    public interface CallbackContract {
        void finish(FlowEventContext context);
    }

    @FlowBind(modelKey = "approval")
    @FlowComplete(onApproved = "finish")
    public static class Callback implements CallbackContract {
        final JdbcTemplate jdbc;
        final String name;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger advice = new AtomicInteger();
        volatile boolean fail;
        CountDownLatch entered;
        CountDownLatch release;

        Callback(JdbcTemplate jdbc, String name) {
            this.jdbc = jdbc;
            this.name = name;
        }

        @Override
        @FlowCallback
        @Transactional
        public void finish(FlowEventContext context) {
            calls.incrementAndGet();
            jdbc.update("INSERT INTO callback_effect VALUES (?,?)", name, context.getEventId());
            awaitRelease();
            if (fail) {
                throw new IllegalStateException("fixture-callback-failure");
            }
        }

        private void awaitRelease() {
            if (entered == null) {
                return;
            }
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("fixture-latch-timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
    }
}
