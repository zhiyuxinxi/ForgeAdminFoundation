package com.mdframe.forge.plugin.ai.session.service;

import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiSessionAccessServiceTest {
    @Test
    void onlyLiveOwnedSessionCanBeResolved() {
        AiChatSessionMapper mapper = mock(AiChatSessionMapper.class);
        AiChatSession session = session();
        when(mapper.selectById("session")).thenReturn(session);
        AiSessionAccessService access = new AiSessionAccessService(mapper);
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            assertSame(session, access.requireOwned("session"));
            identity.when(SessionHelper::getUserId).thenReturn(11L);
            assertThrows(BusinessException.class, () -> access.requireOwned("session"));
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(21L);
            assertThrows(BusinessException.class, () -> access.requireOwned("session"));
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            session.setStatus("1");
            assertThrows(BusinessException.class, () -> access.requireOwned("session"));
            assertThrows(BusinessException.class, () -> access.requireOwned("missing"));
            assertThrows(BusinessException.class, () -> access.requireOwned(""));
        }
    }

    @Test
    void existingSessionCannotBeClaimedOrTouchedByAnotherOwner() {
        AiChatSessionService service = spy(new AiChatSessionService(mock(AiChatRecordService.class)));
        AiChatSession session = session();
        doReturn(session).when(service).getById("session");
        assertThrows(BusinessException.class, () -> service.getOrCreate("session", 11L, 20L, "agent", "text"));
        assertThrows(BusinessException.class, () -> service.getOrCreate("session", 10L, 21L, "agent", "text"));
        assertThrows(BusinessException.class, () -> service.getOrCreate("session", null, 20L, "agent", "text"));
        assertEquals(10L, session.getUserId());
    }

    @Test
    void newSessionRemainsBoundToAuthenticatedIdentity() {
        AiChatSessionService service = spy(new AiChatSessionService(mock(AiChatRecordService.class)));
        doReturn(null).when(service).getById("new");
        doReturn(true).when(service).save(org.mockito.ArgumentMatchers.any(AiChatSession.class));
        AiChatSession result = service.getOrCreate("new", 10L, 20L, "agent", "hello");
        assertEquals(10L, result.getUserId());
        assertEquals(20L, result.getTenantId());
        assertEquals("new", result.getId());
    }

    static AiChatSession session() {
        return AiChatSession.builder().id("session").userId(10L).tenantId(20L).status("0").build();
    }
}
