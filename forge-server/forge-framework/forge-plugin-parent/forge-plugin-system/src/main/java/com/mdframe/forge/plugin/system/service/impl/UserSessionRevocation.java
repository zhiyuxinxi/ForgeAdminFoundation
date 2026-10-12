package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

/** Revokes cached authorization only after the corresponding account change commits. */
final class UserSessionRevocation {
    private UserSessionRevocation() {
    }

    static void afterCommit(Long userId, Long tenantId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    revoke(userId, tenantId);
                }
            });
        } else {
            revoke(userId, tenantId);
        }
    }

    private static void revoke(Long userId, Long tenantId) {
        if (tenantId == null) {
            StpUtil.kickout(userId);
            return;
        }
        for (String token : StpUtil.getTokenValueListByLoginId(userId)) {
            SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(token, false);
            Object snapshot = session == null ? null : session.get("loginUser");
            if (!(snapshot instanceof LoginUser user) || !Objects.equals(userId, user.getUserId())
                    || Objects.equals(tenantId, user.getTenantId())) {
                StpUtil.kickoutByTokenValue(token);
            }
        }
    }
}
