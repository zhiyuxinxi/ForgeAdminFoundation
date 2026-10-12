package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CurrentUserProfileAuditTest {
    private final ISysUserService users = mock(ISysUserService.class);
    private final SysUserController controller = new SysUserController(users);

    @AfterEach
    void clearContext() {
        OperationAuditContext.clear();
        TenantContextHolder.clear();
    }

    @Test
    void selfProfileAuditDoesNotRequireManagementPermissionOrUseDtoTarget() {
        SysUserDTO dto = new SysUserDTO();
        dto.setId(200L);
        dto.setRealName("new name");
        TenantContextHolder.setTenantId(7L);
        when(users.selectUserById(anyLong())).thenThrow(new BusinessException("management denied"));
        when(users.getById(100L)).thenAnswer(call -> {
            assertThat(TenantContextHolder.isIgnore()).isTrue();
            return account("old name");
        }).thenReturn(account("new name"));
        when(users.updateUserProfile(dto)).thenAnswer(call -> {
            assertThat(TenantContextHolder.isIgnore()).isFalse();
            return true;
        });
        try (MockedStatic<SessionHelper> session = caller(100L)) {
            controller.updateProfile(dto);
        }
        verify(users, times(2)).getById(100L);
        verify(users, never()).getById(200L);
        verify(users, never()).selectUserById(anyLong());
        verify(users).updateUserProfile(dto);
        Map<?, ?> before = (Map<?, ?>) OperationAuditContext.snapshot().getBeforeData();
        Map<?, ?> after = (Map<?, ?>) OperationAuditContext.snapshot().getAfterData();
        assertThat(before.get("id")).isEqualTo(100L);
        assertThat(before.get("realName")).isEqualTo("old name");
        assertThat(after.get("realName")).isEqualTo("new name");
        assertThat(before.toString()).doesNotContain("13812345678", "owner@example.com", "secret-hash");
        assertThat(before.containsKey("roleIds")).isFalse();
        assertThat(before.containsKey("password")).isFalse();
        assertThat(OperationAuditContext.snapshot().getDiffData().toString()).contains("realName");
        assertThat(TenantContextHolder.isIgnore()).isFalse();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
    }

    @Test
    void unauthenticatedRequestDoesNotReadOrWriteAccounts() {
        try (MockedStatic<SessionHelper> session = caller(null)) {
            assertThatThrownBy(() -> controller.updateProfile(new SysUserDTO()))
                    .isInstanceOf(BusinessException.class).hasMessage("用户未登录");
        }
        verifyNoInteractions(users);
    }

    @Test
    void failedUpdateDoesNotInventAnAfterSnapshot() {
        when(users.getById(100L)).thenReturn(account("old name"));
        try (MockedStatic<SessionHelper> session = caller(100L)) {
            controller.updateProfile(new SysUserDTO());
        }
        verify(users).getById(100L);
        assertThat(OperationAuditContext.snapshot().getBeforeData()).isNotNull();
        assertThat(OperationAuditContext.snapshot().getAfterData()).isNull();
        assertThat(OperationAuditContext.snapshot().getDiffData()).isNull();
    }

    @Test
    void databaseFailureIsPreservedAndTenantScopeIsRestored() {
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(users.getById(100L)).thenThrow(failure);
        try (MockedStatic<SessionHelper> session = caller(100L)) {
            assertThatThrownBy(() -> controller.updateProfile(new SysUserDTO())).isSameAs(failure);
        }
        verify(users, never()).updateUserProfile(any());
        assertThat(TenantContextHolder.getIgnoreValue()).isNull();
    }

    private MockedStatic<SessionHelper> caller(Long id) {
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getUserId).thenReturn(id);
        return session;
    }

    private SysUser account(String name) {
        SysUser user = new SysUser();
        user.setId(100L);
        user.setTenantId(1L);
        user.setRealName(name);
        user.setPhone("13812345678");
        user.setEmail("owner@example.com");
        user.setPassword("secret-hash");
        return user;
    }
}
