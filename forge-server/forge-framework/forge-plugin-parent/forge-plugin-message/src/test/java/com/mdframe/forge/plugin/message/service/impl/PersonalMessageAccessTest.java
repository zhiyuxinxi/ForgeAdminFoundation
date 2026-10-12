package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.plugin.message.controller.MessageController;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageReceiver;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonalMessageAccessTest {
    private final SysMessageMapper messages = mock(SysMessageMapper.class);
    private final SysMessageReceiverMapper receivers = mock(SysMessageReceiverMapper.class);
    private final MessageServiceImpl service = new MessageServiceImpl(messages, receivers,
            null, null, null, null, null, null, null, null, null);
    private MockedStatic<SessionHelper> identity;

    @BeforeEach
    void login() {
        LoginUser user = new LoginUser();
        user.setUserId(10L);
        user.setTenantId(20L);
        user.setUserType(2);
        user.setPermissions(Set.of());
        identity = mockStatic(SessionHelper.class, CALLS_REAL_METHODS);
        identity.when(SessionHelper::getLoginUser).thenReturn(user);
    }

    @AfterEach
    void logout() {
        identity.close();
    }

    @Test
    void nonRecipientCannotLoadMessageBodyOrImpersonateAnotherRecipient() {
        assertNull(service.getMessageDetail(7L, 10L));
        verifyNoInteractions(messages);
        assertThrows(BusinessException.class, () -> service.getMessageDetail(7L, 11L));
    }

    @Test
    void ownMessageRemainsReadableAndCanBeMarkedReadWithoutManagementPermission() {
        SysMessageReceiver receiver = recipient(20L);
        when(receivers.selectOne(any())).thenReturn(receiver);
        SysMessage message = new SysMessage();
        message.setTenantId(20L);
        message.setContent("private body");
        when(messages.selectById(7L)).thenReturn(message);
        assertEquals("private body", service.getMessageDetail(7L, 10L).getContent());
        new MessageController(service).markRead(7L);
        assertEquals(1, receiver.getReadFlag());
        verify(receivers).updateById(receiver);
    }

    @Test
    void crossTenantReceiverAndMessageAreBothRefused() {
        when(receivers.selectOne(any())).thenReturn(recipient(99L));
        assertNull(service.getMessageDetail(7L, 10L));
        verifyNoInteractions(messages);
        when(receivers.selectOne(any())).thenReturn(recipient(20L));
        SysMessage foreign = new SysMessage();
        foreign.setTenantId(99L);
        when(messages.selectById(7L)).thenReturn(foreign);
        assertNull(service.getMessageDetail(7L, 10L));
    }

    @Test
    void personalListAndReadActionsUseOnlyTheAuthenticatedUserId() {
        MessageService personal = mock(MessageService.class);
        MessageController controller = new MessageController(personal);
        controller.page(null, 1, 10);
        controller.markReadBatch(List.of(7L, 8L));
        controller.markAllRead();
        controller.getUnreadCount();
        verify(personal).pageUserMessages(10L, null, 1, 10);
        verify(personal).markReadBatch(List.of(7L, 8L), 10L);
        verify(personal).markAllRead(10L);
        verify(personal).getUnreadCount(10L);
    }

    private SysMessageReceiver recipient(Long tenantId) {
        SysMessageReceiver receiver = new SysMessageReceiver();
        receiver.setMessageId(7L);
        receiver.setUserId(10L);
        receiver.setTenantId(tenantId);
        receiver.setReadFlag(0);
        return receiver;
    }
}
