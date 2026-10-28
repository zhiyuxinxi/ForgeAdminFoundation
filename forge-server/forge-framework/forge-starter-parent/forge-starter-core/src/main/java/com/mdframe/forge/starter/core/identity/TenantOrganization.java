package com.mdframe.forge.starter.core.identity;

/**
 * Minimal current-organization projection. Names are display values, not identity or
 * authorization. Parent ID describes the stored hierarchy; zero denotes a root.
 * Management fields, contact details and mutable ancestry are deliberately absent.
 */
public record TenantOrganization(Long orgId, Long parentId, String orgName) {
}
