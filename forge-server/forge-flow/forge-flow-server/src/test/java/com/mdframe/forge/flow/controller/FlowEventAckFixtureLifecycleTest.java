package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.SaManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlowEventAckFixtureLifecycleTest {
    @Test
    void fixtureUsesOnAccessExpiryAndRestoresGlobalComponentsWithoutLeakingRefreshThreads() throws Exception {
        int period = SaManager.getConfig().getDataRefreshPeriod();
        FlowEventAckFixture fixture;
        try (var value = new FlowEventAckFixture()) {
            fixture = value;
            assertNull(value.dao.refreshThread);
            value.use(value.token("501", 97));
        }
        assertSame(fixture.previousDao, SaManager.getSaTokenDao());
        assertSame(fixture.previousLogic, cn.dev33.satoken.stp.StpUtil.getStpLogic());
        assertEquals(period, SaManager.getConfig().getDataRefreshPeriod());
    }
}
