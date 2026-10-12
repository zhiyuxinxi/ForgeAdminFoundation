package com.mdframe.forge.admin.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ApiResourceUniqueIdentityMigrationTest {

    private static final String MIGRATION =
            "V1.0.20261011__align_api_resource_unique_identity.sql";

    @Test
    void shouldUpgradeCoarseKeysAndReuseCompatibleKeysWithoutChangingAuthorizationRows() throws Exception {
        String jdbcUrl = System.getenv("FORGE_MIGRATION_TEST_JDBC_URL");
        assumeTrue(jdbcUrl != null && !jdbcUrl.isBlank(),
                "Real MySQL verification requires an isolated forge_api_identity_test_* database");
        String user = System.getenv("FORGE_MIGRATION_TEST_USER");
        assertNotNull(user, "FORGE_MIGRATION_TEST_USER is required");
        String password = System.getenv("FORGE_MIGRATION_TEST_PASSWORD");
        String migration = Files.readString(resolveRepositoryFile("forge-server/db/migration/" + MIGRATION));
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password == null ? "" : password)) {
            String database = scalar(connection, "SELECT DATABASE()");
            assertTrue(database != null && database.matches("forge_api_identity_test_[A-Za-z0-9_]+"),
                    "Refuse a non-test database");
            assertEquals("0", scalar(connection,
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()"),
                    "The test database must be empty before fixture creation");

            // 0: historical initialization dump; 1: V1.0.133; 2: already compatible Harness migration.
            for (int baseline = 0; baseline < 3; baseline++) {
                try {
                    createFixture(connection, baseline);
                    if (baseline < 2) {
                        assertDuplicate(connection, 31, 4, "fixture:read", "POST", "/fixture/page", "pc", 0);
                    }
                    List<List<String>> originalRows = rows(connection,
                            "SELECT id,tenant_id,resource_type,perms,api_method,api_url,client_code,del_flag,resource_name_en "
                                    + "FROM sys_resource ORDER BY id");
                    List<List<String>> originalGrants = rows(connection, "SELECT * FROM sys_role_resource ORDER BY id");
                    String originalDdl = createTableSql(connection);

                    applyMigration(connection, migration);
                    assertEquals(originalRows, rows(connection,
                            "SELECT id,tenant_id,resource_type,perms,api_method,api_url,client_code,del_flag,resource_name_en "
                                    + "FROM sys_resource ORDER BY id"));
                    assertEquals(originalGrants, rows(connection, "SELECT * FROM sys_role_resource ORDER BY id"));
                    if (baseline == 2) {
                        assertEquals(originalDdl, createTableSql(connection), "Compatible existing definition must be reused");
                    }
                    String migratedDdl = createTableSql(connection);
                    applyMigration(connection, migration);
                    assertEquals(migratedDdl, createTableSql(connection), "Repeated migration must not rebuild the target definition");

                    insert(connection, 31, 4, "fixture:read", "POST", "/fixture/page", "pc", 0);
                    insert(connection, 32, 4, "fixture:read", "GET", "/fixture/detail", "pc", 0);
                    assertDuplicate(connection, 33, 4, "fixture:read", "get", "/fixture/page", "pc", 0);
                    assertDuplicate(connection, 34, 4, "fixture:read", "POST", "/fixture/page", "pc", 0);
                    assertDuplicate(connection, 11, 2, "fixture:list", null, null, "pc", 0);
                    assertDuplicate(connection, 21, 3, "fixture:read", null, null, "pc", 0);
                    insert(connection, 12, 2, "fixture:list", null, null, "h5", 0);
                    insert(connection, 35, 4, "fixture:read", "GET", "/fixture/page", "h5", 0);

                    execute(connection, "UPDATE sys_resource SET del_flag=id WHERE id=30");
                    insert(connection, 36, 4, "fixture:read", "GET", "/fixture/page", "pc", 0);
                    execute(connection, "UPDATE sys_resource SET del_flag=id WHERE id=36");
                    insert(connection, 37, 4, "fixture:read", "GET", "/fixture/page", "pc", 0);
                    insert(connection, 50, 4, null, null, "/fixture/public", "pc", 0);
                    assertDuplicate(connection, 51, 4, "", "", "/fixture/public", "pc", 0);
                    assertEquals("3", scalar(connection,
                            "SELECT COUNT(*) FROM sys_resource WHERE id IN (30,36,40) AND del_flag=id"));
                    assertEquals(originalGrants, rows(connection, "SELECT * FROM sys_role_resource ORDER BY id"),
                            "Adding endpoint records must not grant them to an existing role");
                } finally {
                    execute(connection, "DROP TABLE IF EXISTS sys_role_resource");
                    execute(connection, "DROP TABLE IF EXISTS sys_resource");
                }
            }
        }
    }

    private static void createFixture(Connection connection, int baseline) throws SQLException {
        String derivedColumn = baseline == 2
                ? ",resource_unique_key VARCHAR(400) GENERATED ALWAYS AS (CASE WHEN resource_type = 4 THEN "
                    + "CONCAT(COALESCE(perms,''),'|',UPPER(COALESCE(api_method,'')),'|',COALESCE(api_url,'')) "
                    + "ELSE perms END) STORED COMMENT '资源有效唯一键：API按权限+方法+路径，其余按权限'"
                : "";
        String indexColumns = baseline == 0 ? "tenant_id,resource_type,perms,del_flag"
                : baseline == 1 ? "tenant_id,resource_type,perms,client_code,del_flag"
                : "tenant_id,resource_type,resource_unique_key,client_code,del_flag";
        execute(connection, "CREATE TABLE sys_resource (id BIGINT PRIMARY KEY,tenant_id BIGINT NOT NULL DEFAULT 1,"
                + "resource_type TINYINT NOT NULL,perms VARCHAR(100),api_method VARCHAR(10),api_url VARCHAR(255),"
                + "client_code VARCHAR(50) DEFAULT 'pc',del_flag BIGINT NOT NULL DEFAULT 0,resource_name_en VARCHAR(100)"
                + derivedColumn + ",UNIQUE KEY uk_tenant_resource_active (" + indexColumns + ")) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
        execute(connection, "CREATE TABLE sys_role_resource (id BIGINT PRIMARY KEY,role_id BIGINT,resource_id BIGINT)");
        insert(connection, 10, 2, "fixture:list", null, null, "pc", 0);
        insert(connection, 20, 3, "fixture:read", null, null, "pc", 0);
        insert(connection, 30, 4, "fixture:read", "GET", "/fixture/page", "pc", 0);
        insert(connection, 40, 4, "fixture:read", "GET", "/fixture/page", "pc", 40);
        execute(connection, "INSERT INTO sys_role_resource (id,role_id,resource_id) VALUES (1,2,30)");
    }

    private static void insert(Connection connection, long id, int type, String permission,
                               String method, String url, String client, long deleted) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sys_resource (id,tenant_id,resource_type,perms,api_method,api_url,client_code,del_flag,resource_name_en) "
                        + "VALUES (?,1,?,?,?,?,?,?,?)")) {
            statement.setLong(1, id);
            statement.setInt(2, type);
            statement.setString(3, permission);
            statement.setString(4, method);
            statement.setString(5, url);
            statement.setString(6, client);
            statement.setLong(7, deleted);
            statement.setString(8, "Owner's fixture");
            statement.executeUpdate();
        }
    }

    private static void assertDuplicate(Connection connection, long id, int type, String permission,
                                        String method, String url, String client, long deleted) {
        SQLException rejected = assertThrows(SQLException.class,
                () -> insert(connection, id, type, permission, method, url, client, deleted));
        assertEquals(1062, rejected.getErrorCode(), "Expected a real MySQL duplicate-key rejection");
        assertTrue(rejected.getMessage().contains("uk_tenant_resource_active"));
    }

    private static void applyMigration(Connection connection, String migration) throws SQLException {
        // This migration contains simple statements and prepared DDL, without semicolons in literals.
        String statements = migration.replaceAll("(?m)^\\s*--[^\\r\\n]*", "");
        for (String statement : statements.split(";")) {
            if (!statement.isBlank()) {
                execute(connection, statement);
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        return rows(connection, sql).get(0).get(0);
    }

    private static String createTableSql(Connection connection) throws SQLException {
        return rows(connection, "SHOW CREATE TABLE sys_resource").get(0).get(1);
    }

    private static List<List<String>> rows(Connection connection, String sql) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int column = 1; column <= result.getMetaData().getColumnCount(); column++) {
                    row.add(result.getString(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static Path resolveRepositoryFile(String relative) throws IOException {
        Path root = Path.of("").toAbsolutePath();
        while (root != null) {
            Path file = root.resolve(relative);
            if (Files.isRegularFile(file)) {
                return file;
            }
            root = root.getParent();
        }
        throw new IOException("Repository migration not found: " + relative);
    }
}
