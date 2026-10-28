package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

class LocalFlowStartLocksTest {
    @Test
    void callerBeforeTryLockKeepsTheSameRegistrationAlive() throws Exception {
        CountDownLatch beforeTryLock = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        LocalFlowStartLocks locks = new LocalFlowStartLocks(() -> {
            created.incrementAndGet();
            return new ReentrantLock() {
                private final AtomicInteger attempts = new AtomicInteger();

                @Override
                public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
                    if (attempts.incrementAndGet() == 2) {
                        beforeTryLock.countDown();
                        if (!resume.await(2, TimeUnit.SECONDS)) {
                            throw new AssertionError("paused caller was not resumed");
                        }
                    }
                    return super.tryLock(timeout, unit);
                }
            };
        });
        var executor = Executors.newSingleThreadExecutor();
        var first = locks.tryAcquire("same", 1, TimeUnit.SECONDS);
        LocalFlowStartLocks.Handle third = null;
        try {
            var second = executor.submit(() -> {
                var handle = locks.tryAcquire("same", 50, TimeUnit.MILLISECONDS);
                if (handle != null) handle.close();
                return handle != null;
            });
            assertTrue(beforeTryLock.await(2, TimeUnit.SECONDS));
            first.close();
            third = locks.tryAcquire("same", 1, TimeUnit.SECONDS);
            assertNotNull(third);
            resume.countDown();
            assertFalse(second.get(2, TimeUnit.SECONDS), "same key must not acquire a different lock");
            assertEquals(1, created.get());
        } finally {
            resume.countDown();
            first.close();
            if (third != null) third.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
        try (var next = locks.tryAcquire("same", 1, TimeUnit.SECONDS)) {
            assertNotNull(next);
            assertEquals(2, created.get(), "idle registrations must be released after timeout/close");
        }
    }

    @Test
    void interruptedAcquisitionDoesNotLeakARegistration() throws Exception {
        AtomicInteger created = new AtomicInteger();
        LocalFlowStartLocks locks = new LocalFlowStartLocks(() -> {
            created.incrementAndGet();
            return new ReentrantLock();
        });
        var first = locks.tryAcquire("same", 1, TimeUnit.SECONDS);
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedException.class,
                    () -> locks.tryAcquire("same", 1, TimeUnit.SECONDS));
        } finally {
            Thread.interrupted();
            first.close();
        }
        try (var next = locks.tryAcquire("same", 1, TimeUnit.SECONDS)) {
            assertNotNull(next);
            assertEquals(2, created.get());
        }
    }

    @Test
    void nestedHandlesAndRepeatedCloseKeepOuterOwnership() throws Exception {
        LocalFlowStartLocks locks = new LocalFlowStartLocks();
        var outer = locks.tryAcquire("same", 1, TimeUnit.SECONDS);
        var inner = locks.tryAcquire("same", 1, TimeUnit.SECONDS);
        var executor = Executors.newSingleThreadExecutor();
        try {
            inner.close();
            inner.close();
            assertFalse(executor.submit(() -> {
                try (var handle = locks.tryAcquire("same", 50, TimeUnit.MILLISECONDS)) {
                    return handle != null;
                }
            }).get(2, TimeUnit.SECONDS));
            assertTrue(executor.submit(() -> {
                try (var other = locks.tryAcquire("other", 1, TimeUnit.SECONDS)) {
                    return other != null;
                }
            }).get(2, TimeUnit.SECONDS));
        } finally {
            inner.close();
            outer.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
