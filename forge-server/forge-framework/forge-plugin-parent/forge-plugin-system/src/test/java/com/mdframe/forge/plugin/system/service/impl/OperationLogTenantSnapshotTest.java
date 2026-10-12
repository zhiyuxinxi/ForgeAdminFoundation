package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysLoginLogMapper;
import com.mdframe.forge.plugin.system.mapper.SysOperationLogMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.log.aspect.OperationLogAspect;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.handler.DefaultTenantLineHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationLogTenantSnapshotTest {
    private final SysOperationLogMapper logs = mock(SysOperationLogMapper.class);
    private final SysUserMapper users = mock(SysUserMapper.class);
    private final SystemLogServiceImpl service = new SystemLogServiceImpl(
            logs, mock(SysLoginLogMapper.class), users);
    private final Queue<Runnable> pending = new ArrayDeque<>();

    @AfterEach
    void clearContexts() {
        RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.clear();
    }

    @Test
    void requestIdentitySurvivesSessionChangeAndAsyncWorkerContext() throws Throwable {
        LoginUser user = user();
        ProceedingJoinPoint call = requestCall();
        when(call.proceed()).thenAnswer(invocation -> {
            user.setTenantId(303L);
            user.setUsername("later-name");
            return "ok";
        });
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            assertEquals("ok", aspect().around(call));
        }
        verifyNoInteractions(logs, users);
        TenantContextHolder.setTenantId(909L);
        when(logs.insert(any(SysOperationLog.class))).thenAnswer(invocation -> {
            assertEquals(202L, TenantContextHolder.getTenantId());
            return 1;
        });
        pending.remove().run();
        SysOperationLog saved = savedLog();
        assertEquals(202L, saved.getTenantId());
        assertEquals(7L, saved.getUserId());
        assertEquals("request-user", saved.getUsername());
        assertEquals("Request Name", saved.getOperatorName());
        assertEquals(909L, TenantContextHolder.getTenantId());
        verifyNoInteractions(users);
    }

    @Test
    void failedRequestStillPersistsItsOriginalIdentityAndFailure() throws Throwable {
        ProceedingJoinPoint call = requestCall();
        IllegalStateException failure = new IllegalStateException("operation failed");
        when(call.proceed()).thenThrow(failure);
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(user());
            assertSame(failure, assertThrows(IllegalStateException.class, () -> aspect().around(call)));
        }
        pending.remove().run();
        SysOperationLog saved = savedLog();
        assertEquals(202L, saved.getTenantId());
        assertEquals(0, saved.getOperationStatus());
        assertEquals("operation failed", saved.getErrorMsg());
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void displayEnrichmentCannotReplaceEventTenantOrExistingName() {
        SysUser account = new SysUser();
        account.setTenantId(101L);
        account.setUsername("home-user");
        account.setRealName("Current Account Name");
        when(users.selectById(7L)).thenReturn(account);
        OperationLogInfo event = event();
        event.setOperatorName(null);
        service.saveOperationLog(event);
        SysOperationLog saved = savedLog();
        assertEquals(202L, saved.getTenantId());
        assertEquals("request-user", saved.getUsername());
        assertEquals("Current Account Name", saved.getOperatorName());
    }

    @Test
    void missingEventTenantCannotBecomeHomeTenantOrWorkerTenant() {
        SysUser account = new SysUser();
        account.setTenantId(101L);
        when(users.selectById(7L)).thenReturn(account);
        OperationLogInfo event = event();
        event.setTenantId(null);
        event.setOperatorName(null);
        TenantContextHolder.setTenantId(909L);
        TenantProperties properties = new TenantProperties();
        properties.setStrictMode(true);
        DefaultTenantLineHandler tenantGuard = new DefaultTenantLineHandler(properties);
        when(logs.insert(any(SysOperationLog.class))).thenAnswer(invocation -> {
            SysOperationLog saved = invocation.getArgument(0);
            assertNull(saved.getTenantId());
            assertNull(TenantContextHolder.getTenantId());
            return tenantGuard.getTenantId();
        });
        assertDoesNotThrow(() -> service.saveOperationLog(event));
        assertNull(event.getTenantId());
        assertEquals(909L, TenantContextHolder.getTenantId());
        verify(logs).insert(any(SysOperationLog.class));
    }

    @Test
    void databaseFailureRestoresWorkerContextWithoutReassigningEvent() {
        TenantContextHolder.setTenantId(909L);
        when(logs.insert(any(SysOperationLog.class))).thenThrow(new IllegalStateException("database unavailable"));
        OperationLogInfo event = event();
        assertDoesNotThrow(() -> service.saveOperationLog(event));
        assertEquals(202L, event.getTenantId());
        assertEquals(909L, TenantContextHolder.getTenantId());
    }

    @Test
    void anonymousRequestDoesNotBorrowWorkerIdentity() throws Throwable {
        ProceedingJoinPoint call = requestCall();
        when(call.proceed()).thenReturn("ok");
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(null);
            aspect().around(call);
        }
        TenantContextHolder.setTenantId(909L);
        pending.remove().run();
        SysOperationLog saved = savedLog();
        assertNull(saved.getUserId());
        assertNull(saved.getTenantId());
        verifyNoInteractions(users);
    }

    private OperationLogAspect aspect() {
        return new OperationLogAspect(service, new LogProperties(), pending::add, mock(IApiConfigManager.class));
    }

    private ProceedingJoinPoint requestCall() throws NoSuchMethodException {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/system/user/edit")));
        ProceedingJoinPoint call = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(call.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(Endpoint.class.getDeclaredMethod("edit"));
        when(call.getTarget()).thenReturn(new Endpoint());
        return call;
    }

    private SysOperationLog savedLog() {
        ArgumentCaptor<SysOperationLog> saved = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(logs).insert(saved.capture());
        return saved.getValue();
    }

    private static LoginUser user() {
        LoginUser user = new LoginUser();
        user.setUserId(7L);
        user.setTenantId(202L);
        user.setUsername("request-user");
        user.setRealName("Request Name");
        return user;
    }

    private static OperationLogInfo event() {
        OperationLogInfo event = new OperationLogInfo();
        event.setUserId(7L);
        event.setTenantId(202L);
        event.setUsername("request-user");
        event.setOperatorName("Request Name");
        return event;
    }

    private static class Endpoint {
        @OperationLog(module = "system", saveRequestParams = false, saveResponseResult = false)
        public void edit() {
            // Marker for the real aspect invocation.
        }
    }
}
