package com.mdframe.forge.starter.tenant.datasource;

/**
 * 租户业务数据源解析扩展点。
 */
public interface TenantBusinessDataSourceResolver {

    TenantBusinessDataSourceInfo resolve();

    TenantBusinessDataSourceInfo resolve(Long tenantId);

    TenantBusinessDataSourceScope use(TenantBusinessDataSourceInfo info);

    TenantBusinessDataSourceScope use(Long tenantId);
}
