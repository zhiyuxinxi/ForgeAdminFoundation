package com.mdframe.forge.starter.core.identity;

/** Current post projection. orgId may be null for a tenant-wide post; names are display values. */
public record TenantPost(Long postId, Long orgId, String postCode, String postName) {
}
