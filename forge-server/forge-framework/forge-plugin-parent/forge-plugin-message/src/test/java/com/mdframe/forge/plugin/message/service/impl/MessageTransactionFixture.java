package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.event.MessageSendEvent;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.service.SysMessageReceiverService;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Spring transaction/event proxies; Mapper doubles write through the enlisted JDBC connection. */
final class MessageTransactionFixture implements AutoCloseable {
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    final MessageClient client = mock(MessageClient.class);
    final JdbcTemplate jdbc;
    final TransactionTemplate outer;
    boolean failFinalWrite;

    MessageTransactionFixture() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        var manager = new DataSourceTransactionManager(dataSource);
        outer = new TransactionTemplate(manager);
        schema();
        SysMessageMapper messages = messageMapper();
        SysMessageReceiverMapper receivers = receiverMapper();
        SysMessageSendRecordMapper records = recordMapper();
        SysMessageReceiverService recipientQuery = recipientQuery();
        context.register(Transactions.class);
        context.registerBean(SysMessageMapper.class, () -> messages);
        context.registerBean(DataSourceTransactionManager.class, () -> manager);
        context.registerBean(MessageDeliveryPersistence.class, () -> new MessageDeliveryPersistence(messages, receivers, records));
        context.registerBean(MessageServiceImpl.class, () -> new MessageServiceImpl(messages, receivers, records,
                null, client, null, null, recipientQuery, null, context,
                context.getBean(MessageDeliveryPersistence.class)));
        context.refresh();
    }

    private void schema() {
        jdbc.execute("CREATE TABLE messages (id BIGINT PRIMARY KEY, status INT, platform VARCHAR(30))");
        jdbc.execute("CREATE TABLE recipients (message_id BIGINT, user_id BIGINT, status VARCHAR(20))");
        jdbc.execute("CREATE TABLE records (message_id BIGINT, status INT, successful INT, failed INT)");
    }

    private SysMessageMapper messageMapper() {
        SysMessageMapper mapper = mock(SysMessageMapper.class);
        when(mapper.updateById(any(SysMessage.class))).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            SysMessage value = call.getArgument(0);
            int count = jdbc.update("UPDATE messages SET status=?,platform=? WHERE id=?", value.getStatus(), value.getPlatform(), value.getId());
            if (failFinalWrite) {
                throw new IllegalStateException("injected final message write failure");
            }
            return count;
        });
        return mapper;
    }

    private SysMessageReceiverMapper receiverMapper() {
        SysMessageReceiverMapper mapper = mock(SysMessageReceiverMapper.class);
        when(mapper.updateDeliveryResult(anyLong(), anyLong(), anyString(), any(), any(), any(), any())).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return jdbc.update("UPDATE recipients SET status=? WHERE message_id=? AND user_id=?",
                    call.getArgument(2), call.getArgument(0), call.getArgument(1));
        });
        return mapper;
    }

    private SysMessageSendRecordMapper recordMapper() {
        SysMessageSendRecordMapper mapper = mock(SysMessageSendRecordMapper.class);
        when(mapper.insert(any(SysMessageSendRecord.class))).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            SysMessageSendRecord value = call.getArgument(0);
            return jdbc.update("INSERT INTO records VALUES (?,?,?,?)", value.getMessageId(), value.getStatus(),
                    value.getSuccessCount(), value.getFailCount());
        });
        return mapper;
    }

    private SysMessageReceiverService recipientQuery() {
        SysMessageReceiver first = new SysMessageReceiver();
        first.setUserId(10L);
        SysMessageReceiver second = new SysMessageReceiver();
        second.setUserId(11L);
        SysMessageReceiverService service = mock(SysMessageReceiverService.class);
        LambdaQueryChainWrapper<SysMessageReceiver> query = mock(LambdaQueryChainWrapper.class);
        when(service.lambdaQuery()).thenReturn(query);
        when(query.eq(any(SFunction.class), any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(first, second));
        return service;
    }

    void channelResult(boolean failure) {
        when(client.sendToRecipients(anyString(), any())).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            if (failure) {
                throw new IllegalStateException("injected provider failure");
            }
            return new MessageChannel.ChannelSendResult("request", List.of(
                    MessageChannel.RecipientDeliveryResult.sent(10L, "sent"),
                    MessageChannel.RecipientDeliveryResult.failed(11L, "unavailable", "retry later")), "platform");
        });
    }

    void publish(boolean collaboration, boolean rollback) {
        outer.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO messages VALUES (7,0,NULL)");
            jdbc.update("INSERT INTO recipients VALUES (7,10,'PENDING'),(7,11,'PENDING')");
            SysMessage message = new SysMessage();
            message.setId(7L);
            message.setTenantId(20L);
            MessageSendRequestDTO request = new MessageSendRequestDTO();
            String channel = collaboration ? "COLLABORATION" : "WEB";
            request.setChannel(channel);
            context.publishEvent(new MessageSendEvent(message, request, "title", "body", channel, 20L, 2, collaboration));
            org.junit.jupiter.api.Assertions.assertEquals(0, count("SELECT COUNT(*) FROM records"));
            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Override
    public void close() {
        context.close();
        jdbc.execute("DROP ALL OBJECTS");
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
    }
}
