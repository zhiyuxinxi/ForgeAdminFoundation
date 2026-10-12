package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;

import java.util.LinkedHashSet;
import java.util.Set;

/** Captures both role binding paths before mutation so removed bindings cannot hide stale sessions. */
final class RoleSessionRevocation {
    private final Long tenantId;
    private final Set<Long> userIds;

    private RoleSessionRevocation(Long tenantId, Set<Long> userIds) {
        this.tenantId = tenantId;
        this.userIds = userIds;
    }

    static RoleSessionRevocation capture(SysRole role, SysUserRoleMapper users, SysUserOrgRoleMapper orgUsers) {
        Set<Long> affected = TenantContextHolder.executeIgnore(() -> {
            Set<Long> ids = new LinkedHashSet<>();
            users.selectList(new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getTenantId, role.getTenantId())
                            .eq(SysUserRole::getRoleId, role.getId())
                            .select(SysUserRole::getUserId))
                    .forEach(binding -> ids.add(binding.getUserId()));
            orgUsers.selectList(new LambdaQueryWrapper<SysUserOrgRole>()
                            .eq(SysUserOrgRole::getTenantId, role.getTenantId())
                            .eq(SysUserOrgRole::getRoleId, role.getId())
                            .select(SysUserOrgRole::getUserId))
                    .forEach(binding -> ids.add(binding.getUserId()));
            ids.remove(null);
            return ids;
        });
        return new RoleSessionRevocation(role.getTenantId(), affected);
    }

    void afterCommit() {
        userIds.forEach(userId -> UserSessionRevocation.afterCommit(userId, tenantId));
    }
}
