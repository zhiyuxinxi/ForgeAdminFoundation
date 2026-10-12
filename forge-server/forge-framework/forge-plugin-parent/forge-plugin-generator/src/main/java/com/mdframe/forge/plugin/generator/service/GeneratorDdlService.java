package com.mdframe.forge.plugin.generator.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/** Restricted legacy-generator DDL; never falls back to the application's master connection. */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratorDdlService {
    public static final String PERMISSION = "ai:lowcode:deploy-ddl";
    public static final String DATASOURCE = "generatorDdl";
    private final DataSource dataSource;

    public void createTable(String sql) {
        StpUtil.checkPermission(PERMISSION);
        var validated = GeneratorCreateTableValidator.validate(sql);
        DataSource ddlDataSource = dataSource instanceof DynamicRoutingDataSource routing
                ? routing.getDataSources().get(DATASOURCE) : null;
        if (ddlDataSource == null) {
            throw new BusinessException(UiMessages.label(
                    "请先配置 generatorDdl 专用最小权限数据源，禁止使用主数据源执行生成器DDL",
                    "Configure the dedicated least-privilege generatorDdl data source before running generator DDL"));
        }
        Object operator = StpUtil.getLoginId();
        try (Connection connection = ddlDataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            statement.execute(validated.sql());
            log.info("Generator DDL succeeded: operator={}, table={}, datasource={}", operator, validated.tableName(), DATASOURCE);
        } catch (Exception exception) {
            log.error("Generator DDL failed: operator={}, table={}, datasource={}", operator, validated.tableName(), DATASOURCE, exception);
            throw new BusinessException(UiMessages.label("建表失败，请查看操作日志", "Table creation failed; check the operation log"));
        }
    }
}
