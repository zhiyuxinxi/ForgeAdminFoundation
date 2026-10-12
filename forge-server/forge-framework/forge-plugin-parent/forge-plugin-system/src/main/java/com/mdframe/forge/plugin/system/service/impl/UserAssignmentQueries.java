package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 用户角色、组织、岗位及组织角色的只读装配。 */
@RequiredArgsConstructor
final class UserAssignmentQueries {

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysOrgMapper orgMapper;
    private final UserAccessPolicy access;
    private final UserRolePolicy rolePolicy;

    List<Long> selectUserRoleIds(Long userId) {
        return selectUserRoleIds(userId, null);
    }

    List<Long> selectUserRoleIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        access.assertCanReadUser(userId);

        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, access.resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserRole::getRoleId);
        return userRoleMapper.selectList(wrapper)
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(rolePolicy::isRoleVisibleForCurrentUser)
                .collect(Collectors.toList());
    }

    List<Long> selectUserOrgIds(Long userId) {
        return selectUserOrgIds(userId, null);
    }

    List<Long> selectUserOrgIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        access.assertCanReadUser(userId);

        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, access.resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserOrg::getOrgId);
        return userOrgMapper.selectList(wrapper)
                .stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
    }

    List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        access.assertCanReadUser(userId);

        Long operationTenantId = access.resolveTenantScopedOperationTenantId(userId, tenantId);
        List<SysUserOrg> userOrgs = TenantContextHolder.executeIgnore(() ->
                userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getUserId, userId)
                        .eq(SysUserOrg::getTenantId, operationTenantId)));
        if (userOrgs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orgIds = userOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
        List<SysOrg> orgs = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                        .eq(SysOrg::getTenantId, operationTenantId)
                        .in(SysOrg::getId, orgIds)));
        Map<Long, SysOrg> orgMap = orgs.stream()
                .collect(Collectors.toMap(SysOrg::getId, item -> item, (left, right) -> left));

        return userOrgs.stream()
                .map(userOrg -> {
                    SysOrg org = orgMap.get(userOrg.getOrgId());
                    List<String> roleNames = userOrgRoleMapper.selectRoleNamesByUserOrg(
                            operationTenantId, userId, userOrg.getOrgId());
                    UserOrgBindingVO vo = new UserOrgBindingVO();
                    vo.setTenantId(operationTenantId);
                    vo.setUserId(userId);
                    vo.setOrgId(userOrg.getOrgId());
                    vo.setOrgName(org == null ? null : org.getOrgName());
                    vo.setParentId(org == null ? null : org.getParentId());
                    vo.setAncestors(org == null ? null : org.getAncestors());
                    vo.setIsMain(userOrg.getIsMain());
                    vo.setRoleNames(roleNames);
                    vo.setRoleCount(roleNames == null ? 0 : roleNames.size());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId) {
        if (userId == null || orgId == null) {
            return new ArrayList<>();
        }
        access.assertCanReadUser(userId);
        Long operationTenantId = access.resolveTenantScopedOperationTenantId(userId, tenantId);
        rolePolicy.validateUserOrgMembership(userId, orgId, operationTenantId);
        return userOrgRoleMapper.selectUserOrgRoleIds(operationTenantId, userId, orgId)
                .stream()
                .filter(rolePolicy::isRoleVisibleForCurrentUser)
                .collect(Collectors.toList());
    }

    List<Long> selectUserPostIds(Long userId) {
        return selectUserPostIds(userId, null);
    }

    List<Long> selectUserPostIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        access.assertCanReadUser(userId);

        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, access.resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserPost::getPostId);
        return userPostMapper.selectList(wrapper)
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
    }
}
