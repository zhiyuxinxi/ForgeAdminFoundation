package com.mdframe.forge.business.core.demo.vo;

import com.mdframe.forge.business.core.demo.domain.BusinessDatasourceDemoRecord;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceInfo;
import lombok.Data;

import java.util.List;

/**
 * 租户业务数据源路由演示结果。
 */
@Data
public class BusinessDatasourceDemoResult {

    private TenantBusinessDataSourceInfo datasource;

    private String currentDsKey;

    private String databaseName;

    private BusinessDatasourceDemoRecord record;

    private List<BusinessDatasourceDemoRecord> records;
}
