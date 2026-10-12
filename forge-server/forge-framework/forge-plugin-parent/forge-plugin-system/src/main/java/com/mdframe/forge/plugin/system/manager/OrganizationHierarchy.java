package com.mdframe.forge.plugin.system.manager;

import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/** Owns the persisted ancestor chain; callers hold a transaction for the whole tree change. */
@RequiredArgsConstructor
public final class OrganizationHierarchy {
    private final SysOrgMapper mapper;

    public void lock(Long tenantId) {
        Long locked = TenantContextHolder.executeIgnore(() -> mapper.lockOrganizationTenant(tenantId));
        if (locked == null) {
            throw new IllegalArgumentException(UiMessages.label("租户不存在或已禁用", "Tenant does not exist or is disabled"));
        }
    }

    public SysOrg current(Long id, Long tenantId) {
        return TenantContextHolder.executeIgnore(() -> mapper.selectOrgForUpdate(id, tenantId));
    }

    public String ancestors(Long id, Long parentId, Long tenantId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysOrg parent = current(parentId, tenantId);
        if (parent == null) {
            throw new IllegalArgumentException(UiMessages.label(
                    "上级组织不存在或不属于当前操作租户", "Parent organization does not exist in this tenant"));
        }
        if (Objects.equals(id, parentId) || contains(parent.getAncestors(), id)) {
            throw new IllegalArgumentException(UiMessages.label(
                    "不能将组织移动到自身或下级组织", "An organization cannot be moved under itself or its descendants"));
        }
        if (parent.getAncestors() == null || parent.getAncestors().isBlank()) {
            throw new IllegalStateException(UiMessages.label(
                    "上级组织祖先链无效", "Parent organization has an invalid ancestor chain"));
        }
        return parent.getAncestors() + "," + parentId;
    }

    public void moveDescendants(SysOrg existing, String nextAncestors) {
        if (!Objects.equals(existing.getAncestors(), nextAncestors)) {
            TenantContextHolder.executeIgnore(() -> mapper.updateDescendantAncestors(
                    existing.getTenantId(), existing.getId(), nextAncestors));
        }
    }

    private boolean contains(String ancestors, Long id) {
        return id != null && ancestors != null && Arrays.asList(ancestors.split(",")).contains(id.toString());
    }
}
