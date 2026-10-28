package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.mapper.TenantOrganizationDirectoryMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.identity.TenantOrganization;
import com.mdframe.forge.starter.core.identity.TenantOrganizationDirectory;
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

/** Current committed organization reads belong to the system database. */
@Service
@DS("master")
@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
@RequiredArgsConstructor
public class SystemTenantOrganizationDirectory implements TenantOrganizationDirectory {
    private final TenantOrganizationDirectoryMapper mapper;

    @Override
    public List<TenantOrganization> findActiveOrganizations(Collection<Long> orgIds) {
        Long tenantId = requireCurrentTenant();
        if (orgIds == null || orgIds.size() > MAX_BATCH_SIZE
                || orgIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalidInput();
        }
        if (orgIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = orgIds.stream().distinct().toList();
        return onMaster(() -> mapper.selectActiveOrganizations(tenantId, ids));
    }

    @Override
    public List<TenantOrganization> findActiveChildren(Long parentId, Long afterOrgId, int limit) {
        Long tenantId = requireCurrentTenant();
        if (parentId == null || parentId < 0 || (afterOrgId != null && afterOrgId <= 0)
                || limit < 1 || limit > MAX_PAGE_SIZE) {
            throw invalidInput();
        }
        return onMaster(() -> mapper.selectActiveChildren(tenantId, parentId, afterOrgId, limit));
    }

    private Long requireCurrentTenant() {
        LoginUser caller = SessionHelper.getLoginUser();
        if (caller == null || caller.getUserId() == null || caller.getUserId() <= 0
                || caller.getTenantId() == null || caller.getTenantId() <= 0) {
            throw new BusinessException(401, UiMessages.label(
                    "组织目录查询需要已认证租户身份",
                    "An authenticated tenant identity is required for organization lookup"));
        }
        Long contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant != null && !contextTenant.equals(caller.getTenantId())) {
            throw new BusinessException(403, UiMessages.label(
                    "组织目录查询租户上下文不一致",
                    "Organization lookup tenant context does not match the authenticated tenant"));
        }
        return caller.getTenantId();
    }

    private BusinessException invalidInput() {
        return new BusinessException(400, UiMessages.label(
                "组织目录查询参数超出允许范围",
                "Organization lookup parameters are outside the allowed bounds"));
    }

    private List<TenantOrganization> onMaster(Supplier<List<TenantOrganization>> query) {
        boolean wasSkippingDataScope = DataScopeContextHolder.isSkip();
        DynamicDataSourceContextHolder.push("master");
        // Bounded directory queries have explicit tenant predicates. Business record
        // and selectable-organization authorization remain the caller's responsibility.
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
