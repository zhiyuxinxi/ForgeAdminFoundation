package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Expands resource ancestors and combines client catalogs read through the resource mapper. */
@RequiredArgsConstructor
final class RoleResourceCatalog {
    private final SysResourceMapper resources;

    Map<Long, SysResource> withAncestors(Set<Long> ids) {
        return resourceMap(loadWithAncestors(ids));
    }

    Set<Long> clientIds(String clientCode) {
        return new HashSet<>(resources.selectRoleAssignableResourceIds(clientCode));
    }

    Map<Long, SysResource> replacement(Set<Long> selected, String clientCode) {
        if (StringUtils.isBlank(clientCode)) {
            return resourceMap(loadWithAncestors(selected));
        }
        Map<Long, SysResource> catalog = resourceMap(resources.selectRoleAssignableResources(clientCode));
        Set<Long> missing = new HashSet<>(selected);
        missing.removeAll(catalog.keySet());
        for (SysResource resource : loadWithAncestors(missing)) {
            catalog.putIfAbsent(resource.getId(), resource);
        }
        return catalog;
    }

    private List<SysResource> loadWithAncestors(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysResource> loaded = new HashMap<>();
        Set<Long> pending = new HashSet<>(ids);
        while (!pending.isEmpty()) {
            List<SysResource> batch = resources.selectBatchIds(pending);
            pending.clear();
            for (SysResource resource : batch) {
                if (loaded.put(resource.getId(), resource) == null) {
                    Long parent = resource.getParentId();
                    if (parent != null && parent != 0L && !loaded.containsKey(parent)) {
                        pending.add(parent);
                    }
                }
            }
        }
        return new ArrayList<>(loaded.values());
    }

    private Map<Long, SysResource> resourceMap(List<SysResource> entries) {
        return entries.stream().collect(Collectors.toMap(SysResource::getId, r -> r, (left, right) -> left));
    }
}
