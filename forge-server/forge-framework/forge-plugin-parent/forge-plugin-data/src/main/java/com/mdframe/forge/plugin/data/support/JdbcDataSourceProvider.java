package com.mdframe.forge.plugin.data.support;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcDataSourceProvider {

    private final PersistentCryptoService persistentCryptoService;

    private final JdbcConnectionPools pools;
    private static final JdbcConnectionPools.Sizing SIZING = new JdbcConnectionPools.Sizing(10, 2, 300_000);

    public DataSource getDataSource(DataConnection connection) {
        String password = decryptPassword(connection);
        JdbcConnectionPools.Key key = new JdbcConnectionPools.Key(
                JdbcConnectionPools.Scope.DATA_CONNECTION, connection.getTenantId(), connection.getId());
        return pools.get(key, configuration(connection, password));
    }

    /** Temporary test pools remain caller-owned and are never cached. */
    public DataSource createTempDataSource(DataConnection connection, String password) {
        return new HikariDataSource(configuration(connection, password).toHikariConfig());
    }

    private JdbcConnectionPools.Configuration configuration(DataConnection connection, String password) {
        return new JdbcConnectionPools.Configuration(connection.getDriverClassName(), connection.getJdbcUrl(),
                connection.getUsername(), password, connection.getTestSql(), SIZING);
    }

    public Connection getConnection(DataConnection connection) throws SQLException {
        DataSource dataSource = getDataSource(connection);
        return dataSource.getConnection();
    }

    public void closeDataSource(Long connectionId) {
        pools.invalidate(JdbcConnectionPools.Scope.DATA_CONNECTION, connectionId);
    }

    public void closeAll() {
        pools.clear(JdbcConnectionPools.Scope.DATA_CONNECTION);
    }

    private String decryptPassword(DataConnection connection) {
        String cipherText = connection.getPasswordCipher();
        if (cipherText == null || cipherText.isEmpty()) {
            log.warn("Password cipher is empty");
            return cipherText;
        }
        try {
            String decrypted = persistentCryptoService.decrypt(cipherText, null);
            log.debug("Password decrypted successfully");
            return decrypted;
        } catch (Exception e) {
            log.error("数据连接密码解密失败，connectionId={}, dbType={}, exceptionType={}",
                    connection.getId(), connection.getDbType(), e.getClass().getSimpleName());
            throw new BusinessException("数据连接密码解密失败，请检查持久化密钥配置", e);
        }
    }
}
