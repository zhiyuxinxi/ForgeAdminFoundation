package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.*;

/** Applies an already validated resource selection inside the calling application's transaction. */
@RequiredArgsConstructor
final class RoleResourceBindings {
    private final SysRoleResourceMapper resources;
    private final SysRoleModuleDataScopeMapper moduleScopes;

    int replace(SysRole role, RoleResourceSelection.Replacement selection) {
        LambdaQueryWrapper<SysRoleResource> delete = roleBindings(role);
        if (selection.clientScoped()) {
            delete.in(SysRoleResource::getResourceId, selection.replaceableIds());
        }
        int deleted = resources.delete(delete);
        insertResources(role, selection.selectedIds());
        return deleted;
    }

    boolean replaceScoped(SysRole role, RoleResourceSelection.Scoped selection,
                          Set<String> scopeModules, Map<String, Integer> requestedModules) {
        return TenantContextHolder.executeIgnore(() -> {
            replaceScopedResources(role, selection);
            replaceScopedModules(role, scopeModules, requestedModules);
            return true;
        });
    }

    boolean unbind(SysRole role, Long[] resourceIds) {
        LambdaQueryWrapper<SysRoleResource> delete = roleBindings(role)
                .in(SysRoleResource::getResourceId, Arrays.asList(resourceIds));
        return TenantContextHolder.executeIgnore(() -> resources.delete(delete) > 0);
    }

    private void replaceScopedResources(SysRole role, RoleResourceSelection.Scoped selection) {
        Set<Long> retained = new HashSet<>(resources.selectResourceIdsByRole(role.getTenantId(), role.getId()));
        retained.removeAll(selection.scopeIds());
        if (!selection.scopeIds().isEmpty()) {
            resources.delete(roleBindings(role).in(SysRoleResource::getResourceId, selection.scopeIds()));
        }
        List<Long> missing = selection.selectedIds().stream().filter(id -> !retained.contains(id)).toList();
        insertResources(role, missing);
    }

    private void replaceScopedModules(SysRole role, Set<String> scopeModules, Map<String, Integer> requested) {
        if (!scopeModules.isEmpty()) {
            moduleScopes.deleteByRoleAndModules(role.getTenantId(), role.getId(), scopeModules);
        }
        List<SysRoleModuleDataScope> bindings = requested.entrySet().stream().map(entry -> {
            SysRoleModuleDataScope binding = new SysRoleModuleDataScope();
            binding.setTenantId(role.getTenantId());
            binding.setRoleId(role.getId());
            binding.setModuleCode(entry.getKey());
            binding.setDataScope(entry.getValue());
            return binding;
        }).toList();
        if (!bindings.isEmpty()) {
            moduleScopes.insertBatch(bindings);
        }
    }

    private void insertResources(SysRole role, Collection<Long> ids) {
        List<SysRoleResource> bindings = ids.stream().map(id -> {
            SysRoleResource binding = new SysRoleResource();
            binding.setTenantId(role.getTenantId());
            binding.setRoleId(role.getId());
            binding.setResourceId(id);
            return binding;
        }).toList();
        if (!bindings.isEmpty()) {
            resources.insertBatch(bindings);
        }
    }

    private LambdaQueryWrapper<SysRoleResource> roleBindings(SysRole role) {
        return new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getRoleId, role.getId())
                .eq(SysRoleResource::getTenantId, role.getTenantId());
    }
}
