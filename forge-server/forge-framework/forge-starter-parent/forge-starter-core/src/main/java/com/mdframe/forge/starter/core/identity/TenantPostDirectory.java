package com.mdframe.forge.starter.core.identity;

import java.util.Collection;
import java.util.List;

/**
 * Read-only posts owned by system, in the authenticated caller's current tenant.
 * Consumers authorize their business operation and organization scope; this directory
 * neither grants permissions nor implies that a member holds a post. No tenant input.
 */
public interface TenantPostDirectory {
    int MAX_BATCH_SIZE = 500;
    int MAX_PAGE_SIZE = 200;

    /**
     * Validates up to 500 stable post IDs against active tenant/posts and, for an owned
     * post, its active organization in that tenant. Null directOrgId adds no org filter:
     * explicitly requested tenant-wide posts (orgId=null) may be returned. A non-null
     * org requires that exact direct org and never includes tenant-wide/descendant posts.
     * Empty input returns empty. Missing/inactive IDs are omitted; callers requiring all
     * IDs compare the sets. Results are ordered by postId. Not a historical-name lookup;
     * existing records must retain their IDs/display snapshots when a post is disabled.
     */
    List<TenantPost> findActivePosts(Collection<Long> postIds, Long directOrgId);

    /**
     * Bounded candidates from one required, positive, caller-authorized directOrgId.
     * No implicit tenant-wide or descendant inclusion. Keyword matches code/name;
     * afterPostId is the last returned stable ID (null for first page), limit is 1..200.
     * Keep org/keyword stable between pages. Passing null here is invalid, never an all-post query.
     */
    List<TenantPost> findActivePostsByOrg(Long directOrgId, String keyword,
                                        Long afterPostId, int limit);

    /**
     * Explicit tenant-wide candidates with orgId=null only, never department-owned posts.
     * The caller must authorize tenant-wide post selection; current tenant is still trusted
     * identity only. Keyword/cursor/limit follow the same bounded contract as by-org lookup.
     */
    List<TenantPost> findActiveTenantWidePosts(String keyword, Long afterPostId, int limit);
}
