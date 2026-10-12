package com.mdframe.forge.starter.auth.listener;

import cn.dev33.satoken.listener.SaTokenListenerForSimple;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketSessions;
import org.springframework.beans.factory.ObjectProvider;

/** Consume the same token-specific revocation events as the HTTP authentication layer. */
public class WebSocketTokenRevocationListener extends SaTokenListenerForSimple {

    private final ObjectProvider<AuthenticatedWebSocketSessions> sessions;

    public WebSocketTokenRevocationListener(ObjectProvider<AuthenticatedWebSocketSessions> sessions) {
        this.sessions = sessions;
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        revoke(loginType, tokenValue);
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        revoke(loginType, tokenValue);
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        revoke(loginType, tokenValue);
    }

    private void revoke(String loginType, String tokenValue) {
        if (StpUtil.getLoginType().equals(loginType)) {
            sessions.ifAvailable(registry -> registry.revoke(tokenValue));
        }
    }
}
