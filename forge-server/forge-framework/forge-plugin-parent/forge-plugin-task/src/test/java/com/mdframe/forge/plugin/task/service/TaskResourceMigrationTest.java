package com.mdframe.forge.plugin.task.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TaskResourceMigrationTest {
    @Test
    void registersVisibleBilingualActionsAndRoutesWithoutGrantingManagementToOrdinaryRoles() throws Exception {
        JdbcDataSource datasource = new JdbcDataSource();
        datasource.setURL("jdbc:h2:mem:task_resources_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE sys_tenant(id BIGINT PRIMARY KEY,del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,role_key VARCHAR(50),del_flag BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_resource(tenant_id BIGINT,role_id BIGINT,resource_id BIGINT,create_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE sys_resource(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,"
                + "resource_name VARCHAR(200),resource_name_en VARCHAR(200),parent_id BIGINT,resource_type INT,sort INT,"
                + "is_external INT,open_target VARCHAR(30),is_public INT,menu_status INT,visible INT,perms VARCHAR(100),"
                + "api_method VARCHAR(20),api_url VARCHAR(200),keep_alive INT,always_show INT,create_by BIGINT,"
                + "create_time TIMESTAMP,update_by BIGINT,update_time TIMESTAMP,create_dept BIGINT,"
                + "client_code VARCHAR(20),del_flag BIGINT DEFAULT 0)");
        jdbc.update("INSERT INTO sys_tenant VALUES(7,0),(8,0),(9,1)");
        jdbc.update("INSERT INTO sys_role VALUES(70,7,'admin',0),(71,7,'executor',0),(80,8,'admin',0),(81,8,'executor',0)");
        String sql = Files.readString(Path.of("../../../db/migration/V1.0.2026101603__public_task_authorization_resources.sql"));
        execute(jdbc, sql);
        execute(jdbc, sql);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_resource WHERE tenant_id=7", Integer.class)).isEqualTo(23);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_resource WHERE tenant_id=9", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_resource WHERE role_id IN (71,81)", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_resource WHERE role_id=70", Integer.class)).isEqualTo(23);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_resource rr JOIN sys_resource r ON rr.resource_id=r.id "
                + "WHERE rr.tenant_id != r.tenant_id", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_resource WHERE visible != 1 OR resource_name_en IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_resource WHERE tenant_id=7 AND resource_type=3 "
                + "AND perms IN ('task:definition:write','task:schedule:write')", Integer.class)).isEqualTo(2);
        // Existing role assignment can grant execution routes without either management permission.
        jdbc.update("INSERT INTO sys_role_resource SELECT 7,71,id,NOW() FROM sys_resource "
                + "WHERE tenant_id=7 AND perms='task:instance:use'");
        assertThat(jdbc.queryForList("SELECT DISTINCT r.perms FROM sys_resource r JOIN sys_role_resource rr "
                + "ON rr.resource_id=r.id WHERE rr.role_id=71", String.class)).containsExactly("task:instance:use");
        assertThat(jdbc.queryForList("SELECT r.api_method || ' ' || r.api_url FROM sys_resource r JOIN sys_role_resource rr "
                + "ON rr.resource_id=r.id WHERE rr.role_id=71", String.class))
                .contains("GET /api/task/assigned", "POST /api/task/*/complete", "GET /ai/business-tasks/*/form")
                .doesNotContain("POST /api/task/schedules", "POST /api/task/definition");
    }

    private void execute(JdbcTemplate jdbc, String sql) {
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement);
            }
        }
    }
}
