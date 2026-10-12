package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Computes assignable resources and ancestor closure before any role binding is changed. */
final class RoleResourceSelection {
    private final ISysResourceService resources;
    private final RoleResourceCatalog catalog;

    RoleResourceSelection(ISysResourceService resources, SysResourceMapper resourceMapper) {
        this.resources = resources;
        this.catalog = new RoleResourceCatalog(resourceMapper);
    }

    record Replacement(Set<Long> selectedIds, Set<Long> replaceableIds, boolean clientScoped) {
        boolean isNoOp() { return clientScoped && replaceableIds.isEmpty(); }
    }

    record Scoped(Set<Long> scopeIds, Set<Long> selectedIds) { }

    Replacement replacement(Long[] requested, String clientCode) {
        Set<Long> selected = requested == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(requested));
        boolean clientScoped = StringUtils.isNotBlank(clientCode);
        Map<Long, SysResource> catalog = this.catalog.replacement(selected, clientCode);
        Set<Long> replaceable = clientScoped ? catalog.values().stream()
                .filter(resource -> compatible(resource, clientCode)).map(SysResource::getId)
                .collect(Collectors.toSet()) : new HashSet<>();
        if (clientScoped) {
            requireContains(replaceable, selected, "权限溢出：不能分配其他客户端的资源权限");
        }
        LoginUser user = SessionHelper.getLoginUser();
        Set<Long> owned = user != null && !user.isAdmin()
                ? new HashSet<>(resources.selectCurrentUserResourceIds()) : null;
        if (owned != null) {
            requireContains(owned, selected, "权限溢出：不能分配自己没有的资源权限");
        }
        Set<Long> complete = ancestors(selected, catalog, clientCode, false);
        if (owned != null) {
            requireContains(owned, complete, "权限溢出：不能分配自己没有的父级资源权限");
        }
        if (clientScoped) {
            Set<Long> addedParents = new HashSet<>(complete);
            addedParents.removeAll(selected);
            replaceable.addAll(addedParents);
            requireContains(replaceable, complete, "权限溢出：不能分配其他客户端的父级资源权限");
        }
        return new Replacement(complete, replaceable, clientScoped);
    }

    Scoped scoped(ScopedRolePermissionDTO settings) {
        Set<Long> scope = normalizeIds(settings.getScopeResourceIds());
        Set<Long> selected = normalizeIds(settings.getSelectedResourceIds());
        requireContains(scope, selected, "权限溢出：选择了授权范围以外的资源");
        Map<Long, SysResource> catalog = this.catalog.withAncestors(scope);
        requireContains(catalog.keySet(), scope, "授权范围包含不存在的系统资源");
        if (StringUtils.isNotBlank(settings.getClientCode()) && scope.stream()
                .map(catalog::get).anyMatch(resource -> !compatible(resource, settings.getClientCode()))) {
            throw new RuntimeException("权限溢出：授权范围包含其他客户端资源");
        }
        requireCurrentUserCanAssign(scope);
        Set<Long> complete = ancestors(selected, catalog, settings.getClientCode(), true);
        requireCurrentUserCanAssign(complete);
        return new Scoped(scope, complete);
    }

    private Set<Long> ancestors(Set<Long> selected, Map<Long, SysResource> catalog,
                                String clientCode, boolean retainMissingParent) {
        Set<Long> complete = retainMissingParent ? new LinkedHashSet<>(selected) : new HashSet<>(selected);
        for (Long id : selected) {
            addParents(catalog.get(id), catalog, clientCode, retainMissingParent, complete);
        }
        return complete;
    }

    private void addParents(SysResource resource, Map<Long, SysResource> catalog,
                            String clientCode, boolean retainMissingParent, Set<Long> complete) {
        Long parentId = resource == null ? null : resource.getParentId();
        while (parentId != null && parentId != 0L && !complete.contains(parentId)) {
            SysResource parent = catalog.get(parentId);
            if (parent == null) {
                if (retainMissingParent) {
                    complete.add(parentId);
                }
                return;
            }
            if (StringUtils.isNotBlank(clientCode) && !compatible(parent, clientCode)) {
                throw new RuntimeException("权限溢出：不能分配其他客户端的父级资源权限");
            }
            complete.add(parentId);
            parentId = parent.getParentId();
        }
    }

    private void requireCurrentUserCanAssign(Set<Long> selected) {
        LoginUser user = SessionHelper.getLoginUser();
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        if (!user.isAdmin() && !selected.isEmpty()) {
            requireContains(new HashSet<>(resources.selectCurrentUserResourceIds()), selected,
                    "权限溢出：不能分配自己没有的资源权限");
        }
    }

    private boolean compatible(SysResource resource, String clientCode) {
        return resource != null && (StringUtils.isBlank(resource.getClientCode())
                || clientCode.equals(resource.getClientCode()));
    }

    private Set<Long> normalizeIds(Collection<Long> ids) {
        return ids == null ? new LinkedHashSet<>() : ids.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void requireContains(Set<Long> allowed, Set<Long> selected, String message) {
        if (!allowed.containsAll(selected)) {
            throw new RuntimeException(message);
        }
    }
}
