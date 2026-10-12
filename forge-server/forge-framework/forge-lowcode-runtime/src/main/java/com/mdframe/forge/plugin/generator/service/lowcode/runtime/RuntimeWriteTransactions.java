package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Binds a runtime write unit to the same DataSource used by its JDBC statements. */
@Component
@RequiredArgsConstructor
public class RuntimeWriteTransactions {
    private final RuntimeJdbcTemplateProvider jdbcTemplates;

    /** This is a single-database transaction, not an atomic transaction across different sources. */
    public <T> T execute(LowcodeRuntimeDataSourceContext context, Supplier<T> operation) {
        TransactionTemplate transaction = new TransactionTemplate(
                new DataSourceTransactionManager(jdbcTemplates.resolveDataSource(context)));
        return transaction.execute(status -> operation.get());
    }
}
