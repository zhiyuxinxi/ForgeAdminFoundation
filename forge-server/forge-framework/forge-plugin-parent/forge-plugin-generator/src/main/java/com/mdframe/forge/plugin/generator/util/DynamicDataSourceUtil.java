package com.mdframe.forge.plugin.generator.util;

import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.support.GeneratorJdbcDataSources;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Compatibility facade for existing static callers in the Forge Spring application.
 * New consumers inject GeneratorJdbcDataSources; this facade never owns or caches pools.
 */
public class DynamicDataSourceUtil {

    public static Connection getConnection(GenDatasource datasource) throws SQLException {
        return owner().getConnection(datasource);
    }

    public static DataSource getDataSource(GenDatasource datasource) {
        return owner().getDataSource(datasource);
    }

    public static boolean testConnection(GenDatasource datasource) {
        return owner().testConnection(datasource);
    }

    public static void removeDataSource(Long datasourceId) {
        owner().removeDataSource(datasourceId);
    }

    public static void clearAll() {
        owner().clearAll();
    }

    public static ResultSet executeQuery(GenDatasource datasource, String sql) throws SQLException {
        return owner().executeQuery(datasource, sql);
    }

    private static GeneratorJdbcDataSources owner() {
        return SpringUtil.getBean(GeneratorJdbcDataSources.class);
    }
}
