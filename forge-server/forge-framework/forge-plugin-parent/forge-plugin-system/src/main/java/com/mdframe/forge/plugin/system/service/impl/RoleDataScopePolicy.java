package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import cn.hutool.core.collection.CollUtil;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.RoleModuleDataScopeDTO;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Validates role and module ranges against the trusted caller and assigned users. */
@RequiredArgsConstructor
final class RoleDataScopePolicy {
    private final SysRoleMapper roleMapper;
    private final LoginUser loginUser;

    void validateSetting(SysRole role, Integer dataScope) {
        validateSupportedDataScope(dataScope);
        validateDataScopeAllowedForCurrentUser(dataScope);
        validateDataScopeAllowedForBoundUsers(role, dataScope);
    }

    void validateDataScopeAllowedForCurrentUser(Integer dataScope) {
        if (!isDataScopeAllowedForUserType(dataScope, normalizeUserType(loginUser.getUserType()))) {
            throw new RuntimeException("不能设置超过当前用户类型上限的数据范围");
        }
    }

    void validateDataScopeAllowedForBoundUsers(SysRole role, Integer dataScope) {
        if (dataScope == null) {
            return;
        }
        Long exceedCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countRoleUsersExceedingDataScope(dataScope, role.getId(), role.getTenantId()));
        if (exceedCount != null && exceedCount > 0) {
            throw new RuntimeException("角色数据范围超过目标用户类型上限");
        }
    }

    void validateSupportedDataScope(Integer dataScope) {
        if (DataScopeType.getByRoleDataScope(dataScope, false) == null) {
            throw new RuntimeException("不支持的数据权限范围");
        }
    }

    void validateDataScopeAllowedForUsers(Integer dataScope, List<Long> userIds, Long tenantId) {
        if (dataScope == null || CollUtil.isEmpty(userIds)) {
            return;
        }
        Long exceedCount = TenantContextHolder.executeIgnore(() ->
                roleMapper.countUsersExceedingDataScope(dataScope, userIds, tenantId));
        if (exceedCount != null && exceedCount > 0) {
            throw new RuntimeException("角色数据范围超过目标用户类型上限");
        }
    }

    boolean isDataScopeAllowedForUserType(Integer dataScope, int userType) {
        if (dataScope == null || userType == SystemConstants.UserType.SYSTEM_ADMIN) {
            return true;
        }
        if (userType == SystemConstants.UserType.TENANT_ADMIN) {
            return dataScope != SystemConstants.RoleDataScope.ALL;
        }
        return dataScope != SystemConstants.RoleDataScope.ALL
                && dataScope != SystemConstants.RoleDataScope.TENANT;
    }

    int normalizeUserType(Integer userType) {
        if (userType == null) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        if (userType < SystemConstants.UserType.SYSTEM_ADMIN || userType > SystemConstants.UserType.NORMAL_USER) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        return userType;
    }

    Set<String> normalizeModuleCodes(Collection<String> moduleCodes) {
        if (moduleCodes == null) {
            return new LinkedHashSet<>();
        }
        return moduleCodes.stream().map(StringUtils::trimToNull).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    Map<String, Integer> moduleOverrides(List<RoleModuleDataScopeDTO> scopes,
                                         Set<String> allowedCodes, SysRole role, boolean scoped) {
        Map<String, Integer> result = new LinkedHashMap<>();
        Set<String> seenCodes = new HashSet<>();
        if (scopes == null) {
            return result;
        }
        for (RoleModuleDataScopeDTO scope : scopes) {
            String code = checkedModuleCode(scope, allowedCodes, scoped);
            if (scoped ? result.containsKey(code) : !seenCodes.add(code)) {
                throw new RuntimeException(UiMessages.translate("业务模块重复配置：") + code);
            }
            if (scope.getDataScope() != null) {
                validateSetting(role, scope.getDataScope());
                result.put(code, scope.getDataScope());
            }
        }
        return result;
    }

    private String checkedModuleCode(RoleModuleDataScopeDTO scope, Set<String> allowedCodes, boolean scoped) {
        if (scope == null || StringUtils.isBlank(scope.getModuleCode())) {
            throw new RuntimeException("业务模块编码不能为空");
        }
        String code = scope.getModuleCode().trim();
        if (!allowedCodes.contains(code)) {
            String message = scoped ? "业务模块不属于当前授权范围：" : "业务模块不存在或未启用：";
            throw new RuntimeException(UiMessages.translate(message) + code);
        }
        return code;
    }

}
