package com.mdframe.forge.plugin.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.mapper.DataDimensionItemMapper;
import com.mdframe.forge.plugin.data.support.*;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataQueryBoundaryTest {
    private DataQueryExecutor executor;
    private DataDataset dataset;
    private DataConnection connection;
    private DataDatasetRowScopeService rowScopes;
    private JdbcDataSourceProvider provider;
    private final List<DataDatasetField> fields = List.of(field("id", "NONE", 1),
            field("phone", "MASK", 1), field("secret", "HIDDEN", 1), field("disabled", "NONE", 0));

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var conn = DriverManager.getConnection(url); var statement = conn.createStatement()) {
            statement.execute("CREATE TABLE members(id INT, phone VARCHAR(30), secret VARCHAR(30), disabled INT, tenant_id INT)");
            statement.execute("INSERT INTO members VALUES (1,'13812345678','s1',1,7),(2,'13912345678','s2',2,7),(3,'13712345678','s3',3,8),(4,'13612345678','s4',4,7)");
        }
        provider = mock(JdbcDataSourceProvider.class);
        when(provider.getConnection(any())).thenAnswer(call -> DriverManager.getConnection(url));
        rowScopes = mock(DataDatasetRowScopeService.class);
        when(rowScopes.buildCondition(any(), any())).thenReturn(
                DataDatasetRowScopeCondition.of("AND tenant_id = :scopeTenant", Map.of("scopeTenant", 7)));
        DataQueryRuntimeCache cache = mock(DataQueryRuntimeCache.class);
        when(cache.get(any(), any(), any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        executor = new DataQueryExecutor(new DatasetJdbcQueryRunner(provider, new SqlParameterBinder()),
                new DbDialectFactory(new MySqlDialect()), new SqlSafetyValidator(),
                new DatasetParamSchemaParser(new ObjectMapper()), mock(DataDimensionItemMapper.class),
                mock(DataDatasetFieldViewAssembler.class), rowScopes, cache);
        dataset = new DataDataset();
        dataset.setId(1L);
        dataset.setDatasetType("SQL");
        dataset.setSqlText("SELECT * FROM members WHERE 1=1 /*DATA_SCOPE*/ ORDER BY id");
        dataset.setMaxRows(100);
        connection = new DataConnection();
        connection.setDbType("mysql");
    }

    @Test
    void sqlPagesMustKeepScopeMaskingAndOnlySelectedColumns() {
        DataDatasetQueryDTO query = query(1);
        DataDatasetQueryResultVO first = executor.execute(dataset, connection, fields, query);
        query.setPageNum(2);
        DataDatasetQueryResultVO second = executor.execute(dataset, connection, fields, query);
        assertEquals(3L, first.getTotal());
        assertEquals(3L, second.getTotal());
        assertEquals(List.of(1, 2), first.getSource().stream().map(row -> row.get("id")).toList());
        assertEquals(List.of(4), second.getSource().stream().map(row -> row.get("id")).toList());
        assertEquals(Map.of("id", 1, "phone", "13****78"), first.getSource().get(0));
        query.setFields(List.of("id", "secret", "disabled"));
        assertEquals(Map.of("id", 4), executor.execute(dataset, connection, fields, query).getSource().get(0));
        query.setPageNum(3);
        DataDatasetQueryResultVO empty = executor.execute(dataset, connection, fields, query);
        assertTrue(empty.getSource().isEmpty());
        assertEquals(3L, empty.getTotal());
        query.setPageNum(Integer.MAX_VALUE);
        assertTrue(executor.execute(dataset, connection, fields, query).getSource().isEmpty());
    }

    @Test
    void tableQueriesRetainScopeAndParameterBinding() {
        dataset.setDatasetType("TABLE");
        dataset.setTableName("members");
        DataDatasetQueryDTO query = query(1);
        query.setParams(Map.of("id", 2));
        DataDatasetQueryResultVO result = executor.execute(dataset, connection, fields, query);
        assertEquals(1L, result.getTotal());
        assertEquals(Map.of("id", 2, "phone", "13****78"), result.getSource().get(0));
        query.setParams(Map.of("id", 3));
        assertTrue(executor.execute(dataset, connection, fields, query).getSource().isEmpty());
    }

    @Test
    void missingScopePlaceholderMustRejectBeforeJdbc() {
        dataset.setSqlText("SELECT * FROM members");
        assertThrows(BusinessException.class, () -> executor.execute(dataset, connection, fields, query(1)));
        verifyNoInteractions(provider);
    }

    @Test
    void hiddenOnlyRequestReturnsNoRowsWithoutJdbc() {
        DataDatasetQueryDTO query = query(2);
        query.setFields(List.of("secret"));
        DataDatasetQueryResultVO result = executor.execute(dataset, connection, fields, query);
        assertTrue(result.getSource().isEmpty());
        assertEquals(2, result.getPageNum());
        verifyNoInteractions(provider);
    }

    private DataDatasetQueryDTO query(int page) {
        DataDatasetQueryDTO query = new DataDatasetQueryDTO();
        query.setPageNum(page);
        query.setPageSize(2);
        query.setFields(List.of("id", "phone"));
        return query;
    }

    private static DataDatasetField field(String name, String sensitivity, int enabled) {
        DataDatasetField field = new DataDatasetField();
        field.setFieldName(name);
        field.setDisplayEnabled(enabled);
        field.setQueryEnabled(1);
        field.setSensitiveLevel(sensitivity);
        return field;
    }
}
