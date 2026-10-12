package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleOrg;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/** Owns organization selection and synchronizes role bindings within the caller transaction. */
@RequiredArgsConstructor
final class RoleOrganizationScope {
    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysOrgMapper orgMapper;

    List<Long> selectedIds(SysRole role) {
        return TenantContextHolder.executeIgnore(() -> roleOrgMapper.selectList(new LambdaQueryWrapper<SysRoleOrg>()
                        .eq(SysRoleOrg::getTenantId, role.getTenantId())
                        .eq(SysRoleOrg::getRoleId, role.getId())
                        .select(SysRoleOrg::getOrgId)))
                .stream()
                .map(SysRoleOrg::getOrgId)
                .collect(Collectors.toList());
    }

    void replace(SysRole role, List<Long> orgIds) {
        List<Long> requested = normalizeOrgIds(orgIds);
        List<Long> existing = selectedIds(role);
        if (requested.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                    .eq(SysRoleOrg::getTenantId, role.getTenantId())
                    .eq(SysRoleOrg::getRoleId, role.getId())));
            updateRoleOrgScopeType(role, SystemConstants.RoleOrgScope.GLOBAL);
            return;
        }
        validateOrgTenant(requested, role.getTenantId());
        updateRoleOrgScopeType(role, SystemConstants.RoleOrgScope.CUSTOM);
        removeDeselected(role, existing.stream().filter(id -> !requested.contains(id)).toList());
        addMissing(role, requested, new HashSet<>(existing));
    }

    private void removeDeselected(SysRole role, List<Long> removedIds) {
        if (removedIds.isEmpty()) {
            return;
        }
        TenantContextHolder.executeIgnore(() -> {
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getTenantId, role.getTenantId())
                    .eq(SysUserOrgRole::getRoleId, role.getId())
                    .in(SysUserOrgRole::getOrgId, removedIds));
            roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                    .eq(SysRoleOrg::getTenantId, role.getTenantId())
                    .eq(SysRoleOrg::getRoleId, role.getId())
                    .in(SysRoleOrg::getOrgId, removedIds));
        });
    }

    private void addMissing(SysRole role, List<Long> requested, Set<Long> existing) {
        TenantContextHolder.executeIgnore(() -> {
            for (Long id : requested) {
                if (!existing.contains(id)) {
                    SysRoleOrg binding = new SysRoleOrg();
                    binding.setTenantId(role.getTenantId());
                    binding.setRoleId(role.getId());
                    binding.setOrgId(id);
                    roleOrgMapper.insert(binding);
                }
            }
        });
    }

    static Integer resolveOrgScopeType(Integer orgScopeType) {
        return Objects.equals(orgScopeType, SystemConstants.RoleOrgScope.CUSTOM)
                ? SystemConstants.RoleOrgScope.CUSTOM
                : SystemConstants.RoleOrgScope.GLOBAL;
    }

    private void updateRoleOrgScopeType(SysRole role, Integer orgScopeType) {
        SysRole update = new SysRole();
        update.setId(role.getId());
        update.setTenantId(role.getTenantId());
        update.setOrgScopeType(resolveOrgScopeType(orgScopeType));
        TenantContextHolder.executeIgnore(() -> {
            roleMapper.updateById(update);
        });
        role.setOrgScopeType(update.getOrgScopeType());
    }

    private List<Long> normalizeOrgIds(List<Long> orgIds) {
        if (orgIds == null) {
            return new ArrayList<>();
        }
        return orgIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateOrgTenant(List<Long> orgIds, Long tenantId) {
        if (CollUtil.isEmpty(orgIds)) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                        .in(SysOrg::getId, orgIds)
                        .eq(SysOrg::getTenantId, tenantId)));
        if (count == null || count != orgIds.size()) {
            throw new RuntimeException("组织不属于当前角色租户");
        }
    }

}
