package com.mdframe.forge.starter.tenant.datasource;

/**
 * 租户业务数据源作用域句柄。
 */
public interface TenantBusinessDataSourceScope extends AutoCloseable {

    @Override
    void close();
}
