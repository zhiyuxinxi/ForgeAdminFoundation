package com.mdframe.forge.plugin.data.support;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatasetJdbcQueryRunnerTest {
    @Test
    void previewMustApplyReadOnlyTimeoutRowLimitAndBindParameters() throws Exception {
        JdbcDataSourceProvider provider = mock(JdbcDataSourceProvider.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(provider.getConnection(any())).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(metadata.getColumnLabel(1)).thenReturn("id");
        DatasetJdbcQueryRunner.Result preview = new DatasetJdbcQueryRunner(provider, new SqlParameterBinder())
                .preview(new DataConnection(), "SELECT id FROM members WHERE id = :id", Map.of("id", 7),
                        new MySqlDialect(), 20000, 999);
        assertEquals(java.util.List.of("id"), preview.columns());
        verify(connection).setReadOnly(true);
        verify(statement).setQueryTimeout(120);
        verify(statement).setMaxRows(10000);
        verify(statement).setObject(1, 7);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void previewTimeoutMustFailAndCloseResources() throws Exception {
        JdbcDataSourceProvider provider = mock(JdbcDataSourceProvider.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(provider.getConnection(any())).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(new SQLTimeoutException("private SQL"));
        BusinessException error = assertThrows(BusinessException.class, () ->
                new DatasetJdbcQueryRunner(provider, new SqlParameterBinder()).preview(new DataConnection(),
                        "SELECT 1", Map.of(), new MySqlDialect(), 10, null));
        assertFalse(error.getMessage().contains("private SQL"));
        verify(statement).setQueryTimeout(30);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void failedReadOnlySetupMustNotExecuteSql() throws Exception {
        JdbcDataSourceProvider provider = mock(JdbcDataSourceProvider.class);
        Connection connection = mock(Connection.class);
        when(provider.getConnection(any())).thenReturn(connection);
        doThrow(new SQLException("not supported")).when(connection).setReadOnly(true);
        assertThrows(BusinessException.class, () -> new DatasetJdbcQueryRunner(provider, new SqlParameterBinder())
                .preview(new DataConnection(), "SELECT 1", Map.of(), new MySqlDialect(), 10, 1));
        verify(connection, never()).prepareStatement(anyString());
        verify(connection).close();
    }
}
