package com.mdframe.forge.business.core.datasource;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TenantBusinessDataSourceTaskDecorator")
class TenantBusinessDataSourceTaskDecoratorTest {

    private final TenantBusinessDataSourceTaskDecorator taskDecorator = new TenantBusinessDataSourceTaskDecorator();

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
    }

    @Test
    @DisplayName("propagates captured tenant and datasource then restores worker context")
    void propagatesCapturedContextAndRestoresWorkerContext() {
        TenantContextHolder.setTenantId(100L);
        DynamicDataSourceContextHolder.push("tenant_business_a");
        Runnable decorated = taskDecorator.decorate(() -> {
            assertEquals(100L, TenantContextHolder.getTenantId());
            assertEquals("tenant_business_a", DynamicDataSourceContextHolder.peek());
        });

        TenantContextHolder.setTenantId(999L);
        DynamicDataSourceContextHolder.push("worker_old");
        decorated.run();

        assertEquals(999L, TenantContextHolder.getTenantId());
        assertEquals("worker_old", DynamicDataSourceContextHolder.peek());
    }

    @Test
    @DisplayName("clears stale worker datasource when submitter has no datasource context")
    void clearsStaleWorkerDatasourceWhenSubmitterHasNoDatasourceContext() {
        Runnable decorated = taskDecorator.decorate(() -> {
            assertNull(TenantContextHolder.getTenantId());
            assertNull(DynamicDataSourceContextHolder.peek());
        });

        TenantContextHolder.setTenantId(999L);
        DynamicDataSourceContextHolder.push("worker_old");
        decorated.run();

        assertEquals(999L, TenantContextHolder.getTenantId());
        assertEquals("worker_old", DynamicDataSourceContextHolder.peek());
    }

    @Test
    @DisplayName("propagates ignore tenant flag then restores worker flag")
    void propagatesIgnoreTenantFlagAndRestoresWorkerFlag() {
        TenantContextHolder.setIgnore(true);
        Runnable decorated = taskDecorator.decorate(() -> assertTrue(TenantContextHolder.isIgnore()));

        TenantContextHolder.clear();
        decorated.run();

        assertFalse(TenantContextHolder.isIgnore());
    }
}
