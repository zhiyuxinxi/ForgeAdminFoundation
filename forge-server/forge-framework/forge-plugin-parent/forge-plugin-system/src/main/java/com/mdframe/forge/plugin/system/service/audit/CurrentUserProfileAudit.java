package com.mdframe.forge.plugin.system.service.audit;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Audits self-service profile fields without entering user-management authorization. */
public final class CurrentUserProfileAudit {
    private final ISysUserService users;

    public CurrentUserProfileAudit(ISysUserService users) {
        this.users = users;
    }

    public Map<String, Object> capture() {
        Long userId = SessionHelper.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        // Accounts can belong to several tenants; the immutable lookup key is the trusted caller,
        // exactly as in the self-service update. Never accept a target ID from the request body.
        SysUser user = TenantContextHolder.executeIgnore(() -> users.getById(userId));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", userId);
        snapshot.put("exists", user != null);
        if (user != null) {
            snapshot.put("username", user.getUsername());
            snapshot.put("realName", user.getRealName());
            snapshot.put("email", SensitiveDataUtil.maskEmail(user.getEmail()));
            snapshot.put("phone", SensitiveDataUtil.maskPhone(user.getPhone()));
            snapshot.put("avatar", user.getAvatar());
        }
        return snapshot;
    }
}
