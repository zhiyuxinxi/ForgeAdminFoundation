package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
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

/** 用户角色及组织角色关系维护，写入范围由角色政策约束。 */
@RequiredArgsConstructor
final class UserRoleAssignments {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final UserAccessPolicy access;
    private final UserProfileOperations profile;
    private final TenantMembershipOperations memberships;
    private final UserRolePolicy rolePolicy;
    private final UserAssignmentQueries queries;

    boolean bindUserRoles(Long userId, Long[] roleIds) {
        return bindUserRoles(userId, roleIds, null);
    }

    boolean bindUserRoles(Long userId, Long[] roleIds, Long tenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }
        return syncUserRoles(userId, Arrays.asList(roleIds), tenantId);
    }

    boolean batchBindUserRoles(BatchUserRoleBindDTO dto) {
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty()
                || dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            return false;
        }
        List<Long> userIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> roleIds = dto.getRoleIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty() || roleIds.isEmpty()) {
            return false;
        }

        Long tenantId = dto.getTenantId();
        for (Long userId : userIds) {
            access.assertNotSelfManagement(userId);
            Set<Long> mergedRoleIds = new HashSet<>(queries.selectUserRoleIds(userId, tenantId));
            mergedRoleIds.addAll(roleIds);
            syncUserRoles(userId, new ArrayList<>(mergedRoleIds), tenantId);
        }
        return true;
    }

    boolean unbindUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null || roleIds.length == 0) {
            return false;
        }
        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        Long tenantId = access.resolveTenantScopedOperationTenantId(userId);

        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId)
                .in(SysUserRole::getRoleId, Arrays.asList(roleIds));
        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .in(SysUserOrgRole::getRoleId, Arrays.asList(roleIds)));
        boolean deleted = userRoleMapper.delete(wrapper) > 0;
        if (deleted) {
            profile.syncCurrentUserOrgSession(userId, tenantId);
        }
        return deleted;
    }

    boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId) {
        if (userId == null || orgId == null || roleIds == null) {
            return false;
        }
        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);

        Long operationTenantId = access.resolveTenantScopedOperationTenantId(userId, tenantId);
        rolePolicy.validateUserOrgMembership(userId, orgId, operationTenantId);

        List<Long> normalizedRoleIds = rolePolicy.normalizeRoleIds(roleIds);
        LoginUser loginUser = access.requireLoginUser();
        Set<Long> manageableRoleIds = rolePolicy.resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin()) {
            if (!Objects.equals(orgId, loginUser.getActiveOrgId())) {
                throw new RuntimeException("只能维护当前组织下的用户角色");
            }
            if (!manageableRoleIds.containsAll(normalizedRoleIds)) {
                throw new RuntimeException("权限溢出：不能分配自己没有的角色");
            }
        }

        rolePolicy.validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), operationTenantId);
        rolePolicy.validateRoleStatus(normalizedRoleIds, operationTenantId);
        rolePolicy.validateRolesApplicableToOrg(normalizedRoleIds, orgId, operationTenantId);
        rolePolicy.validateRoleDataScopeForTarget(normalizedRoleIds, userId, operationTenantId);

        LambdaQueryWrapper<SysUserOrgRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserOrgRole::getTenantId, operationTenantId)
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getOrgId, orgId);
        if (!loginUser.isAdmin()) {
            if (manageableRoleIds.isEmpty()) {
                return normalizedRoleIds.isEmpty();
            }
            deleteWrapper.in(SysUserOrgRole::getRoleId, manageableRoleIds);
        }
        if (!normalizedRoleIds.isEmpty()) {
            deleteWrapper.notIn(SysUserOrgRole::getRoleId, normalizedRoleIds);
        }
        userOrgRoleMapper.delete(deleteWrapper);

        if (!normalizedRoleIds.isEmpty()) {
            Set<Long> existingRoleIds = new HashSet<>(userOrgRoleMapper.selectUserOrgRoleIds(
                    operationTenantId, userId, orgId));
            for (Long roleId : normalizedRoleIds) {
                if (existingRoleIds.contains(roleId)) {
                    continue;
                }
                SysUserOrgRole userOrgRole = new SysUserOrgRole();
                userOrgRole.setTenantId(operationTenantId);
                userOrgRole.setUserId(userId);
                userOrgRole.setOrgId(orgId);
                userOrgRole.setRoleId(roleId);
                userOrgRoleMapper.insert(userOrgRole);
            }
        }

        profile.syncCurrentUserOrgSession(userId, operationTenantId);
        return true;
    }

    boolean syncUserRoles(Long userId, List<Long> roleIds) {
        return syncUserRoles(userId, roleIds, null);
    }

    boolean syncUserRoles(Long userId, List<Long> roleIds, Long requestedTenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }

        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }

        Long tenantId = access.resolveRoleBindTenantId(user, requestedTenantId);
        memberships.ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));

        List<Long> normalizedRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        LoginUser loginUser = access.requireLoginUser();
        Set<Long> manageableRoleIds = rolePolicy.resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin() && !manageableRoleIds.containsAll(normalizedRoleIds)) {
            throw new RuntimeException("权限溢出：不能分配自己没有的角色");
        }
        rolePolicy.validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), tenantId);
        rolePolicy.validateRoleDataScopeForTarget(normalizedRoleIds, userId, tenantId);

        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId);
        if (!loginUser.isAdmin()) {
            if (manageableRoleIds.isEmpty()) {
                return normalizedRoleIds.isEmpty();
            }
            deleteWrapper.in(SysUserRole::getRoleId, manageableRoleIds);
        }
        if (!normalizedRoleIds.isEmpty()) {
            deleteWrapper.notIn(SysUserRole::getRoleId, normalizedRoleIds);
        }
        userRoleMapper.delete(deleteWrapper);

        if (normalizedRoleIds.isEmpty()) {
            syncLegacyRolesToUserOrgs(userId, tenantId, normalizedRoleIds, loginUser, manageableRoleIds);
            return true;
        }

        List<Long> existingRoleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getTenantId, tenantId)
                        .in(SysUserRole::getRoleId, normalizedRoleIds))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        Set<Long> existingRoleIdSet = new HashSet<>(existingRoleIds);

        for (Long roleId : normalizedRoleIds) {
            if (existingRoleIdSet.contains(roleId)) {
                continue;
            }
            SysUserRole userRole = new SysUserRole();
            userRole.setTenantId(tenantId);
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }

        syncLegacyRolesToUserOrgs(userId, tenantId, normalizedRoleIds, loginUser, manageableRoleIds);
        return true;
    }

    void syncLegacyRolesToUserOrgs(Long userId,
                                           Long tenantId,
                                           List<Long> roleIds,
                                           LoginUser loginUser,
                                           Set<Long> manageableRoleIds) {
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId));
        if (userOrgs.isEmpty()) {
            return;
        }

        for (SysUserOrg userOrg : userOrgs) {
            List<Long> applicableRoleIds = roleIds.stream()
                    .filter(roleId -> rolePolicy.isRoleApplicableToOrg(roleId, userOrg.getOrgId(), tenantId))
                    .collect(Collectors.toList());

            LambdaQueryWrapper<SysUserOrgRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserOrgRole::getUserId, userId)
                    .eq(SysUserOrgRole::getTenantId, tenantId)
                    .eq(SysUserOrgRole::getOrgId, userOrg.getOrgId());
            if (!loginUser.isAdmin()) {
                if (manageableRoleIds.isEmpty()) {
                    continue;
                }
                deleteWrapper.in(SysUserOrgRole::getRoleId, manageableRoleIds);
            }
            if (!applicableRoleIds.isEmpty()) {
                deleteWrapper.notIn(SysUserOrgRole::getRoleId, applicableRoleIds);
            }
            userOrgRoleMapper.delete(deleteWrapper);

            Set<Long> existingRoleIds = new HashSet<>(userOrgRoleMapper.selectUserOrgRoleIds(
                    tenantId, userId, userOrg.getOrgId()));
            for (Long roleId : applicableRoleIds) {
                if (existingRoleIds.contains(roleId)) {
                    continue;
                }
                SysUserOrgRole userOrgRole = new SysUserOrgRole();
                userOrgRole.setTenantId(tenantId);
                userOrgRole.setUserId(userId);
                userOrgRole.setOrgId(userOrg.getOrgId());
                userOrgRole.setRoleId(roleId);
                userOrgRoleMapper.insert(userOrgRole);
            }
        }
        profile.syncCurrentUserOrgSession(userId, tenantId);
    }
}
