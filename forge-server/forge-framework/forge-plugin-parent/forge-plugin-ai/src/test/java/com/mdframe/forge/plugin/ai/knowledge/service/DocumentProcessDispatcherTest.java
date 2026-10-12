package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DocumentProcessDispatcherTest {
    private final ExecutorService executor = mock(ExecutorService.class);
    private final DocumentProcessDispatcher dispatcher = new DocumentProcessDispatcher(executor);

    @AfterEach
    void clearContext() {
        TransactionSynchronizationManager.clear();
        TenantContextHolder.clear();
        LocaleContextHolder.resetLocaleContext();
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
        });
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
        dispatcher.dispatch(() -> { throw new AssertionError("rolled back document processed"); });
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(executor);
    }

    @Test
    void confirmationOutsideTransactionDispatchesAndShutdownReleasesOwnedExecutor() {
        dispatcher.dispatch(() -> { });
        verify(executor).execute(any(Runnable.class));
        dispatcher.shutdown();
        verify(executor).shutdown();
    }
}
