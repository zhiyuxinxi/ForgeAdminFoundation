package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 租户成员加入、默认租户切换和成员关系清理。 */
@RequiredArgsConstructor
final class TenantMembershipOperations {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final UserAccessPolicy access;

    List<SysUserTenantVO> selectUserTenants(Long userId) {
        access.assertCanManageUser(userId);
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(userId, false));
    }

    boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以绑定用户租户");
        }
        access.assertNotSelfManagement(userId);
        if (userId == null || dto == null || dto.getTenantIds() == null || dto.getTenantIds().isEmpty()) {
            return false;
        }
        List<Long> tenantIds = access.normalizeTenantIdList(dto.getTenantIds());
        if (tenantIds.isEmpty()) {
            return false;
        }
        Long defaultTenantId = dto.getDefaultTenantId();
        if (defaultTenantId == null || !tenantIds.contains(defaultTenantId)) {
            defaultTenantId = tenantIds.get(0);
        }
        Integer memberType = normalizeMemberType(dto.getMemberType());
        tenantIds.forEach(access::validateTenantEnabled);

        LambdaQueryWrapper<SysUserTenant> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserTenant::getUserId, userId)
                .notIn(SysUserTenant::getTenantId, tenantIds);
        List<Long> removedTenantIds = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectList(deleteWrapper))
                .stream()
                .map(SysUserTenant::getTenantId)
                .collect(Collectors.toList());
        if (!removedTenantIds.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> {
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .in(SysUserRole::getTenantId, removedTenantIds));
                userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                        .eq(SysUserOrgRole::getUserId, userId)
                        .in(SysUserOrgRole::getTenantId, removedTenantIds));
                userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getUserId, userId)
                        .in(SysUserOrg::getTenantId, removedTenantIds));
                userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                        .eq(SysUserPost::getUserId, userId)
                        .in(SysUserPost::getTenantId, removedTenantIds));
            });
        }
        TenantContextHolder.executeIgnore(() -> userTenantMapper.delete(deleteWrapper));

        for (Long tenantId : tenantIds) {
            upsertUserTenant(userId, tenantId, memberType, Objects.equals(tenantId, defaultTenantId));
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(defaultTenantId);
        TenantContextHolder.executeIgnore(() -> userMapper.updateById(user));
        return true;
    }

    boolean batchBindUserTenant(BatchUserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以批量加入用户租户");
        }
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty() || dto.getTenantId() == null) {
            return false;
        }
        Long tenantId = dto.getTenantId();
        access.validateTenantEnabled(tenantId);
        Integer memberType = normalizeMemberType(dto.getMemberType());
        List<Long> userIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return false;
        }

        // 批量查询所有用户，避免循环 selectById
        List<SysUser> users = TenantContextHolder.executeIgnore(() -> userMapper.selectBatchIds(userIds));
        Map<Long, SysUser> userMap = users.stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));

        // 批量查询有启用租户关系的 userId 集合，避免循环 selectCount
        Set<Long> usersWithMembership = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectList(new LambdaQueryWrapper<SysUserTenant>()
                        .in(SysUserTenant::getUserId, userIds)
                        .eq(SysUserTenant::getStatus, 1)
                        .select(SysUserTenant::getUserId))
                        .stream()
                        .map(SysUserTenant::getUserId)
                        .collect(Collectors.toSet()));

        for (Long userId : userIds) {
            access.assertNotSelfManagement(userId);
            SysUser user = userMap.get(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            boolean defaultTenant = user.getTenantId() == null || !usersWithMembership.contains(userId);
            upsertUserTenant(userId, tenantId, memberType, defaultTenant);
            if (defaultTenant) {
                SysUser updateUser = new SysUser();
                updateUser.setId(userId);
                updateUser.setTenantId(tenantId);
                TenantContextHolder.executeIgnore(() -> userMapper.updateById(updateUser));
            }
        }
        return true;
    }

    void upsertUserTenant(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        if (defaultTenant) {
            SysUserTenant update = new SysUserTenant();
            update.setIsDefault(0);
            LambdaQueryWrapper<SysUserTenant> defaultWrapper = new LambdaQueryWrapper<>();
            defaultWrapper.eq(SysUserTenant::getUserId, userId);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.update(update, defaultWrapper));
        }

        LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId);
        SysUserTenant existing = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectOne(wrapper));
        if (existing == null) {
            SysUserTenant member = new SysUserTenant();
            member.setUserId(userId);
            member.setTenantId(tenantId);
            member.setMemberType(normalizeMemberType(memberType));
            member.setIsDefault(defaultTenant ? 1 : 0);
            member.setStatus(1);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.insert(member));
            return;
        }

        existing.setMemberType(normalizeMemberType(memberType));
        existing.setIsDefault(defaultTenant ? 1 : 0);
        existing.setStatus(1);
        TenantContextHolder.executeIgnore(() -> userTenantMapper.updateById(existing));
    }

    void ensureUserTenantBound(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)));
        if (count == null || count == 0) {
            upsertUserTenant(userId, tenantId, memberType, defaultTenant);
        }
    }

    Integer normalizeMemberType(Integer memberType) {
        return memberType != null && memberType == 1 ? 1 : 2;
    }

    boolean removeUserFromTenant(Long userId, Long tenantId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getUserId, userId)
                    .eq(SysUserOrgRole::getTenantId, tenantId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                    .eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId));

            int deleted = userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, tenantId));

            Long remainingTenants = userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getStatus, 1));
            if (remainingTenants == null || remainingTenants == 0) {
                userMapper.deleteById(userId);
            }
            return deleted > 0;
        });
    }

    boolean deleteUserGlobally(Long userId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>().eq(SysUserOrgRole::getUserId, userId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getUserId, userId));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
            userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId));
            return userMapper.deleteById(userId) > 0;
        });
    }
}
