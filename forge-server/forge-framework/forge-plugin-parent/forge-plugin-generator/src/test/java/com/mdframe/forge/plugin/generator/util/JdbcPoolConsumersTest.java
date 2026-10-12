package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.support.JdbcConnectionPools;
import com.mdframe.forge.plugin.data.support.JdbcDataSourceProvider;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.support.GeneratorJdbcDataSources;
import cn.hutool.extra.spring.SpringUtil;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JdbcPoolConsumersTest {

    @Test
    void realConsumersKeepSizingDecryptionAndTemporaryPoolOwnership() throws Exception {
        PersistentCryptoService crypto = mock(PersistentCryptoService.class);
        when(crypto.decrypt("cipher-one", null)).thenReturn("password-one");
        when(crypto.decrypt("cipher-two", null)).thenReturn("password-two");
        try (JdbcConnectionPools pools = new JdbcConnectionPools()) {
            JdbcDataSourceProvider provider = new JdbcDataSourceProvider(crypto, pools);
            GeneratorJdbcDataSources generator = new GeneratorJdbcDataSources(pools);
            DataConnection definition = dataConnection("data_one", "cipher-one");
            HikariDataSource first = (HikariDataSource) provider.getDataSource(definition);
            assertEquals(1, queryOne(first));
            assertEquals(10, first.getMaximumPoolSize());
            assertEquals(2, first.getMinimumIdle());
            assertEquals(300_000, first.getIdleTimeout());
            assertEquals("password-one", first.getPassword());
            GenDatasource generated = generatorConnection("generator_one");
            HikariDataSource other = (HikariDataSource) generator.getDataSource(generated);
            assertEquals(1, queryOne(other));
            assertEquals(5, other.getMaximumPoolSize());
            assertEquals(1, other.getMinimumIdle());
            assertEquals(600_000, other.getIdleTimeout());
            assertTrue(generator.testConnection(generated));
            assertNotSame(first, other);

            definition.setJdbcUrl("jdbc:h2:mem:data_two");
            definition.setPasswordCipher("cipher-two");
            HikariDataSource replacement = (HikariDataSource) provider.getDataSource(definition);
            assertTrue(first.isClosed());
            assertEquals("password-two", replacement.getPassword());
            assertEquals(1, queryOne(replacement));
            try (HikariDataSource temporary = (HikariDataSource) provider.createTempDataSource(definition, "password-two")) {
                provider.closeAll();
                assertTrue(replacement.isClosed());
                assertFalse(temporary.isClosed());
                assertFalse(other.isClosed());
                assertEquals(1, queryOne(temporary));
            }
            generator.removeDataSource(11L);
            assertTrue(other.isClosed());
            verify(crypto, times(1)).decrypt("cipher-one", null);
            verify(crypto, times(1)).decrypt("cipher-two", null);
            verifyNoMoreInteractions(crypto);
        }
    }

    @Test
    void decryptionFailureNeverFallsBackToPreviouslyCachedPool() {
        PersistentCryptoService crypto = mock(PersistentCryptoService.class);
        when(crypto.decrypt("cipher-one", null)).thenReturn("password-one");
        when(crypto.decrypt("broken-cipher", null)).thenThrow(new IllegalStateException("key unavailable"));
        try (JdbcConnectionPools pools = new JdbcConnectionPools()) {
            JdbcDataSourceProvider provider = new JdbcDataSourceProvider(crypto, pools);
            DataConnection definition = dataConnection("decrypt_guard", "cipher-one");
            assertNotNull(provider.getDataSource(definition));
            definition.setPasswordCipher("broken-cipher");
            assertThrows(BusinessException.class, () -> provider.getDataSource(definition));
            verify(crypto).decrypt("broken-cipher", null);
        }
    }

    @Test
    void legacyQueryReturnsDetachedRowsAndClosesJdbcResourcesEvenWhenSqlFails() throws Exception {
        try (JdbcConnectionPools pools = new JdbcConnectionPools()) {
            GeneratorJdbcDataSources generator = new GeneratorJdbcDataSources(pools);
            GenDatasource definition = generatorConnection("detached_query");
            HikariDataSource pool = (HikariDataSource) generator.getDataSource(definition);
            try (ResultSet result = generator.executeQuery(definition, "SELECT 42 AS value_id")) {
                assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections());
                assertThrows(java.sql.SQLException.class,
                        () -> generator.executeQuery(definition, "SELECT missing_column"));
                assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections());
                generator.clearAll();
                assertTrue(pool.isClosed());
                assertTrue(result.next());
                assertEquals(42, result.getInt("value_id"));
                assertFalse(result.next());
            }
        }
    }

    @Test
    void springShutdownClosesBothConsumerPools() {
        HikariDataSource dataPool;
        HikariDataSource generatorPool;
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PersistentCryptoService.class, () -> mock(PersistentCryptoService.class));
            context.register(JdbcConnectionPools.class, JdbcDataSourceProvider.class, GeneratorJdbcDataSources.class, SpringUtil.class);
            context.refresh();
            dataPool = (HikariDataSource) context.getBean(JdbcDataSourceProvider.class)
                    .getDataSource(dataConnection("shutdown_data", ""));
            generatorPool = (HikariDataSource) context.getBean(GeneratorJdbcDataSources.class)
                    .getDataSource(generatorConnection("shutdown_generator"));
            assertFalse(dataPool.isClosed());
            assertFalse(generatorPool.isClosed());
            assertSame(generatorPool, DynamicDataSourceUtil.getDataSource(generatorConnection("shutdown_generator")));
            assertTrue(DynamicDataSourceUtil.testConnection(generatorConnection("shutdown_generator")));
        }
        assertTrue(dataPool.isClosed());
        assertTrue(generatorPool.isClosed());
    }

    private static int queryOne(HikariDataSource pool) throws Exception {
        try (Connection connection = pool.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static DataConnection dataConnection(String database, String cipher) {
        DataConnection definition = new DataConnection();
        definition.setId(11L);
        definition.setTenantId(3L);
        definition.setDriverClassName("org.h2.Driver");
        definition.setJdbcUrl("jdbc:h2:mem:" + database);
        definition.setUsername("sa");
        definition.setPasswordCipher(cipher);
        definition.setTestSql("SELECT 1");
        return definition;
    }

    private static GenDatasource generatorConnection(String database) {
        GenDatasource definition = new GenDatasource();
        definition.setDatasourceId(11L);
        definition.setDriverClassName("org.h2.Driver");
        definition.setUrl("jdbc:h2:mem:" + database);
        definition.setUsername("sa");
        definition.setPassword("");
        return definition;
    }
}
