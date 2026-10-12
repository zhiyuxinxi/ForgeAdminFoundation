package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantMemberDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
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

/** Current committed membership reads belong to the system database, not a business datasource. */
@Service
@DS("master")
@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
@RequiredArgsConstructor
public class SystemTenantMemberDirectory implements TenantMemberDirectory {
    private final TenantMemberDirectoryMapper mapper;

    @Override
    public List<TenantMember> findActiveMembers(Collection<Long> userIds, Long directOrgId) {
        Long tenantId = requireCurrentTenant();
        checkOptionalId(directOrgId);
        if (userIds == null || userIds.size() > MAX_BATCH_SIZE
                || userIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalidInput();
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = userIds.stream().distinct().toList();
        return onMaster(() -> mapper.selectActiveMembers(tenantId, ids, directOrgId));
    }

    @Override
    public List<TenantMember> findActiveMembersByOrg(Long directOrgId, String keyword,
                                                   Long afterUserId, int limit) {
        Long tenantId = requireCurrentTenant();
        checkOptionalId(directOrgId);
        checkOptionalId(afterUserId);
        if (directOrgId == null || limit < 1 || limit > MAX_PAGE_SIZE
                || (keyword != null && keyword.length() > 100)) {
            throw invalidInput();
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return onMaster(() -> mapper.selectActiveMembersByOrg(
                tenantId, directOrgId, normalizedKeyword, afterUserId, limit));
    }

    private Long requireCurrentTenant() {
        LoginUser caller = SessionHelper.getLoginUser();
        if (caller == null || caller.getUserId() == null || caller.getUserId() <= 0
                || caller.getTenantId() == null || caller.getTenantId() <= 0) {
            throw new BusinessException(401, UiMessages.label(
                    "有效成员查询需要已认证租户身份", "An authenticated tenant identity is required for member lookup"));
        }
        Long contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant != null && !contextTenant.equals(caller.getTenantId())) {
            throw new BusinessException(403, UiMessages.label(
                    "成员查询租户上下文不一致", "Member lookup tenant context does not match the authenticated tenant"));
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
                "成员查询参数超出允许范围", "Member lookup parameters are outside the allowed bounds"));
    }

    private List<TenantMember> onMaster(Supplier<List<TenantMember>> query) {
        boolean wasSkippingDataScope = DataScopeContextHolder.isSkip();
        DynamicDataSourceContextHolder.push("master");
        // This bounded identity lookup has explicit tenant/member predicates. Business
        // record data-scope policies belong to its caller, not to identity validation.
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
