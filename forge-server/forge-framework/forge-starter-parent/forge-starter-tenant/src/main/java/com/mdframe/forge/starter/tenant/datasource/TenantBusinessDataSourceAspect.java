package com.mdframe.forge.starter.tenant.datasource;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 在事务开启前切换到当前租户默认业务数据源。
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TenantBusinessDataSourceResolver.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantBusinessDataSourceAspect {

    private final TenantBusinessDataSourceResolver dataSourceResolver;

    @Around("@within(com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSource) "
            + "|| @annotation(com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSource)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        TenantBusinessDataSourceInfo info = dataSourceResolver.resolve();
        validateTransactionBoundary(info);
        try (TenantBusinessDataSourceScope ignored = dataSourceResolver.use(info)) {
            return joinPoint.proceed();
        }
    }

    private void validateTransactionBoundary(TenantBusinessDataSourceInfo info) {
        if (info == null || !info.shouldRoute() || !TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        String currentDsKey = DynamicDataSourceContextHolder.peek();
        if (!info.getDsKey().equals(currentDsKey)) {
            throw new BusinessException("不能在已开启的其他数据源事务中切换到租户业务数据源");
        }
    }
}
