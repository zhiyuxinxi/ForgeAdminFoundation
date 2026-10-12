package com.mdframe.forge.starter.tenant.handler;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.tenant.config.TenantProperties;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 租户表结构检测器。只缓存成功探测的结果，无法确认表结构时拒绝放行。
 * 启动扫描在单例初始化后执行；失败后允许首次访问重新探测具体表。
 *
 * @author forge
 */
@Slf4j
@RequiredArgsConstructor
public class TenantTableChecker implements SmartInitializingSingleton {

    private final DataSource dataSource;
    private final TenantProperties tenantProperties;

    private volatile ConcurrentMap<TableKey, Boolean> tableCache = new ConcurrentHashMap<>();
    private volatile boolean initialized;

    /** 同一动态数据源入口下，实际数据库、用户及命名空间也必须匹配。 */
    private record DatabaseKey(DataSource source, String url, String user, String catalog,
                               String schema, String tenantColumn) {
    }

    private record TableKey(DatabaseKey database, String table) {
    }

    @Override
    public void afterSingletonsInstantiated() {
        init();
    }

    /** 完整扫描成功后一次发布，失败不发布部分负缓存。 */
    public synchronized void init() {
        if (initialized) {
            return;
        }
        if (!tenantProperties.getAutoDetectTenantColumn()) {
            initialized = true;
            return;
        }
        TenantContextHolder.executeIgnore(() -> {
            Connection connection = null;
            try {
                connection = DataSourceUtils.getConnection(dataSource);
                DatabaseMetaData metadata = connection.getMetaData();
                DatabaseKey database = databaseKey(metadata, connection);
                ConcurrentMap<TableKey, Boolean> detected = new ConcurrentHashMap<>();
                try (ResultSet tables = metadata.getTables(database.catalog(), database.schema(),
                        "%", new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String table = tables.getString("TABLE_NAME");
                        detected.put(new TableKey(database, table),
                                checkTableHasColumn(metadata, database, table));
                    }
                }
                tableCache = detected;
                initialized = true;
                log.info("租户表结构扫描完成，共 {} 张表", detected.size());
            } catch (Exception exception) {
                log.error("扫描数据库表结构失败，后续访问必须重新确认表结构", exception);
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        });
    }

    private DatabaseKey databaseKey(DatabaseMetaData metadata, Connection connection) throws SQLException {
        String product = metadata.getDatabaseProductName().toUpperCase(Locale.ROOT);
        // MySQL/MariaDB 使用 catalog，其余数据库按当前连接 schema 限定。
        String schema = product.contains("MYSQL") || product.contains("MARIADB")
                ? null : connection.getSchema();
        return new DatabaseKey(dataSource, metadata.getURL(), metadata.getUserName(),
                connection.getCatalog(), schema, tenantProperties.getColumn());
    }

    private boolean checkTableHasColumn(DatabaseMetaData metadata, DatabaseKey database,
                                        String table) throws SQLException {
        boolean foundColumn = false;
        // JDBC 此参数是模式；转义下划线/百分号，避免探测到相似表而缓存错误结论。
        String escape = metadata.getSearchStringEscape();
        String pattern = table;
        if (escape != null && !escape.isEmpty()) {
            pattern = table.replace(escape, escape + escape)
                    .replace("_", escape + "_").replace("%", escape + "%");
        }
        try (ResultSet columns = metadata.getColumns(database.catalog(), database.schema(), pattern, null)) {
            while (columns.next()) {
                String column = columns.getString("COLUMN_NAME");
                if (column != null) {
                    foundColumn = true;
                    if (column.equalsIgnoreCase(database.tenantColumn())) {
                        return true;
                    }
                }
            }
        }
        if (!foundColumn) {
            // 无任何可见字段不等于已确认无租户列（可能是表名、权限或元数据异常）。
            throw new SQLException("No visible columns for tenant table detection: " + table);
        }
        return false;
    }

    /**
     * @return true 已确认有租户列；false 已确认无租户列
     * @throws IllegalStateException 无法确认表结构，调用者不得按全局表放行
     */
    public boolean hasTenantColumn(String tableName) {
        if (!tenantProperties.getAutoDetectTenantColumn()) {
            return true;
        }
        return TenantContextHolder.executeIgnore(() -> {
            Connection connection = null;
            try {
                // 复用事务绑定连接；先确定实际数据库身份，再读取缓存。
                connection = DataSourceUtils.getConnection(dataSource);
                DatabaseMetaData metadata = connection.getMetaData();
                DatabaseKey database = databaseKey(metadata, connection);
                TableKey key = new TableKey(database, tableName);
                ConcurrentMap<TableKey, Boolean> cache = tableCache;
                Boolean cached = cache.get(key);
                if (cached != null) {
                    return cached;
                }
                boolean hasColumn = checkTableHasColumn(metadata, database, tableName);
                cache.put(key, hasColumn);
                return hasColumn;
            } catch (Exception exception) {
                throw new IllegalStateException(UiMessages.label(
                        "无法确认表的租户字段，已拒绝访问", "Cannot verify the tenant column; access denied"), exception);
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        });
    }

    /** 刷新期间及失败后不复用旧负缓存；新扫描只有完整成功才发布。 */
    public synchronized void refresh() {
        tableCache = new ConcurrentHashMap<>();
        initialized = false;
        init();
    }

    public Set<String> getTablesWithTenantColumn() {
        return tablesByResult(true);
    }

    public Set<String> getTablesWithoutTenantColumn() {
        return tablesByResult(false);
    }

    private Set<String> tablesByResult(boolean hasColumn) {
        return tableCache.entrySet().stream()
                .filter(entry -> entry.getValue() == hasColumn)
                .map(entry -> entry.getKey().table())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isInitialized() {
        return initialized;
    }
}
