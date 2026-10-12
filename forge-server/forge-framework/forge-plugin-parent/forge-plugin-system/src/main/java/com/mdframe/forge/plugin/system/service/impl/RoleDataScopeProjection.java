package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.vo.RoleDataScopeSettingsVO;
import com.mdframe.forge.plugin.system.vo.RoleModuleDataScopeVO;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Projects configured module ranges and resource names without changing grants. */
@RequiredArgsConstructor
final class RoleDataScopeProjection {
    RoleDataScopeSettingsVO project(SysRole role, List<SysDataScopeConfig> configs,
                                    List<SysRoleModuleDataScope> overrides, List<SysResource> resources) {
        Map<String, Integer> overrideMap = overrides.stream()
                .collect(Collectors.toMap(SysRoleModuleDataScope::getModuleCode,
                        SysRoleModuleDataScope::getDataScope, (left, right) -> right));

        Map<Long, SysResource> resourceMap = resources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource, (left, right) -> left));

        Map<String, List<SysDataScopeConfig>> groupedConfigs = configs.stream()
                .filter(config -> StringUtils.isNotBlank(config.getResourceCode()))
                .collect(Collectors.groupingBy(SysDataScopeConfig::getResourceCode,
                        LinkedHashMap::new, Collectors.toList()));

        List<RoleModuleDataScopeVO> modules = groupedConfigs.entrySet().stream()
                .map(entry -> buildModuleDataScope(entry.getKey(), entry.getValue(), role.getDataScope(),
                        overrideMap.get(entry.getKey()), resources, resourceMap))
                .sorted(Comparator.comparing(RoleModuleDataScopeVO::getModuleName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        RoleDataScopeSettingsVO result = new RoleDataScopeSettingsVO();
        result.setDefaultDataScope(role.getDataScope());
        result.setModules(modules);
        return result;
    }

    private RoleModuleDataScopeVO buildModuleDataScope(String moduleCode,
                                                       List<SysDataScopeConfig> configs,
                                                       Integer defaultDataScope,
                                                       Integer overrideDataScope,
                                                       List<SysResource> resources,
                                                       Map<Long, SysResource> resourceMap) {
        RoleModuleDataScopeVO module = new RoleModuleDataScopeVO();
        module.setModuleCode(moduleCode);
        module.setModuleName(resolveModuleName(moduleCode, configs, resources, resourceMap));
        module.setRuleCount(configs.size());
        module.setDataScope(overrideDataScope);
        module.setEffectiveDataScope(overrideDataScope != null ? overrideDataScope : defaultDataScope);
        return module;
    }

    private String resolveModuleName(String moduleCode,
                                     List<SysDataScopeConfig> configs,
                                     List<SysResource> resources,
                                     Map<Long, SysResource> resourceMap) {
        Map<String, Long> menuNameCounts = resources.stream()
                .filter(resource -> resourceMatchesModule(resource, moduleCode))
                .map(resource -> findMenuAncestor(resource, resourceMap))
                .filter(Objects::nonNull)
                .map(SysResource::getResourceName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        if (!menuNameCounts.isEmpty()) {
            return menuNameCounts.entrySet().stream()
                    .max(Map.Entry.<String, Long>comparingByValue()
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .orElse(moduleCode);
        }
        return configs.stream()
                .map(SysDataScopeConfig::getResourceName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(moduleCode);
    }

    private boolean resourceMatchesModule(SysResource resource, String moduleCode) {
        String permission = resource.getPerms();
        return StringUtils.isNotBlank(permission)
                && (permission.equals(moduleCode) || permission.startsWith(moduleCode + ":"));
    }

    private SysResource findMenuAncestor(SysResource resource, Map<Long, SysResource> resourceMap) {
        SysResource current = resource;
        Set<Long> visited = new HashSet<>();
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            if (Integer.valueOf(2).equals(current.getResourceType())) {
                return current;
            }
            current = resourceMap.get(current.getParentId());
        }
        return null;
    }
}
