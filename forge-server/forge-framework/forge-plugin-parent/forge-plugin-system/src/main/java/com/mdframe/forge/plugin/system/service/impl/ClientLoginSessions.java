package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.starter.auth.domain.LoginResult;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/** Owns client session issuance after credential/SSO authentication, retaining the existing token policy. */
@RequiredArgsConstructor
@Slf4j
final class ClientLoginSessions {
    private static final String DEFAULT_USER_CLIENT = "pc";
    private final ISysOnlineUserService onlineUserService;
    private final AuthProperties authProperties;

    LoginResult issue(LoginUser loginUser, SysClient client, String userClient) {
        String resolvedClient = StrUtil.blankToDefault(userClient, DEFAULT_USER_CLIENT);
        if (loginUser.getTenantId() == null) {
            throw new IllegalStateException("登录用户缺少租户信息");
        }
        executeWithRequiredTenant(loginUser.getTenantId(),
                () -> handleSameAccountLogin(loginUser.getUserId(), client, resolvedClient));

        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setUserClient(resolvedClient);

        ClientTokenIssuer.login(loginUser.getUserId(), client, resolvedClient);
        SessionHelper.setLoginUser(loginUser);
        try {
            onlineUserService.addOnlineUser(StpUtil.getTokenValue(), loginUser.getUserId());
        } catch (Exception e) {
            log.error("记录在线用户失败: userId={}, client={}", loginUser.getUserId(), resolvedClient, e);
        }

        log.info("用户登录成功: username={}, userId={}, client={}, tokenTimeout={}s",
                loginUser.getUsername(),
                loginUser.getUserId(),
                resolvedClient,
                client.getTokenTimeout());

        return currentResult(loginUser);
    }

    private void handleSameAccountLogin(Long userId, SysClient client, String userClient) {
        if (!authProperties.getEnableOnlineUserManagement()) {
            return;
        }

        if (Boolean.TRUE.equals(client.getConcurrentLogin())) {
            log.debug("客户端允许并发登录: userId={}, client={}", userId, client.getClientCode());
            return;
        }

        List<String> sameClientTokens = getUserTokensByClient(userId, userClient);
        String strategy = authProperties.getSameAccountLoginStrategy();

        switch (strategy) {
            case "allow_concurrent":
                log.debug("允许同一账号并发登录: userId={}", userId);
                break;

            case "replace_old":
                try {
                    kickoutUserTokens(sameClientTokens);
                    log.info("同一账号新登录踢出旧登录: userId={}, client={}", userId, userClient);
                } catch (Exception e) {
                    log.error("踢出旧会话失败: userId={}", userId, e);
                }
                break;

            case "reject_new":
                if (CollUtil.isNotEmpty(sameClientTokens)) {
                    throw new RuntimeException("该账号已在当前客户端登录,请先退出后再登录");
                }
                log.info("同一账号拒绝新登录: userId={}, client={}", userId, userClient);
                break;

            default:
                log.warn("未知的同一账号登录策略: {}", strategy);
        }
    }

    LoginResult currentResult(LoginUser loginUser) {
        String token = StpUtil.getTokenValue();
        long tokenTimeout = StpUtil.getTokenTimeout();

        return LoginResult.builder()
                .accessToken(token)
                .expiresIn(tokenTimeout)
                .tokenType("Bearer")
                .userInfo(loginUser)
                .forcePasswordChange(Boolean.TRUE.equals(loginUser.getForcePasswordChange()))
                .build();
    }

    private List<String> getUserTokensByClient(Long userId, String userClient) {
        String resolvedClient = StrUtil.blankToDefault(userClient, DEFAULT_USER_CLIENT);
        return onlineUserService.getUserTokens(userId).stream()
                .filter(token -> isTokenBelongsToClient(token, resolvedClient))
                .toList();
    }

    private boolean isTokenBelongsToClient(String token, String userClient) {
        try {
            SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
            Object loginUserObj = tokenSession == null ? null : tokenSession.get("loginUser");
            if (!(loginUserObj instanceof LoginUser loginUser)) {
                return false;
            }
            return resolvedClientEquals(loginUser.getUserClient(), userClient);
        } catch (Exception e) {
            log.warn("过滤客户端会话失败: client={}", userClient, e);
            return false;
        }
    }

    private boolean resolvedClientEquals(String actualClient, String expectedClient) {
        return StrUtil.blankToDefault(actualClient, DEFAULT_USER_CLIENT)
                .equals(StrUtil.blankToDefault(expectedClient, DEFAULT_USER_CLIENT));
    }

    private void kickoutUserTokens(List<String> tokenValues) {
        if (CollUtil.isEmpty(tokenValues)) {
            return;
        }
        tokenValues.forEach(onlineUserService::kickoutUser);
    }

    private void executeWithRequiredTenant(Long tenantId, Runnable action) {
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        try {
            TenantContextHolder.setIgnore(false);
            TenantContextHolder.executeWithTenant(tenantId, action);
        } finally {
            if (previousIgnore == null) {
                TenantContextHolder.clearIgnore();
            } else {
                TenantContextHolder.setIgnore(previousIgnore);
            }
        }
    }
}
