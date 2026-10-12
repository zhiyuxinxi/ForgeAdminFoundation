package com.mdframe.forge.starter.datascope.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Refreshes permission metadata only after the transaction that changed it commits. */
public final class DataScopeCacheRefresh {
    private DataScopeCacheRefresh() { }

    public static void afterCommit(IDataScopeService service) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            service.refreshDataScopeCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                service.refreshDataScopeCache();
            }
        });
    }
}
