package com.mdframe.forge.business.core.demo.service.impl;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.business.core.demo.domain.BusinessDatasourceDemoRecord;
import com.mdframe.forge.business.core.demo.mapper.BusinessDatasourceDemoMapper;
import com.mdframe.forge.business.core.demo.service.BusinessDatasourceDemoService;
import com.mdframe.forge.business.core.demo.vo.BusinessDatasourceDemoResult;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSource;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceExecutor;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceInfo;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户业务数据源路由演示服务实现。
 */
@Service
@RequiredArgsConstructor
public class BusinessDatasourceDemoServiceImpl implements BusinessDatasourceDemoService {

    private static final int DEFAULT_LIMIT = 10;

    private final BusinessDatasourceDemoMapper demoMapper;
    private final IdentifierGenerator identifierGenerator;
    private final TenantBusinessDataSourceExecutor dataSourceExecutor;
    private final TenantBusinessDataSourceResolver dataSourceResolver;

    @Override
    @TenantBusinessDataSource
    public BusinessDatasourceDemoResult current() {
        demoMapper.createTable();
        return buildResult(null, demoMapper.selectRecent(resolveTenantId(), DEFAULT_LIMIT));
    }

    @Override
    public BusinessDatasourceDemoResult currentForTenant(Long tenantId) {
        return dataSourceExecutor.execute(tenantId, () -> {
            demoMapper.createTable();
            return buildResult(null, demoMapper.selectRecent(resolveTenantId(), DEFAULT_LIMIT));
        });
    }

    @Override
    @TenantBusinessDataSource
    public BusinessDatasourceDemoResult prepare(String title) {
        return doPrepare(title);
    }

    @Override
    public BusinessDatasourceDemoResult prepareForTenant(Long tenantId, String title) {
        return dataSourceExecutor.execute(tenantId, () -> doPrepare(title));
    }

    @Override
    @TenantBusinessDataSource
    public BusinessDatasourceDemoResult list(Integer limit) {
        demoMapper.createTable();
        return buildResult(null, demoMapper.selectRecent(resolveTenantId(), normalizeLimit(limit)));
    }

    @Override
    public BusinessDatasourceDemoResult listForTenant(Long tenantId, Integer limit) {
        return dataSourceExecutor.execute(tenantId, () -> {
            demoMapper.createTable();
            return buildResult(null, demoMapper.selectRecent(resolveTenantId(), normalizeLimit(limit)));
        });
    }

    private BusinessDatasourceDemoResult doPrepare(String title) {
        demoMapper.createTable();
        BusinessDatasourceDemoRecord record = new BusinessDatasourceDemoRecord();
        record.setId(identifierGenerator.nextId(record).longValue());
        record.setTenantId(resolveTenantId());
        record.setTitle(title);
        record.setRouteKey(DynamicDataSourceContextHolder.peek());
        record.setCreateBy(SessionHelper.getUserId());
        record.setCreateDept(SessionHelper.getMainOrgId());
        record.setUpdateBy(record.getCreateBy());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(record.getCreateTime());
        demoMapper.insertRecord(record);
        return buildResult(record, demoMapper.selectRecent(record.getTenantId(), DEFAULT_LIMIT));
    }

    private BusinessDatasourceDemoResult buildResult(BusinessDatasourceDemoRecord record,
                                                     List<BusinessDatasourceDemoRecord> records) {
        TenantBusinessDataSourceInfo datasource = dataSourceResolver.resolve();
        BusinessDatasourceDemoResult result = new BusinessDatasourceDemoResult();
        result.setDatasource(datasource);
        result.setCurrentDsKey(DynamicDataSourceContextHolder.peek());
        result.setDatabaseName(demoMapper.selectDatabaseName());
        result.setRecord(record);
        result.setRecords(records);
        return result;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        tenantId = SessionHelper.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }
}
