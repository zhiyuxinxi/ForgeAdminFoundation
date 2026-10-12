package com.mdframe.forge.starter.auth.service.impl;

import com.mdframe.forge.starter.auth.config.CaptchaProperties;
import com.mdframe.forge.starter.cache.service.ICacheService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CaptchaServiceImplSliderConsumptionTest {
    @Test void validOffsetSucceedsOnlyForTheRequestThatConsumesTheChallenge() {
        ICacheService cache = mock(ICacheService.class);
        when(cache.get("captcha:slider:challenge")).thenReturn("120");
        when(cache.delete("captcha:slider:challenge")).thenReturn(true, false);
        CaptchaServiceImpl service = new CaptchaServiceImpl(cache, new CaptchaProperties(),
                new MockEnvironment(), Optional.empty());
        assertThat(service.validateAndDeleteSliderCaptcha("challenge", 120)).isTrue();
        assertThat(service.validateAndDeleteSliderCaptcha("challenge", 120)).isFalse();
    }

    @Test void incorrectOffsetDoesNotConsumeAValidChallenge() {
        ICacheService cache = mock(ICacheService.class);
        when(cache.get("captcha:slider:challenge")).thenReturn("120");
        CaptchaServiceImpl service = new CaptchaServiceImpl(cache, new CaptchaProperties(),
                new MockEnvironment(), Optional.empty());
        assertThat(service.validateAndDeleteSliderCaptcha("challenge", 10)).isFalse();
        verify(cache, never()).delete(anyString());
    }
}
