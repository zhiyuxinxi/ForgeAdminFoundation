package com.mdframe.forge.plugin.task.service;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.task.controller.BusinessTaskController;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.interceptor.TenantInterceptor;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TaskHttpTenantIdentityTest {
    private final TenantInterceptor interceptor = new TenantInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/task/assigned");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void superAdministratorRetainsTrustedTenantForStrictTaskActorAndClearsAfterRequest() throws Exception {
        request.addHeader("X-Tenant-Id", "999");
        withUser(user(7L, 0), () -> {
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            assertThat(TaskInputs.actor().tenantId()).isEqualTo(7L);
            assertThat(TaskInputs.actor().userId()).isEqualTo(10L);
        });
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    void normalMemberKeepsSqlIsolationAndIgnoresUntrustedTenantHeader() throws Exception {
        request.addHeader("X-Tenant-Id", "999");
        withUser(user(7L, 2), () -> {
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            assertThat(TaskInputs.actor().tenantId()).isEqualTo(7L);
        });
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void changedTenantStillFailsStrictTaskIdentityCheckAndCleanupRunsOnFailure() throws Exception {
        assertThatThrownBy(() -> withUser(user(7L, 0), () -> {
            TenantContextHolder.setTenantId(8L);
            TaskInputs.actor();
        })).isInstanceOf(RuntimeException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    @Test
    void administratorWithoutTenantCannotAcquireTaskIdentityFromHeader() throws Exception {
        request.addHeader("X-Tenant-Id", "7");
        withUser(user(null, 0), () -> {
            assertThat(TenantContextHolder.getTenantId()).isNull();
            assertThatThrownBy(TaskInputs::actor).isInstanceOf(RuntimeException.class);
        });
    }

    @Test
    void anonymousRequestCannotAcquireTaskIdentityFromHeader() throws Exception {
        request.addHeader("X-Tenant-Id", "7");
        withUser(null, () -> {
            assertThat(TenantContextHolder.getTenantId()).isNull();
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            assertThatThrownBy(TaskInputs::actor).isInstanceOf(RuntimeException.class);
        });
    }

    private void withUser(LoginUser user, Runnable assertion) throws Exception {
        var handler = new HandlerMethod(new BusinessTaskController(mock(BusinessTaskEngine.class)),
                "assigned", TaskState.class, Long.class, int.class);
        var apiConfig = mock(IApiConfigManager.class);
        try (var beans = mockStatic(SpringUtil.class); var session = mockStatic(SessionHelper.class)) {
            beans.when(() -> SpringUtil.getBean(IApiConfigManager.class)).thenReturn(apiConfig);
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            try {
                assertThat(interceptor.preHandle(request, response, handler)).isTrue();
                assertion.run();
            } finally {
                interceptor.afterCompletion(request, response, handler, null);
            }
        }
    }

    private LoginUser user(Long tenantId, int type) {
        var user = new LoginUser();
        user.setTenantId(tenantId);
        user.setUserId(10L);
        user.setUserType(type);
        return user;
    }
}
