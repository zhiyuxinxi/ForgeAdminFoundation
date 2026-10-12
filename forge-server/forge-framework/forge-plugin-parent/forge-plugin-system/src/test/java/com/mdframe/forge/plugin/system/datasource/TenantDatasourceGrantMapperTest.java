package com.mdframe.forge.plugin.system.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.mapper.TenantDatasourceGrantMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantDatasourceGrantMapperTest {
    @Test
    void migrationIsIdempotentAndRealMapperKeepsGrantsIsolatedWithoutCredentials() throws Exception {
        var dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:tenant-grants-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("db/migration"))) {
            root = root.getParent();
            if (root == null) {
                throw new IllegalStateException("forge-server migration directory not found");
            }
        }
        String migration = Files.readString(root.resolve(
                "db/migration/V1.0.2026100908__tenant_business_datasource_grants.sql"));
        try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute(migration);
            sql.execute(migration);
            sql.execute("CREATE TABLE gen_datasource (datasource_id BIGINT PRIMARY KEY, datasource_code VARCHAR(60), "
                    + "datasource_name VARCHAR(60), usage_scope VARCHAR(40), is_enabled INT, readonly INT, "
                    + "allow_runtime_write INT, url VARCHAR(200), password VARCHAR(100))");
            sql.execute("INSERT INTO gen_datasource VALUES (3, 'business-a', 'Business A', 'TENANT_BUSINESS', "
                    + "1, 0, 1, 'jdbc:secret', 'must-not-leak')");
        }
        Configuration configuration = new Configuration(new Environment("grant-test",
                new JdbcTransactionFactory(), dataSource));
        try (var xml = Files.newBufferedReader(Path.of("src/main/resources/mapper/TenantDatasourceGrantMapper.xml"),
                StandardCharsets.UTF_8)) {
            new XMLMapperBuilder(xml, configuration, "TenantDatasourceGrantMapper.xml", configuration.getSqlFragments()).parse();
        }
        var factory = new SqlSessionFactoryBuilder().build(configuration);
        try (var session = factory.openSession(true)) {
            TenantDatasourceGrantMapper mapper = session.getMapper(TenantDatasourceGrantMapper.class);
            assertThat(mapper.selectDatasource(7L, 3L).isGranted()).isFalse();
            mapper.grant(7L, 3L, 100L);
            mapper.grant(7L, 3L, 100L);
            TenantDatasourceOption option = mapper.selectDatasource(7L, 3L);
            assertThat(option.isGranted()).isTrue();
            assertThat(option.isSelectable()).isTrue();
            assertThat(option.getDatasourceCode()).isEqualTo("business-a");
            assertThat(mapper.selectOptions(7L)).hasSize(1);
            assertThat(mapper.selectDatasource(8L, 3L).isGranted()).isFalse();
            String json = new ObjectMapper().writeValueAsString(option);
            assertThat(json).doesNotContain("password", "url", "jdbc:secret", "must-not-leak");
            mapper.revoke(8L, 3L);
            assertThat(mapper.selectDatasource(7L, 3L).isGranted()).isTrue();
            // A different request revokes the grant while this SqlSession remains alive.
            try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
                sql.executeUpdate("DELETE FROM sys_tenant_datasource_grant WHERE tenant_id = 7 AND datasource_id = 3");
            }
            assertThat(mapper.selectDatasource(7L, 3L).isGranted()).isFalse();
        }
    }
}
