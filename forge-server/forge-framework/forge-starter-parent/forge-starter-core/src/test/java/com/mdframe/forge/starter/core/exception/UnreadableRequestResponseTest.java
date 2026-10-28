package com.mdframe.forge.starter.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UnreadableRequestResponseTest {
    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void unreadableBodyReturnsSafeDisplayAndTraceWithoutInternalDetails() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        HttpServletRequest request = request();
        var response = new GlobalExceptionHandler().handleUnreadableRequest(
                mock(HttpMessageNotReadableException.class), request);
        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("请求参数格式有误，请检查后重试");
        assertThat(response.getTraceId()).isEqualTo("trace-example-1");
    }

    @Test
    void ioFailureLocalizesOnlyThePublicMessage() {
        LocaleContextHolder.setLocale(Locale.US);
        var response = new GlobalExceptionHandler().handleUnreadableRequest(
                new IOException("internal-path-and-driver-details"), request());
        assertThat(response.getMessage()).isEqualTo("The request format is invalid. Check it and try again");
        assertThat(response.getMessage()).doesNotContain("internal-path");
        assertThat(response.getTraceId()).isEqualTo("trace-example-1");
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/example/record");
        when(request.getHeader("traceId")).thenReturn("trace-example-1");
        return request;
    }
}
