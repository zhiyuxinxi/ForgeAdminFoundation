package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentProcessDispatcherTest {
    private final ExecutorService executor = mock(ExecutorService.class);
    private final DocumentProcessDispatcher dispatcher = new DocumentProcessDispatcher(executor, 0);

    @AfterEach
    void clearContext() {
        TransactionSynchronizationManager.clear();
        TenantContextHolder.clear();
        LocaleContextHolder.resetLocaleContext();
        DynamicDataSourceContextHolder.clear();
    }

    @Test
    void dispatchesOnlyAfterCommitAndRestoresWorkerContext() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TenantContextHolder.setTenantId(7L);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        AtomicInteger calls = new AtomicInteger();
        dispatcher.dispatch(() -> {
            assertEquals(7L, TenantContextHolder.getTenantId());
            assertEquals(Locale.ENGLISH, LocaleContextHolder.getLocale());
            calls.incrementAndGet();
        }, () -> fail("unexpected rejection"));
        verifyNoInteractions(executor);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
        ArgumentCaptor<Runnable> work = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(work.capture());
        TenantContextHolder.setTenantId(9L);
        LocaleContextHolder.setLocale(Locale.CHINESE);
        work.getValue().run();
        assertEquals(1, calls.get());
        assertEquals(9L, TenantContextHolder.getTenantId());
        assertEquals(Locale.CHINESE, LocaleContextHolder.getLocale());
    }

    @Test
    void rollbackDoesNotDispatchInvisibleDocument() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        dispatcher.dispatch(() -> fail("rolled back document processed"), () -> fail("rollback is not rejection"));
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(executor);
    }

    @Test
    void confirmationOutsideTransactionDispatchesAndShutdownReleasesOwnedExecutor() throws Exception {
        when(executor.awaitTermination(0, TimeUnit.SECONDS)).thenReturn(true);
        dispatcher.dispatch(() -> { }, () -> fail("unexpected rejection"));
        verify(executor).execute(any(Runnable.class));
        dispatcher.shutdown();
        verify(executor).shutdown();
        verify(executor, never()).shutdownNow();
    }

    @Test
    void boundedQueueRejectsOverflowAndShutdownDiscardsWithOriginalContext() throws Exception {
        DocumentProcessDispatcher bounded = new DocumentProcessDispatcher(1, 1, 0);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger rejected = new AtomicInteger();
        try {
            bounded.dispatch(() -> {
                running.countDown();
                try {
                    release.await();
                } catch (InterruptedException expected) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
            }, () -> fail("first task rejected"));
            assertTrue(running.await(5, TimeUnit.SECONDS));
            TenantContextHolder.setTenantId(7L);
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            DynamicDataSourceContextHolder.push("tenant-seven");
            bounded.dispatch(() -> fail("queued task must not run after forced shutdown"), () -> {
                assertEquals(7L, TenantContextHolder.getTenantId());
                assertEquals(Locale.ENGLISH, LocaleContextHolder.getLocale());
                assertEquals("tenant-seven", DynamicDataSourceContextHolder.peek());
                rejected.incrementAndGet();
            });
            bounded.dispatch(() -> fail("overflow must not run on caller thread"), rejected::incrementAndGet);
            assertEquals(1, rejected.get());
            TenantContextHolder.setTenantId(9L);
            LocaleContextHolder.setLocale(Locale.CHINESE);
            DynamicDataSourceContextHolder.clear();
            bounded.shutdown();
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            assertEquals(2, rejected.get());
            assertEquals(9L, TenantContextHolder.getTenantId());
            assertEquals(Locale.CHINESE, LocaleContextHolder.getLocale());
            assertNull(DynamicDataSourceContextHolder.peek());
        } finally {
            release.countDown();
            bounded.shutdown();
        }
    }

    @Test
    void gracefulShutdownWaitsForAcceptedWork() throws Exception {
        DocumentProcessDispatcher bounded = new DocumentProcessDispatcher(1, 1, 5);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        bounded.dispatch(() -> {
            running.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
                completed.incrementAndGet();
            } catch (InterruptedException unexpected) {
                Thread.currentThread().interrupt();
                fail("graceful task interrupted");
            }
        }, () -> fail("unexpected rejection"));
        assertTrue(running.await(5, TimeUnit.SECONDS));
        release.countDown();
        bounded.shutdown();
        assertEquals(1, completed.get());
    }

    @Test
    void ignoresCloseEventsFromOtherContexts() throws Exception {
        try (GenericApplicationContext context = new GenericApplicationContext();
             GenericApplicationContext other = new GenericApplicationContext()) {
            dispatcher.setApplicationContext(context);
            dispatcher.contextClosed(new ContextClosedEvent(other));
            verifyNoInteractions(executor);
            when(executor.awaitTermination(0, TimeUnit.SECONDS)).thenReturn(true);
            dispatcher.contextClosed(new ContextClosedEvent(context));
            verify(executor).shutdown();
        }
    }

    @Test
    void actualContextCloseRejectsQueuedWorkBeforeDestroyingDependencies() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger observedAtDestruction = new AtomicInteger(-1);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            DocumentProcessDispatcher bounded = new DocumentProcessDispatcher(1, 1, 0);
            context.registerBean("documentProcessDispatcher", DocumentProcessDispatcher.class, () -> bounded);
            context.refresh();
            context.getDefaultListableBeanFactory().registerDisposableBean("dependency", () -> observedAtDestruction.set(rejected.get()));
            bounded.dispatch(() -> {
                running.countDown();
                try {
                    release.await();
                } catch (InterruptedException expected) {
                    Thread.currentThread().interrupt();
                }
            }, () -> fail("first task rejected"));
            assertTrue(running.await(5, TimeUnit.SECONDS));
            bounded.dispatch(() -> fail("queued work ran during forced shutdown"), rejected::incrementAndGet);
        } finally {
            release.countDown();
        }
        assertEquals(1, rejected.get());
        assertEquals(1, observedAtDestruction.get());
    }
}
