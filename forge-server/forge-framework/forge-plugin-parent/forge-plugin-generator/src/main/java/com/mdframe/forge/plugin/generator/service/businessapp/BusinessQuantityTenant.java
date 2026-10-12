package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;

/** The same authenticated tenant boundary applies to quantity reads and writes. */
final class BusinessQuantityTenant {
    private BusinessQuantityTenant() {
    }

    static Long require() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (RuntimeException error) {
            throw new BusinessException(UiMessages.label("无法获取数量台账租户上下文", "Cannot resolve the quantity ledger tenant context"), error);
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(UiMessages.label("数量台账缺少有效租户上下文", "Quantity ledger requires a valid tenant context"));
        }
        return tenantId;
    }
}
