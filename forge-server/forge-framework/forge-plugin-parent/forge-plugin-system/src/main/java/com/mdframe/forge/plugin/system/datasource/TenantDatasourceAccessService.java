package com.mdframe.forge.plugin.system.datasource;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.TenantDatasourceGrantMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Platform grants are checked on every save and resolution; selections never imply grants. */
@Service
@DS("master")
@RequiredArgsConstructor
public class TenantDatasourceAccessService {
    private final TenantDatasourceGrantMapper grants;
    private final SysTenantMapper tenants;

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public TenantDatasourceOption requireWritable(Long tenantId, Long datasourceId, String code) {
        if (tenantId == null || datasourceId == null || code == null || code.isBlank()) {
            throw denied();
        }
        return onMaster(() -> {
            TenantDatasourceOption option = grants.selectDatasource(tenantId, datasourceId);
            if (option == null || !option.isGranted() || !option.isSelectable()
                    || !Objects.equals(option.getDatasourceCode(), code)) {
                throw denied();
            }
            return option;
        });
    }

    public List<TenantDatasourceOption> options(Long tenantId, boolean grantedOnly) {
        requireAdministrator();
        return onMaster(() -> {
            requireTenant(tenantId);
            return grants.selectOptions(tenantId).stream()
                    .filter(option -> !grantedOnly || option.isGranted() && option.isSelectable())
                    .toList();
        });
    }

    public void setGrant(Long tenantId, Long datasourceId, boolean enabled) {
        LoginUser administrator = requireAdministrator();
        if (datasourceId == null) {
            throw denied();
        }
        onMaster(() -> {
            requireTenant(tenantId);
            if (enabled) {
                TenantDatasourceOption option = grants.selectDatasource(tenantId, datasourceId);
                if (option == null || !option.isSelectable()) {
                    throw denied();
                }
                grants.grant(tenantId, datasourceId, administrator.getUserId());
            } else {
                grants.revoke(tenantId, datasourceId);
            }
            return null;
        });
    }

    private LoginUser requireAdministrator() {
        LoginUser caller = SessionHelper.getLoginUser();
        if (caller == null || caller.getUserId() == null || !caller.isAdmin()) {
            throw new BusinessException(403, UiMessages.translate("只有超级管理员可以操作租户"));
        }
        return caller;
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenants.selectById(tenantId) == null) {
            throw denied();
        }
    }

    private BusinessException denied() {
        return new BusinessException(403, UiMessages.translate("租户业务数据源未授权或不可写"));
    }

    private <T> T onMaster(Supplier<T> action) {
        DynamicDataSourceContextHolder.push("master");
        try {
            return TenantContextHolder.executeIgnore(action);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }
}
