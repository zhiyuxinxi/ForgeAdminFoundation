package com.mdframe.forge.plugin.data.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JdbcConnectionPoolsTest {
    private static final JdbcConnectionPools.Key KEY = new JdbcConnectionPools.Key(
            JdbcConnectionPools.Scope.DATA_CONNECTION, 7L, 9L);
    private static final JdbcConnectionPools.Sizing SIZING = new JdbcConnectionPools.Sizing(10, 2, 300_000);

    @Test
    void concurrentRequestsCreateOnePoolAndShutdownIsTerminal() throws Exception {
        AtomicInteger created = new AtomicInteger();
        ExecutorService workers = Executors.newFixedThreadPool(8);
        JdbcConnectionPools pools = new JdbcConnectionPools(config -> {
            created.incrementAndGet();
            return new TrackingPool();
        });
        try {
            List<Future<HikariDataSource>> results = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                results.add(workers.submit(() -> pools.get(KEY, configuration("one", "secret"))));
            }
            TrackingPool pool = (TrackingPool) results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<HikariDataSource> result : results) {
                assertSame(pool, result.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, created.get());
            pools.close();
            pools.close();
            assertEquals(1, pool.closes.get());
            assertThrows(IllegalStateException.class, () -> pools.get(KEY, configuration("one", "secret")));
        } finally {
            workers.shutdownNow();
            pools.close();
        }
    }

    @Test
    void changesToConnectionAndPoolConfigurationCloseEachPreviousPool() {
        List<HikariConfig> observed = new ArrayList<>();
        try (JdbcConnectionPools pools = new JdbcConnectionPools(config -> {
            observed.add(config);
            return new TrackingPool();
        })) {
            JdbcConnectionPools.Configuration initial = configuration("one", "secret");
            List<JdbcConnectionPools.Configuration> changes = List.of(
                    configuration("two", "secret"), configuration("two", "new-secret"),
                    new JdbcConnectionPools.Configuration("org.h2.Driver", "jdbc:h2:mem:two", "other",
                            "new-secret", "SELECT 1", SIZING),
                    new JdbcConnectionPools.Configuration("org.h2.Driver", "jdbc:h2:mem:two", "other",
                            "new-secret", "SELECT 2", SIZING),
                    new JdbcConnectionPools.Configuration("org.h2.Driver", "jdbc:h2:mem:two", "other",
                            "new-secret", "SELECT 2", new JdbcConnectionPools.Sizing(5, 1, 600_000)));
            TrackingPool previous = (TrackingPool) pools.get(KEY, initial);
            for (JdbcConnectionPools.Configuration change : changes) {
                TrackingPool replacement = (TrackingPool) pools.get(KEY, change);
                assertNotSame(previous, replacement);
                assertEquals(1, previous.closes.get());
                assertSame(replacement, pools.get(KEY, change));
                previous = replacement;
            }
            assertEquals(6, observed.size());
            assertEquals(5, observed.get(5).getMaximumPoolSize());
            assertEquals("new-secret", observed.get(5).getPassword());
            assertFalse(changes.get(1).toString().contains("new-secret"));
            assertFalse(changes.get(1).toString().contains("jdbc:h2"));
        }
    }

    @Test
    void failedReplacementCannotReturnOldCredentialsAndCanBeRetried() {
        AtomicInteger attempts = new AtomicInteger();
        try (JdbcConnectionPools pools = new JdbcConnectionPools(config -> {
            if (attempts.incrementAndGet() == 2) {
                throw new IllegalStateException("simulated pool creation failure");
            }
            return new TrackingPool();
        })) {
            TrackingPool old = (TrackingPool) pools.get(KEY, configuration("one", "old"));
            assertThrows(IllegalStateException.class, () -> pools.get(KEY, configuration("one", "new")));
            assertEquals(1, old.closes.get());
            HikariDataSource retried = pools.get(KEY, configuration("one", "new"));
            assertNotSame(old, retried);
            assertEquals(3, attempts.get());
        }
    }

    @Test
    void invalidationWaitsForInFlightCreationAndDoesNotLeaveAnOrphan() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch finishCreation = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try (JdbcConnectionPools pools = new JdbcConnectionPools(config -> {
            creating.countDown();
            await(finishCreation);
            return new TrackingPool();
        })) {
            Future<HikariDataSource> get = workers.submit(() -> pools.get(KEY, configuration("one", "secret")));
            assertTrue(creating.await(5, TimeUnit.SECONDS));
            Future<?> invalidation = workers.submit(() -> pools.invalidate(KEY.scope(), KEY.resourceId()));
            finishCreation.countDown();
            TrackingPool pool = (TrackingPool) get.get(5, TimeUnit.SECONDS);
            invalidation.get(5, TimeUnit.SECONDS);
            assertEquals(1, pool.closes.get());
            assertNotSame(pool, pools.get(KEY, configuration("one", "secret")));
        } finally {
            finishCreation.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void tenantAndConsumerIdentityAreIsolatedWhileInvalidationRespectsScope() {
        try (JdbcConnectionPools pools = new JdbcConnectionPools(config -> new TrackingPool())) {
            HikariDataSource tenantOne = pools.get(KEY, configuration("one", "secret"));
            HikariDataSource tenantTwo = pools.get(new JdbcConnectionPools.Key(KEY.scope(), 8L, 9L),
                    configuration("one", "secret"));
            HikariDataSource generator = pools.get(new JdbcConnectionPools.Key(
                    JdbcConnectionPools.Scope.GENERATOR, null, 9L), configuration("one", "secret"));
            assertNotSame(tenantOne, tenantTwo);
            assertNotSame(tenantOne, generator);
            pools.clear(JdbcConnectionPools.Scope.DATA_CONNECTION);
            assertTrue(tenantOne.isClosed());
            assertTrue(tenantTwo.isClosed());
            assertFalse(generator.isClosed());
            assertThrows(NullPointerException.class, () -> new JdbcConnectionPools.Key(KEY.scope(), null, 9L));
        }
    }

    private static JdbcConnectionPools.Configuration configuration(String database, String password) {
        return new JdbcConnectionPools.Configuration("org.h2.Driver", "jdbc:h2:mem:" + database,
                "sa", password, "SELECT 1", SIZING);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test creation barrier");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Creation barrier interrupted", exception);
        }
    }

    private static final class TrackingPool extends HikariDataSource {
        private final AtomicInteger closes = new AtomicInteger();

        @Override
        public void close() {
            closes.incrementAndGet();
            super.close();
        }
    }
}
