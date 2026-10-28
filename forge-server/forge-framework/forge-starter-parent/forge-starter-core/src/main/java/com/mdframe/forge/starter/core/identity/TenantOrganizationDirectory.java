package com.mdframe.forge.starter.core.identity;

import java.util.Collection;
import java.util.List;

/**
 * Read-only directory of enabled organizations in the authenticated current tenant.
 * System owns the master data. Business callers retain action and selectable-scope
 * authorization; a directory result never grants permission to read or write a record.
 * No arbitrary tenant parameter, name-to-ID resolution or organization mutation.
 */
public interface TenantOrganizationDirectory {
    int MAX_BATCH_SIZE = 500;
    int MAX_PAGE_SIZE = 200;

    /**
     * Finds enabled, non-deleted organizations by positive stable IDs, ordered by orgId.
     * At most MAX_BATCH_SIZE input IDs; duplicates are collapsed. Empty input returns
     * empty, never all organizations. Missing/inactive IDs are omitted; callers requiring
     * all references to be valid must compare the result with the requested ID set.
     * Checks each organization's own status, not ancestor or business authorization.
     * This is not historical lookup: retain historical IDs and display snapshots.
     */
    List<TenantOrganization> findActiveOrganizations(Collection<Long> orgIds);

    /**
     * Lists one required direct parent's enabled children, ordered by orgId. Zero means
     * tenant roots; a positive parent must itself be enabled in the current tenant.
     * Null/negative parent is invalid. Null afterOrgId starts a page; otherwise it must
     * be positive. Continue with the last returned orgId and the same parent.
     * Limit is 1..MAX_PAGE_SIZE. This is a bounded page, not a complete tree or snapshot.
     * The caller must authorize the business use and requested organization scope.
     */
    List<TenantOrganization> findActiveChildren(Long parentId, Long afterOrgId, int limit);
}
