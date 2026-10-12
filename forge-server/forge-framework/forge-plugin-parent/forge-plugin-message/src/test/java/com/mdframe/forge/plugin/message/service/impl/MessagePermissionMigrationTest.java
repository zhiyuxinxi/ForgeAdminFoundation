package com.mdframe.forge.plugin.message.service.impl;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagePermissionMigrationTest {
    @Test
    void migrationRegistersActualManagementEndpointsIdempotentlyWithoutGrantingOrdinaryRoles() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL")) {
            schema(connection);
            String sql = Files.readString(migration()).lines().filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            for (int run = 0; run < 2; run++) {
                for (String statement : sql.split(";")) {
                    if (!statement.isBlank()) {
                        execute(connection, statement);
                    }
                }
            }
            assertEquals(6, count(connection, "SELECT COUNT(*) FROM sys_resource WHERE resource_type=4"));
            assertEquals(6, count(connection, "SELECT COUNT(*) FROM sys_role_resource"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM sys_role_resource WHERE role_id=2"));
            assertEquals(0, count(connection, "SELECT COUNT(*) FROM sys_role_resource rr JOIN sys_role r ON rr.role_id=r.id WHERE rr.tenant_id != r.tenant_id"));
            assertEquals(6, count(connection, "SELECT COUNT(*) FROM sys_resource WHERE resource_type=4 AND resource_name_en IS NOT NULL"));
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM sys_resource WHERE api_method='POST' AND api_url='/api/message/manage/page' AND perms='message:manage:query'"));
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM sys_resource WHERE api_method='GET' AND api_url='/api/message/manage/*/detail' AND perms='message:manage:query'"));
            assertEquals(2, count(connection, "SELECT COUNT(*) FROM sys_resource WHERE api_method='POST' AND api_url='/api/message/send' AND perms='message:manage:send'"));
        }
    }

    private void schema(Connection connection) throws Exception {
        execute(connection, "CREATE TABLE sys_resource (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT, resource_name VARCHAR, resource_name_en VARCHAR, parent_id BIGINT, resource_type INT, sort INT, is_external INT, open_target VARCHAR, is_public INT, menu_status INT, visible INT, perms VARCHAR, api_method VARCHAR, api_url VARCHAR, keep_alive INT, always_show INT, create_by BIGINT, create_time TIMESTAMP, update_by BIGINT, update_time TIMESTAMP, create_dept BIGINT, client_code VARCHAR, path VARCHAR, del_flag BIGINT DEFAULT 0)");
        execute(connection, "CREATE TABLE sys_role (id BIGINT, tenant_id BIGINT, role_key VARCHAR, del_flag BIGINT)");
        execute(connection, "CREATE TABLE sys_role_resource (tenant_id BIGINT, role_id BIGINT, resource_id BIGINT, create_time TIMESTAMP)");
        execute(connection, "INSERT INTO sys_resource (tenant_id,resource_type,path,client_code) VALUES (20,2,'/message/manage','pc'),(21,2,'/message/manage','pc')");
        execute(connection, "INSERT INTO sys_role VALUES (1,20,'admin',0),(2,20,'ordinary',0),(3,21,'admin',0)");
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int count(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private Path migration() {
        Path root = Path.of("").toAbsolutePath();
        while (root != null) {
            Path candidate = root.resolve("forge-server/db/migration/V1.0.20261018__secure_message_management_resources.sql");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            root = root.getParent();
        }
        throw new IllegalStateException("Message permission migration is missing");
    }
}
