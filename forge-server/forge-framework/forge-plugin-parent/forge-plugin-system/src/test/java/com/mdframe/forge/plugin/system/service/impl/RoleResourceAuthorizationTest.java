package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoleResourceAuthorizationTest {
    @BeforeAll
    static void metadata() {
        for (Class<?> type : new Class<?>[]{SysResource.class, SysRoleResource.class,
                SysUserRole.class, SysUserOrgRole.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), type);
        }
    }

    @Test
    void clientReplacementIncludesLegacyAncestorsAndDeletesOnlyItsAssignableSet() {
        try (Fixture f = new Fixture()) {
            assertThat(f.service.bindRoleResources(7L, new Long[]{3L}, "pc")).isTrue();
            assertThat(f.inserted()).containsExactlyInAnyOrder(1L, 2L, 3L);
            LambdaQueryWrapper<SysRoleResource> deleted = f.deleted();
            assertThat(deleted.getSqlSegment()).contains("role_id =", "tenant_id =", "resource_id IN");
            assertThat(deleted.getParamNameValuePairs().values()).contains(7L, 20L, 1L, 2L, 3L, 5L)
                    .doesNotContain(9L);
        }
    }

    @Test
    void clientAndCallerAncestorPermissionsAreCheckedBeforeAnyWrite() {
        try (Fixture f = new Fixture()) {
            assertThatThrownBy(() -> f.service.bindRoleResources(7L, new Long[]{9L}, "pc"))
                    .hasMessageContaining("其他客户端的资源权限");
            f.user.setUserType(1);
            when(f.resources.selectCurrentUserResourceIds()).thenReturn(List.of(3L));
            assertThatThrownBy(() -> f.service.bindRoleResources(7L, new Long[]{3L}, "pc"))
                    .hasMessageContaining("自己没有的父级资源权限");
            verifyNoInteractions(f.bindings);
        }
    }

    @Test
    void scopedSaveRetainsOutsideBindingsAndAddsOnlyMissingAncestors() {
        try (Fixture f = new Fixture()) {
            when(f.bindings.selectResourceIdsByRole(20L, 7L)).thenReturn(List.of(1L, 9L));
            ScopedRolePermissionDTO settings = new ScopedRolePermissionDTO();
            settings.setClientCode("pc");
            settings.setScopeResourceIds(Set.of(2L, 3L));
            settings.setSelectedResourceIds(Set.of(3L));
            assertThat(f.service.saveScopedRolePermissions(7L, settings)).isTrue();
            assertThat(f.inserted()).containsExactlyInAnyOrder(2L, 3L);
            LambdaQueryWrapper<SysRoleResource> deleted = f.deleted();
            deleted.getSqlSegment();
            assertThat(deleted.getParamNameValuePairs().values()).contains(20L, 7L, 2L, 3L)
                    .doesNotContain(1L, 9L);
        }
    }

    @Test
    void resourceProjectionKeepsClientCallerAndLeafFiltering() {
        try (Fixture f = new Fixture()) {
            when(f.bindings.selectRoleResourceIdsForProjection(20L, 7L)).thenReturn(List.of(1L, 2L, 3L, 9L));
            assertThat(f.service.selectRoleResourceIds(7L, "pc", true)).containsExactly(1L, 2L, 3L);
            assertThat(f.service.selectRoleResourceIds(7L, "pc", false)).containsExactly(3L);
            f.user.setUserType(1);
            when(f.resources.selectCurrentUserResourceIds()).thenReturn(List.of(1L, 2L));
            assertThat(f.service.selectRoleResourceIds(7L, "pc", false)).containsExactly(2L);
        }
    }

    @Test
    void emptyClientCatalogDoesNotDeleteAndNullClientStillMeansFullReplacement() {
        try (Fixture f = new Fixture()) {
            when(f.catalog.selectRoleAssignableResources("pc")).thenReturn(List.of());
            assertThat(f.service.bindRoleResources(7L, null, "pc")).isTrue();
            verifyNoInteractions(f.bindings);
            assertThat(f.service.bindRoleResources(7L, null, null)).isTrue();
            assertThat(f.deleted().getSqlSegment()).doesNotContain("resource_id IN");
            assertThat(f.service.bindRoleResources(null, null, null)).isFalse();
            assertThat(f.service.unbindRoleResources(7L, new Long[0])).isFalse();
        }
    }

    private static SysResource resource(long id, long parentId, String client) {
        SysResource resource = new SysResource();
        resource.setId(id);
        resource.setParentId(parentId);
        resource.setClientCode(client);
        return resource;
    }

    private static final class Fixture implements AutoCloseable {
        final SysRoleResourceMapper bindings = mock(SysRoleResourceMapper.class);
        final SysResourceMapper catalog = mock(SysResourceMapper.class);
        final ISysResourceService resources = mock(ISysResourceService.class);
        final LoginUser user = new LoginUser();
        final MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        final SysRoleServiceImpl service;

        Fixture() {
            SysRoleMapper roles = mock(SysRoleMapper.class);
            service = new SysRoleServiceImpl(roles, mock(SysRoleOrgMapper.class), bindings,
                    mock(SysTenantMapper.class), mock(SysUserRoleMapper.class), mock(SysUserOrgRoleMapper.class),
                    mock(SysOrgMapper.class), mock(SysUserMapper.class), mock(SysUserTenantMapper.class), catalog,
                    mock(SysDataScopeConfigMapper.class), mock(SysRoleModuleDataScopeMapper.class),
                    mock(IDataScopeService.class), resources);
            SysRole role = new SysRole();
            role.setId(7L);
            role.setTenantId(20L);
            when(roles.selectById(7L)).thenReturn(role);
            user.setUserId(10L);
            user.setTenantId(20L);
            user.setUserType(0);
            user.setRoleIds(List.of(8L));
            user.setPermissions(Set.of("system:role:edit"));
            session.when(SessionHelper::getLoginUser).thenReturn(user);
            List<SysResource> available = List.of(resource(1, 0, null), resource(2, 1, ""),
                    resource(3, 2, "pc"), resource(5, 0, "pc"), resource(9, 0, "h5"));
            Map<Long, SysResource> byId = available.stream().collect(Collectors.toMap(SysResource::getId, r -> r));
            when(catalog.selectRoleAssignableResources("pc")).thenReturn(available.subList(0, 4));
            when(catalog.selectRoleAssignableResourceIds("pc")).thenReturn(List.of(1L, 2L, 3L, 5L));
            when(catalog.selectBatchIds(anyCollection())).thenAnswer(call -> {
                Collection<Long> ids = call.getArgument(0);
                return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
            });
        }

        List<Long> inserted() {
            ArgumentCaptor<List<SysRoleResource>> captor = ArgumentCaptor.forClass(List.class);
            verify(bindings).insertBatch(captor.capture());
            return captor.getValue().stream().map(SysRoleResource::getResourceId).toList();
        }

        LambdaQueryWrapper<SysRoleResource> deleted() {
            ArgumentCaptor<Wrapper<SysRoleResource>> captor = ArgumentCaptor.forClass(Wrapper.class);
            verify(bindings).delete(captor.capture());
            return (LambdaQueryWrapper<SysRoleResource>) captor.getValue();
        }

        @Override public void close() { session.close(); }
    }
}
