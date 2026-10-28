package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantPostDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.identity.TenantPost;
import com.mdframe.forge.starter.core.identity.TenantPostDirectory;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Current committed post references always belong to the system master database. */
@Service
@DS("master")
@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
@RequiredArgsConstructor
public class SystemTenantPostDirectory implements TenantPostDirectory {
    private final TenantPostDirectoryMapper mapper;

    @Override
    public List<TenantPost> findActivePosts(Collection<Long> postIds, Long directOrgId) {
        Long tenantId = requireCurrentTenant();
        checkOptionalId(directOrgId);
        if (postIds == null || postIds.size() > MAX_BATCH_SIZE
                || postIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalidInput();
        }
        if (postIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = postIds.stream().distinct().toList();
        return onMaster(() -> mapper.selectActivePosts(tenantId, ids, directOrgId));
    }

    @Override
    public List<TenantPost> findActivePostsByOrg(Long directOrgId, String keyword,
                                               Long afterPostId, int limit) {
        Long tenantId = requireCurrentTenant();
        checkOptionalId(directOrgId);
        checkPage(keyword, afterPostId, limit);
        if (directOrgId == null) {
            throw invalidInput();
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return onMaster(() -> mapper.selectActivePostsByOrg(
                tenantId, directOrgId, normalizedKeyword, afterPostId, limit));
    }

    @Override
    public List<TenantPost> findActiveTenantWidePosts(String keyword, Long afterPostId, int limit) {
        Long tenantId = requireCurrentTenant();
        checkPage(keyword, afterPostId, limit);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return onMaster(() -> mapper.selectActiveTenantWidePosts(tenantId, normalizedKeyword, afterPostId, limit));
    }

    private void checkPage(String keyword, Long afterPostId, int limit) {
        checkOptionalId(afterPostId);
        if (limit < 1 || limit > MAX_PAGE_SIZE || (keyword != null && keyword.length() > 100)) {
            throw invalidInput();
        }
    }

    private Long requireCurrentTenant() {
        LoginUser caller = SessionHelper.getLoginUser();
        if (caller == null || caller.getUserId() == null || caller.getUserId() <= 0
                || caller.getTenantId() == null || caller.getTenantId() <= 0) {
            throw new BusinessException(401, UiMessages.label(
                    "岗位目录查询需要已认证租户身份", "An authenticated tenant identity is required for post lookup"));
        }
        Long contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant != null && !contextTenant.equals(caller.getTenantId())) {
            throw new BusinessException(403, UiMessages.label(
                    "岗位目录查询租户上下文不一致", "Post lookup tenant context does not match the authenticated tenant"));
        }
        return caller.getTenantId();
    }

    private void checkOptionalId(Long id) {
        if (id != null && id <= 0) {
            throw invalidInput();
        }
    }

    private BusinessException invalidInput() {
        return new BusinessException(400, UiMessages.label(
                "岗位目录查询参数超出允许范围", "Post lookup parameters are outside the allowed bounds"));
    }

    private List<TenantPost> onMaster(Supplier<List<TenantPost>> query) {
        boolean wasSkippingDataScope = DataScopeContextHolder.isSkip();
        DynamicDataSourceContextHolder.push("master");
        // XML owns explicit current-tenant identity predicates. Business scope stays
        // with the caller, not the business-record data-scope rewrite of this lookup.
        DataScopeContextHolder.skipDataScope();
        try {
            return List.copyOf(query.get());
        } finally {
            if (!wasSkippingDataScope) {
                DataScopeContextHolder.clearSkip();
            }
            DynamicDataSourceContextHolder.poll();
        }
    }
}
