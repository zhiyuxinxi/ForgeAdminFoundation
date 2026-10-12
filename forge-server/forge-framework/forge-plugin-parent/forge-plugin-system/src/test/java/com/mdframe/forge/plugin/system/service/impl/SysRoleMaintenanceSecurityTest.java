package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.*;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.*;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.auth.config.StpInterfaceImpl;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real Sa-Token and transaction synchronization; only persistence/resource catalog are mocked. */
class SysRoleMaintenanceSecurityTest {
    private static final Long ROLE = 42L;
    private static final Long TENANT = 7L;
    private final SysRoleMapper roles = mock(SysRoleMapper.class);
    private final SysRoleOrgMapper roleOrgs = mock(SysRoleOrgMapper.class);
    private final SysRoleResourceMapper roleResources = mock(SysRoleResourceMapper.class);
    private final SysUserRoleMapper userRoles = mock(SysUserRoleMapper.class);
    private final SysUserOrgRoleMapper orgRoles = mock(SysUserOrgRoleMapper.class);
    private final SysOrgMapper orgs = mock(SysOrgMapper.class);
    private final ISysResourceService resources = mock(ISysResourceService.class);
    private final SysResourceMapper resourceRows = mock(SysResourceMapper.class);
    private final SysDataScopeConfigMapper scopeConfigs = mock(SysDataScopeConfigMapper.class);
    private final SysRoleModuleDataScopeMapper moduleScopes = mock(SysRoleModuleDataScopeMapper.class);
    private final IDataScopeService scopeCache = mock(IDataScopeService.class);
    private final SysRoleServiceImpl service = new SysRoleServiceImpl(roles, roleOrgs, roleResources,
            mock(SysTenantMapper.class), userRoles, orgRoles, orgs, mock(SysUserMapper.class),
            mock(SysUserTenantMapper.class), resourceRows, scopeConfigs, moduleScopes, scopeCache, resources);
    private final TransactionTemplate transaction =
            new TransactionTemplate(new UserSessionRevocationTest.TestTransactions());
    private final SysRole role = new SysRole();
    private StpLogic previousLogic;
    private SaTokenContext previousContext;
    private StpInterface previousPermissions;
    private SaTokenDao previousDao;
    private SaTokenDaoDefaultImpl tokenDao;
    private StpLogic logic;
    private String caller;
    private String legacyUser;
    private String orgUser;
    private String otherTenant;
    private String otherUser;

    @BeforeAll
    static void initializeMetadata() {
        for (Class<?> entity : new Class<?>[]{SysRole.class, SysRoleOrg.class, SysRoleResource.class,
                SysUserRole.class, SysUserOrgRole.class, SysOrg.class, SysRoleModuleDataScope.class}) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "auth013"), entity);
        }
    }

    @BeforeEach
    void setUp() {
        previousLogic = StpUtil.getStpLogic();
        previousContext = SaManager.getSaTokenContext();
        previousPermissions = SaManager.getStpInterface();
        previousDao = SaManager.getSaTokenDao();
        tokenDao = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(tokenDao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        SaManager.setStpInterface(new StpInterfaceImpl());
        logic = new StpLogic("auth013-" + UUID.randomUUID()).setConfig(new SaTokenConfig()
                .setTokenName("satoken").setIsConcurrent(true).setIsShare(false).setIsReadCookie(false));
        StpUtil.setStpLogic(logic);
        caller = token(100L, TENANT, 1);
        legacyUser = token(200L, TENANT, 2);
        orgUser = token(201L, TENANT, 2);
        otherTenant = token(200L, 99L, 2);
        otherUser = token(300L, TENANT, 2);
        bind(caller);
        role.setId(ROLE);
        role.setTenantId(TENANT);
        role.setDataScope(3);
        role.setIsSystem(0);
        when(roles.selectById(ROLE)).thenReturn(role);
        when(roles.updateById(any(SysRole.class))).thenReturn(1);
        when(roles.deleteById(ROLE)).thenReturn(1);
        when(roleResources.delete(any(Wrapper.class))).thenReturn(1);
        SysUserRole legacy = new SysUserRole();
        legacy.setUserId(200L);
        when(userRoles.selectList(any())).thenReturn(List.of(legacy));
        SysUserOrgRole org = new SysUserOrgRole();
        org.setUserId(201L);
        when(orgRoles.selectList(any())).thenReturn(List.of(org));
        when(resources.selectCurrentUserResourceIds()).thenReturn(List.of(50L));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        StpUtil.setStpLogic(previousLogic);
        SaManager.removeStpLogic(logic.getLoginType());
        SaManager.setSaTokenContext(previousContext);
        SaManager.setStpInterface(previousPermissions);
        SaManager.setSaTokenDao(previousDao);
        tokenDao.destroy();
    }

    @Test
    void tenantAdministratorListsAndReadsOtherRolesWithoutChangingTenantBoundary() {
        when(roles.selectRolePage(any(), any())).thenReturn(new Page<>());
        when(roles.selectRoleById(ROLE)).thenReturn(role);
        SysRoleQuery query = new SysRoleQuery();
        query.setTenantId(99L);
        query.setAccessibleRoleIds(List.of(31L));
        service.selectRolePage(query);
        assertThat(query.getTenantId()).isEqualTo(TENANT);
        assertThat(query.getAccessibleRoleIds()).isNull();
        assertThat(service.selectRoleById(ROLE)).isSameAs(role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "resources", "grant", "unbind", "scoped", "organizations", "dataScope"})
    void tenantAdministratorCanMaintainOtherRoleAndBothBindingPathsLoseStaleSessionsAfterCommit(String operation) {
        transaction.executeWithoutResult(state -> {
            assertThat(mutate(operation)).isTrue();
            assertThat(logic.getLoginIdByToken(legacyUser)).isNotNull();
            assertThat(logic.getLoginIdByToken(orgUser)).isNotNull();
        });
        assertRevoked(legacyUser);
        assertRevoked(orgUser);
        assertAllowed(otherTenant);
        assertAllowed(otherUser);
        assertAllowed(caller);
    }

    @Test
    void unboundRoleCanBeDeletedAndBoundRoleStillCannot() {
        when(userRoles.selectList(any())).thenReturn(List.of());
        when(orgRoles.selectList(any())).thenReturn(List.of());
        assertThat(service.deleteRoleById(ROLE)).isTrue();
        when(roles.countUsersByRole(ROLE, TENANT)).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteRoleById(ROLE)).hasMessage("当前角色已绑定用户，不能删除");
        verify(roles).deleteById(ROLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"foreign", "missingTenant", "builtin", "self", "ordinary", "ordinary-held"})
    void tenantBoundaryBuiltinsSelfRolesAndOrdinaryUsersStayProtected(String target) {
        LoginUser user = snapshot(caller);
        switch (target) {
            case "foreign" -> role.setTenantId(99L);
            case "missingTenant" -> {
                user.setTenantId(null);
                role.setTenantId(null);
            }
            case "builtin" -> role.setIsSystem(1);
            case "self" -> user.setRoleIds(List.of(ROLE));
            case "ordinary" -> user.setUserType(2);
            case "ordinary-held" -> {
                user.setUserType(2);
                user.setRoleIds(List.of(ROLE));
            }
            default -> throw new AssertionError(target);
        }
        assertThatThrownBy(() -> service.updateRole(update())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.bindRoleResources(ROLE, new Long[0])).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.deleteRoleById(ROLE)).isInstanceOf(RuntimeException.class);
        verify(roles, never()).updateById(any(SysRole.class));
        verify(roles, never()).deleteById(ROLE);
        verifyNoInteractions(roleResources);
    }

    @Test
    void tenantAdministratorCannotGrantResourcesOrDataScopeBeyondItsAuthority() {
        SysRoleDTO dto = update();
        dto.setDataScope(1);
        assertThatThrownBy(() -> service.updateRole(dto)).hasMessageContaining("超过当前用户类型");
        SysResource resource = new SysResource();
        resource.setId(99L);
        resource.setParentId(0L);
        when(resourceRows.selectBatchIds(anyCollection())).thenReturn(List.of(resource));
        assertThatThrownBy(() -> service.bindRoleResources(ROLE, new Long[]{99L}))
                .hasMessageContaining("不能分配自己没有的资源");
        verify(roles, never()).updateById(any(SysRole.class));
        verifyNoInteractions(roleResources);
    }

    @Test
    void roleReadPermissionsCannotCreateRoles() {
        snapshot(caller).setUserType(2);
        snapshot(caller).setPermissions(Set.of("system:role:list", "system:role:query"));
        assertThatThrownBy(() -> service.insertRole(update())).hasMessage("无权访问角色管理功能");
        verify(roles, never()).insert(any(SysRole.class));
    }

    @Test
    void ordinaryRoleEditRefreshesScopeOnlyAfterCommit() {
        transaction.executeWithoutResult(state -> {
            assertThat(service.updateRole(update())).isTrue();
            verifyNoInteractions(scopeCache);
        });
        verify(scopeCache).refreshDataScopeCache();
    }

    @Test
    void rolledBackRoleEditDoesNotPublishNewScope() {
        transaction.executeWithoutResult(state -> {
            assertThat(service.updateRole(update())).isTrue();
            state.setRollbackOnly();
        });
        verifyNoInteractions(scopeCache);
    }

    @Test
    void ordinaryRoleListStaysLimitedToHeldRoles() {
        snapshot(caller).setUserType(2);
        when(roles.selectRolePage(any(), any())).thenReturn(new Page<>());
        SysRoleQuery query = new SysRoleQuery();
        service.selectRolePage(query);
        assertThat(query.getAccessibleRoleIds()).containsExactly(31L);
        assertThat(query.getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void rollbackAndZeroWritesDoNotRevokeSessions() {
        transaction.executeWithoutResult(state -> {
            assertThat(service.unbindRoleResources(ROLE, new Long[]{50L})).isTrue();
            state.setRollbackOnly();
        });
        when(roleResources.delete(any(Wrapper.class))).thenReturn(0);
        when(roles.updateById(any(SysRole.class))).thenReturn(0);
        assertThat(service.unbindRoleResources(ROLE, new Long[]{50L})).isFalse();
        assertThat(service.updateRole(update())).isFalse();
        assertAllowed(legacyUser);
        assertAllowed(orgUser);
    }

    @Test
    void removingOrganizationOnlyRoleReturnsSuccessAndRevokesOnlyThatUserInTenant() {
        when(orgRoles.delete(any(Wrapper.class))).thenReturn(1);
        when(userRoles.delete(any(Wrapper.class))).thenReturn(0);
        transaction.executeWithoutResult(state -> assertThat(service.removeUserRole(ROLE, 201L)).isTrue());
        assertRevoked(orgUser);
        assertAllowed(legacyUser);
        assertAllowed(otherTenant);
    }

    @Test
    void dataScopePolicyRejectsCallerAndBoundUserEscalationBeforeWriting() {
        RoleDataScopeSettingsDTO settings = scopeSettings(1);
        assertThatThrownBy(() -> service.saveRoleDataScopeSettings(ROLE, settings))
                .hasMessageContaining("超过当前用户类型");
        settings.setDefaultDataScope(3);
        when(roles.countRoleUsersExceedingDataScope(3, ROLE, TENANT)).thenReturn(1L);
        assertThatThrownBy(() -> service.saveRoleDataScopeSettings(ROLE, settings))
                .hasMessage("角色数据范围超过目标用户类型上限");
        verify(roles, never()).updateById(any(SysRole.class));
        verifyNoInteractions(moduleScopes, scopeCache);
    }

    @Test
    void moduleScopeValidationKeepsNullInheritanceDuplicateAndAllowedModuleRules() {
        when(scopeConfigs.selectEnabledModuleConfigs(TENANT)).thenReturn(List.of(scopeConfig("ORDER", "订单")));
        RoleDataScopeSettingsDTO settings = scopeSettings(3);
        settings.setModuleScopes(List.of(moduleScope(" OTHER ", 3)));
        assertThatThrownBy(() -> service.saveRoleDataScopeSettings(ROLE, settings))
                .hasMessageContaining("业务模块不存在或未启用：OTHER");
        settings.setModuleScopes(List.of(moduleScope("ORDER", null), moduleScope(" ORDER ", 3)));
        assertThatThrownBy(() -> service.saveRoleDataScopeSettings(ROLE, settings))
                .hasMessageContaining("业务模块重复配置：ORDER");
        settings.setModuleScopes(List.of(moduleScope(" ORDER ", null)));
        assertThat(service.saveRoleDataScopeSettings(ROLE, settings)).isTrue();
        verify(moduleScopes).deleteByRoleAndModules(TENANT, ROLE, Set.of("ORDER"));
        verify(moduleScopes, never()).insert(any(SysRoleModuleDataScope.class));
    }

    @Test
    void organizationSelectionRejectsForeignRowsBeforeRoleOrBindingWrites() {
        when(orgs.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.bindRoleOrgs(ROLE, List.of(30L, 30L)))
                .hasMessage("组织不属于当前角色租户");
        verify(roles, never()).updateById(any(SysRole.class));
        verify(roleOrgs, never()).delete(any(Wrapper.class));
        verify(orgRoles, never()).delete(any(Wrapper.class));
        verify(orgs).selectCount(argThat(wrapper -> {
            var query = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOrg>) wrapper;
            assertThat(query.getSqlSegment()).contains("tenant_id", "id IN");
            assertThat(query.getParamNameValuePairs().values()).containsExactlyInAnyOrder(30L, TENANT);
            return true;
        }));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dataScopeCacheAndSessionsFollowCommitRatherThanSave(boolean rollback) {
        when(scopeConfigs.selectEnabledModuleConfigs(TENANT)).thenReturn(List.of(scopeConfig("ORDER", "订单")));
        RoleDataScopeSettingsDTO settings = scopeSettings(3);
        settings.setModuleScopes(List.of(moduleScope("ORDER", 4)));
        transaction.executeWithoutResult(state -> {
            assertThat(service.saveRoleDataScopeSettings(ROLE, settings)).isTrue();
            verifyNoInteractions(scopeCache);
            assertThat(logic.getLoginIdByToken(legacyUser)).isNotNull();
            if (rollback) {
                state.setRollbackOnly();
            }
        });
        verify(moduleScopes).insert(argThat((SysRoleModuleDataScope value) ->
                TENANT.equals(value.getTenantId()) && ROLE.equals(value.getRoleId())
                        && "ORDER".equals(value.getModuleCode()) && Integer.valueOf(4).equals(value.getDataScope())));
        if (rollback) {
            verifyNoInteractions(scopeCache);
            assertAllowed(legacyUser);
            assertAllowed(orgUser);
        } else {
            verify(scopeCache).refreshDataScopeCache();
            assertRevoked(legacyUser);
            assertRevoked(orgUser);
        }
        assertAllowed(otherTenant);
        assertAllowed(otherUser);
    }

    @Test
    void moduleProjectionKeepsMenuNamesCountsAndInheritedRanges() {
        when(scopeConfigs.selectEnabledModuleConfigs(TENANT)).thenReturn(List.of(
                scopeConfig("ORDER", "fallback"), scopeConfig("ORDER", "fallback"), scopeConfig("USER", "用户")));
        SysResource menu = new SysResource();
        menu.setId(1L);
        menu.setResourceType(2);
        menu.setResourceName("订单");
        SysResource action = new SysResource();
        action.setId(2L);
        action.setParentId(1L);
        action.setPerms("ORDER:query");
        when(resourceRows.selectModuleNamingResources(TENANT)).thenReturn(List.of(menu, action));
        SysRoleModuleDataScope override = new SysRoleModuleDataScope();
        override.setModuleCode("ORDER");
        override.setDataScope(4);
        when(moduleScopes.selectByRole(TENANT, ROLE)).thenReturn(List.of(override));
        var result = service.getRoleDataScopeSettings(ROLE);
        assertThat(result.getDefaultDataScope()).isEqualTo(3);
        assertThat(result.getModules()).extracting("moduleCode", "moduleName", "ruleCount", "dataScope", "effectiveDataScope")
                .containsExactly(tuple("USER", "用户", 1, null, 3), tuple("ORDER", "订单", 2, 4, 4));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void organizationSelectionPreservesGlobalAndSelectiveBindingSemantics(boolean global) {
        SysRoleOrg old = new SysRoleOrg();
        old.setOrgId(10L);
        when(roleOrgs.selectList(any())).thenReturn(List.of(old));
        when(orgs.selectCount(any())).thenReturn(1L);
        List<Long> requested = global ? null : java.util.Arrays.asList(null, 30L, 30L);
        transaction.executeWithoutResult(state -> assertThat(service.bindRoleOrgs(ROLE, requested)).isTrue());
        verify(roles).updateById(argThat((SysRole value) -> ROLE.equals(value.getId())
                && TENANT.equals(value.getTenantId()) && Integer.valueOf(global ? SystemConstants.RoleOrgScope.GLOBAL : SystemConstants.RoleOrgScope.CUSTOM).equals(value.getOrgScopeType())));
        if (global) {
            verify(orgRoles, never()).delete(any(Wrapper.class));
            verify(roleOrgs, never()).insert(any(SysRoleOrg.class));
        } else {
            verify(roleOrgs).insert(argThat((SysRoleOrg value) -> ROLE.equals(value.getRoleId())
                    && TENANT.equals(value.getTenantId()) && Long.valueOf(30).equals(value.getOrgId())));
            verify(orgRoles).delete(argThat(wrapper -> {
                var query = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserOrgRole>) wrapper;
                assertThat(query.getSqlSegment()).contains("tenant_id", "role_id", "org_id IN");
                assertThat(query.getParamNameValuePairs().values()).containsExactlyInAnyOrder(TENANT, ROLE, 10L);
                return true;
            }));
        }
        assertRevoked(legacyUser);
        assertRevoked(orgUser);
        assertAllowed(otherTenant);
    }

    @Test
    void scopedModulesKeepTheirSeparateNullAndScopeBoundaryContract() {
        ScopedRolePermissionDTO settings = new ScopedRolePermissionDTO();
        settings.setScopeResourceIds(Set.of());
        settings.setSelectedResourceIds(Set.of());
        settings.setScopeModuleCodes(Set.of(" ORDER "));
        settings.setModuleScopes(List.of(moduleScope("OTHER", 3)));
        assertThatThrownBy(() -> service.saveScopedRolePermissions(ROLE, settings))
                .hasMessageContaining("业务模块不属于当前授权范围：OTHER");
        verifyNoInteractions(moduleScopes);
        settings.setModuleScopes(List.of(moduleScope("ORDER", null), moduleScope(" ORDER ", 4)));
        assertThat(service.saveScopedRolePermissions(ROLE, settings)).isTrue();
        verify(moduleScopes).deleteByRoleAndModules(TENANT, ROLE, Set.of("ORDER"));
        verify(scopeCache).refreshDataScopeCache();
    }

    private RoleDataScopeSettingsDTO scopeSettings(int scope) {
        RoleDataScopeSettingsDTO settings = new RoleDataScopeSettingsDTO();
        settings.setDefaultDataScope(scope);
        return settings;
    }

    private RoleModuleDataScopeDTO moduleScope(String code, Integer scope) {
        RoleModuleDataScopeDTO module = new RoleModuleDataScopeDTO();
        module.setModuleCode(code);
        module.setDataScope(scope);
        return module;
    }

    private SysDataScopeConfig scopeConfig(String code, String name) {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setResourceCode(code);
        config.setResourceName(name);
        return config;
    }

    private boolean mutate(String operation) {
        return switch (operation) {
            case "update" -> service.updateRole(update());
            case "resources" -> service.bindRoleResources(ROLE, new Long[0]);
            case "grant" -> {
                SysResource resource = new SysResource();
                resource.setId(50L);
                resource.setParentId(0L);
                when(resourceRows.selectBatchIds(anyCollection())).thenReturn(List.of(resource));
                yield service.bindRoleResources(ROLE, new Long[]{50L});
            }
            case "unbind" -> service.unbindRoleResources(ROLE, new Long[]{50L});
            case "scoped" -> {
                ScopedRolePermissionDTO dto = new ScopedRolePermissionDTO();
                dto.setScopeResourceIds(Set.of());
                dto.setSelectedResourceIds(Set.of());
                dto.setScopeModuleCodes(Set.of());
                yield service.saveScopedRolePermissions(ROLE, dto);
            }
            case "organizations" -> {
                SysRoleOrg old = new SysRoleOrg();
                old.setOrgId(10L);
                when(roleOrgs.selectList(any())).thenReturn(List.of(old));
                when(orgs.selectCount(any())).thenReturn(1L);
                boolean result = service.bindRoleOrgs(ROLE, List.of(30L));
                var order = inOrder(orgRoles);
                order.verify(orgRoles).selectList(any());
                order.verify(orgRoles).delete(any(Wrapper.class));
                yield result;
            }
            case "dataScope" -> {
                RoleDataScopeSettingsDTO dto = new RoleDataScopeSettingsDTO();
                dto.setDefaultDataScope(3);
                yield service.saveRoleDataScopeSettings(ROLE, dto);
            }
            default -> throw new AssertionError(operation);
        };
    }

    private SysRoleDTO update() {
        SysRoleDTO dto = new SysRoleDTO();
        dto.setId(ROLE);
        dto.setRoleName("Managed role");
        return dto;
    }

    private String token(long userId, long tenantId, int type) {
        bind(null);
        String token = logic.createLoginSession(userId, new SaLoginModel());
        LoginUser snapshot = new LoginUser();
        snapshot.setUserId(userId);
        snapshot.setTenantId(tenantId);
        snapshot.setUserType(type);
        snapshot.setRoleIds(List.of(31L));
        snapshot.setPermissions(Set.of("system:role:edit"));
        logic.getTokenSessionByToken(token).set("loginUser", snapshot);
        return token;
    }

    private LoginUser snapshot(String token) {
        return (LoginUser) logic.getTokenSessionByToken(token).get("loginUser");
    }

    private void bind(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("satoken", token);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void assertRevoked(String token) {
        bind(token);
        assertThatThrownBy(StpUtil::checkLogin).isInstanceOf(NotLoginException.class);
    }

    private void assertAllowed(String token) {
        bind(token);
        assertThatCode(() -> {
            StpUtil.checkLogin();
            StpUtil.checkPermission("system:role:edit");
        }).doesNotThrowAnyException();
    }
}
