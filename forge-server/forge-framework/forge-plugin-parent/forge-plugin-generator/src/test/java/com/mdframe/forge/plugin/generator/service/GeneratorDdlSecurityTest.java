package com.mdframe.forge.plugin.generator.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratorDdlSecurityTest {
    private static final String COLUMNS = "id BIGINT NOT NULL PRIMARY KEY, tenant_id BIGINT NOT NULL DEFAULT 1, "
            + "create_by BIGINT, create_time DATETIME, create_dept BIGINT, update_by BIGINT, update_time DATETIME";
    private static final String VALID = "CREATE TABLE biz_audit_example (" + COLUMNS + ", title VARCHAR(128) COMMENT '备注') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    @Test
    void parsesCommentsWhitespaceAndReconstructsAcceptedSql() {
        var validated = GeneratorCreateTableValidator.validate("/* generator */\n" + VALID + "; -- done\n");
        assertEquals("biz_audit_example", validated.tableName());
        assertFalse(validated.sql().contains("/*"));
        assertTrue(validated.sql().contains("`tenant_id` BIGINT NOT NULL DEFAULT 1"));
        var indexed = GeneratorCreateTableValidator.validate("CREATE TABLE `biz_indexed` ("
                + COLUMNS.replace(" PRIMARY KEY", "") + ", PRIMARY KEY (`id`), KEY idx_tenant (tenant_id))");
        assertTrue(indexed.sql().contains("KEY `idx_tenant` (`tenant_id`)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE biz_copy AS SELECT * FROM sys_user",
            "CREATE TABLE biz_copy LIKE sys_user",
            "CREATE TABLE other.biz_copy (%s)", "CREATE TABLE sys_user_copy (%s)",
            "CREATE TABLE biz_first (%s); DROP TABLE sys_user",
            "CREATE TEMPORARY TABLE biz_copy (%s)", "CREATE TABLE biz_copy (id BIGINT PRIMARY KEY)",
            "CREATE TABLE biz_copy (%s, secret VARCHAR(20) DEFAULT (current_user()))",
            "CREATE TABLE biz_copy (%s) ENGINE=FEDERATED CONNECTION='mysql://anything'",
            "CREATE TABLE biz_copy (%s, FOREIGN KEY (tenant_id) REFERENCES sys_user(id))",
            "CREATE TABLE biz_copy (%s, computed BIGINT GENERATED ALWAYS AS (1))",
            "CREATE TABLE biz_copy (%s) DATA DIRECTORY='/tmp/example'"
    })
    void rejectsUnrestrictedDdlBeforeOpeningAnyConnection(String template) {
        String sql = template.contains("%s") ? template.formatted(COLUMNS) : template;
        DataSource source = mock(DataSource.class);
        try (var permission = mockStatic(StpUtil.class)) {
            assertThrows(BusinessException.class, () -> new GeneratorDdlService(source).createTable(sql));
            permission.verify(() -> StpUtil.checkPermission(GeneratorDdlService.PERMISSION));
            verifyNoInteractions(source);
        }
    }

    @Test
    void serviceEnforcesPermissionEvenWithoutApiResourceConfiguration() {
        DataSource source = mock(DataSource.class);
        try (var permission = mockStatic(StpUtil.class)) {
            permission.when(() -> StpUtil.checkPermission(GeneratorDdlService.PERMISSION))
                    .thenThrow(new IllegalStateException("denied"));
            assertThrows(IllegalStateException.class, () -> new GeneratorDdlService(source).createTable(VALID));
            verifyNoInteractions(source);
        }
    }

    @Test
    void missingDedicatedDataSourceCannotFallBackToMaster() {
        DynamicRoutingDataSource routing = mock(DynamicRoutingDataSource.class);
        DataSource master = mock(DataSource.class);
        when(routing.getDataSources()).thenReturn(Map.of("master", master));
        try (var permission = mockStatic(StpUtil.class)) {
            assertThrows(BusinessException.class, () -> new GeneratorDdlService(routing).createTable(VALID));
            verifyNoInteractions(master);
        }
    }

    @Test
    void executesOnlyValidatedSqlOnDedicatedConnectionAndClosesResources() throws Exception {
        DynamicRoutingDataSource routing = mock(DynamicRoutingDataSource.class);
        DataSource dedicated = mock(DataSource.class);
        DataSource master = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(routing.getDataSources()).thenReturn(Map.of("master", master, GeneratorDdlService.DATASOURCE, dedicated));
        when(dedicated.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        try (var permission = mockStatic(StpUtil.class)) {
            new GeneratorDdlService(routing).createTable(VALID);
            verify(statement).execute(GeneratorCreateTableValidator.validate(VALID).sql());
            verify(statement).setQueryTimeout(10);
            verify(statement).close();
            verify(connection).close();
            verifyNoInteractions(master);
            when(statement.execute(anyString())).thenThrow(new SQLException("database detail"));
            BusinessException failure = assertThrows(BusinessException.class,
                    () -> new GeneratorDdlService(routing).createTable(VALID));
            assertFalse(failure.getMessage().contains("database detail"));
        }
    }

    @Test
    void validationMessageUsesRequestLocale() {
        try {
            LocaleContextHolder.setLocale(Locale.US);
            assertTrue(assertThrows(BusinessException.class,
                    () -> GeneratorCreateTableValidator.validate("select 1")).getMessage().startsWith("Only one"));
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertTrue(assertThrows(BusinessException.class,
                    () -> GeneratorCreateTableValidator.validate("select 1")).getMessage().startsWith("仅允许"));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
