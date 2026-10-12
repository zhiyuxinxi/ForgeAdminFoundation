package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/** 用户账户读写编排；关联写入在公开服务入口的原事务内完成。 */
@RequiredArgsConstructor
final class UserAccountOperations {

    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final UserAccessPolicy access;
    private final UserProfileOperations profile;
    private final TenantMembershipOperations memberships;
    private final UserOrganizationAssignments organizations;
    private final UserRoleAssignments roles;

    IPage<SysUser> selectUserPage(SysUserQuery query) {
        access.normalizeUserQueryTenant(query);
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return TenantContextHolder.executeIgnore(() -> userMapper.selectUserPage(page, query));
    }

    List<SysUser> selectExportList(SysUserQuery query) {
        access.normalizeUserQueryTenant(query);
        return TenantContextHolder.executeIgnore(() -> userMapper.selectExportList(query));
    }

    SysUser selectUserById(Long id) {
        access.assertCanReadUser(id);
        return TenantContextHolder.executeIgnore(() -> userMapper.selectById(id));
    }

    boolean insertUser(SysUserDTO dto) {
        access.assertUserActionAllowed("system:user:add");
        LoginUser loginUser = access.requireLoginUser();
        List<Long> tenantIds = dto.getTenantIds() == null
                ? List.of(access.resolveWriteTenantId(dto.getTenantId()))
                : access.resolveWriteTenantIds(dto.getTenantIds(), dto.getTenantId(), loginUser);
        Long tenantId = access.resolveDefaultTenantId(tenantIds, dto.getTenantId());
        access.validateUserTypeForWrite(dto);
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        user.setTenantId(tenantId);
        user.setUserType(access.resolveWriteUserType(dto.getUserType()));
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setForcePasswordChange(true);
        boolean inserted = userMapper.insert(user) > 0;
        if (inserted) {
            for (Long bindTenantId : tenantIds) {
                memberships.upsertUserTenant(user.getId(), bindTenantId, user.getUserType(), Objects.equals(bindTenantId, tenantId));
            }
            List<Long> orgIds = dto.getOrgIds() == null ? List.of() : dto.getOrgIds();
            if (!orgIds.isEmpty()) {
                Long mainOrgId = dto.getMainOrgId() != null ? dto.getMainOrgId() : orgIds.get(0);
                organizations.bindUserOrgs(user.getId(), orgIds, mainOrgId, tenantId);
            } else if (!loginUser.isAdmin() && loginUser.getActiveOrgId() != null) {
                organizations.bindUserOrgs(user.getId(), List.of(loginUser.getActiveOrgId()), loginUser.getActiveOrgId(), tenantId);
            }
            // 同步绑定角色
            if (dto.getRoleIds() != null) {
                roles.syncUserRoles(user.getId(), dto.getRoleIds(), tenantId);
            }
            // 同步绑定岗位
            if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
                organizations.bindUserPosts(user.getId(), dto.getPostIds(), dto.getPostIds().get(0), tenantId);
            }
        }
        return inserted;
    }

    boolean updateUser(SysUserDTO dto) {
        access.assertUserActionAllowed("system:user:edit");
        LoginUser loginUser = access.requireLoginUser();
        if (!loginUser.isAdmin() && access.isCurrentLoginUser(dto.getId(), loginUser)) {
            return profile.updateCurrentUserFromManagement(dto);
        }
        access.assertCanManageUser(dto.getId());
        access.assertNotSelfManagement(dto.getId());
        access.validateUserTypeForWrite(dto);
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        // 修改时不更新密码
        user.setPassword(null);

        Long tenantId = null;
        List<Long> tenantIds = null;
        if (loginUser.isAdmin()) {
            if (dto.getTenantIds() != null) {
                tenantIds = access.resolveWriteTenantIds(dto.getTenantIds(), dto.getTenantId(), loginUser);
                tenantId = access.resolveDefaultTenantId(tenantIds, dto.getTenantId());
            } else {
                tenantId = access.resolveWriteTenantId(dto.getTenantId());
            }
            user.setTenantId(tenantId);
            user.setUserType(access.resolveWriteUserType(dto.getUserType()));
        } else {
            // 租户管理员只维护当前租户成员资料，不改变用户默认租户和全局用户类型。
            user.setTenantId(null);
            user.setUserType(null);
        }

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated && loginUser.isAdmin()) {
            if (tenantIds != null) {
                UserTenantBindDTO tenantBindDTO = new UserTenantBindDTO();
                tenantBindDTO.setTenantIds(tenantIds);
                tenantBindDTO.setDefaultTenantId(tenantId);
                tenantBindDTO.setMemberType(user.getUserType());
                memberships.bindUserTenants(user.getId(), tenantBindDTO);
            } else {
                memberships.upsertUserTenant(user.getId(), tenantId, user.getUserType(), true);
            }
        }
        // 同步绑定角色。roleIds 传空数组表示清空当前可管理范围内的角色。
        if (updated && dto.getRoleIds() != null) {
            roles.syncUserRoles(user.getId(), dto.getRoleIds(), tenantId);
        }
        // 同步绑定岗位
        if (updated && dto.getPostIds() != null) {
            organizations.bindUserPosts(user.getId(), dto.getPostIds(), !dto.getPostIds().isEmpty() ? dto.getPostIds().get(0) : null, tenantId);
        }
        return updated;
    }

    boolean deleteUserById(Long id) {
        access.assertUserActionAllowed("system:user:remove");
        access.assertCanManageUser(id);
        access.assertNotSelfManagement(id);
        LoginUser loginUser = access.requireLoginUser();
        if (!loginUser.isAdmin()) {
            return memberships.removeUserFromTenant(id, loginUser.getTenantId());
        }
        return memberships.deleteUserGlobally(id);
    }

    boolean deleteUserByIds(Long[] ids) {
        access.assertUserActionAllowed("system:user:remove");
        for (Long id : ids) {
            deleteUserById(id);
        }
        return true;
    }

    void doUntieDisable(Long userId) {
        access.assertUserActionAllowed("system:user:edit");
        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        StpUtil.untieDisable(userId);
        // 同时更新数据库状态为正常
        this.updateUserStatus(userId, 1);
    }

    boolean resetPassword(Long userId, String newPassword) {
        access.assertUserActionAllowed("system:user:edit");
        access.assertCanManageUser(userId);
        access.assertNotSelfManagement(userId);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(PasswordUtil.encrypt(newPassword));
        user.setForcePasswordChange(true);
        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated) {
            UserSessionRevocation.afterCommit(userId, null);
        }
        return updated;
    }

    boolean updateUserStatus(Long userId, Integer status) {
        access.assertUserActionAllowed("system:user:edit");
        access.assertCanManageUser(userId, true);
        access.assertNotSelfManagement(userId);
        LoginUser loginUser = access.requireLoginUser();
        if (!loginUser.isAdmin()) {
            SysUserTenant member = new SysUserTenant();
            member.setStatus(status != null && status == 1 ? 1 : 0);
            LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, loginUser.getTenantId());
            boolean updated = TenantContextHolder.executeIgnore(() -> userTenantMapper.update(member, wrapper) > 0);
            if (updated && !Integer.valueOf(1).equals(status)) {
                UserSessionRevocation.afterCommit(userId, loginUser.getTenantId());
            }
            return updated;
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUserStatus(status);
        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated && !Integer.valueOf(1).equals(status)) {
            UserSessionRevocation.afterCommit(userId, null);
        }
        return updated;
    }
}
