package com.mdframe.forge.plugin.system.datasource;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.BusinessDataSourceProperties;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceInfo;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceResolver;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 基于系统租户配置的业务数据源解析器。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SysTenantBusinessDataSourceResolver implements TenantBusinessDataSourceResolver {

    public static final String TENANT_ROUTING_CONFIG_KEY = "business.datasource.tenant-routing-enabled";

    private final BusinessDataSourceProperties properties;
    private final SysTenantMapper tenantMapper;
    private final ISysConfigService sysConfigService;
    private final ObjectProvider<DynamicRoutingDataSource> dynamicRoutingDataSourceProvider;
    private final TenantDatasourceAccessService datasourceAccess;

    @Override
    public TenantBusinessDataSourceInfo resolve() {
        return resolve(resolveTenantId());
    }

    @Override
    public TenantBusinessDataSourceInfo resolve(Long tenantId) {
        if (!properties.isEnabled()) {
            log.debug("租户业务数据源路由未启用: forge.business.datasource.enabled=false");
            return TenantBusinessDataSourceInfo.master();
        }
        if (tenantId == null) {
            return TenantBusinessDataSourceInfo.master();
        }
        return executeOnMaster(() -> {
            if (!isTenantRoutingEnabled()) {
                log.debug("租户业务数据源路由未启用: {}=false", TENANT_ROUTING_CONFIG_KEY);
                return TenantBusinessDataSourceInfo.master();
            }
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || tenant.getDefaultBusinessDatasourceId() == null
                    && (tenant.getDefaultBusinessDatasourceCode() == null
                    || tenant.getDefaultBusinessDatasourceCode().isEmpty())) {
                return TenantBusinessDataSourceInfo.master();
            }
            TenantDatasourceOption option = datasourceAccess.requireWritable(tenantId,
                    tenant.getDefaultBusinessDatasourceId(), tenant.getDefaultBusinessDatasourceCode());
            String dsKey = option.getDatasourceCode();
            validateDatasourceKey(dsKey);
            return TenantBusinessDataSourceInfo.builder()
                    .master(false)
                    .dsKey(dsKey)
                    .tenantId(tenantId)
                    .datasourceId(option.getDatasourceId())
                    .datasourceCode(dsKey)
                    .datasourceName(option.getDatasourceName())
                    .allowWrite(true)
                    .build();
        });
    }

    @Override
    public TenantBusinessDataSourceScope use(TenantBusinessDataSourceInfo info) {
        if (info == null || !info.shouldRoute()) {
            return () -> {
            };
        }
        TenantDatasourceOption option = datasourceAccess.requireWritable(info.getTenantId(),
                info.getDatasourceId(), info.getDatasourceCode());
        validateDatasourceKey(option.getDatasourceCode());
        DynamicDataSourceContextHolder.push(option.getDatasourceCode());
        return DynamicDataSourceContextHolder::poll;
    }

    @Override
    public TenantBusinessDataSourceScope use(Long tenantId) {
        return use(resolve(tenantId));
    }

    private boolean isTenantRoutingEnabled() {
        String configValue = sysConfigService.selectConfigByKey(TENANT_ROUTING_CONFIG_KEY);
        if (StringUtils.isBlank(configValue)) {
            return properties.isTenantRoutingEnabledDefault();
        }
        return Boolean.parseBoolean(configValue);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SessionHelper.getTenantId();
    }

    private void validateDatasourceKey(String dsKey) {
        DynamicRoutingDataSource dynamicRoutingDataSource = dynamicRoutingDataSourceProvider.getIfAvailable();
        if (dynamicRoutingDataSource == null) {
            throw new BusinessException("dynamic-datasource 未启用，无法切换租户业务数据源");
        }
        if (!dynamicRoutingDataSource.getDataSources().containsKey(dsKey)) {
            throw new BusinessException(UiMessages.translate("租户业务数据源未在 dynamic-datasource 中配置: ") + dsKey);
        }
    }

    private <T> T executeOnMaster(Supplier<T> action) {
        DynamicDataSourceContextHolder.push("master");
        try {
            return action.get();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }
}
