package com.mdframe.forge.plugin.data.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Owns the configured JDBC pools, including replacement and application shutdown. */
@Component
public class JdbcConnectionPools implements AutoCloseable {

    public enum Scope { DATA_CONNECTION, GENERATOR }

    /** Generator definitions are global; data connections belong to a tenant. */
    public record Key(Scope scope, Long tenantId, Long resourceId) {
        public Key {
            Objects.requireNonNull(scope, "Pool scope is required");
            Objects.requireNonNull(resourceId, "Pool resource ID is required");
            if (scope == Scope.DATA_CONNECTION) {
                Objects.requireNonNull(tenantId, "Data connection tenant is required");
            } else if (tenantId != null) {
                throw new IllegalArgumentException("Generator pools use global resource identity");
            }
        }
    }

    /** The two consumers retain their existing capacity and idle policies. */
    public record Sizing(int maximum, int minimumIdle, long idleTimeout) { }

    /** Credentials are already decrypted by the owning consumer; never log this snapshot. */
    public record Configuration(String driver, String url, String username, String password,
                                String testQuery, Sizing sizing) {
        public Configuration {
            Objects.requireNonNull(sizing, "Pool sizing is required");
        }

        public HikariConfig toHikariConfig() {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setConnectionTestQuery(testQuery);
            config.setMaximumPoolSize(sizing.maximum());
            config.setMinimumIdle(sizing.minimumIdle());
            config.setIdleTimeout(sizing.idleTimeout());
            config.setConnectionTimeout(30_000);
            config.setMaxLifetime(1_800_000);
            return config;
        }

        @Override
        public String toString() {
            return "JdbcPoolConfiguration[connection details redacted]";
        }
    }

    private record Entry(Configuration configuration, HikariDataSource pool) { }

    private final Map<Key, Entry> entries = new HashMap<>();
    private final Function<HikariConfig, HikariDataSource> factory;
    private boolean closed;

    public JdbcConnectionPools() {
        this(HikariDataSource::new);
    }

    JdbcConnectionPools(Function<HikariConfig, HikariDataSource> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    // The same monitor covers creation and disposal: clear/close cannot orphan a late pool.
    public synchronized HikariDataSource get(Key key, Configuration configuration) {
        Objects.requireNonNull(key, "Pool key is required");
        Objects.requireNonNull(configuration, "Pool configuration is required");
        if (closed) {
            throw new IllegalStateException("JDBC pool owner is closed");
        }
        Entry current = entries.get(key);
        if (current != null && !current.pool().isClosed() && current.configuration().equals(configuration)) {
            return current.pool();
        }
        // A failed replacement must not serve the old URL or credentials.
        if (current != null) {
            entries.remove(key);
            current.pool().close();
        }
        HikariConfig config = configuration.toHikariConfig();
        config.setPoolName("forge-" + key.scope() + "-" + key.tenantId() + "-" + key.resourceId());
        HikariDataSource pool = factory.apply(config);
        entries.put(key, new Entry(configuration, pool));
        return pool;
    }

    /** Resource IDs are database IDs; invalidate all tenant partitions of that definition. */
    public synchronized void invalidate(Scope scope, Long resourceId) {
        removeMatching(scope, Objects.requireNonNull(resourceId));
    }

    public synchronized void clear(Scope scope) {
        removeMatching(Objects.requireNonNull(scope), null);
    }

    private void removeMatching(Scope scope, Long resourceId) {
        Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Entry> entry = iterator.next();
            if (entry.getKey().scope() == scope
                    && (resourceId == null || resourceId.equals(entry.getKey().resourceId()))) {
                entry.getValue().pool().close();
                iterator.remove();
            }
        }
    }

    @Override
    @PreDestroy
    public synchronized void close() {
        closed = true;
        entries.values().forEach(entry -> entry.pool().close());
        entries.clear();
    }
}
