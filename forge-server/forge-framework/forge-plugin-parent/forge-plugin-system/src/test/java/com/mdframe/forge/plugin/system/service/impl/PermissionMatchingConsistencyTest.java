package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.starter.auth.config.StpInterfaceImpl;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.PermissionMatcher;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionMatchingConsistencyTest {
    @Test
    void realSaTokenAndThreeConsumersAgreeWithoutReplacingItsStrategy() {
        var previousLogic = StpUtil.getStpLogic();
        var previousProvider = SaManager.getStpInterface();
        StpUtil.setStpLogic(new StpLogic("login") {
            @Override
            public Object getLoginId() {
                return 501L;
            }
        });
        SaManager.setStpInterface(new StpInterfaceImpl());
        try {
            var roles = mock(SysRoleServiceImpl.class, CALLS_REAL_METHODS);
            var users = new UserAccessPolicy(null, null, null);
            String[][] matrix = {
                    {"system:user:query", "system:user:query", "true"},
                    {"system:user:query", "system:user:edit", "false"},
                    {"system:user:*", "system:user:edit", "true"},
                    {"system:*", "system:role:query", "true"},
                    {"system:*:query", "system:user:query", "true"},
                    {"system:*:query", "system:user:edit", "false"},
                    {"system:user:*", "system:role:edit", "false"},
                    {"*", "plainPermission", "true"},
                    {"*:*:*", "plainPermission", "true"},
                    {"*:*:*", "system:user:edit", "true"}
            };
            for (String[] row : matrix) {
                LoginUser user = user(row[0]);
                boolean expected = Boolean.parseBoolean(row[2]);
                try (var ignored = ExecutionIdentityContextHolder.open(identity(user))) {
                    assertEquals(expected, SessionHelper.hasPermission(row[1]));
                    assertEquals(expected, users.hasAnyPermission(user, row[1]));
                    assertEquals(expected, ReflectionTestUtils.invokeMethod(roles,
                            "hasAnyPermission", user, new String[]{row[1]}));
                    if (expected) {
                        assertDoesNotThrow(() -> StpUtil.checkPermission(row[1]));
                    } else {
                        assertThrows(NotPermissionException.class, () -> StpUtil.checkPermission(row[1]));
                    }
                    assertEquals(Set.of(row[0]), SessionHelper.getPermissions(), "Do not mutate stored grants");
                }
            }
        } finally {
            StpUtil.setStpLogic(previousLogic);
            SaManager.setStpInterface(previousProvider);
        }
    }

    @Test
    void anyAndAllRequireRealRequestedPermissionsAndIgnoreInvalidGrants() {
        assertFalse(PermissionMatcher.hasPermission(null, "system:user:query"));
        assertFalse(PermissionMatcher.hasPermission(Set.of("*"), null));
        assertFalse(PermissionMatcher.hasPermission(Set.of("*"), " "));
        assertFalse(PermissionMatcher.hasAnyPermission(Set.of("*"), (String[]) null));
        assertFalse(PermissionMatcher.hasAllPermissions(Set.of("*")));
        assertFalse(PermissionMatcher.hasAllPermissions(Set.of("*"), "system:user:edit", null));
        assertTrue(PermissionMatcher.hasPermission(Arrays.asList(null, "", "system:user:*"), "system:user:query"));
        try (var ignored = ExecutionIdentityContextHolder.open(identity(user("system:user:*")))) {
            assertTrue(SessionHelper.hasAnyPermission("system:role:edit", "system:user:edit"));
            assertTrue(SessionHelper.hasAllPermissions("system:user:query", "system:user:edit"));
            assertFalse(SessionHelper.hasAllPermissions("system:user:query", "system:role:edit"));
            assertFalse(SessionHelper.hasAllPermissions());
        }
    }

    @Test
    void readMatchingDoesNotGrantUserWriteOrRoleMaintenance() {
        try (var ignored = ExecutionIdentityContextHolder.open(identity(user("system:*:query")))) {
            var users = new UserAccessPolicy(null, null, null);
            assertDoesNotThrow(() -> {
                users.assertUserManagementAllowed();
            });
            assertEquals(403, assertThrows(BusinessException.class,
                    () -> users.assertUserActionAllowed("system:user:edit")).getCode());
            var roles = mock(SysRoleServiceImpl.class, CALLS_REAL_METHODS);
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(roles, "assertRoleManagementAllowed"));
            SysRole own = new SysRole();
            own.setId(10L);
            own.setTenantId(7L);
            assertThrows(RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(roles, "assertCanMaintainRole", own));
        }
    }

    @Test
    void fullGrantDoesNotBypassRoleTenantAndTargetPoliciesOrChangeAdminPolicy() {
        var user = user("*:*:*");
        try (var ignored = ExecutionIdentityContextHolder.open(identity(user))) {
            var mapper = mock(SysRoleMapper.class);
            var roles = mock(SysRoleServiceImpl.class, CALLS_REAL_METHODS);
            ReflectionTestUtils.setField(roles, "roleMapper", mapper);
            var otherTenant = new SysRole();
            otherTenant.setId(10L);
            otherTenant.setTenantId(8L);
            when(mapper.selectById(10L)).thenReturn(otherTenant);
            assertThrows(RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(roles, "loadRoleForAccess", 10L));
            var builtIn = new SysRole();
            builtIn.setId(11L);
            builtIn.setIsSystem(1);
            assertThrows(RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(roles, "assertCanMaintainRole", builtIn));
        }
        user.setUserType(0);
        try (var ignored = ExecutionIdentityContextHolder.open(identity(user))) {
            assertTrue(SessionHelper.hasPermission("plainPermission"));
            assertDoesNotThrow(() -> new UserAccessPolicy(null, null, null)
                    .assertUserActionAllowed("system:user:edit"));
        }
    }

    private LoginUser user(String grant) {
        var user = new LoginUser();
        user.setUserId(501L);
        user.setUserType(2);
        user.setTenantId(7L);
        user.setRoleIds(List.of(10L));
        user.setPermissions(Set.of(grant));
        return user;
    }

    private ExecutionIdentity identity(LoginUser user) {
        return new ExecutionIdentity(user, "USER", user.getUserId(), null, 301L,
                "permission-test", "matrix", Set.of());
    }
}
