package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleOrg;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 角色分配的租户、组织适用性、可管理范围和数据范围判断。 */
@RequiredArgsConstructor
final class UserRolePolicy {

    private final SysUserOrgMapper userOrgMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final UserAccessPolicy access;

    void validateRoleTenant(Long[] roleIds, Long tenantId) {
        if (roleIds == null || roleIds.length == 0) {
            return;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, Arrays.asList(roleIds))
                .eq(SysRole::getTenantId, tenantId));
        if (count == null || count != roleIds.length) {
            throw new RuntimeException("角色不属于当前操作租户");
        }
    }

    List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null) {
            return new ArrayList<>();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    void validateUserOrgMembership(Long userId, Long orgId, Long tenantId) {
        Long count = TenantContextHolder.executeIgnore(() ->
                userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getTenantId, tenantId)
                        .eq(SysUserOrg::getUserId, userId)
                        .eq(SysUserOrg::getOrgId, orgId)));
        if (count == null || count == 0) {
            throw new RuntimeException("用户未加入目标组织");
        }
    }

    void validateRoleStatus(List<Long> roleIds, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleStatus, 1));
        if (count == null || count != roleIds.size()) {
            throw new RuntimeException("角色不存在或已禁用");
        }
    }

    void validateRolesApplicableToOrg(List<Long> roleIds, Long orgId, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .in(SysRole::getId, roleIds)
                .select(SysRole::getId, SysRole::getOrgScopeType));
        List<Long> customRoleIds = roles.stream()
                .filter(role -> !isGlobalRoleScope(role))
                .map(SysRole::getId)
                .collect(Collectors.toList());
        if (customRoleIds.isEmpty()) {
            return;
        }
        Long count = roleOrgMapper.selectCount(new LambdaQueryWrapper<SysRoleOrg>()
                .eq(SysRoleOrg::getTenantId, tenantId)
                .eq(SysRoleOrg::getOrgId, orgId)
                .in(SysRoleOrg::getRoleId, customRoleIds));
        if (count == null || count != customRoleIds.size()) {
            throw new RuntimeException("角色不适用于目标组织");
        }
    }

    boolean isRoleApplicableToOrg(Long roleId, Long orgId, Long tenantId) {
        if (roleId == null || orgId == null || tenantId == null) {
            return false;
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getId, roleId)
                .select(SysRole::getId, SysRole::getOrgScopeType));
        if (isGlobalRoleScope(role)) {
            return true;
        }
        Long count = roleOrgMapper.selectCount(new LambdaQueryWrapper<SysRoleOrg>()
                .eq(SysRoleOrg::getTenantId, tenantId)
                .eq(SysRoleOrg::getRoleId, roleId)
                .eq(SysRoleOrg::getOrgId, orgId));
        return count != null && count > 0;
    }

    boolean isGlobalRoleScope(SysRole role) {
        return role != null && Objects.equals(role.getOrgScopeType(), SystemConstants.RoleOrgScope.GLOBAL);
    }

    void validateRoleDataScopeForTarget(List<Long> roleIds, Long userId, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        int targetUserType = access.resolveEffectiveUserType(userId, tenantId);
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, tenantId));
        for (SysRole role : roles) {
            if (!isDataScopeAllowedForUserType(role.getDataScope(), targetUserType)) {
                throw new RuntimeException("角色数据范围超过目标用户类型上限");
            }
        }
    }

    boolean isDataScopeAllowedForUserType(Integer dataScope, int userType) {
        if (dataScope == null || userType == SystemConstants.UserType.SYSTEM_ADMIN) {
            return true;
        }
        if (userType == SystemConstants.UserType.TENANT_ADMIN) {
            return dataScope != SystemConstants.RoleDataScope.ALL;
        }
        return dataScope != SystemConstants.RoleDataScope.ALL
                && dataScope != SystemConstants.RoleDataScope.TENANT;
    }

    Set<Long> resolveManageableRoleIds(LoginUser loginUser) {
        if (loginUser == null || loginUser.isAdmin()) {
            return new HashSet<>();
        }
        List<Long> roleIds = loginUser.getRoleIds();
        return roleIds == null ? new HashSet<>() : new HashSet<>(roleIds);
    }

    boolean isRoleVisibleForCurrentUser(Long roleId) {
        LoginUser loginUser = access.requireLoginUser();
        return loginUser.isAdmin()
                || (loginUser.getRoleIds() != null && loginUser.getRoleIds().contains(roleId));
    }
}
