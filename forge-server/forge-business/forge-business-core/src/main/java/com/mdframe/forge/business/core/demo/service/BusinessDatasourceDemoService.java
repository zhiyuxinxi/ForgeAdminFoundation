package com.mdframe.forge.business.core.demo.service;

import com.mdframe.forge.business.core.demo.vo.BusinessDatasourceDemoResult;

/**
 * 租户业务数据源路由演示服务。
 */
public interface BusinessDatasourceDemoService {

    BusinessDatasourceDemoResult current();

    BusinessDatasourceDemoResult currentForTenant(Long tenantId);

    BusinessDatasourceDemoResult prepare(String title);

    BusinessDatasourceDemoResult prepareForTenant(Long tenantId, String title);

    BusinessDatasourceDemoResult list(Integer limit);

    BusinessDatasourceDemoResult listForTenant(Long tenantId, Integer limit);
}
