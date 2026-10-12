package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.util.SaValue2Box;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.*;
import com.mdframe.forge.starter.auth.domain.*;
import com.mdframe.forge.starter.auth.interceptor.ApiPermissionInterceptor;
import com.mdframe.forge.starter.auth.strategy.AuthStrategyFactory;
import com.mdframe.forge.starter.auth.strategy.IAuthStrategy;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual login and SSO entry points with real Sa-Token issuance; credentials and DB/cache collaborators are mocked. */
class SystemAuthServiceImplClientPolicyTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final IUserLoadService userLoader = mock(IUserLoadService.class);
    private final IClientService clients = mock(IClientService.class);
    private final ICacheService cache = mock(ICacheService.class);
    private final AuthStrategyFactory strategies = mock(AuthStrategyFactory.class);
    private final LoginCaptchaPolicyResolver captcha = mock(LoginCaptchaPolicyResolver.class);
    private final AuthProperties auth = new AuthProperties();
    private final ISysOnlineUserService online = mock(ISysOnlineUserService.class);
    private final SaTokenConfig defaults = new SaTokenConfig().setTimeout(7200).setActiveTimeout(-1)
            .setIsConcurrent(true).setIsShare(false).setIsReadCookie(false);
    private final SysClient pc = spy(client("pc", 300, 30, false, false));
    private final SysClient app = client("app", 600, 90, true, true);
    private SaTokenConfig previousDefaults;
    private StpLogic previousLogic;
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private SaTokenDaoDefaultImpl dao;
    private StpLogic shared;
    private AnnotationConfigApplicationContext context;
    private SystemAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        previousDefaults = SaManager.getConfig();
        previousLogic = StpUtil.getStpLogic();
        previousContext = SaManager.getSaTokenContext();
        previousDao = SaManager.getSaTokenDao();
        SaManager.setConfig(defaults);
        dao = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(dao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        shared = new StpLogic("auth011-" + UUID.randomUUID());
        StpUtil.setStpLogic(shared);
        context = new AnnotationConfigApplicationContext();
        context.registerBean("runtimeSaTokenConfiguration", SaTokenConfig.class, () -> defaults);
        context.registerBean(AuthProperties.class, () -> auth);
        context.registerBean(ApiPermissionInterceptor.class, () -> mock(ApiPermissionInterceptor.class));
        context.register(com.mdframe.forge.starter.auth.config.SaTokenConfig.class);
        context.refresh();
        auth.setEnableOnlineUserManagement(false);
        IAuthStrategy strategy = mock(IAuthStrategy.class);
        when(strategy.authenticate(any())).thenAnswer(call -> user());
        when(strategies.getStrategy("password", "pc")).thenReturn(strategy);
        when(strategies.getStrategy("password", "app")).thenReturn(strategy);
        when(captcha.resolve(anyString())).thenReturn(LoginCaptchaPolicy.builder().enableCaptcha(false).build());
        when(clients.getByCode("pc")).thenReturn(pc);
        when(clients.getByCode("app")).thenReturn(app);
        service = new SystemAuthServiceImpl(users, null, strategies, userLoader,
                online, auth, null, clients, cache, captcha, null, null);
    }

    @AfterEach
    void restore() {
        TenantContextHolder.clear();
        TenantContextHolder.clearIgnore();
        RequestContextHolder.resetRequestAttributes();
        context.close();
        StpUtil.setStpLogic(previousLogic);
        SaManager.setConfig(previousDefaults);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setSaTokenDao(previousDao);
        dao.destroy();
    }

    @Test
    void interleavedClientsRetainTheirOwnTimeoutsAndLeaveGlobalConfigurationUntouched() throws Exception {
        CountDownLatch pcPaused = new CountDownLatch(1);
        CountDownLatch resumePc = new CountDownLatch(1);
        doAnswer(call -> {
            pcPaused.countDown();
            if (!resumePc.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test rendezvous timed out");
            }
            return false;
        }).when(pc).getConcurrentLogin();
        var executor = Executors.newSingleThreadExecutor();
        try {
            var pcLogin = executor.submit(() -> {
                try {
                    return login("pc");
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            });
            assertThat(pcPaused.await(2, TimeUnit.SECONDS)).isTrue();
            LoginResult appLogin = login("app");
            assertGlobalConfiguration();
            resumePc.countDown();
            assertPolicy(pcLogin.get(3, TimeUnit.SECONDS), 300, 30);
            assertPolicy(appLogin, 600, 90);
            assertGlobalConfiguration();
        } finally {
            resumePc.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void repeatedLoginsApplyReplacementAndSharingOnlyToTheSelectedClient() {
        LoginResult firstPc = login("pc");
        LoginResult firstApp = login("app");
        LoginResult secondPc = login("pc");
        LoginResult secondApp = login("app");
        assertThat(secondPc.getAccessToken()).isNotEqualTo(firstPc.getAccessToken());
        assertThat(shared.getLoginIdByToken(firstPc.getAccessToken())).isNull();
        assertThat(shared.getLoginIdByToken(secondPc.getAccessToken())).isNotNull();
        assertThat(secondApp.getAccessToken()).isEqualTo(firstApp.getAccessToken());
        assertThat(shared.getLoginIdByToken(firstApp.getAccessToken())).isNotNull();
        assertPolicy(secondPc, 300, 30);
        assertPolicy(secondApp, 600, 90);
        assertGlobalConfiguration();
    }

    @Test
    void ssoExchangeUsesTargetClientPolicyWithoutChangingSourceSession() {
        LoginResult source = login("pc");
        SsoTicketPayload payload = new SsoTicketPayload();
        payload.setSourceToken(source.getAccessToken());
        payload.setUserId(200L);
        payload.setTenantId(7L);
        payload.setTargetClient("app");
        when(cache.get("auth:sso:ticket:test-ticket", SsoTicketPayload.class)).thenReturn(payload);
        when(cache.delete("auth:sso:ticket:test-ticket")).thenReturn(true);
        SysUser account = new SysUser();
        account.setUserStatus(1);
        account.setUsername("policy-user");
        when(users.selectById(200L)).thenReturn(account);
        when(userLoader.loadUserByUsername("policy-user", 7L)).thenReturn(user());
        SsoExchangeRequest exchange = new SsoExchangeRequest();
        exchange.setTicket("test-ticket");
        bindRequest();
        LoginResult target = service.exchangeSsoTicket(exchange);
        assertPolicy(source, 300, 30);
        assertPolicy(target, 600, 90);
        assertThat(shared.getLoginIdByToken(source.getAccessToken())).isNotNull();
        assertGlobalConfiguration();
        verify(cache).delete("auth:sso:ticket:test-ticket");
    }

    @Test
    void failedIssuanceDoesNotLeakClientPolicyIntoTheNextLogin() {
        var originalGenerator = SaStrategy.instance.createToken;
        try {
            SaStrategy.instance.createToken = (userId, loginType) -> {
                throw new IllegalStateException("controlled token generation failure");
            };
            assertThatThrownBy(() -> login("pc")).hasMessage("controlled token generation failure");
        } finally {
            SaStrategy.instance.createToken = originalGenerator;
        }
        assertGlobalConfiguration();
        assertPolicy(login("app"), 600, 90);
        assertGlobalConfiguration();
    }

    @Test
    void activityConsumerRejectsExpiredClientButPreservesUnlimitedActivity() {
        LoginResult limited = login("pc");
        app.setTokenActivityTimeout(-1L);
        LoginResult unlimited = login("app");
        long earlier = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(45);
        dao.update(shared.splicingKeyLastActiveTime(limited.getAccessToken()), new SaValue2Box(earlier, 30L).toString());
        dao.update(shared.splicingKeyLastActiveTime(unlimited.getAccessToken()), new SaValue2Box(earlier, -1L).toString());
        bindRequest();
        shared.setTokenValueToStorage(limited.getAccessToken());
        assertThatThrownBy(shared::checkLogin).isInstanceOf(NotLoginException.class);
        bindRequest();
        shared.setTokenValueToStorage(unlimited.getAccessToken());
        assertThatCode(shared::checkLogin).doesNotThrowAnyException();
        assertThat(shared.getTokenUseActiveTimeoutOrGlobalConfig(unlimited.getAccessToken())).isEqualTo(-1);
        assertGlobalConfiguration();
    }

    @Test
    void onlineReplacementFiltersClientAndRestoresIgnoredOuterTenant() {
        LoginResult oldPc = login("pc");
        LoginResult oldApp = login("app");
        auth.setEnableOnlineUserManagement(true);
        auth.setSameAccountLoginStrategy("replace_old");
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setIgnore(true);
        when(online.getUserTokens(200L)).thenAnswer(call -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            assertThat(TenantContextHolder.getIgnoreValue()).isFalse();
            return List.of(oldPc.getAccessToken(), oldApp.getAccessToken(), "missing-token");
        });
        doAnswer(call -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            assertThat(TenantContextHolder.getIgnoreValue()).isFalse();
            return null;
        }).when(online).kickoutUser(oldPc.getAccessToken());
        LoginResult next = login("pc");
        verify(online).kickoutUser(oldPc.getAccessToken());
        verify(online, never()).kickoutUser(oldApp.getAccessToken());
        verify(online, never()).kickoutUser("missing-token");
        assertThat(shared.getLoginIdByToken(oldApp.getAccessToken())).isNotNull();
        assertThat(SessionHelper.getLoginUser().getUserClient()).isEqualTo("pc");
        assertThat(next.getTokenType()).isEqualTo("Bearer");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(99L);
        assertThat(TenantContextHolder.getIgnoreValue()).isTrue();
    }

    @Test
    void onlineRejectionOnlyBlocksSameClientAndRestoresExplicitTenantMode() {
        LoginResult other = login("app");
        auth.setEnableOnlineUserManagement(true);
        auth.setSameAccountLoginStrategy("reject_new");
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setIgnore(false);
        when(online.getUserTokens(200L)).thenReturn(List.of(other.getAccessToken()));
        LoginResult first = login("pc");
        when(online.getUserTokens(200L)).thenReturn(List.of(other.getAccessToken(), first.getAccessToken()));
        clearInvocations(online);
        assertThatThrownBy(() -> login("pc")).hasMessage("该账号已在当前客户端登录,请先退出后再登录");
        verify(online, never()).addOnlineUser(anyString(), anyLong());
        verify(online, never()).kickoutUser(anyString());
        assertThat(shared.getLoginIdByToken(first.getAccessToken())).isNotNull();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(99L);
        assertThat(TenantContextHolder.getIgnoreValue()).isFalse();
    }

    @Test
    void onlineLookupFailurePropagatesAndRestoresAbsentTenantAndIgnore() {
        auth.setEnableOnlineUserManagement(true);
        auth.setSameAccountLoginStrategy("reject_new");
        IllegalStateException failure = new IllegalStateException("online-index-unavailable");
        when(online.getUserTokens(200L)).thenThrow(failure);
        assertThatThrownBy(() -> login("pc")).isSameAs(failure);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getIgnoreValue()).isNull();
        verify(online, never()).addOnlineUser(anyString(), anyLong());
    }

    @Test
    void failedOnlineRecordingKeepsIssuedSessionAndSuccessfulResult() {
        doThrow(new IllegalStateException("online-record-unavailable"))
                .when(online).addOnlineUser(anyString(), eq(200L));
        LoginResult result = login("pc");
        assertThat(String.valueOf(shared.getLoginIdByToken(result.getAccessToken()))).isEqualTo("200");
        assertThat(SessionHelper.getLoginUser().getTenantId()).isEqualTo(7L);
        assertThat(result.getUserInfo()).isSameAs(SessionHelper.getLoginUser());
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.getIgnoreValue()).isNull();
        assertPolicy(result, 300, 30);
    }

    private LoginResult login(String client) {
        bindRequest();
        LoginRequest request = new LoginRequest();
        request.setUserClient(client);
        request.setAppId(client + "-public");
        request.setAuthType("password");
        return service.login(request);
    }

    private void bindRequest() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private void assertPolicy(LoginResult result, long timeout, long activeTimeout) {
        assertThat(result.getExpiresIn()).isBetween(timeout - 2, timeout);
        assertThat(shared.getTokenTimeout(result.getAccessToken())).isBetween(timeout - 2, timeout);
        assertThat(shared.getTokenUseActiveTimeoutOrGlobalConfig(result.getAccessToken())).isEqualTo(activeTimeout);
    }

    private void assertGlobalConfiguration() {
        assertThat(SaManager.getConfig()).isSameAs(defaults);
        assertThat(defaults.getTimeout()).isEqualTo(7200);
        assertThat(defaults.getActiveTimeout()).isEqualTo(-1);
        assertThat(defaults.getIsConcurrent()).isTrue();
        assertThat(defaults.getIsShare()).isFalse();
        assertThat(defaults.getDynamicActiveTimeout()).isTrue();
        assertThat(StpUtil.getStpLogic()).isSameAs(shared);
        assertThat(SaManager.getStpLogic(shared.getLoginType())).isSameAs(shared);
    }

    private static LoginUser user() {
        LoginUser user = new LoginUser();
        user.setUserId(200L);
        user.setTenantId(7L);
        user.setUsername("policy-user");
        return user;
    }

    private static SysClient client(String code, long timeout, long activity, boolean concurrent, boolean share) {
        SysClient client = new SysClient();
        client.setClientCode(code);
        client.setStatus(1);
        client.setAppId(code + "-public");
        client.setAuthTypes("password");
        client.setTokenTimeout(timeout);
        client.setTokenActivityTimeout(activity);
        client.setConcurrentLogin(concurrent);
        client.setShareToken(share);
        return client;
    }
}
