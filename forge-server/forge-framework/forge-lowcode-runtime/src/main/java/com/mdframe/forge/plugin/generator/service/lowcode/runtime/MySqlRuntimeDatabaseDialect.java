package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * MySQL 运行时方言。
 */
@Component
public class MySqlRuntimeDatabaseDialect implements RuntimeDatabaseDialect {

    @Override
    public String dbType() {
        return "MySQL";
    }

    @Override
    public boolean supports(String dbType) {
        return StringUtils.equalsAnyIgnoreCase(dbType, "mysql", "mariadb");
    }

    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String tableExistsSql() {
        return """
            SELECT COUNT(1)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    }

    @Override
    public String listColumnsSql() {
        return """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String listColumnMetadataSql() {
        return """
            SELECT column_name, column_type, is_nullable, column_default, extra, column_comment, generation_expression
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String listIndexesSql() {
        return """
            SELECT DISTINCT index_name
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    }

    @Override
    public String primaryKeyMetadataSql() {
        return """
            SELECT column_name, data_type, extra
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_key = 'PRI'
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String paginate(String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }
}
