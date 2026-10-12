package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.datasource.TenantDatasourceAccessService;
import com.mdframe.forge.plugin.system.datasource.TenantDatasourceOption;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SysTenantServiceImplPermissionTest {
    private static final long TENANT = 7L;
    private static final long CALLER = 100L;
    private static final long TARGET = 200L;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : new Class<?>[]{SysTenant.class, SysUser.class, SysUserTenant.class,
                SysUserRole.class, SysUserOrgRole.class, SysUserOrg.class, SysUserPost.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), entity);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"system:user:list", "system:user:remove", "*"})
    void ordinaryMemberCannotManageTenantEvenWithUserManagementPermissions(String permission) {
        Dependencies d = new Dependencies();
        try (MockedStatic<SessionHelper> ignored = login(2, permission)) {
            denied(() -> d.service.updateTenant(request(TENANT)));
            denied(() -> d.service.removeTenantUser(TENANT, TARGET));
            denied(() -> d.service.selectTenantUsers(TENANT, new SysUserQuery()));
        }
        verifyNoInteractions(d.tenants, d.users, d.members, d.roles, d.orgRoles, d.orgs, d.posts);
    }

    @Test
    void memberCanStillReadOwnTenantConfigurationButCannotReadAnotherTenant() {
        Dependencies d = new Dependencies();
        SysTenant tenant = new SysTenant();
        when(d.tenants.selectById(TENANT)).thenReturn(tenant);
        try (MockedStatic<SessionHelper> ignored = login(2)) {
            assertThat(d.service.selectUserTenantConfig(TENANT)).isSameAs(tenant);
            assertThatThrownBy(() -> d.service.selectUserTenantConfig(99L)).isInstanceOf(RuntimeException.class);
        }
        verify(d.tenants, never()).selectById(99L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"status", "limit", "expiry", "datasourceId", "datasourceCode"})
    void tenantAdministratorCannotSmugglePlatformFieldsIntoBrandingUpdate(String field) {
        Dependencies d = new Dependencies();
        SysTenantDTO dto = request(TENANT);
        switch (field) {
            case "status" -> dto.setTenantStatus(0);
            case "limit" -> dto.setUserLimit(0);
            case "expiry" -> dto.setExpireTime(LocalDateTime.of(2099, 1, 1, 0, 0));
            case "datasourceId" -> dto.setDefaultBusinessDatasourceId(99L);
            case "datasourceCode" -> dto.setDefaultBusinessDatasourceCode("configured-but-unassigned");
            default -> throw new IllegalArgumentException(field);
        }
        try (MockedStatic<SessionHelper> ignored = login(1)) {
            denied(() -> d.service.updateTenant(dto));
        }
        verifyNoInteractions(d.tenants);
    }

    @Test
    void tenantAdministratorCanUpdateOnlyBrandingAndContactFields() {
        Dependencies d = new Dependencies();
        SysTenantDTO dto = request(TENANT);
        dto.setTenantName("Acme 中文");
        dto.setContactPerson("张三");
        dto.setContactPhone("12345");
        dto.setSystemName("Acme Portal");
        dto.setSystemLogo("file-id");
        dto.setThemeConfig("{\"primaryColor\":\"blue\"}");
        when(d.tenants.updateById(any(SysTenant.class))).thenReturn(1);
        try (MockedStatic<SessionHelper> ignored = login(1)) {
            assertThat(d.service.updateTenant(dto)).isTrue();
        }
        ArgumentCaptor<SysTenant> update = ArgumentCaptor.forClass(SysTenant.class);
        verify(d.tenants).updateById(update.capture());
        SysTenant tenant = update.getValue();
        assertThat(tenant.getId()).isEqualTo(TENANT);
        assertThat(tenant.getTenantName()).isEqualTo("Acme 中文");
        assertThat(tenant.getContactPerson()).isEqualTo("张三");
        assertThat(tenant.getSystemLogo()).isEqualTo("file-id");
        assertThat(tenant.getThemeConfig()).isEqualTo(dto.getThemeConfig());
        assertThat(tenant.getUserLimit()).isNull();
        assertThat(tenant.getTenantStatus()).isNull();
        assertThat(tenant.getExpireTime()).isNull();
        assertThat(tenant.getDefaultBusinessDatasourceId()).isNull();
        assertThat(tenant.getDefaultBusinessDatasourceCode()).isNull();
    }

    @Test
    void tenantAdministratorCannotManageAnotherTenantOrCreateAndDeleteTenants() {
        Dependencies d = new Dependencies();
        try (MockedStatic<SessionHelper> ignored = login(1)) {
            denied(() -> d.service.updateTenant(request(99L)));
            denied(() -> d.service.selectTenantUsers(99L, null));
            denied(() -> d.service.removeTenantUser(99L, TARGET));
            assertThatThrownBy(() -> d.service.insertTenant(request(TENANT))).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> d.service.deleteTenantById(TENANT)).isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(d.tenants, d.users, d.members);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void tenantAdministratorCannotRemoveSuperAdministratorTenantAdministratorOrUnknownMember(int kind) {
        Dependencies d = removableTarget();
        d.target.setUserType(kind == 0 ? 0 : 2);
        d.binding.setMemberType(kind == 1 ? 1 : 2);
        if (kind == 3) {
            d.binding.setMemberType(null);
        }
        try (MockedStatic<SessionHelper> ignored = login(1)) {
            assertThatThrownBy(() -> d.service.removeTenantUser(TENANT, TARGET)).isInstanceOf(RuntimeException.class);
        }
        verify(d.members, never()).delete(any(Wrapper.class));
        verifyNoInteractions(d.roles, d.orgRoles, d.orgs, d.posts);
    }

    @Test
    void tenantAdministratorCanListAndRemoveOrdinaryMemberWithoutDeletingOtherTenantMembership() {
        Dependencies d = removableTarget();
        SysUserTenant other = new SysUserTenant();
        other.setTenantId(8L);
        other.setIsDefault(1);
        when(d.members.selectList(any())).thenReturn(List.of(other));
        when(d.members.delete(any(Wrapper.class))).thenReturn(1);
        Page<SysUser> page = new Page<>();
        when(d.users.selectUserPage(any(), any())).thenReturn(page);
        SysUserQuery query = new SysUserQuery();
        query.setTenantId(99L);
        try (MockedStatic<SessionHelper> ignored = login(1)) {
            assertThat(d.service.selectTenantUsers(TENANT, query)).isSameAs(page);
            assertThat(query.getTenantId()).isEqualTo(TENANT);
            assertThat(d.service.removeTenantUser(TENANT, TARGET)).isTrue();
            assertThatThrownBy(() -> d.service.removeTenantUser(TENANT, CALLER))
                    .hasMessage("不能将当前登录用户移出当前租户");
        }
        ArgumentCaptor<Wrapper<SysUserTenant>> scope = ArgumentCaptor.forClass(Wrapper.class);
        verify(d.members).delete(scope.capture());
        assertThat(scope.getValue().getSqlSegment()).contains("user_id", "tenant_id");
        verify(d.users, never()).deleteById(anyLong());
    }

    @Test
    void superAdministratorCanManagePlatformFieldsAndMembersAcrossTenants() {
        Dependencies d = removableTarget();
        SysTenantDTO dto = request(99L);
        dto.setTenantStatus(1);
        dto.setUserLimit(100);
        dto.setExpireTime(LocalDateTime.of(2030, 1, 1, 0, 0));
        dto.setDefaultBusinessDatasourceId(3L);
        dto.setDefaultBusinessDatasourceCode("trusted-business");
        TenantDatasourceOption option = new TenantDatasourceOption();
        option.setDatasourceId(3L);
        option.setDatasourceCode("trusted-business");
        when(d.access.requireWritable(99L, 3L, "trusted-business")).thenReturn(option);
        when(d.tenants.updateById(any(SysTenant.class))).thenReturn(1);
        when(d.members.delete(any(Wrapper.class))).thenReturn(1);
        when(d.members.selectList(any())).thenReturn(List.of());
        try (MockedStatic<SessionHelper> ignored = login(0)) {
            assertThat(d.service.updateTenant(dto)).isTrue();
            d.service.selectTenantUsers(99L, null);
            assertThat(d.service.removeTenantUser(99L, TARGET)).isTrue();
        }
        ArgumentCaptor<SysTenant> update = ArgumentCaptor.forClass(SysTenant.class);
        verify(d.tenants).updateById(update.capture());
        assertThat(update.getValue().getUserLimit()).isEqualTo(100);
        assertThat(update.getValue().getDefaultBusinessDatasourceCode()).isEqualTo("trusted-business");
    }

    private static void denied(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(403));
    }

    private static SysTenantDTO request(long id) {
        SysTenantDTO dto = new SysTenantDTO();
        dto.setId(id);
        return dto;
    }

    private static MockedStatic<SessionHelper> login(int type, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(CALLER);
        user.setTenantId(TENANT);
        user.setUserType(type);
        user.setPermissions(Set.of(permissions));
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getLoginUser).thenReturn(user);
        return session;
    }

    private static Dependencies removableTarget() {
        Dependencies d = new Dependencies();
        d.target.setId(TARGET);
        d.target.setTenantId(TENANT);
        d.target.setUserType(2);
        d.binding.setTenantId(TENANT);
        d.binding.setUserId(TARGET);
        d.binding.setMemberType(2);
        when(d.users.selectById(TARGET)).thenReturn(d.target);
        when(d.members.selectOne(any())).thenReturn(d.binding);
        return d;
    }

    private static final class Dependencies {
        final SysTenantMapper tenants = mock(SysTenantMapper.class);
        final SysUserMapper users = mock(SysUserMapper.class);
        final SysUserTenantMapper members = mock(SysUserTenantMapper.class);
        final SysUserRoleMapper roles = mock(SysUserRoleMapper.class);
        final SysUserOrgRoleMapper orgRoles = mock(SysUserOrgRoleMapper.class);
        final SysUserOrgMapper orgs = mock(SysUserOrgMapper.class);
        final SysUserPostMapper posts = mock(SysUserPostMapper.class);
        final SysUser target = new SysUser();
        final SysUserTenant binding = new SysUserTenant();
        final TenantDatasourceAccessService access = mock(TenantDatasourceAccessService.class);
        final SysTenantServiceImpl service = new SysTenantServiceImpl(tenants, users, members,
                roles, orgRoles, orgs, posts, mock(IUserLoadService.class), access);
    }
}
