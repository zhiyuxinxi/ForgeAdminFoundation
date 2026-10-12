package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.controller.MessageController;
import com.mdframe.forge.plugin.message.domain.dto.MessageManageQueryDTO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.mapper.SysMessageMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageReceiverMapper;
import com.mdframe.forge.plugin.message.mapper.SysMessageSendRecordMapper;
import com.mdframe.forge.plugin.message.security.MessageAccess;
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

class MessageManagementAccessTest {
    private final SysMessageMapper messages = mock(SysMessageMapper.class);
    private final SysMessageReceiverMapper receivers = mock(SysMessageReceiverMapper.class);
    private final SysMessageSendRecordMapper records = mock(SysMessageSendRecordMapper.class);
    private final MessageManageServiceImpl management = new MessageManageServiceImpl(messages, receivers, records);
    private final LoginUser user = new LoginUser();
    private MockedStatic<SessionHelper> identity;

    @BeforeEach
    void login() {
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
    void ordinaryUserCannotListOrReadManagementDetailsBeforeAnyQuery() {
        assertThrows(BusinessException.class, () -> management.pageMessages(new MessageManageQueryDTO(), 1, 10));
        assertThrows(BusinessException.class, () -> management.getMessageDetail(7L));
        verifyNoInteractions(messages, receivers, records);
    }

    @Test
    void explicitlyGrantedManagerCanListAndInspectTheSameTenant() {
        user.setPermissions(Set.of(MessageAccess.QUERY));
        when(messages.selectManagementPage(any(Page.class), eq(20L), any(), isNull(), isNull())).thenReturn(new Page<SysMessage>());
        assertEquals(0, management.pageMessages(new MessageManageQueryDTO(), 1, 10).getTotal());
        SysMessage message = new SysMessage();
        message.setId(7L);
        message.setTenantId(20L);
        when(messages.selectById(7L)).thenReturn(message);
        when(receivers.selectReceiversWithUserInfo(7L)).thenReturn(List.of());
        assertSame(message, management.getMessageDetail(7L).getMessage());
    }

    @Test
    void managementPermissionDoesNotReadAnotherTenantsRecipients() {
        user.setPermissions(Set.of(MessageAccess.QUERY));
        SysMessage message = new SysMessage();
        message.setTenantId(99L);
        when(messages.selectById(7L)).thenReturn(message);
        assertNull(management.getMessageDetail(7L));
        verifyNoInteractions(receivers, records);
    }

    @Test
    void httpSendRequiresItsOwnPermissionWhileInternalSenderContractIsUntouched() {
        MessageService sender = mock(MessageService.class);
        MessageController controller = new MessageController(sender);
        MessageSendRequestDTO request = new MessageSendRequestDTO();
        user.setPermissions(Set.of(MessageAccess.QUERY));
        assertThrows(BusinessException.class, () -> controller.send(request));
        verifyNoInteractions(sender);
        user.setPermissions(Set.of(MessageAccess.SEND));
        controller.send(request);
        verify(sender).send(request);
    }

    @Test
    void defaultAdministratorStillWorksButMissingTenantDoesNot() {
        user.setUserType(0);
        assertEquals(20L, MessageAccess.requirePermission(MessageAccess.QUERY));
        user.setTenantId(null);
        assertThrows(BusinessException.class, () -> management.getMessageDetail(7L));
        verifyNoInteractions(messages);
    }
}
