package com.mdframe.forge.plugin.generator.service.lowcode.runtime;


import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 低代码运行时 JDBC 模板提供器。
 */
@Component
@RequiredArgsConstructor
public class RuntimeJdbcTemplateProvider {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final RuntimeDataSourceAccess datasourceAccess;

    public JdbcTemplate jdbcTemplate(LowcodeRuntimeDataSourceContext context) {
        if (context == null || context.isMaster()) {
            return jdbcTemplate;
        }
        return new JdbcTemplate(resolveDataSource(context));
    }

    public NamedParameterJdbcTemplate namedJdbcTemplate(LowcodeRuntimeDataSourceContext context) {
        if (context == null || context.isMaster()) {
            return namedParameterJdbcTemplate;
        }
        return new NamedParameterJdbcTemplate(resolveDataSource(context));
    }

    public DataSource resolveDataSource(LowcodeRuntimeDataSourceContext context) {
        if (context == null || context.isMaster()) {
            return jdbcTemplate.getDataSource();
        }
        return datasourceAccess.dataSource(context.getDatasourceId());
    }
}
