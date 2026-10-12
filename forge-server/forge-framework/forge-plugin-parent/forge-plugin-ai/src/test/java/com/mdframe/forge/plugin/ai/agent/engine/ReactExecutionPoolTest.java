package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ReactExecutionPoolTest {
    @Test
    void cancellationRetainsSlotUntilUncooperativeWorkActuallyExits() throws Exception {
        ReactExecutionPool pool = new ReactExecutionPool(1, 5000);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ended = new CountDownLatch(1);
        ReactContext context = context("one");
        try {
            pool.start(context, () -> { }, () -> {
                started.countDown();
                boolean waiting = true;
                while (waiting) {
                    try {
                        release.await();
                        waiting = false;
                    } catch (InterruptedException cancelled) {
                        // Deliberately emulate a tool that ignores interruption; capacity must stay held.
                        assertTrue(context.isCancelled());
                    }
                }
            }, ended::countDown);
            assertTrue(started.await(2, TimeUnit.SECONDS));
            pool.cancel(context);
            assertThrows(BusinessException.class, () -> pool.start(context("one"), () -> { }, () -> { }, () -> { }));
            assertThrows(BusinessException.class, () -> pool.start(context("two"), () -> { }, () -> { }, () -> { }));
            release.countDown();
            assertTrue(ended.await(2, TimeUnit.SECONDS));
            assertNull(pool.activeContext("one"));
        } finally {
            release.countDown();
            pool.close();
        }
    }

    @Test
    void configuredLimitsReachSpringConsumerAndRejectAfterShutdownBeforeCallback() {
        try (var application = new AnnotationConfigApplicationContext()) {
            application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("limits", Map.of(
                    "forge.ai.engine.max-concurrent", "1", "forge.ai.engine.timeout-millis", "500")));
            application.register(ReactExecutionPool.class);
            application.refresh();
            var pool = application.getBean(ReactExecutionPool.class);
            pool.close();
            assertThrows(BusinessException.class, () -> pool.start(context("closed"),
                    () -> fail("Shutdown rejection must not persist or consume approval"), () -> { }, () -> { }));
        }
    }

    private ReactContext context(String session) {
        ReactContext context = new ReactContext();
        context.setSessionId(session);
        return context;
    }
}
