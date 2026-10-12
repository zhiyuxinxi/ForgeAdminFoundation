package com.mdframe.forge.starter.core.identity;

import java.util.Collection;
import java.util.List;

/**
 * Read-only directory of enabled members in the caller's authenticated current tenant.
 * The system plugin owns membership/account/organization semantics; business consumers
 * depend only on this contract and retain their own action and object-scope checks.
 * There is deliberately no arbitrary tenant parameter, name-to-identity resolution,
 * account mutation, or business-specific approver policy.
 */
public interface TenantMemberDirectory {
    int MAX_BATCH_SIZE = 500;
    int MAX_PAGE_SIZE = 200;

    /**
     * Finds active members by stable IDs, optionally requiring a direct binding to an
     * enabled organization of the same tenant. Null directOrgId means no org filter.
     * Missing/inactive IDs are omitted; callers requiring all IDs must compare the
     * result with the requested set. Empty input returns an empty list, never all users.
     * At most MAX_BATCH_SIZE input IDs; results are ordered by userId.
     * Not a historical-name lookup: inactive users must not make historical records vanish.
     */
    List<TenantMember> findActiveMembers(Collection<Long> userIds, Long directOrgId);

    /**
     * Bounded candidate lookup in one required, enabled, directly joined organization.
     * Keyword searches username/display name; identity remains userId. Null afterUserId
     * starts the first page; pass the last returned ID for the next page, preserving
     * org/keyword. Limit must be 1..MAX_PAGE_SIZE; no unbounded or silently truncated list.
     * The caller must authorize the business operation and requested organization.
     */
    List<TenantMember> findActiveMembersByOrg(Long directOrgId, String keyword,
                                            Long afterUserId, int limit);
}
