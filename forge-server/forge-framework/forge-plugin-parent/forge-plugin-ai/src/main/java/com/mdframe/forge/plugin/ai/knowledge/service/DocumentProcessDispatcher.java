package com.mdframe.forge.plugin.ai.knowledge.service;

import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import jakarta.annotation.PreDestroy;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Dispatches document work only after its database transaction commits. */
@Component
public class DocumentProcessDispatcher {
    private final ExecutorService executor;

    public DocumentProcessDispatcher() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), task -> {
            Thread thread = new Thread(task, "doc-process");
            thread.setDaemon(true);
            return thread;
        }));
    }

    DocumentProcessDispatcher(ExecutorService executor) {
        this.executor = executor;
    }

    public void dispatch(Runnable task) {
        Runnable contextualTask = new TenantBusinessDataSourceTaskDecorator().decorate(task);
        LocaleContext locale = LocaleContextHolder.getLocaleContext();
        Runnable work = () -> {
            LocaleContext previous = LocaleContextHolder.getLocaleContext();
            try {
                LocaleContextHolder.setLocaleContext(locale);
                contextualTask.run();
            } finally {
                LocaleContextHolder.setLocaleContext(previous);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(work);
                }
            });
        } else {
            executor.execute(work);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
