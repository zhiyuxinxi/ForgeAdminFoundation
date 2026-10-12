package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.vo.RoleDataScopeSettingsVO;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.datascope.service.DataScopeCacheRefresh;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Reads and persists validated module configuration; refreshes its cache only after commit. */
@RequiredArgsConstructor
final class RoleDataScopeConfiguration {
    private final SysRoleMapper roleMapper;
    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final SysRoleModuleDataScopeMapper roleModuleDataScopeMapper;
    private final SysResourceMapper resourceMapper;
    private final IDataScopeService dataScopeService;

    RoleDataScopeSettingsVO read(SysRole role) {
        return new RoleDataScopeProjection().project(role, listEnabledDataScopeConfigs(role.getTenantId()),
                listRoleModuleDataScopes(role), TenantContextHolder.executeIgnore(() ->
                        resourceMapper.selectModuleNamingResources(role.getTenantId())));
    }

    Set<String> availableModules(SysRole role) {
        return listEnabledDataScopeConfigs(role.getTenantId()).stream()
                .map(SysDataScopeConfig::getResourceCode).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    boolean save(SysRole role, Integer defaultDataScope, Set<String> availableCodes,
                 Map<String, Integer> overrides) {
        return TenantContextHolder.executeIgnore(() -> {
            SysRole update = new SysRole();
            update.setId(role.getId());
            update.setTenantId(role.getTenantId());
            update.setDataScope(defaultDataScope);
            if (roleMapper.updateById(update) <= 0) {
                return false;
            }
            if (!availableCodes.isEmpty()) {
                roleModuleDataScopeMapper.deleteByRoleAndModules(role.getTenantId(), role.getId(), availableCodes);
            }
            overrides.forEach((code, scope) -> insertOverride(role, code, scope));
            return true;
        });
    }

    private void insertOverride(SysRole role, String code, Integer scope) {
        SysRoleModuleDataScope override = new SysRoleModuleDataScope();
        override.setTenantId(role.getTenantId());
        override.setRoleId(role.getId());
        override.setModuleCode(code);
        override.setDataScope(scope);
        roleModuleDataScopeMapper.insert(override);
    }

    private List<SysDataScopeConfig> listEnabledDataScopeConfigs(Long tenantId) {
        return TenantContextHolder.executeIgnore(() ->
                dataScopeConfigMapper.selectEnabledModuleConfigs(tenantId));
    }

    private List<SysRoleModuleDataScope> listRoleModuleDataScopes(SysRole role) {
        return TenantContextHolder.executeIgnore(() ->
                roleModuleDataScopeMapper.selectByRole(role.getTenantId(), role.getId()));
    }

    Integer mostPermissiveRange(SysRole role) {
        Integer defaultDataScope = role.getDataScope() != null
                ? role.getDataScope()
                : DataScopeType.SELF.getCode();
        return listRoleModuleDataScopes(role).stream()
                .map(SysRoleModuleDataScope::getDataScope)
                .filter(Objects::nonNull)
                .reduce(defaultDataScope, Math::min);
    }

    void refreshAfterCommit() {
        DataScopeCacheRefresh.afterCommit(dataScopeService);
    }

}
