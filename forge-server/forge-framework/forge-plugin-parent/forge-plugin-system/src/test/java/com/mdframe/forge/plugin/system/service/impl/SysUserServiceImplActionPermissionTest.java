package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SysUserServiceImplActionPermissionTest {
    private static final long CALLER = 100L;
    private static final long TARGET = 200L;
    private static final long TENANT = 7L;
    private static final long ROLE = 300L;
    private static final long ORG = 400L;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : new Class<?>[]{SysUser.class, SysUserTenant.class, SysTenant.class,
                SysUserRole.class, SysUserOrgRole.class, SysUserOrg.class, SysUserPost.class,
                SysRole.class, SysOrg.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entity);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"system:user:list", "system:user:query", "system:org:list",
            "system:org:query", "system:role:list", "system:role:query"})
    void readPermissionCannotAuthorizeAnyUserWriteOrUnlock(String permission) {
        Dependencies d = new Dependencies();
        try (MockedStatic<SessionHelper> session = login(2, permission);
             MockedStatic<StpUtil> tokens = mockStatic(StpUtil.class)) {
            String denied = UiMessages.message("customer.userOrganizationDenied");
            assertThatThrownBy(() -> d.service.insertUser(dto(TARGET))).hasMessage(denied)
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.getCode()).isEqualTo(403));
            assertThatThrownBy(() -> d.service.updateUser(dto(TARGET))).hasMessage(denied);
            assertThatThrownBy(() -> d.service.updateUser(dto(CALLER))).hasMessage(denied);
            assertThatThrownBy(() -> d.service.deleteUserById(TARGET)).hasMessage(denied);
            assertThatThrownBy(() -> d.service.deleteUserByIds(new Long[]{TARGET})).hasMessage(denied);
            assertThatThrownBy(() -> d.service.resetPassword(TARGET, "NewPassword!234")).hasMessage(denied);
            assertThatThrownBy(() -> d.service.updateUserStatus(TARGET, 0)).hasMessage(denied);
            assertThatThrownBy(() -> d.service.doUntieDisable(TARGET)).hasMessage(denied);
            tokens.verifyNoInteractions();
        }
        verifyNoInteractions(d.users, d.members, d.tenants);
    }

    @Test
    void addPermissionCreatesOnlyNormalUserInCurrentTenantAndDoesNotGrantEditOrDelete() {
        Dependencies d = new Dependencies();
        when(d.tenants.selectCount(any())).thenReturn(1L);
        when(d.users.insert(any(SysUser.class))).thenAnswer(call -> {
            ((SysUser) call.getArgument(0)).setId(TARGET);
            return 1;
        });
        SysUserDTO request = dto(TARGET);
        request.setTenantId(99L);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:add")) {
            assertThatThrownBy(() -> d.service.insertUser(request)).hasMessage("默认租户必须包含在所属租户中");
            request.setTenantId(TENANT);
            assertThat(d.service.insertUser(request)).isTrue();
            assertThatThrownBy(() -> d.service.updateUser(request))
                    .hasMessage(UiMessages.message("customer.userOrganizationDenied"));
            assertThatThrownBy(() -> d.service.deleteUserById(TARGET))
                    .hasMessage(UiMessages.message("customer.userOrganizationDenied"));
        }
        ArgumentCaptor<SysUser> created = ArgumentCaptor.forClass(SysUser.class);
        verify(d.users).insert(created.capture());
        assertThat(created.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(created.getValue().getUserType()).isEqualTo(2);
        assertThat(created.getValue().getForcePasswordChange()).isTrue();
        assertThat(PasswordUtil.matches(request.getPassword(), created.getValue().getPassword())).isTrue();
        verify(d.members).insert(any(SysUserTenant.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"system:user:edit", "system:user:*", "system:*", "*:*:*", "*"})
    void editPermissionAndExistingWildcardsAllowEditResetStatusAndUnlock(String permission) {
        Dependencies d = manageableTarget();
        try (MockedStatic<SessionHelper> session = login(2, permission);
             MockedStatic<StpUtil> tokens = mockStatic(StpUtil.class)) {
            assertThat(d.service.updateUser(dto(TARGET))).isTrue();
            assertThat(d.service.resetPassword(TARGET, "NewPassword!234")).isTrue();
            assertThat(d.service.updateUserStatus(TARGET, 0)).isTrue();
            d.service.doUntieDisable(TARGET);
            tokens.verify(() -> StpUtil.untieDisable(TARGET));
        }
        ArgumentCaptor<SysUser> changes = ArgumentCaptor.forClass(SysUser.class);
        verify(d.users, times(2)).updateById(changes.capture());
        assertThat(changes.getAllValues().get(0).getPassword()).isNull();
        assertThat(changes.getAllValues().get(0).getUserType()).isNull();
        SysUser passwordChange = changes.getAllValues().get(1);
        assertThat(PasswordUtil.matches("NewPassword!234", passwordChange.getPassword())).isTrue();
        assertThat(passwordChange.getForcePasswordChange()).isTrue();
        ArgumentCaptor<SysUserTenant> status = ArgumentCaptor.forClass(SysUserTenant.class);
        verify(d.members, times(2)).update(status.capture(), any(Wrapper.class));
        assertThat(status.getAllValues()).extracting(SysUserTenant::getStatus).containsExactly(0, 1);
    }

    @Test
    void removePermissionDeletesOnlyTenantMembershipAndDoesNotGrantReset() {
        Dependencies d = manageableTarget();
        when(d.members.delete(any(Wrapper.class))).thenReturn(1);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:remove")) {
            assertThat(d.service.deleteUserByIds(new Long[]{TARGET})).isTrue();
            assertThatThrownBy(() -> d.service.resetPassword(TARGET, "NewPassword!234"))
                    .hasMessage(UiMessages.message("customer.userOrganizationDenied"));
        }
        verify(d.members).delete(any(Wrapper.class));
        verify(d.users, never()).deleteById(anyLong());
        verify(d.users, never()).updateById(any(SysUser.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void existingAdministratorQualificationStillAllowsResetWithoutExplicitButtons(int userType) {
        Dependencies d = manageableTarget();
        try (MockedStatic<SessionHelper> session = login(userType)) {
            assertThat(d.service.resetPassword(TARGET, "NewPassword!234")).isTrue();
        }
        verify(d.users).updateById(any(SysUser.class));
    }

    @Test
    void correctActionPermissionStillRejectsOtherTenantAndCannotUnlockIt() {
        Dependencies d = manageableTarget();
        when(d.members.selectCount(any())).thenReturn(0L);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit");
             MockedStatic<StpUtil> tokens = mockStatic(StpUtil.class)) {
            assertThatThrownBy(() -> d.service.resetPassword(TARGET, "NewPassword!234"))
                    .hasMessage("无权操作非本租户用户");
            assertThatThrownBy(() -> d.service.doUntieDisable(TARGET))
                    .hasMessage("无权操作非本租户用户");
            tokens.verifyNoInteractions();
        }
        verify(d.users, never()).updateById(any(SysUser.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void correctActionPermissionStillRejectsPrivilegedTarget(int targetType) {
        Dependencies d = manageableTarget();
        d.target.setUserType(targetType);
        d.member.setMemberType(targetType);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit");
             MockedStatic<StpUtil> tokens = mockStatic(StpUtil.class)) {
            String denied = targetType == 0 ? "无权操作超级管理员" : "租户管理员只能维护普通用户";
            assertThatThrownBy(() -> d.service.resetPassword(TARGET, "NewPassword!234")).hasMessage(denied);
            assertThatThrownBy(() -> d.service.doUntieDisable(TARGET)).hasMessage(denied);
            tokens.verifyNoInteractions();
        }
        verify(d.users, never()).updateById(any(SysUser.class));
    }

    @Test
    void actionPermissionsDoNotAllowSelfResetDisableOrPrivilegeEscalation() {
        Dependencies d = manageableTarget();
        when(d.users.selectById(CALLER)).thenReturn(d.target);
        SysUserDTO elevated = dto(TARGET);
        elevated.setUserType(0);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:*");
             MockedStatic<StpUtil> tokens = mockStatic(StpUtil.class)) {
            assertThatThrownBy(() -> d.service.resetPassword(CALLER, "NewPassword!234"))
                    .hasMessage("不能在用户管理中维护当前登录用户");
            assertThatThrownBy(() -> d.service.updateUserStatus(CALLER, 0))
                    .hasMessage("不能在用户管理中维护当前登录用户");
            assertThatThrownBy(() -> d.service.doUntieDisable(CALLER))
                    .hasMessage("不能在用户管理中维护当前登录用户");
            assertThatThrownBy(() -> d.service.updateUser(elevated)).hasMessage("租户管理员只能维护普通用户");
            assertThatThrownBy(() -> d.service.insertUser(elevated)).hasMessage("租户管理员只能维护普通用户");
            tokens.verifyNoInteractions();
        }
        verify(d.users, never()).updateById(any(SysUser.class));
        verify(d.users, never()).insert(any(SysUser.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"system:user:list", "system:user:query", "system:org:list",
            "system:org:query", "system:role:list", "system:role:query",
            "system:user:add", "system:user:remove"})
    void relationWritesRequireEditPermissionBeforeAnyRepositoryAccess(String permission) {
        Dependencies d = new Dependencies();
        try (MockedStatic<SessionHelper> session = login(2, permission)) {
            for (BooleanSupplier write : relationWrites(d, TARGET)) {
                assertThatThrownBy(write::getAsBoolean)
                        .hasMessage(UiMessages.message("customer.userOrganizationDenied"))
                        .isInstanceOfSatisfying(BusinessException.class,
                                error -> assertThat(error.getCode()).isEqualTo(403));
            }
        }
        verifyNoInteractions(d.users, d.members, d.tenants, d.userRoles, d.userOrgRoles,
                d.userOrgs, d.userPosts, d.roles, d.orgs);
    }

    @ParameterizedTest
    @ValueSource(strings = {"system:user:edit", "system:user:*"})
    void relationWritesPreserveEditAndWildcardPermissions(String permission) {
        Dependencies d = relationTarget();
        try (MockedStatic<SessionHelper> session = login(2, permission)) {
            allowRelationAssignment();
            relationWrites(d, TARGET).forEach(write -> assertThat(write.getAsBoolean()).isTrue());
        }
        verifyRelationInserts(d, 3, 2, 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void relationWritesPreserveAdministratorQualification(int userType) {
        Dependencies d = relationTarget();
        try (MockedStatic<SessionHelper> session = login(userType)) {
            allowRelationAssignment();
            relationWrites(d, TARGET).forEach(write -> assertThat(write.getAsBoolean()).isTrue());
        }
        verifyRelationInserts(d, 3, 2, 1);
    }

    @Test
    void relationWritesStillRejectOtherTenantWithEdit() {
        Dependencies d = relationTarget();
        when(d.members.selectCount(any())).thenReturn(0L);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit")) {
            allowRelationAssignment();
            relationWrites(d, TARGET).forEach(write -> assertThatThrownBy(write::getAsBoolean)
                    .hasMessage("无权操作非本租户用户"));
        }
        verifyNoInteractions(d.userRoles, d.userOrgRoles, d.userOrgs, d.userPosts);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void relationWritesStillRejectPrivilegedTargetsWithEdit(int targetType) {
        Dependencies d = relationTarget();
        d.target.setUserType(targetType);
        d.member.setMemberType(targetType);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit")) {
            allowRelationAssignment();
            String denied = targetType == 0 ? "无权操作超级管理员" : "普通用户";
            relationWrites(d, TARGET).forEach(write -> assertThatThrownBy(write::getAsBoolean)
                    .hasMessageContaining(denied));
        }
        verifyNoInteractions(d.userRoles, d.userOrgRoles, d.userOrgs, d.userPosts);
    }

    @Test
    void relationWritesStillRejectSelfWithEdit() {
        Dependencies d = relationTarget();
        when(d.users.selectById(CALLER)).thenReturn(d.target);
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit")) {
            allowRelationAssignment();
            relationWrites(d, CALLER).forEach(write -> assertThatThrownBy(write::getAsBoolean)
                    .hasMessage("不能在用户管理中维护当前登录用户"));
        }
        verifyNoInteractions(d.userRoles, d.userOrgRoles, d.userOrgs, d.userPosts);
    }

    @Test
    void relationWritesStillRejectUnownedRoleAndOtherOrganization() {
        Dependencies d = relationTarget();
        try (MockedStatic<SessionHelper> session = login(2, "system:user:edit")) {
            SessionHelper.getLoginUser().setRoleIds(List.of());
            assertThatThrownBy(() -> d.service.bindUserRoles(TARGET, new Long[]{ROLE}, TENANT))
                    .hasMessage("权限溢出：不能分配自己没有的角色");
            allowRelationAssignment();
            assertThatThrownBy(() -> d.service.bindUserOrgRoles(TARGET, ORG + 1, List.of(ROLE), TENANT))
                    .hasMessage("只能维护当前组织下的用户角色");
        }
        verify(d.userRoles, never()).insert(any(SysUserRole.class));
        verify(d.userRoles, never()).delete(any(Wrapper.class));
        verifyNoInteractions(d.userOrgRoles);
    }

    @Test
    void addPermissionRetainsInternalRelationBindingsButCannotUsePublicWrites() {
        Dependencies d = relationTarget();
        when(d.users.insert(any(SysUser.class))).thenAnswer(call -> {
            ((SysUser) call.getArgument(0)).setId(TARGET);
            return 1;
        });
        SysUserDTO request = dto(TARGET);
        request.setOrgIds(List.of(ORG));
        request.setMainOrgId(ORG);
        request.setRoleIds(List.of(ROLE));
        try (MockedStatic<SessionHelper> session = login(2, "system:user:add")) {
            allowRelationAssignment();
            assertThat(d.service.insertUser(request)).isTrue();
            relationWrites(d, TARGET).forEach(write -> assertThatThrownBy(write::getAsBoolean)
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.getCode()).isEqualTo(403)));
        }
        verify(d.users).insert(any(SysUser.class));
        verifyRelationInserts(d, 1, 1, 0);
        verify(d.userRoles).delete(any(Wrapper.class));
        verify(d.userOrgs, never()).delete(any(Wrapper.class));
        verify(d.userOrgRoles, never()).delete(any(Wrapper.class));
    }

    private static List<BooleanSupplier> relationWrites(Dependencies d, long target) {
        BatchUserRoleBindDTO batch = new BatchUserRoleBindDTO();
        batch.setUserIds(List.of(target));
        batch.setRoleIds(List.of(ROLE));
        batch.setTenantId(TENANT);
        return List.of(
                () -> d.service.bindUserRoles(target, new Long[]{ROLE}),
                () -> d.service.bindUserRoles(target, new Long[]{ROLE}, TENANT),
                () -> d.service.batchBindUserRoles(batch),
                () -> d.service.bindUserOrgs(target, List.of(ORG), ORG),
                () -> d.service.bindUserOrgs(target, List.of(ORG), ORG, TENANT),
                () -> d.service.bindUserOrgRoles(target, ORG, List.of(ROLE), TENANT));
    }

    private static void allowRelationAssignment() {
        SessionHelper.getLoginUser().setRoleIds(List.of(ROLE));
        SessionHelper.getLoginUser().setActiveOrgId(ORG);
    }

    private static Dependencies relationTarget() {
        Dependencies d = manageableTarget();
        SysRole role = new SysRole();
        role.setId(ROLE);
        role.setTenantId(TENANT);
        role.setOrgScopeType(SystemConstants.RoleOrgScope.GLOBAL);
        when(d.roles.selectCount(any())).thenReturn(1L);
        when(d.roles.selectList(any())).thenReturn(List.of(role));
        when(d.orgs.selectCount(any())).thenReturn(1L);
        when(d.userOrgs.selectCount(any())).thenReturn(1L);
        return d;
    }

    private static void verifyRelationInserts(Dependencies d, int roleCount, int orgCount, int orgRoleCount) {
        ArgumentCaptor<SysUserRole> role = ArgumentCaptor.forClass(SysUserRole.class);
        verify(d.userRoles, times(roleCount)).insert(role.capture());
        assertThat(role.getAllValues()).allSatisfy(value -> {
            assertThat(value.getUserId()).isEqualTo(TARGET);
            assertThat(value.getTenantId()).isEqualTo(TENANT);
            assertThat(value.getRoleId()).isEqualTo(ROLE);
        });
        ArgumentCaptor<SysUserOrg> org = ArgumentCaptor.forClass(SysUserOrg.class);
        verify(d.userOrgs, times(orgCount)).insert(org.capture());
        assertThat(org.getAllValues()).allSatisfy(value -> {
            assertThat(value.getUserId()).isEqualTo(TARGET);
            assertThat(value.getTenantId()).isEqualTo(TENANT);
            assertThat(value.getOrgId()).isEqualTo(ORG);
            assertThat(value.getIsMain()).isEqualTo(1);
        });
        ArgumentCaptor<SysUserOrgRole> orgRole = ArgumentCaptor.forClass(SysUserOrgRole.class);
        verify(d.userOrgRoles, times(orgRoleCount)).insert(orgRole.capture());
        assertThat(orgRole.getAllValues()).allSatisfy(value -> {
            assertThat(value.getUserId()).isEqualTo(TARGET);
            assertThat(value.getTenantId()).isEqualTo(TENANT);
            assertThat(value.getOrgId()).isEqualTo(ORG);
            assertThat(value.getRoleId()).isEqualTo(ROLE);
        });
    }

    private static SysUserDTO dto(long userId) {
        SysUserDTO dto = new SysUserDTO();
        dto.setId(userId);
        dto.setUsername("action-permission-user");
        dto.setPassword("InitialPassword!234");
        dto.setUserType(2);
        dto.setTenantId(TENANT);
        return dto;
    }

    private static MockedStatic<SessionHelper> login(int userType, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(CALLER);
        user.setTenantId(TENANT);
        user.setUserType(userType);
        user.setPermissions(Set.of(permissions));
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        session.when(SessionHelper::getUserId).thenReturn(CALLER);
        return session;
    }

    private static Dependencies manageableTarget() {
        Dependencies d = new Dependencies();
        d.target.setId(TARGET);
        d.target.setTenantId(TENANT);
        d.target.setUserType(2);
        d.member.setUserId(TARGET);
        d.member.setTenantId(TENANT);
        d.member.setMemberType(2);
        d.member.setStatus(1);
        when(d.users.selectById(TARGET)).thenReturn(d.target);
        when(d.members.selectCount(any())).thenReturn(1L);
        when(d.members.selectOne(any())).thenReturn(d.member);
        when(d.members.update(any(SysUserTenant.class), any(Wrapper.class))).thenReturn(1);
        when(d.users.updateById(any(SysUser.class))).thenReturn(1);
        when(d.tenants.selectCount(any())).thenReturn(1L);
        return d;
    }

    private static final class Dependencies {
        final SysUserMapper users = mock(SysUserMapper.class);
        final SysUserTenantMapper members = mock(SysUserTenantMapper.class);
        final SysTenantMapper tenants = mock(SysTenantMapper.class);
        final SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
        final SysUserOrgRoleMapper userOrgRoles = mock(SysUserOrgRoleMapper.class);
        final SysUserOrgMapper userOrgs = mock(SysUserOrgMapper.class);
        final SysUserPostMapper userPosts = mock(SysUserPostMapper.class);
        final SysRoleMapper roles = mock(SysRoleMapper.class);
        final SysOrgMapper orgs = mock(SysOrgMapper.class);
        final SysUser target = new SysUser();
        final SysUserTenant member = new SysUserTenant();
        final SysUserServiceImpl service = new SysUserServiceImpl(users,
                userRoles, userOrgRoles, userOrgs, userPosts, members, tenants, roles,
                mock(SysRoleOrgMapper.class), orgs, mock(SysPostMapper.class),
                mock(SysRegionMapper.class), mock(IUserLoadService.class));
    }
}
