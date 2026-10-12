package com.mdframe.forge.plugin.system.strategy;

import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UsernamePasswordCaptchaAuthStrategyServerChallengeTest {
    private final ICaptchaService captcha = mock(ICaptchaService.class);
    private final UsernamePasswordCaptchaAuthStrategy strategy = new UsernamePasswordCaptchaAuthStrategy();

    @Test void clientSuccessMarkerAndMalformedOffsetsCannotAuthenticate() {
        ReflectionTestUtils.setField(strategy, "captchaService", captcha);
        for (String value : new String[]{"verified", "-1", "99999999999999", "1.2", null}) {
            assertThat(validate("challenge", value)).isFalse();
        }
        assertThat(validate(null, "120")).isFalse();
        verifyNoInteractions(captcha);
    }

    @Test void onlySuccessfullyConsumedServerChallengesAreAccepted() {
        ReflectionTestUtils.setField(strategy, "captchaService", captcha);
        when(captcha.validateAndDeleteSliderCaptcha("challenge", 120)).thenReturn(true, false);
        assertThat(validate("challenge", "120")).isTrue();
        assertThat(validate("challenge", "120")).isFalse();
        verify(captcha, times(2)).validateAndDeleteSliderCaptcha("challenge", 120);
    }

    @Test void captchaStrategyCannotSkipTheChallengeWhenGlobalPolicyIsFalseOrNull() {
        for (Boolean enabled : new Boolean[]{false, null}) {
            IUserLoadService loader = configureGraphicalPolicy(enabled);
            assertThatThrownBy(() -> strategy.authenticate(graphicalRequest()))
                    .isInstanceOf(BusinessException.class).hasMessage("验证码错误或已过期");
            verify(loader).validateCode("challenge", "answer");
            verify(loader, never()).loadUserByUsername(anyString(), any());
        }
    }

    @Test void validChallengeStillContinuesToCredentialsWhenGlobalPolicyIsFalseOrNull() {
        for (Boolean enabled : new Boolean[]{false, null}) {
            IUserLoadService loader = configureGraphicalPolicy(enabled);
            when(loader.validateCode("challenge", "answer")).thenReturn(true);
            BusinessException reachedCredentials = new BusinessException("credentials-boundary");
            when(loader.loadUserByUsername("user", null)).thenThrow(reachedCredentials);
            assertThatThrownBy(() -> strategy.authenticate(graphicalRequest())).isSameAs(reachedCredentials);
            verify(loader).validateCode("challenge", "answer");
            verify(loader).loadUserByUsername("user", null);
        }
    }

    private IUserLoadService configureGraphicalPolicy(Boolean enabled) {
        LoginCaptchaPolicyResolver policies = mock(LoginCaptchaPolicyResolver.class);
        when(policies.resolve("pc")).thenReturn(LoginCaptchaPolicy.builder()
                .enableCaptcha(enabled).captchaType("graphical").build());
        IUserLoadService loader = mock(IUserLoadService.class);
        ReflectionTestUtils.setField(strategy, "captchaPolicyResolver", policies);
        ReflectionTestUtils.setField(strategy, "userLoadService", loader);
        return loader;
    }

    private LoginRequest graphicalRequest() {
        LoginRequest request = new LoginRequest();
        request.setUserClient("pc");
        request.setUsername("user");
        request.setPassword("test-password");
        request.setCodeKey("challenge");
        request.setCode("answer");
        return request;
    }

    private Boolean validate(String key, String code) {
        LoginRequest request = new LoginRequest();
        request.setCodeKey(key);
        request.setCode(code);
        return ReflectionTestUtils.invokeMethod(strategy, "validateCaptchaByType", request, "slider");
    }
}
