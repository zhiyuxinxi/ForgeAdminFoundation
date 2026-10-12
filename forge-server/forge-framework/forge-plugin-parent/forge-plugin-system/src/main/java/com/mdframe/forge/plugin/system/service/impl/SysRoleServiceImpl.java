package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.RoleDataScopeSettingsDTO;
import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.SysRoleDTO;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleOrg;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.plugin.system.service.ISysRoleService;
import com.mdframe.forge.plugin.system.vo.RoleDataScopeSettingsVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.PermissionMatcher;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private static final String[] ROLE_MANAGEMENT_PERMISSIONS = {
            "system:role:list", "system:role:query", "system:role:add", "system:role:edit", "system:role:remove"
    };

    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysResourceMapper resourceMapper;
    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final SysRoleModuleDataScopeMapper roleModuleDataScopeMapper;
    private final IDataScopeService dataScopeService;
    @Lazy
    private final ISysResourceService resourceService;

    @Override
    public IPage<SysRole> selectRolePage(SysRoleQuery query) {
        assertRoleManagementAllowed();
        query = query == null ? new SysRoleQuery() : query;
        normalizeRoleQueryTenant(query);
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        SysRoleQuery finalQuery = query;
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRolePage(page, finalQuery));
    }

    @Override
    public SysRole selectRoleById(Long id) {
        loadRoleForAccess(id);
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRoleById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertRole(SysRoleDTO dto) {
        assertRoleActionAllowed("system:role:add");
        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        role.setTenantId(resolveWriteTenantId(dto.getTenantId()));
        role.setOrgScopeType(RoleOrganizationScope.resolveOrgScopeType(dto.getOrgScopeType()));
        dataScopePolicy().validateDataScopeAllowedForCurrentUser(role.getDataScope());
        boolean inserted = TenantContextHolder.executeIgnore(() -> roleMapper.insert(role) > 0);
        if (inserted && dto.getOrgIds() != null) {
            organizationScope().replace(role, dto.getOrgIds());
        }
        if (inserted) {
            scopeConfiguration().refreshAfterCommit();
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRoleDTO dto) {
        assertRoleActionAllowed("system:role:edit");
        SysRole existing = loadRoleForAccess(dto.getId());
        assertCanMaintainRole(existing);
        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        role.setOrgScopeType(null);
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : existing.getTenantId();
            role.setTenantId(resolveWriteTenantId(tenantId));
        } else {
            role.setTenantId(null);
        }
        Integer nextDataScope = dto.getDataScope() != null ? dto.getDataScope() : existing.getDataScope();
        dataScopePolicy().validateDataScopeAllowedForCurrentUser(nextDataScope);
        dataScopePolicy().validateDataScopeAllowedForBoundUsers(existing, nextDataScope);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(existing, userRoleMapper, userOrgRoleMapper);
        boolean updated = TenantContextHolder.executeIgnore(() -> roleMapper.updateById(role) > 0);
        if (updated && dto.getOrgIds() != null) {
            bindRoleOrgs(existing.getId(), dto.getOrgIds());
        }
        if (updated) {
            scopeConfiguration().refreshAfterCommit();
            revocation.afterCommit();
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleById(Long id) {
        assertRoleActionAllowed("system:role:remove");
        SysRole role = loadRoleForAccess(id);
        assertCanMaintainRole(role, "system:role:remove");
        validateRoleDeletable(role);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        boolean deleted = TenantContextHolder.executeIgnore(() -> {
            roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>()
                    .eq(SysRoleResource::getRoleId, id)
                    .eq(SysRoleResource::getTenantId, role.getTenantId()));
            roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                    .eq(SysRoleOrg::getRoleId, id)
                    .eq(SysRoleOrg::getTenantId, role.getTenantId()));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getRoleId, id)
                    .eq(SysUserOrgRole::getTenantId, role.getTenantId()));
            return roleMapper.deleteById(id) > 0;
        });
        if (deleted) {
            scopeConfiguration().refreshAfterCommit();
            revocation.afterCommit();
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return false;
        }
        for (Long id : ids) {
            deleteRoleById(id);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleResources(Long roleId, Long[] resourceIds) {
        return bindRoleResources(roleId, resourceIds, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleResources(Long roleId, Long[] resourceIds, String clientCode) {
        if (roleId == null) {
            return false;
        }
        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        RoleResourceSelection.Replacement selection = new RoleResourceSelection(resourceService, resourceMapper)
                .replacement(resourceIds, clientCode);
        if (selection.isNoOp()) {
            return true;
        }
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        int deleted = resourceBindings().replace(role, selection);
        if (deleted > 0 || !selection.selectedIds().isEmpty()) {
            revocation.afterCommit();
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveScopedRolePermissions(Long roleId, ScopedRolePermissionDTO settings) {
        if (settings == null) {
            throw new RuntimeException("范围化角色权限不能为空");
        }
        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        RoleResourceSelection.Scoped selection = new RoleResourceSelection(resourceService, resourceMapper).scoped(settings);
        Set<String> scopeModuleCodes = dataScopePolicy().normalizeModuleCodes(settings.getScopeModuleCodes());
        Map<String, Integer> moduleScopes = dataScopePolicy().moduleOverrides(
                settings.getModuleScopes(), scopeModuleCodes, role, true);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        boolean saved = resourceBindings().replaceScoped(role, selection, scopeModuleCodes, moduleScopes)
                && refreshDataScopeCacheAfterScopedSave();
        if (saved) {
            revocation.afterCommit();
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindRoleResources(Long roleId, Long[] resourceIds) {
        if (roleId == null || resourceIds == null || resourceIds.length == 0) {
            return false;
        }
        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        boolean deleted = resourceBindings().unbind(role, resourceIds);
        if (deleted) {
            revocation.afterCommit();
        }
        return deleted;
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId) {
        return selectRoleResourceIds(roleId, null);
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId, String clientCode) {
        return selectRoleResourceIds(roleId, clientCode, false);
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId, String clientCode, boolean includeParents) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        SysRole role = loadRoleForAccess(roleId);
        return new RoleResourceProjection(roleResourceMapper, resourceService, resourceMapper)
                .selectedIds(role, clientCode, includeParents);
    }

    private RoleResourceBindings resourceBindings() {
        return new RoleResourceBindings(roleResourceMapper, roleModuleDataScopeMapper);
    }

    @Override
    public List<Long> selectRoleOrgIds(Long roleId) {
        return roleId == null ? new ArrayList<>() : organizationScope().selectedIds(loadRoleForAccess(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleOrgs(Long roleId, List<Long> orgIds) {
        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        organizationScope().replace(role, orgIds);
        revocation.afterCommit();
        return true;
    }

    @Override
    public List<Long> selectCurrentUserRoleIds() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return new ArrayList<>();
        }
        if (loginUser.isAdmin()) {
            return TenantContextHolder.executeIgnore(() -> roleMapper.selectList(new LambdaQueryWrapper<SysRole>().select(SysRole::getId)))
                    .stream().map(SysRole::getId).collect(Collectors.toList());
        }
        return loginUser.getRoleIds();
    }

    @Override
    public RoleDataScopeSettingsVO getRoleDataScopeSettings(Long roleId) {
        return scopeConfiguration().read(loadRoleForAccess(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleDataScopeSettings(Long roleId, RoleDataScopeSettingsDTO settings) {
        if (settings == null) {
            throw new RuntimeException("数据权限设置不能为空");
        }
        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        RoleDataScopePolicy policy = dataScopePolicy();
        policy.validateSetting(role, settings.getDefaultDataScope());
        RoleDataScopeConfiguration configuration = scopeConfiguration();
        Set<String> availableCodes = configuration.availableModules(role);
        Map<String, Integer> overrides = policy.moduleOverrides(settings.getModuleScopes(), availableCodes, role, false);
        RoleSessionRevocation revocation = RoleSessionRevocation.capture(role, userRoleMapper, userOrgRoleMapper);
        boolean saved = configuration.save(role, settings.getDefaultDataScope(), availableCodes, overrides);
        if (saved) {
            configuration.refreshAfterCommit();
            revocation.afterCommit();
        }
        return saved;
    }

    private boolean refreshDataScopeCacheAfterScopedSave() {
        scopeConfiguration().refreshAfterCommit();
        return true;
    }

    @Override
    public IPage<SysUser> selectRoleUsers(RoleUserQuery query) {
        if (query.getRoleId() == null) {
            return new Page<>();
        }
        SysRole role = loadRoleForAccess(query.getRoleId());
        query.setTenantId(role.getTenantId());
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRoleUsers(page, query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRole(Long roleId, Long userId) {
        if (roleId == null || userId == null) {
            return false;
        }

        SysRole role = loadRoleForAccess(roleId);
        assertCanMaintainRole(role);
        boolean removed = TenantContextHolder.executeIgnore(() -> {
            int orgRemoved = userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getRoleId, roleId)
                    .eq(SysUserOrgRole::getTenantId, role.getTenantId())
                    .eq(SysUserOrgRole::getUserId, userId));
            int legacyRemoved = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, roleId)
                    .eq(SysUserRole::getTenantId, role.getTenantId())
                    .eq(SysUserRole::getUserId, userId));
            return orgRemoved > 0 || legacyRemoved > 0;
        });
        if (removed) {
            UserSessionRevocation.afterCommit(userId, role.getTenantId());
        }
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUsersToRole(Long roleId, List<Long> userIds) {
        throw new RuntimeException("旧角色加人接口已废弃，请选择授权组织后使用组织角色授权");
    }

    private void normalizeRoleQueryTenant(SysRoleQuery query) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            query.setTenantId(loginUser.getTenantId());
            query.setAccessibleRoleIds(loginUser.isTenantAdmin() ? null : loginUser.getRoleIds() == null
                    ? Collections.emptyList()
                    : loginUser.getRoleIds());
        }
    }

    private void assertRoleManagementAllowed() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, ROLE_MANAGEMENT_PERMISSIONS)) {
            throw new RuntimeException("无权访问角色管理功能");
        }
    }

    private void assertRoleActionAllowed(String permission) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && !loginUser.isTenantAdmin() && !hasAnyPermission(loginUser, permission)) {
            throw new RuntimeException("无权访问角色管理功能");
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private boolean hasAnyPermission(LoginUser loginUser, String... permissions) {
        return PermissionMatcher.hasAnyPermission(loginUser == null ? null : loginUser.getPermissions(), permissions);
    }

    private Long resolveWriteTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.isAdmin()
                ? (requestedTenantId != null ? requestedTenantId : loginUser.getTenantId())
                : loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    private void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getId, tenantId)
                        .eq(SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }

    private SysRole loadRoleForAccess(Long roleId) {
        assertRoleManagementAllowed();
        if (roleId == null) {
            throw new RuntimeException("角色ID不能为空");
        }
        SysRole role = TenantContextHolder.executeIgnore(() -> roleMapper.selectById(roleId));
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && (loginUser.getTenantId() == null
                || !Objects.equals(role.getTenantId(), loginUser.getTenantId()))) {
            throw new RuntimeException("无权操作非本租户角色");
        }
        if (!loginUser.isAdmin() && !loginUser.isTenantAdmin()
                && (loginUser.getRoleIds() == null || !loginUser.getRoleIds().contains(roleId))) {
            throw new RuntimeException("无权操作未委派给自己的角色");
        }
        return role;
    }

    private void validateRoleDeletable(SysRole role) {
        Long userCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countUsersByRole(role.getId(), role.getTenantId()));
        if (userCount != null && userCount > 0) {
            throw new RuntimeException("当前角色已绑定用户，不能删除");
        }
    }

    private void assertCanMaintainRole(SysRole role) {
        assertCanMaintainRole(role, "system:role:edit");
    }

    private void assertCanMaintainRole(SysRole role, String permission) {
        assertRoleActionAllowed(permission);
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new RuntimeException("系统内置角色只能由超级管理员维护");
        }
        if (loginUser.getRoleIds() != null && loginUser.getRoleIds().contains(role.getId())) {
            throw new RuntimeException("不能维护自己当前绑定的角色");
        }
    }

    private void validateRoleAssignableToUsers(SysRole role, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(normalizedUserIds)) {
            return;
        }
        Long tenantId = role.getTenantId();
        Long assignableCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countAssignableTargetUsers(normalizedUserIds, tenantId));
        if (assignableCount == null || assignableCount != normalizedUserIds.size()) {
            throw new RuntimeException("目标用户不存在或不属于当前租户");
        }
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            Long nonNormalCount = TenantContextHolder.executeIgnore(() ->
                    roleMapper.countNonNormalTargetUsers(normalizedUserIds, tenantId));
            if (nonNormalCount != null && nonNormalCount > 0) {
                throw new RuntimeException("租户管理员只能给普通用户分配角色");
            }
        }
        dataScopePolicy().validateDataScopeAllowedForUsers(scopeConfiguration().mostPermissiveRange(role), normalizedUserIds, tenantId);
    }

    private RoleDataScopePolicy dataScopePolicy() {
        return new RoleDataScopePolicy(roleMapper, requireLoginUser());
    }

    private RoleOrganizationScope organizationScope() {
        return new RoleOrganizationScope(roleMapper, roleOrgMapper, userOrgRoleMapper, orgMapper);
    }

    private RoleDataScopeConfiguration scopeConfiguration() {
        return new RoleDataScopeConfiguration(roleMapper, dataScopeConfigMapper,
                roleModuleDataScopeMapper, resourceMapper, dataScopeService);
    }
}
