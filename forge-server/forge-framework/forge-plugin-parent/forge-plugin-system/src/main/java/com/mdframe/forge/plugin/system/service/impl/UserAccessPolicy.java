package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.PermissionMatcher;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 用户管理的身份、动作、目标层级与租户边界；不执行用户或关系写入。 */
@RequiredArgsConstructor
final class UserAccessPolicy {

    private static final String[] USER_MANAGEMENT_PERMISSIONS = {
            "system:user:list", "system:user:query", "system:user:add", "system:user:edit", "system:user:remove",
            "system:org:list", "system:org:query",
            "system:role:list", "system:role:query"
    };

    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;

    void normalizeUserQueryTenant(SysUserQuery query) {
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (!loginUser.isAdmin()) {
            query.setTenantId(loginUser.getTenantId());
        }
    }

    LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    Long resolveWriteTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.isAdmin()
                ? (requestedTenantId != null ? requestedTenantId : loginUser.getTenantId())
                : loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    Long resolveCurrentTenantIdForNonAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.getTenantId() == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser.getTenantId();
    }

    List<Long> resolveWriteTenantIds(List<Long> requestedTenantIds, Long defaultTenantId, LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (!loginUser.isAdmin()) {
            return List.of(resolveCurrentTenantIdForNonAdmin());
        }
        List<Long> tenantIds = normalizeTenantIdList(requestedTenantIds);
        if (tenantIds.isEmpty()) {
            tenantIds.add(defaultTenantId != null ? defaultTenantId : loginUser.getTenantId());
        }
        if (defaultTenantId != null && !tenantIds.contains(defaultTenantId)) {
            throw new RuntimeException("默认租户必须包含在所属租户中");
        }
        tenantIds.forEach(this::validateTenantEnabled);
        return tenantIds;
    }

    List<Long> normalizeTenantIdList(List<Long> tenantIds) {
        if (tenantIds == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tenantIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    Long resolveDefaultTenantId(List<Long> tenantIds, Long requestedDefaultTenantId) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            throw new RuntimeException("租户不能为空");
        }
        if (requestedDefaultTenantId != null) {
            if (!tenantIds.contains(requestedDefaultTenantId)) {
                throw new RuntimeException("默认租户必须包含在所属租户中");
            }
            return requestedDefaultTenantId;
        }
        return tenantIds.get(0);
    }

    Long resolveRoleBindTenantId(SysUser user) {
        return resolveRoleBindTenantId(user, null);
    }

    Long resolveRoleBindTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                    ? requestedTenantId
                    : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    void validateUserTypeForWrite(SysUserDTO dto) {
        LoginUser loginUser = requireLoginUser();
        Integer userType = dto.getUserType();
        if (loginUser.isAdmin()) {
            return;
        }
        if (userType != null && userType != 2) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    Integer resolveWriteUserType(Integer requestedUserType) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return requestedUserType != null ? requestedUserType : 2;
        }
        return 2;
    }

    void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<com.mdframe.forge.plugin.system.entity.SysTenant>()
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getId, tenantId)
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }

    void assertCanManageUser(Long userId) {
        assertCanManageUser(userId, false);
    }

    void assertCanManageUser(Long userId, boolean includeDisabledMembership) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId(), includeDisabledMembership)) {
            throw new RuntimeException("无权操作非本租户用户");
        }
        if (resolveEffectiveUserType(userId, loginUser.getTenantId(), includeDisabledMembership)
                != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    void assertCanReadUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户用户");
        }
        if (isCurrentLoginUser(userId, loginUser)) {
            return;
        }
        if (resolveEffectiveUserType(userId, loginUser.getTenantId()) != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException("租户管理员只能查看普通用户");
        }
    }

    Long resolveTenantScopedOperationTenantId(Long userId) {
        return resolveTenantScopedOperationTenantId(userId, null);
    }

    Long resolveTenantScopedOperationTenantId(Long userId, Long requestedTenantId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return resolveTenantScopedOperationTenantId(user, requestedTenantId);
    }

    Long resolveTenantScopedOperationTenantId(SysUser user) {
        return resolveTenantScopedOperationTenantId(user, null);
    }

    Long resolveTenantScopedOperationTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                    ? requestedTenantId
                    : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    boolean isUserInTenant(Long userId, Long tenantId) {
        return isUserInTenant(userId, tenantId, false);
    }

    boolean isUserInTenant(Long userId, Long tenantId, boolean includeDisabledMembership) {
        if (userId == null || tenantId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(!includeDisabledMembership, SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    boolean hasEnabledTenantMembership(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    void assertUserManagementAllowed() {
        assertUserManagementAllowed(requireLoginUser());
    }

    void assertUserActionAllowed(String permission) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && !loginUser.isTenantAdmin()
                && !hasAnyPermission(loginUser, permission)) {
            throw new BusinessException(403, UiMessages.message("customer.userOrganizationDenied"));
        }
    }

    void assertUserManagementAllowed(LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, USER_MANAGEMENT_PERMISSIONS)) {
            throw new RuntimeException("无权访问用户组织管理功能");
        }
    }

    boolean hasAnyPermission(LoginUser loginUser, String... permissions) {
        return PermissionMatcher.hasAnyPermission(loginUser == null ? null : loginUser.getPermissions(), permissions);
    }

    void assertNotSelfManagement(Long userId) {
        LoginUser loginUser = requireLoginUser();
        if (userId != null && Objects.equals(userId, loginUser.getUserId())) {
            throw new RuntimeException("不能在用户管理中维护当前登录用户");
        }
    }

    void assertNotSelfManagementUnlessAdmin(Long userId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (userId != null && Objects.equals(userId, loginUser.getUserId())) {
            throw new RuntimeException("不能在用户管理中维护当前登录用户");
        }
    }

    boolean isCurrentLoginUser(Long userId, LoginUser loginUser) {
        return userId != null && loginUser != null && Objects.equals(userId, loginUser.getUserId());
    }

    int resolveEffectiveUserType(Long userId, Long tenantId) {
        return resolveEffectiveUserType(userId, tenantId, false);
    }

    int resolveEffectiveUserType(Long userId, Long tenantId, boolean includeDisabledMembership) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
            return SystemConstants.UserType.SYSTEM_ADMIN;
        }
        SysUserTenant member = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectOne(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(!includeDisabledMembership, SysUserTenant::getStatus, 1)
                        .last("LIMIT 1")));
        if (member == null) {
            throw new RuntimeException("目标用户不属于当前租户");
        }
        return Objects.equals(member.getMemberType(), SystemConstants.UserType.TENANT_ADMIN)
                ? SystemConstants.UserType.TENANT_ADMIN
                : SystemConstants.UserType.NORMAL_USER;
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
}
