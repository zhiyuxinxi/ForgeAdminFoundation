package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Projects stored role bindings into the caller's client tree, with optional parent nodes. */
@RequiredArgsConstructor
final class RoleResourceProjection {
    private final SysRoleResourceMapper bindings;
    private final ISysResourceService resources;
    private final SysResourceMapper resourceMapper;

    List<Long> selectedIds(SysRole role, String clientCode, boolean includeParents) {
        List<Long> selected = bindings.selectRoleResourceIdsForProjection(role.getTenantId(), role.getId());
        if (StringUtils.isNotBlank(clientCode) && !selected.isEmpty()) {
            Set<Long> compatible = new RoleResourceCatalog(resourceMapper).clientIds(clientCode);
            selected = selected.stream().filter(compatible::contains).collect(Collectors.toList());
        }
        LoginUser user = SessionHelper.getLoginUser();
        if (user != null && !user.isAdmin() && !selected.isEmpty()) {
            Set<Long> owned = new HashSet<>(resources.selectCurrentUserResourceIds());
            selected = selected.stream().filter(owned::contains).collect(Collectors.toList());
        }
        return includeParents || selected.isEmpty() ? selected : leaves(selected);
    }

    private List<Long> leaves(List<Long> selected) {
        Set<Long> parents = resourceMapper.selectBatchIds(selected).stream().map(SysResource::getParentId)
                .filter(id -> id != null && id != 0L).collect(Collectors.toSet());
        return selected.stream().filter(id -> !parents.contains(id)).collect(Collectors.toList());
    }
}
