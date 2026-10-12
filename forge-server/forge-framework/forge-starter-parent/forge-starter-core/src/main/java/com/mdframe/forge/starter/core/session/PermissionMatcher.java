package com.mdframe.forge.starter.core.session;

import cn.dev33.satoken.strategy.SaStrategy;

import java.util.Collection;
import java.util.List;

/** Permission strings only: use the same grant-pattern strategy as Sa-Token, without role or tenant policy. */
public final class PermissionMatcher {
    private PermissionMatcher() {
    }

    public static boolean hasPermission(Collection<String> granted, String required) {
        return matches(toSaTokenPermissions(granted), required);
    }

    public static boolean hasAnyPermission(Collection<String> granted, String... required) {
        if (required == null || required.length == 0) {
            return false;
        }
        List<String> grants = toSaTokenPermissions(granted);
        for (String permission : required) {
            if (matches(grants, permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAllPermissions(Collection<String> granted, String... required) {
        if (required == null || required.length == 0) {
            return false;
        }
        List<String> grants = toSaTokenPermissions(granted);
        for (String permission : required) {
            if (!matches(grants, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(List<String> granted, String required) {
        return required != null && !required.isBlank()
                && Boolean.TRUE.equals(SaStrategy.instance.hasElement.apply(granted, required));
    }

    /** Preserve the project's existing full-permission alias at both manual and Sa-Token consumers. */
    public static List<String> toSaTokenPermissions(Collection<String> granted) {
        return granted == null ? List.of() : granted.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .map(permission -> "*:*:*".equals(permission) ? "*" : permission).toList();
    }
}
