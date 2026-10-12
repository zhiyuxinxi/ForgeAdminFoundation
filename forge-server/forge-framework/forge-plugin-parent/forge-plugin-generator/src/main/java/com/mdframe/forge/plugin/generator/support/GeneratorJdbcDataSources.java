package com.mdframe.forge.plugin.generator.support;

import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.data.support.JdbcConnectionPools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 生成器数据源定义到公共 JDBC 池的适配器；凭据由调用边界解密。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratorJdbcDataSources {

    /**
     * 统一的连接资源所有者
     */
    private final JdbcConnectionPools pools;
    private static final JdbcConnectionPools.Sizing SIZING = new JdbcConnectionPools.Sizing(5, 1, 600_000);

    /**
     * 获取数据源连接
     */
    public Connection getConnection(GenDatasource datasource) throws SQLException {
        return getDataSource(datasource).getConnection();
    }

    /**
     * 获取数据源对象。
     */
    public DataSource getDataSource(GenDatasource datasource) {
        JdbcConnectionPools.Key key = new JdbcConnectionPools.Key(
                JdbcConnectionPools.Scope.GENERATOR, null, datasource.getDatasourceId());
        JdbcConnectionPools.Configuration configuration = new JdbcConnectionPools.Configuration(
                datasource.getDriverClassName(), datasource.getUrl(), datasource.getUsername(),
                datasource.getPassword(), resolveTestQuery(datasource), SIZING);
        return pools.get(key, configuration);
    }

    /**
     * 测试数据源连接
     */
    public boolean testConnection(GenDatasource datasource) {
        try (Connection conn = getConnection(datasource);
             Statement stmt = conn.createStatement()) {
            stmt.execute(resolveTestQuery(datasource));
            log.info("数据源连接测试成功: {}", datasource.getDatasourceName());
            return true;
        } catch (Exception e) {
            log.warn("数据源连接测试失败，datasourceId={}, exceptionType={}",
                    datasource.getDatasourceId(), e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 移除数据源（关闭连接池）
     */
    public void removeDataSource(Long datasourceId) {
        pools.invalidate(JdbcConnectionPools.Scope.GENERATOR, datasourceId);
    }

    public void clearAll() {
        pools.clear(JdbcConnectionPools.Scope.GENERATOR);
    }

    /**
     * 返回已物化的结果；本端关闭 JDBC 资源，调用者只需关闭返回的结果集。
     */
    public ResultSet executeQuery(GenDatasource datasource, String sql) throws SQLException {
        try (Connection connection = getConnection(datasource);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            CachedRowSet detached = RowSetProvider.newFactory().createCachedRowSet();
            detached.populate(result);
            return detached;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String resolveTestQuery(GenDatasource datasource) {
        if (datasource != null && isNotBlank(datasource.getTestQuery())) {
            return datasource.getTestQuery();
        }
        if (datasource != null && "oracle".equalsIgnoreCase(datasource.getDbType())) {
            return "SELECT 1 FROM DUAL";
        }
        return "SELECT 1";
    }
}
