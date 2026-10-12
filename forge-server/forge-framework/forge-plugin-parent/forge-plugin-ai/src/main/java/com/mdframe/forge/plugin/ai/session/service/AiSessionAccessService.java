package com.mdframe.forge.plugin.ai.session.service;

import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves persisted ownership before an engine request can read or change session state. */
@Service
@RequiredArgsConstructor
public class AiSessionAccessService {
    private final AiChatSessionMapper sessionMapper;

    public AiChatSession requireOwned(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw denied();
        }
        AiChatSession session = sessionMapper.selectById(sessionId);
        requireOwner(session, SessionHelper.getUserId(), SessionHelper.getTenantId());
        return session;
    }

    public static void requireOwner(AiChatSession session, Long userId, Long tenantId) {
        if (session == null || !"0".equals(session.getStatus())) {
            throw denied();
        }
        requireSameOwner(userId, tenantId, session.getUserId(), session.getTenantId());
    }

    public static void requireSameOwner(Long userId, Long tenantId, Long ownerId, Long ownerTenantId) {
        if (userId == null || tenantId == null || !userId.equals(ownerId) || !tenantId.equals(ownerTenantId)) {
            throw denied();
        }
    }

    private static BusinessException denied() {
        return new BusinessException(UiMessages.label("会话不存在或无权访问", "Session does not exist or access is denied"));
    }
}
