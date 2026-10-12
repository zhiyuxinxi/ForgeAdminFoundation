package com.mdframe.forge.starter.tenant.handler;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantTableCheckerTest {

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void startupFailurePublishesNoPartialNegativeCacheAndFirstAccessRetries() throws Exception {
        FaultDataSource source = source("create table a_global (id int)",
                "create table b_tenant (id int, tenant_id bigint)");
        TenantTableChecker checker = checker(source);
        source.failTable = "b";
        TenantContextHolder.setIgnore(true);
        checker.afterSingletonsInstantiated();

        assertFalse(checker.isInitialized());
        assertTrue(checker.getTablesWithoutTenantColumn().isEmpty());
        assertEquals(Boolean.TRUE, TenantContextHolder.getIgnoreValue());
        TenantContextHolder.clearIgnore();
        assertThrows(IllegalStateException.class, () -> checker.hasTenantColumn("b_tenant"));
        assertTrue(checker.getTablesWithoutTenantColumn().isEmpty());
        assertNull(TenantContextHolder.getIgnoreValue());

        source.failTable = null;
        assertTrue(checker.hasTenantColumn("b_tenant"));
        assertFalse(checker.hasTenantColumn("a_global"));
    }

    @Test
    void firstAccessFailureIsClosedEvenInRelaxedModeAndNeverCached() throws Exception {
        FaultDataSource source = source("create table orders (id int, tenant_id bigint)");
        TenantProperties properties = new TenantProperties();
        properties.setStrictMode(false);
        TenantTableChecker checker = new TenantTableChecker(source, properties);
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(properties);
        handler.setTenantTableChecker(checker);
        source.failTable = "orders";
        assertThrows(IllegalStateException.class, () -> handler.ignoreTable("orders"));
        assertTrue(checker.getTablesWithoutTenantColumn().isEmpty());
        source.failTable = null;
        TenantContextHolder.setTenantId(7L);
        assertFalse(handler.ignoreTable("orders"));
    }

    @Test
    void schemaFailureAndInvisibleTableCannotBecomeGlobalTables() throws Exception {
        FaultDataSource source = source("create table bizxorders (id int)");
        TenantTableChecker checker = checker(source);
        source.failSchema = true;
        assertThrows(IllegalStateException.class, () -> checker.hasTenantColumn("bizxorders"));
        source.failSchema = false;
        // '_' must not match the different table bizxorders in JDBC metadata patterns.
        assertThrows(IllegalStateException.class, () -> checker.hasTenantColumn("biz_orders"));
        assertTrue(checker.getTablesWithoutTenantColumn().isEmpty());
        assertFalse(checker.hasTenantColumn("bizxorders"));
    }

    @Test
    void sameTableAcrossDatabasesAndSchemasDoesNotReuseNegativeCache() throws Exception {
        FaultDataSource source = source("create table orders (id int)");
        DataSource globalDatabase = source.delegate;
        TenantTableChecker checker = checker(source);
        checker.init();
        assertFalse(checker.hasTenantColumn("orders"));

        source.delegate = source("create table orders (id int, tenant_id bigint)",
                "create schema alternate", "create table alternate.orders (id int)").delegate;
        assertTrue(checker.hasTenantColumn("orders"));
        source.schema = "alternate";
        assertFalse(checker.hasTenantColumn("orders"));
        source.schema = null;
        assertTrue(checker.hasTenantColumn("orders"));
        source.delegate = globalDatabase;
        assertFalse(checker.hasTenantColumn("orders"));
    }

    @Test
    void failedRefreshDiscardsOldNegativeResultAndCannotPublishPartialResults() throws Exception {
        FaultDataSource source = source("create table a_global (id int)",
                "create table b_tenant (id int, tenant_id bigint)");
        TenantTableChecker checker = checker(source);
        checker.init();
        assertFalse(checker.hasTenantColumn("a_global"));
        execute(source.delegate, "alter table a_global add tenant_id bigint");
        source.failTable = "b";
        checker.refresh();
        assertFalse(checker.isInitialized());
        assertTrue(checker.getTablesWithoutTenantColumn().isEmpty());
        assertThrows(IllegalStateException.class, () -> checker.hasTenantColumn("b_tenant"));
        assertTrue(checker.hasTenantColumn("a_global"));
        source.failTable = null;
        checker.refresh();
        assertTrue(checker.isInitialized());
        assertTrue(checker.hasTenantColumn("b_tenant"));
    }

    @Test
    void realInterceptorSqlPreservesReadWriteIsolationAndGlobalTableAccess() throws Exception {
        FaultDataSource source = source("create table orders (id int, tenant_id bigint, amount int)",
                "insert into orders values (1, 7, 10), (2, 8, 20)",
                "create table public_lookup (id int)", "insert into public_lookup values (1), (2)");
        TenantProperties properties = new TenantProperties();
        TenantTableChecker checker = new TenantTableChecker(source, properties);
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(properties);
        handler.setTenantTableChecker(checker);
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor(handler);
        checker.init();
        assertTrue(handler.ignoreTable("public_lookup"));
        assertThrows(IllegalStateException.class, () -> handler.ignoreTable("orders"));
        TenantContextHolder.setTenantId(7L);
        try (Connection connection = source.getConnection()) {
            // Model the normal Spring transaction consumer and verify it is not closed by metadata reads.
            TransactionSynchronizationManager.bindResource(source, new ConnectionHolder(connection));
            try {
                assertEquals(1, count(connection, interceptor.parserSingle("select * from orders", null)));
                assertEquals(2, count(connection, interceptor.parserSingle("select * from public_lookup", null)));
                try (var statement = connection.createStatement()) {
                    assertEquals(1, statement.executeUpdate(interceptor.parserSingle(
                            "update orders set amount = 99", null)));
                    assertEquals(1, statement.executeUpdate(interceptor.parserSingle(
                            "insert into orders (id, amount) values (3, 30)", null)));
                }
                assertEquals(1, count(connection, "select * from orders where tenant_id = 8 and amount = 20"));
                assertEquals(1, count(connection, "select * from orders where id = 3 and tenant_id = 7"));
                assertFalse(connection.isClosed());
            } finally {
                TransactionSynchronizationManager.unbindResource(source);
            }
        }
    }

    private static TenantTableChecker checker(DataSource source) {
        return new TenantTableChecker(source, new TenantProperties());
    }

    private static FaultDataSource source(String... statements) throws SQLException {
        JdbcDataSource database = new JdbcDataSource();
        database.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        execute(database, statements);
        return new FaultDataSource(database);
    }

    private static void execute(DataSource source, String... statements) throws SQLException {
        try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private static int count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            int count = 0;
            while (rows.next()) {
                count++;
            }
            return count;
        }
    }

    /** Real JDBC with only the nominated metadata operation faulted. */
    private static class FaultDataSource extends AbstractDataSource {
        private DataSource delegate;
        private String schema;
        private String failTable;
        private boolean failSchema;

        FaultDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection actual = delegate.getConnection();
            if (schema != null) {
                actual.setSchema(schema);
            }
            DatabaseMetaData metadata = actual.getMetaData();
            DatabaseMetaData faulted = (DatabaseMetaData) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{DatabaseMetaData.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getColumns") && failTable != null
                                && ((String) args[2]).startsWith(failTable)) {
                            throw new SQLException("Injected column metadata failure");
                        }
                        try {
                            return method.invoke(metadata, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getMetaData")) {
                            return faulted;
                        }
                        if (method.getName().equals("getSchema") && failSchema) {
                            throw new SQLException("Injected schema metadata failure");
                        }
                        if (method.getName().equals("equals")) {
                            return proxy == args[0];
                        }
                        try {
                            return method.invoke(actual, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }
    }
}
