package com.mdframe.forge.plugin.data.support;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes already validated dataset SQL with the same JDBC limits for preview and runtime. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatasetJdbcQueryRunner {
    private final JdbcDataSourceProvider dataSourceProvider;
    private final SqlParameterBinder parameterBinder;

    public record Result(List<String> columns, List<Map<String, Object>> rows, long total) { }

    public Result page(DataConnection source, String sql, Map<String, Object> params,
                       DbDialect dialect, int pageSize, long offset, Integer timeout) {
        try (Connection connection = dataSourceProvider.getConnection(source)) {
            connection.setReadOnly(true);
            String wrapped = "SELECT * FROM (" + sql + ") dataset_page";
            long total = count(connection, sql, params, timeout);
            String pageSql = dialect.buildLimitSql(wrapped, pageSize) + " OFFSET " + offset;
            Result result = query(connection, pageSql, params, pageSize, timeout);
            return new Result(result.columns(), result.rows(), total);
        } catch (SQLException exception) {
            throw queryFailure(source, exception);
        }
    }

    public Result preview(DataConnection source, String sql, Map<String, Object> params,
                          DbDialect dialect, Integer requestedRows, Integer timeout) {
        int limit = requestedRows == null || requestedRows <= 0 ? 1000 : Math.min(requestedRows, 10000);
        try (Connection connection = dataSourceProvider.getConnection(source)) {
            connection.setReadOnly(true);
            return query(connection, dialect.buildLimitSql("SELECT * FROM (" + sql + ") dataset_preview", limit),
                    params, limit, timeout);
        } catch (SQLException exception) {
            throw queryFailure(source, exception);
        }
    }

    private long count(Connection connection, String sql, Map<String, Object> params, Integer timeout)
            throws SQLException {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") dataset_count";
        try (PreparedStatement statement = prepare(connection, countSql, params, timeout);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Dataset count returned no row");
            }
            return result.getLong(1);
        }
    }

    private Result query(Connection connection, String sql, Map<String, Object> params,
                         int limit, Integer timeout) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, params, timeout)) {
            statement.setMaxRows(limit);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    columns.add(metadata.getColumnLabel(index));
                }
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 0; index < columns.size(); index++) {
                        row.put(columns.get(index), result.getObject(index + 1));
                    }
                    rows.add(row);
                }
                return new Result(columns, rows, rows.size());
            }
        }
    }

    private PreparedStatement prepare(Connection connection, String sql, Map<String, Object> params,
                                      Integer timeout) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(parameterBinder.convertToPreparedStatement(sql));
        try {
            statement.setQueryTimeout(timeout == null || timeout <= 0 ? 30 : Math.min(timeout, 120));
            for (Map.Entry<Integer, Object> entry : parameterBinder.buildParamIndexMap(sql,
                    params == null ? Map.of() : params).entrySet()) {
                statement.setObject(entry.getKey(), entry.getValue());
            }
            return statement;
        } catch (SQLException | RuntimeException exception) {
            statement.close();
            throw exception;
        }
    }

    private BusinessException queryFailure(DataConnection source, SQLException exception) {
        log.warn("Dataset JDBC query failed, connectionId={}, errorType={}",
                source.getId(), exception.getClass().getSimpleName());
        return new BusinessException("数据集查询失败，请检查连接、参数或查询超时配置");
    }
}
