package com.mdframe.forge.starter.tenant.datasource;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 租户业务数据源作用域执行器。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TenantBusinessDataSourceResolver.class)
public class TenantBusinessDataSourceExecutor {

    private final TenantBusinessDataSourceResolver dataSourceResolver;

    public void runWithTenantDefault(Runnable action) {
        executeWithTenantDefault(() -> {
            action.run();
            return null;
        });
    }

    public <T> T executeWithTenantDefault(Supplier<T> action) {
        TenantBusinessDataSourceInfo info = dataSourceResolver.resolve();
        try (TenantBusinessDataSourceScope ignored = dataSourceResolver.use(info)) {
            return action.get();
        }
    }

    public void run(Long tenantId, Runnable action) {
        execute(tenantId, () -> {
            action.run();
            return null;
        });
    }

    public <T> T execute(Long tenantId, Supplier<T> action) {
        return TenantContextHolder.executeWithTenant(tenantId, () -> {
            TenantBusinessDataSourceInfo info = dataSourceResolver.resolve(tenantId);
            try (TenantBusinessDataSourceScope ignored = dataSourceResolver.use(info)) {
                return action.get();
            }
        });
    }
}
