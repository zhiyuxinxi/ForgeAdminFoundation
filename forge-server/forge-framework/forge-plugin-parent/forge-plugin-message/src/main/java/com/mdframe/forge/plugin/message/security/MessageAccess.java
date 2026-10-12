package com.mdframe.forge.plugin.message.security;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import java.util.Objects;

/** Explicit boundaries for message management and a recipient's own inbox. */
public final class MessageAccess {
    public static final String QUERY = "message:manage:query";
    public static final String SEND = "message:manage:send";

    private MessageAccess() {
    }

    public static Long requirePermission(String permission) {
        Long tenantId = requireCurrentUser(SessionHelper.getUserId());
        if (!SessionHelper.hasPermission(permission)) {
            throw denied();
        }
        return tenantId;
    }

    public static Long requireCurrentUser(Long userId) {
        Long tenantId = SessionHelper.getTenantId();
        Long activeTenant = TenantContextHolder.getTenantId();
        if (userId == null || !Objects.equals(userId, SessionHelper.getUserId()) || tenantId == null
                || (activeTenant != null && !tenantId.equals(activeTenant))) {
            throw denied();
        }
        return tenantId;
    }

    private static BusinessException denied() {
        return new BusinessException(403, UiMessages.label("无权访问此消息操作", "Access to this message operation is denied"));
    }
}
