package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Public service behavior shared by the original implementation and the extracted collaborators. */
class SysUserAssignmentsCompatibilityTest {
    private static final long CALLER = 100L;
    private static final long TARGET = 200L;
    private static final long TENANT = 7L;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : new Class<?>[]{SysUser.class, SysUserTenant.class, SysTenant.class,
                SysUserRole.class, SysUserOrgRole.class, SysUserOrg.class, SysUserPost.class,
                SysOrg.class, SysPost.class, SysRole.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entity);
        }
    }

    @Test
    void replacingOrganizationsDeletesDependentRolesBeforeMembershipsAndUpdatesMainRegion() {
        Dependencies d = new Dependencies();
        SysUserOrg retained = organization(12L, 1);
        when(d.userOrgs.selectList(any())).thenReturn(List.of(organization(11L, 0), retained));
        when(d.orgs.selectCount(any())).thenReturn(2L);
        SysOrg main = new SysOrg();
        main.setRegionCode("R-13");
        when(d.orgs.selectById(13L)).thenReturn(main);
        try (MockedStatic<SessionHelper> session = login(0)) {
            assertThat(d.service.bindUserOrgs(TARGET, List.of(12L, 13L), 13L, TENANT)).isTrue();
        }
        var order = inOrder(d.orgRoles, d.userOrgs, d.users);
        order.verify(d.orgRoles).delete(any(Wrapper.class));
        order.verify(d.userOrgs).delete(any(Wrapper.class));
        order.verify(d.userOrgs).updateById(retained);
        ArgumentCaptor<SysUserOrg> added = ArgumentCaptor.forClass(SysUserOrg.class);
        order.verify(d.userOrgs).insert(added.capture());
        assertThat(added.getValue().getOrgId()).isEqualTo(13L);
        assertThat(added.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(added.getValue().getUserId()).isEqualTo(TARGET);
        assertThat(added.getValue().getIsMain()).isEqualTo(1);
        assertThat(retained.getIsMain()).isZero();
        ArgumentCaptor<SysUser> changed = ArgumentCaptor.forClass(SysUser.class);
        order.verify(d.users).updateById(changed.capture());
        assertThat(changed.getValue().getRegionCode()).isEqualTo("R-13");
    }

    @Test
    void foreignOrganizationIsRejectedBeforeRelationshipWrites() {
        Dependencies d = new Dependencies();
        when(d.orgs.selectCount(any())).thenReturn(0L);
        try (MockedStatic<SessionHelper> session = login(0)) {
            assertThatThrownBy(() -> d.service.bindUserOrgs(TARGET, List.of(99L), 99L, TENANT))
                    .hasMessage("组织不属于当前操作租户");
        }
        verifyNoInteractions(d.userOrgs, d.orgRoles);
    }

    @Test
    void roleClearingStaysInsideCallersRolesAndCannotGrantMissingRole() {
        Dependencies d = new Dependencies();
        try (MockedStatic<SessionHelper> session = login(1)) {
            assertThat(d.service.bindUserRoles(TARGET, new Long[0], TENANT)).isTrue();
            assertThatThrownBy(() -> d.service.bindUserRoles(TARGET, new Long[]{99L}, TENANT))
                    .hasMessage("权限溢出：不能分配自己没有的角色");
        }
        ArgumentCaptor<LambdaQueryWrapper<SysUserRole>> deleted = queryCaptor();
        verify(d.userRoles).delete(deleted.capture());
        LambdaQueryWrapper<SysUserRole> scope = deleted.getValue();
        assertThat(scope.getSqlSegment()).contains("user_id", "tenant_id", "role_id IN");
        assertThat(scope.getParamNameValuePairs().values()).contains(TARGET, TENANT, 31L);
        verify(d.userRoles, never()).insert(any(SysUserRole.class));
    }

    @Test
    void changingTenantsRemovesAllOldRelationshipsBeforeSelectingNewDefault() {
        Dependencies d = new Dependencies();
        SysUserTenant removed = new SysUserTenant();
        removed.setTenantId(9L);
        when(d.members.selectList(any())).thenReturn(List.of(removed));
        UserTenantBindDTO request = new UserTenantBindDTO();
        request.setTenantIds(List.of(TENANT));
        request.setDefaultTenantId(TENANT);
        request.setMemberType(2);
        try (MockedStatic<SessionHelper> session = login(0)) {
            assertThat(d.service.bindUserTenants(TARGET, request)).isTrue();
        }
        var order = inOrder(d.userRoles, d.orgRoles, d.userOrgs, d.userPosts, d.members, d.users);
        order.verify(d.userRoles).delete(any(Wrapper.class));
        order.verify(d.orgRoles).delete(any(Wrapper.class));
        order.verify(d.userOrgs).delete(any(Wrapper.class));
        order.verify(d.userPosts).delete(any(Wrapper.class));
        order.verify(d.members).delete(any(Wrapper.class));
        ArgumentCaptor<SysUser> changed = ArgumentCaptor.forClass(SysUser.class);
        order.verify(d.users).updateById(changed.capture());
        assertThat(changed.getValue().getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void profileUpdateKeepsTrustedIdentityAndDoesNotWriteSecurityFields() {
        Dependencies d = new Dependencies();
        when(d.users.updateById(any(SysUser.class))).thenReturn(1);
        SysUserDTO request = new SysUserDTO();
        request.setId(TARGET);
        request.setRealName("Changed");
        request.setPassword("Untrusted");
        request.setUserType(0);
        request.setTenantId(99L);
        try (MockedStatic<SessionHelper> session = login(2)) {
            assertThat(d.service.updateUserProfile(request)).isTrue();
            session.verify(() -> SessionHelper.setLoginUser(any(LoginUser.class)));
        }
        ArgumentCaptor<SysUser> changed = ArgumentCaptor.forClass(SysUser.class);
        verify(d.users).updateById(changed.capture());
        assertThat(changed.getValue().getId()).isEqualTo(CALLER);
        assertThat(changed.getValue().getRealName()).isEqualTo("Changed");
        assertThat(changed.getValue().getPassword()).isNull();
        assertThat(changed.getValue().getUserType()).isNull();
        assertThat(changed.getValue().getTenantId()).isNull();
    }

    private static SysUserOrg organization(long orgId, int isMain) {
        SysUserOrg binding = new SysUserOrg();
        binding.setUserId(TARGET);
        binding.setTenantId(TENANT);
        binding.setOrgId(orgId);
        binding.setIsMain(isMain);
        return binding;
    }

    private static MockedStatic<SessionHelper> login(int type) {
        LoginUser caller = new LoginUser();
        caller.setUserId(CALLER);
        caller.setTenantId(TENANT);
        caller.setUserType(type);
        caller.setRoleIds(List.of(31L));
        caller.setPermissions(Set.of("system:user:edit"));
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(caller);
        session.when(SessionHelper::getUserId).thenReturn(CALLER);
        return session;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<LambdaQueryWrapper<T>> queryCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    private static final class Dependencies {
        final SysUserMapper users = mock(SysUserMapper.class);
        final SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
        final SysUserOrgRoleMapper orgRoles = mock(SysUserOrgRoleMapper.class);
        final SysUserOrgMapper userOrgs = mock(SysUserOrgMapper.class);
        final SysUserPostMapper userPosts = mock(SysUserPostMapper.class);
        final SysUserTenantMapper members = mock(SysUserTenantMapper.class);
        final SysTenantMapper tenants = mock(SysTenantMapper.class);
        final SysOrgMapper orgs = mock(SysOrgMapper.class);
        final SysUserServiceImpl service = new SysUserServiceImpl(users, userRoles, orgRoles, userOrgs,
                userPosts, members, tenants, mock(SysRoleMapper.class), mock(SysRoleOrgMapper.class),
                orgs, mock(SysPostMapper.class), mock(SysRegionMapper.class), mock(IUserLoadService.class));

        Dependencies() {
            SysUser target = new SysUser();
            target.setId(TARGET);
            target.setTenantId(TENANT);
            target.setUserType(2);
            when(users.selectById(TARGET)).thenReturn(target);
            when(tenants.selectCount(any())).thenReturn(1L);
            when(members.selectCount(any())).thenReturn(1L);
            SysUserTenant member = new SysUserTenant();
            member.setMemberType(2);
            member.setStatus(1);
            when(members.selectOne(any())).thenReturn(member);
        }
    }
}
