package com.mdframe.forge.starter.core.identity;

/**
 * Minimal current-member projection. Names are display values, never authorization keys.
 * No account credentials, contact details or personal profile are exposed.
 */
public record TenantMember(Long userId, String username, String displayName) {
}
