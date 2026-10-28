package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Dispatches document work only after its database transaction commits. */
@Component
@Slf4j
public class DocumentProcessDispatcher implements ApplicationContextAware {
    private final ExecutorService executor;
    private final int shutdownWaitSeconds;
    private ApplicationContext applicationContext;

    @Autowired
    public DocumentProcessDispatcher(
            @Value("${forge.ai.knowledge.processing.workers:4}") int workers,
            @Value("${forge.ai.knowledge.processing.queue-capacity:64}") int queueCapacity,
            @Value("${forge.ai.knowledge.processing.shutdown-wait-seconds:30}") int shutdownWaitSeconds) {
        this(new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), task -> {
            Thread thread = new Thread(task, "doc-process");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy()), shutdownWaitSeconds);
    }

    DocumentProcessDispatcher(ExecutorService executor, int shutdownWaitSeconds) {
        if (shutdownWaitSeconds < 0) {
            throw new IllegalArgumentException("shutdown-wait-seconds must not be negative");
        }
        this.executor = executor;
        this.shutdownWaitSeconds = shutdownWaitSeconds;
    }

    public void dispatch(Runnable task, Runnable onRejected) {
        QueuedWork work = new QueuedWork(withContext(task), withContext(onRejected));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(work);
                }
            });
        } else {
            submit(work);
        }
    }

    private void submit(QueuedWork work) {
        try {
            executor.execute(work);
        } catch (RejectedExecutionException rejected) {
            // The upload may already be committed. Its owner must persist a retryable outcome.
            work.rejected().run();
        }
    }

    private Runnable withContext(Runnable task) {
        Runnable contextualTask = new TenantBusinessDataSourceTaskDecorator().decorate(task);
        LocaleContext locale = LocaleContextHolder.getLocaleContext();
        return () -> {
            LocaleContext previous = LocaleContextHolder.getLocaleContext();
            try {
                LocaleContextHolder.setLocaleContext(locale);
                contextualTask.run();
            } finally {
                LocaleContextHolder.setLocaleContext(previous);
            }
        };
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void contextClosed(ContextClosedEvent event) {
        if (event.getApplicationContext() == applicationContext) {
            // Drain and record discarded work before datasource/storage beans are destroyed.
            shutdown();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        boolean interrupted = false;
        try {
            if (executor.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS)) {
                return;
            }
        } catch (InterruptedException exception) {
            interrupted = true;
        }
        // Only tasks that never started are rejected. Interrupted running work is not called successful.
        for (Runnable task : executor.shutdownNow()) {
            try {
                ((QueuedWork) task).rejected().run();
            } catch (RuntimeException exception) {
                log.error("Unable to record document processing rejection during shutdown", exception);
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private record QueuedWork(Runnable task, Runnable rejected) implements Runnable {
        @Override
        public void run() {
            task.run();
        }
    }
}
