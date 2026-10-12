package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.log.aspect.OperationLogAspect;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.log.service.ILogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationLogCaptureContractTest {
    private final ILogService logs = mock(ILogService.class);

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
        OperationAuditContext.clear();
        MDC.clear();
    }

    @Test
    void decryptedBodyNeverBecomesAnAuditFallback() throws Throwable {
        ProceedingJoinPoint call = call("/sample/update");
        when(call.proceed()).thenReturn(Map.of("safe", "ok"));
        try (MockedStatic<SessionHelper> ignored = mockStatic(SessionHelper.class)) {
            aspect().around(call);
        }
        OperationLogInfo event = saved();
        assertThat(event.getRequestParams()).contains("DECRYPTED_REQUEST_BODY_OMITTED").doesNotContain("secret-value");
        assertThat(event.getAfterData()).contains("safe").doesNotContain("secret-value", "DECRYPTED_REQUEST_BODY_OMITTED");
    }

    @Test
    void explicitAuditValuesTakePrecedenceOverRequestAndResponse() throws Throwable {
        ProceedingJoinPoint call = call("/sample/update");
        when(call.proceed()).thenAnswer(invocation -> {
            OperationAuditContext.setBeforeData(Map.of("value", "before"));
            OperationAuditContext.setAfterData(Map.of("value", "after"));
            OperationAuditContext.setDiffData(Map.of("changed", true));
            OperationAuditContext.setOperationContent("explicit content");
            return "response";
        });
        try (MockedStatic<SessionHelper> ignored = mockStatic(SessionHelper.class)) {
            aspect().around(call);
        }
        OperationLogInfo event = saved();
        assertThat(event.getBeforeData()).contains("before");
        assertThat(event.getAfterData()).contains("after").doesNotContain("response");
        assertThat(event.getDiffData()).contains("changed");
        assertThat(event.getOperationContent()).isEqualTo("explicit content");
        assertThat(OperationAuditContext.snapshot().getBeforeData()).isNull();
    }

    @Test
    void errorIsRecordedAndRethrownWithContextCleared() throws Throwable {
        ProceedingJoinPoint call = call("/sample/update");
        AssertionError failure = new AssertionError("failure");
        when(call.proceed()).thenThrow(failure);
        try (MockedStatic<SessionHelper> ignored = mockStatic(SessionHelper.class)) {
            assertThatThrownBy(() -> aspect().around(call)).isSameAs(failure);
        }
        assertThat(saved().getOperationStatus()).isZero();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(OperationAuditContext.snapshot().getBeforeData()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/login;session=x", "/oauth2/token", "/openapi/v1/capabilities/run"})
    void credentialEndpointsNeverCaptureRequestArguments(String path) throws Throwable {
        ProceedingJoinPoint call = call(path);
        when(call.proceed()).thenReturn("ok");
        assertThat(aspect().around(call)).isEqualTo("ok");
        verify(call, never()).getArgs();
        verifyNoInteractions(logs);
    }

    private OperationLogAspect aspect() {
        LogProperties properties = new LogProperties();
        properties.setEnableOperationLog(true);
        properties.setPrintOperationLog(false);
        return new OperationLogAspect(logs, properties, Runnable::run, mock(IApiConfigManager.class));
    }

    private ProceedingJoinPoint call(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint call = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Endpoint.class.getDeclaredMethod("update", Map.class));
        when(signature.getParameterNames()).thenReturn(new String[]{"body"});
        when(call.getSignature()).thenReturn(signature);
        when(call.getArgs()).thenReturn(new Object[]{Map.of("password", "secret-value")});
        return call;
    }

    private OperationLogInfo saved() {
        var capture = org.mockito.ArgumentCaptor.forClass(OperationLogInfo.class);
        verify(logs).saveOperationLog(capture.capture());
        return capture.getValue();
    }

    @ApiDecrypt
    static class Endpoint {
        @OperationLog(module = "test", type = OperationType.UPDATE, saveResponseResult = true)
        public void update(@RequestBody Map<String, Object> body) { }
    }
}
