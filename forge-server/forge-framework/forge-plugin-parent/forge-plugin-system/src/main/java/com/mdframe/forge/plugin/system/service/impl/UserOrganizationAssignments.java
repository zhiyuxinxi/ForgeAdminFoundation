package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysPost;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 组织和岗位成员关系维护；保留主组织、主岗位和区域同步顺序。 */
@RequiredArgsConstructor
final class UserOrganizationAssignments {

    private final SysUserMapper userMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysOrgMapper orgMapper;
    private final SysPostMapper postMapper;
    private final UserAccessPolicy access;
    private final UserProfileOperations profile;
    private final TenantMembershipOperations memberships;

    boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        if (userId == null || orgId == null) {
            return false;
        }

        // 获取用户信息以获取租户ID
        access.assertCanManageUser(userId);
        access.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = access.resolveTenantScopedOperationTenantId(user);
        memberships.ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validateOrgTenant(List.of(orgId), tenantId);

        // 检查是否已存在
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getOrgId, orgId);
        Long count = userOrgMapper.selectCount(wrapper);

        if (count > 0) {
            return false;
        }

        // 如果是主组织，先取消其他主组织
        if (isMain != null && isMain == 1) {
            LambdaQueryWrapper<SysUserOrg> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .eq(SysUserOrg::getIsMain, 1);
            SysUserOrg updateOrg = new SysUserOrg();
            updateOrg.setIsMain(0);
            userOrgMapper.update(updateOrg, updateWrapper);
        }

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setTenantId(tenantId);
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain != null ? isMain : 0);
        boolean inserted = userOrgMapper.insert(userOrg) > 0;
        if (inserted && isMain != null && isMain == 1) {
            profile.syncUserRegionFromMainOrg(userId, orgId);
            profile.syncCurrentUserOrgSession(userId, tenantId);
        }
        return inserted;
    }

    boolean unbindUserOrg(Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        Long tenantId = access.resolveTenantScopedOperationTenantId(userId);

        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getOrgId, orgId);
        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .eq(SysUserOrgRole::getOrgId, orgId));
        boolean deleted = userOrgMapper.delete(wrapper) > 0;
        if (deleted) {
            profile.syncCurrentUserOrgSession(userId, tenantId);
        }
        return deleted;
    }

    boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId) {
        return bindUserOrgs(userId, orgIds, mainOrgId, null);
    }

    boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId) {
        if (userId == null || orgIds == null || orgIds.isEmpty()) {
            return false;
        }

        // 获取用户信息以获取租户ID
        access.assertCanManageUser(userId);
        access.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = access.resolveTenantScopedOperationTenantId(user, requestedTenantId);
        memberships.ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validateOrgTenant(orgIds, tenantId);

        // 验证主组织是否在组织列表中
        if (mainOrgId != null && !orgIds.contains(mainOrgId)) {
            mainOrgId = null;
        }

        // 获取用户当前的所有组织
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId);
        List<SysUserOrg> existingOrgs = userOrgMapper.selectList(wrapper);
        removeUnselectedOrganizations(userId, tenantId, orgIds, existingOrgs);
        saveOrganizationMemberships(userId, tenantId, orgIds, mainOrgId, existingOrgs);

        if (mainOrgId != null) {
            profile.syncUserRegionFromMainOrg(userId, mainOrgId);
        }
        profile.syncCurrentUserOrgSession(userId, tenantId);
        return true;
    }

    private void removeUnselectedOrganizations(Long userId, Long tenantId, List<Long> orgIds,
                                               List<SysUserOrg> existingOrgs) {
        List<Long> existingOrgIds = existingOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());

        // 删除不再需要的组织
        List<Long> toDelete = existingOrgIds.stream()
                .filter(orgId -> !orgIds.contains(orgId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserOrg> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .in(SysUserOrg::getOrgId, toDelete);
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getUserId, userId)
                    .eq(SysUserOrgRole::getTenantId, tenantId)
                    .in(SysUserOrgRole::getOrgId, toDelete));
            userOrgMapper.delete(deleteWrapper);
        }
    }

    private void saveOrganizationMemberships(Long userId, Long tenantId, List<Long> orgIds,
                                             Long mainOrgId, List<SysUserOrg> existingOrgs) {
        // 添加新的组织或更新现有组织
        for (Long orgId : orgIds) {
            SysUserOrg userOrg = existingOrgs.stream()
                    .filter(org -> org.getOrgId().equals(orgId))
                    .findFirst()
                    .orElse(null);

            if (userOrg == null) {
                // 插入新组织
                userOrg = new SysUserOrg();
                userOrg.setTenantId(tenantId);
                userOrg.setUserId(userId);
                userOrg.setOrgId(orgId);
                userOrg.setIsMain(orgId.equals(mainOrgId) ? 1 : 0);
                userOrgMapper.insert(userOrg);
            } else {
                // 更新现有组织的主组织标记
                int newIsMain = orgId.equals(mainOrgId) ? 1 : 0;
                if (!userOrg.getIsMain().equals(newIsMain)) {
                    userOrg.setIsMain(newIsMain);
                    userOrgMapper.updateById(userOrg);
                }
            }
        }
    }

    void validateOrgTenant(List<Long> orgIds, Long tenantId) {
        List<Long> normalizedOrgIds = orgIds == null ? new ArrayList<>() : orgIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedOrgIds.isEmpty()) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                        .in(SysOrg::getId, normalizedOrgIds)
                        .eq(SysOrg::getTenantId, tenantId)));
        if (count == null || count != normalizedOrgIds.size()) {
            throw new RuntimeException("组织不属于当前操作租户");
        }
    }

    void validatePostTenant(List<Long> postIds, Long tenantId) {
        List<Long> normalizedPostIds = postIds == null ? new ArrayList<>() : postIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedPostIds.isEmpty()) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                        .in(SysPost::getId, normalizedPostIds)
                        .eq(SysPost::getTenantId, tenantId)));
        if (count == null || count != normalizedPostIds.size()) {
            throw new RuntimeException("岗位不属于当前操作租户");
        }
    }

    boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId) {
        return bindUserPosts(userId, postIds, mainPostId, null);
    }

    boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long requestedTenantId) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return false;
        }

        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = access.resolveTenantScopedOperationTenantId(user, requestedTenantId);
        memberships.ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validatePostTenant(postIds, tenantId);

        // 验证主岗位是否在岗位列表中
        if (mainPostId != null && !postIds.contains(mainPostId)) {
            mainPostId = null;
        }

        // 获取当前所有岗位
        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, tenantId);
        List<SysUserPost> existingPosts = userPostMapper.selectList(wrapper);
        List<Long> existingPostIds = existingPosts.stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());

        // 删除不再需要的岗位
        List<Long> toDelete = existingPostIds.stream()
                .filter(postId -> !postIds.contains(postId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserPost> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserPost::getUserId, userId)
                    .eq(SysUserPost::getTenantId, tenantId)
                    .in(SysUserPost::getPostId, toDelete);
            userPostMapper.delete(deleteWrapper);
        }

        // 添加新岗位或更新现有岗位
        for (Long postId : postIds) {
            SysUserPost userPost = existingPosts.stream()
                    .filter(p -> p.getPostId().equals(postId))
                    .findFirst()
                    .orElse(null);

            if (userPost == null) {
                userPost = new SysUserPost();
                userPost.setTenantId(tenantId);
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                userPost.setIsMain(postId.equals(mainPostId) ? 1 : 0);
                userPostMapper.insert(userPost);
            } else {
                int newIsMain = postId.equals(mainPostId) ? 1 : 0;
                if (!userPost.getIsMain().equals(newIsMain)) {
                    userPost.setIsMain(newIsMain);
                    userPostMapper.updateById(userPost);
                }
            }
        }

        return true;
    }
}
