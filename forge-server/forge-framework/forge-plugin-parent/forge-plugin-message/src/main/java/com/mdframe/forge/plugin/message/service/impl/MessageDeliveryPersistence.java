package com.mdframe.forge.plugin.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageSendRecord;
import com.mdframe.forge.plugin.message.event.MessageSendEvent;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/** Commits a channel result independently of the already committed message creation transaction. */
@Service
@RequiredArgsConstructor
public class MessageDeliveryPersistence {
    private final SysMessageMapper messageMapper;
    private final SysMessageReceiverMapper receiverMapper;
    private final SysMessageSendRecordMapper recordMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveStandard(MessageSendEvent event, MessageChannel.SendResult result) {
        SysMessageSendRecord record = record(event);
        record.setSuccessCount(result.success ? event.getReceiverCount() : 0);
        record.setFailCount(result.success ? 0 : event.getReceiverCount());
        record.setExternalId(result.externalId);
        record.setStatus(result.success ? 1 : 2);
        record.setErrorMsg(result.msg);
        persist(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int saveCollaboration(MessageSendEvent event, MessageChannel.ChannelSendResult result) {
        List<MessageChannel.RecipientDeliveryResult> deliveries = result == null || result.deliveries() == null
                ? List.of() : result.deliveries();
        SysMessageSendRecord record = record(event);
        record.setConnectionId(event.getRequest().getConnectionId());
        record.setIdempotencyKey(event.getRequest().getIdempotencyKey());
        record.setAttemptNo(1);
        record.setProviderRequestId(result == null ? null : result.providerRequestId());
        record.setPlatform(result == null ? null : result.platform());
        summarize(record, deliveries, event.getReceiverCount());
        saveRecipients(event.getMessage().getId(), deliveries);
        persist(record);
        return record.getStatus();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveFailure(MessageSendEvent event) {
        SysMessageSendRecord record = record(event);
        record.setSuccessCount(0);
        record.setFailCount(event.getReceiverCount());
        record.setStatus(2);
        record.setErrorMsg(UiMessages.label("渠道发送失败", "Message delivery failed"));
        persist(record);
    }

    private SysMessageSendRecord record(MessageSendEvent event) {
        SysMessageSendRecord record = new SysMessageSendRecord();
        record.setTenantId(event.getTenantId());
        record.setMessageId(event.getMessage().getId());
        record.setChannel(event.getChannel());
        record.setReceiverCount(event.getReceiverCount());
        record.setSendTime(LocalDateTime.now());
        return record;
    }

    private void summarize(SysMessageSendRecord record, List<MessageChannel.RecipientDeliveryResult> deliveries, int count) {
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (MessageChannel.RecipientDeliveryResult delivery : deliveries) {
            switch (delivery.status()) {
                case MessageChannel.RecipientDeliveryResult.STATUS_SENT -> sent++;
                case MessageChannel.RecipientDeliveryResult.STATUS_FAILED -> {
                    failed++;
                    if (record.getErrorMsg() == null) {
                        record.setErrorMsg(StrUtil.blankToDefault(delivery.errorCode(), delivery.errorMessage()));
                    }
                }
                default -> skipped++;
            }
        }
        record.setSuccessCount(sent);
        record.setFailCount(failed);
        record.setStatus(resolveCollaborationStatus(count, sent, failed, skipped));
    }

    private void saveRecipients(Long messageId, List<MessageChannel.RecipientDeliveryResult> deliveries) {
        LocalDateTime now = LocalDateTime.now();
        for (MessageChannel.RecipientDeliveryResult delivery : deliveries) {
            LocalDateTime next = MessageChannel.RecipientDeliveryResult.STATUS_FAILED.equals(delivery.status())
                    ? now.plusMinutes(1) : null;
            requireWrite(receiverMapper.updateDeliveryResult(messageId, delivery.userId(), delivery.status(),
                    delivery.externalId(), delivery.errorCode(), now, next));
        }
    }

    private void persist(SysMessageSendRecord record) {
        requireWrite(recordMapper.insert(record));
        SysMessage message = new SysMessage();
        message.setId(record.getMessageId());
        message.setTenantId(record.getTenantId());
        message.setStatus(record.getStatus());
        message.setPlatform(record.getPlatform());
        requireWrite(messageMapper.updateById(message));
    }

    private void requireWrite(int affected) {
        if (affected != 1) {
            throw new IllegalStateException("Message delivery result was not persisted exactly once");
        }
    }

    static int resolveCollaborationStatus(int receiverCount, int sentCount, int failedCount, int skippedCount) {
        if (receiverCount <= 0) {
            return 1;
        }
        if (sentCount <= 0) {
            return 2;
        }
        return sentCount < receiverCount || failedCount > 0 || skippedCount > 0 ? 3 : 1;
    }
}
