package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.auth.strategy.AuthStrategyFactory;
import com.mdframe.forge.starter.auth.strategy.IAuthStrategy;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemAuthServiceImplEntrySecurityTest {
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final ICaptchaService captcha = mock(ICaptchaService.class);
    private final IUserLoadService userLoader = mock(IUserLoadService.class);
    private final AuthStrategyFactory strategies = mock(AuthStrategyFactory.class);
    private final IClientService clients = mock(IClientService.class);
    private final LoginCaptchaPolicyResolver captchaPolicies = mock(LoginCaptchaPolicyResolver.class);
    private final SystemAuthServiceImpl service = new SystemAuthServiceImpl(users, captcha, strategies,
            userLoader, null, new AuthProperties(), null, clients, null, captchaPolicies, null, null);

    @AfterEach void resetLocale() { LocaleContextHolder.resetLocaleContext(); }

    @Test void recoveryCannotWriteOrEnumerateAccountsEvenWithCorrectCaptcha() {
        for (String username : new String[]{"existing-admin", "missing-user"}) {
            assertThatThrownBy(() -> service.resetPassword(username, "replacement", "correct", "challenge"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("联系管理员");
        }
        verifyNoInteractions(users, userLoader, captcha);
    }

    @Test void recoveryDenialUsesTheRequestedLocale() {
        LocaleContextHolder.setLocale(Locale.US);
        assertThatThrownBy(() -> service.resetPassword("user", "replacement", null, null))
                .hasMessage("Self-service password recovery is unavailable. Please contact an administrator to reset your password.");
        verifyNoInteractions(users, userLoader, captcha);
    }

    @Test void loginCannotSelectPasswordToSkipTheServerCaptchaPolicy() {
        SysClient client = client("password");
        when(clients.getByCode("pc")).thenReturn(client);
        when(captchaPolicies.resolve("pc")).thenReturn(policy(true));
        IAuthStrategy requiredStrategy = mock(IAuthStrategy.class);
        when(strategies.getStrategy("password_captcha", "pc")).thenReturn(requiredStrategy);
        LoginRequest request = request("password");
        BusinessException captchaRequired = new BusinessException("server-captcha-required");
        when(requiredStrategy.authenticate(request)).thenThrow(captchaRequired);

        assertThatThrownBy(() -> service.login(request)).isSameAs(captchaRequired);
        verify(strategies).getStrategy("password_captcha", "pc");
        verify(strategies, never()).getStrategy("password", "pc");
        verifyNoInteractions(users, userLoader);
    }

    @Test void explicitlyDisabledCaptchaPreservesAllowedPasswordLogin() {
        assertThat(service.resolveLoginAuthType("password", client("password"), policy(false)))
                .isEqualTo("password");
    }

    @Test void onlyConfiguredAlternativeMethodsAreAllowed() {
        assertThat(service.resolveLoginAuthType("oauth2", client("password, oauth2"), policy(true)))
                .isEqualTo("oauth2");
        assertThatThrownBy(() -> service.resolveLoginAuthType("phone_captcha", client("password"), policy(true)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resolveLoginAuthType("password", client("oauth2"), policy(true)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resolveLoginAuthType("password", client(""), policy(true)))
                .isInstanceOf(BusinessException.class);
    }

    @Test void deniedMethodIsRejectedBeforeAnyStrategyRunsAndIsLocalized() {
        LocaleContextHolder.setLocale(Locale.US);
        when(clients.getByCode("pc")).thenReturn(client("password"));
        when(captchaPolicies.resolve("pc")).thenReturn(policy(true));
        assertThatThrownBy(() -> service.login(request("oauth2")))
                .hasMessage("This sign-in method is not allowed for this client");
        verifyNoInteractions(strategies, userLoader, users);
    }

    private LoginRequest request(String authType) {
        LoginRequest request = new LoginRequest();
        request.setUserClient("pc");
        request.setAppId("public-app");
        request.setAuthType(authType);
        return request;
    }

    private SysClient client(String allowedMethods) {
        SysClient client = new SysClient();
        client.setClientCode("pc");
        client.setAppId("public-app");
        client.setStatus(1);
        client.setAuthTypes(allowedMethods);
        return client;
    }

    private LoginCaptchaPolicy policy(boolean enabled) {
        return LoginCaptchaPolicy.builder().enableCaptcha(enabled).captchaType("graphical").build();
    }
}
