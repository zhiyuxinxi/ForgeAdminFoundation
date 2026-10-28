package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.StoredAggregateRefreshService;
import com.mdframe.forge.plugin.generator.service.formula.StoredFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.formula.VirtualFormulaRuntime;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeWriteTransactions;
import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public runtime operations retained unchanged before and after child responsibility extraction. */
class DynamicCrudChildLifecycleTest {
    private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
    private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
    private final DynamicDataScopeService dataScope = mock(DynamicDataScopeService.class);
    private final LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
    private final RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
    private final StoredAggregateRefreshService aggregates = mock(StoredAggregateRefreshService.class);
    private final AiCrudConfig config = new AiCrudConfig();
    private final DynamicCrudRepository.SqlCondition scope = new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
    private DynamicCrudService service;

    @BeforeEach
    void setUp() {
        config.setConfigKey("orders");
        config.setObjectCode("orders");
        config.setTableName("orders");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setPublishStatus("PUBLISHED");
        config.setLayoutType("master-detail-crud");
        config.setModelSchema("{\"object\":{\"code\":\"orders\"},\"fields\":[{\"field\":\"title\",\"columnName\":\"title\"}]}");
        config.setEditSchema("[{\"field\":\"title\"},{\"field\":\"lines__name\"}]");
        configureSaveMode("merge");
        when(configs.getByConfigKey("orders")).thenReturn(config);
        when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
        when(sources.getResolution()).thenReturn(runtimeSources);
        when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)))).thenReturn(LowcodeRuntimeDataSourceContext.master("orders"));
        when(dataScope.buildRuntimeCondition(argThat(actual -> {
            var expected = RuntimeCrudConfigAdapter.fromDesign(config);
            expected.setTableName("orders");
            return expected.equals(actual);
        }), eq("orders"), isNull())).thenReturn(scope);
        when(repository.getColumnMapping("orders")).thenReturn(Map.of("id", "id", "title", "title"));
        when(repository.getTableColumns("orders")).thenReturn(Set.of("id", "title"));
        when(repository.getColumnMapping("lines")).thenReturn(Map.of("id", "id", "ordersId", "orders_id", "name", "name", "quantity", "quantity", "tenantId", "tenant_id"));
        when(repository.getTableColumns("lines")).thenReturn(Set.of("id", "orders_id", "name", "quantity", "tenant_id"));
        when(repository.tableExists("lines")).thenReturn(true);
        when(repository.selectFirstIdByColumn("lines", "orders_id", 10L)).thenReturn(null);
        when(repository.selectById("orders", 10L, scope)).thenReturn(Map.of("id", 10L, "title", "before"));
        when(repository.selectById("orders", 10L)).thenReturn(Map.of("id", 10L, "title", "after"));
        when(repository.updateById(eq("orders"), eq(10L), anyMap(), same(scope))).thenReturn(1);
        when(repository.insertReturningKey(eq("orders"), anyMap(), eq("id"), eq(true))).thenReturn(10L);
        ObjectMapper mapper = new ObjectMapper();
        service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                new LowcodeEncryptConfigParser(mapper), dataScope, mock(BusinessDocumentConfigService.class),
                null, aggregates, mock(StoredFormulaRuntime.class), mock(VirtualFormulaRuntime.class), sources);
    }

    @Test
    void detailReadsChildrenOnlyAfterScopedParentAndMapsColumns() {
        when(repository.selectListByColumn("lines", "orders_id", 10L)).thenReturn(List.of(Map.of("id", 21L, "orders_id", 10L, "name", "item")));
        Map<String, Object> result = service.selectById("orders", 10L);
        assertEquals(Map.of("id", 10L, "title", "before"), result.get("main"));
        assertEquals(Map.of("lines", List.of(Map.of("id", 21L, "ordersId", 10L, "name", "item"))), result.get("children"));
        var order = inOrder(repository);
        order.verify(repository).selectById("orders", 10L, scope);
        order.verify(repository).selectListByColumn("lines", "orders_id", 10L);
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void inaccessibleParentStopsChildReadAndWrite() {
        when(repository.selectById("orders", 10L, scope)).thenReturn(null);
        assertNull(service.selectById("orders", 10L));
        assertThrows(BusinessException.class, () -> service.updateById("orders", request(Map.of("name", "item"))));
        verify(repository, never()).selectListByColumn(anyString(), anyString(), any());
        verify(repository, never()).insert(eq("lines"), anyMap());
    }

    @Test
    void mergeInsertOwnsForeignKeyAndIgnoresProtectedOrUnknownInput() {
        service.updateById("orders", request(Map.of("name", "item", "ordersId", 99L, "tenantId", 99L, "unknown", "ignored")));
        verify(repository).insert("lines", Map.of("name", "item", "orders_id", 10L));
        verify(aggregates).refreshRuntimeRecord(RuntimeCrudConfigAdapter.fromDesign(config), 10L);
    }

    @Test
    void mergeRejectsForeignRowsBeforeUpdateOrDelete() {
        when(repository.selectById("lines", "id", 21L, null)).thenReturn(Map.of("orders_id", 99L));
        for (Map<String, Object> row : List.of(Map.<String, Object>of("id", 21L, "name", "edited"), Map.<String, Object>of("id", 21L, "_deleted", true))) {
            assertThrows(BusinessException.class, () -> service.updateById("orders", request(row)));
        }
        verify(repository, never()).updateById(eq("lines"), anyString(), any(), anyMap(), any());
        verify(repository, never()).deleteById(eq("lines"), anyString(), any(), anyBoolean(), any());
    }

    @Test
    void mergeValidatesRequiredAndNumericBoundaries() {
        assertThrows(BusinessException.class, () -> service.updateById("orders", request(Map.of("quantity", 2))));
        assertThrows(BusinessException.class, () -> service.updateById("orders", request(Map.of("name", "item", "quantity", -1))));
        verify(repository, never()).insert(eq("lines"), anyMap());
    }

    @Test
    void mergeDeleteKeepsOwnershipAndAffectedRowFailure() {
        when(repository.selectById("lines", "id", 21L, null)).thenReturn(Map.of("orders_id", "10"));
        when(repository.hasDelFlag("lines")).thenReturn(true);
        when(repository.deleteById("lines", "id", 21L, true, null)).thenReturn(1, 0);
        service.updateById("orders", request(Map.of("id", 21L, "__deleted", "yes")));
        assertThrows(BusinessException.class, () -> service.updateById("orders", request(Map.of("id", 21L, "__deleted", "yes"))));
    }

    @Test
    void replaceDistinguishesOmittedChildrenAndExplicitEmptyList() {
        configureSaveMode("replace");
        service.updateById("orders", new LinkedHashMap<>(Map.of("id", 10L, "title", "edited")));
        verify(repository, never()).deleteByColumn(anyString(), anyString(), any(), anyBoolean());
        assertDoesNotThrow(() -> service.updateById("orders", new LinkedHashMap<>(Map.of("id", 10L, "children", Map.of("lines", List.of())))));
        verify(repository).deleteByColumn("lines", "orders_id", 10L, false);
    }

    @Test
    void unknownChildNameRejectsInsertAndUpdateBeforeAnyWrites() {
        Map<String, Object> children = new LinkedHashMap<>();
        children.put("lines", List.of(Map.of("name", "valid")));
        children.put("misspelled_lines", List.of(Map.of("name", "lost before this fix")));
        assertRejectedBeforeWrites(children);
    }

    @Test
    void malformedChildEnvelopeOrRowsCannotSilentlyClearExistingDetails() {
        configureSaveMode("replace");
        for (Object children : List.of("invalid", List.of(), Map.of("lines", "invalid"),
                Map.of("lines", List.of(Map.of("name", "valid"), "invalid")))) {
            assertRejectedBeforeWrites(children);
        }
        Map<String, Object> nullRows = new LinkedHashMap<>();
        nullRows.put("lines", null);
        assertRejectedBeforeWrites(nullRows);
        assertRejectedBeforeWrites(null);
    }

    @Test
    void legacySingleChildObjectStillWritesNormally() {
        service.insert("orders", new LinkedHashMap<>(Map.of("title", "new", "children",
                Map.of("lines", Map.of("name", "one row")))));
        verify(repository).insert("lines", Map.of("name", "one row", "orders_id", 10L));
    }

    @Test
    void unknownChildMessageUsesRequestLocaleWithoutChangingModelName() {
        org.springframework.context.i18n.LocaleContextHolder.setLocale(java.util.Locale.US);
        try {
            BusinessException failure = assertThrows(BusinessException.class, () -> service.insert("orders",
                    new LinkedHashMap<>(Map.of("title", "new", "children", Map.of("missing_lines", List.of())))));
            assertEquals("Unknown child model name: missing_lines", failure.getMessage());
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    private void assertRejectedBeforeWrites(Object children) {
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("title", "changed"));
        payload.put("children", children);
        assertThrows(BusinessException.class, () -> service.insert("orders", new LinkedHashMap<>(payload)));
        payload.put("id", 10L);
        assertThrows(BusinessException.class, () -> service.updateById("orders", payload));
        verify(repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
        verify(repository, never()).updateById(eq("orders"), any(), anyMap(), any());
        verify(repository, never()).insert(anyString(), anyMap());
        verify(repository, never()).deleteByColumn(anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void insertAndJoinedUpdateKeepChildWritesAndParentCasBoundary() {
        service.insert("orders", new LinkedHashMap<>(Map.of("main", new LinkedHashMap<>(Map.of("title", "new")), "children", Map.of("lines", List.of(Map.of("name", "new line"))))));
        verify(repository).insert("lines", Map.of("name", "new line", "orders_id", 10L));
        config.setLayoutType("crud");
        service.updateById("orders", new LinkedHashMap<>(Map.of("id", 10L, "lines__name", "joined")));
        verify(repository).insert("lines", Map.of("name", "joined", "orders_id", 10L));
        when(repository.updateById(eq("orders"), eq(10L), anyMap(), same(scope))).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.updateById("orders", new LinkedHashMap<>(Map.of("id", 10L, "title", "stale", "lines__name", "must not write"))));
        verify(repository, never()).insert("lines", Map.of("name", "must not write", "orders_id", 10L));
    }

    private Map<String, Object> request(Map<String, Object> row) {
        return new LinkedHashMap<>(Map.of("id", 10L, "children", Map.of("lines", List.of(row))));
    }

    @Test
    void externalMainAndEarlierChildrenRollBackWhenLaterChildFails() {
        JdbcDataSource external = new JdbcDataSource();
        external.setURL("jdbc:h2:mem:external_children;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(external);
        jdbc.execute("CREATE TABLE orders(id BIGINT PRIMARY KEY, title VARCHAR(40))");
        jdbc.execute("CREATE TABLE lines(name VARCHAR(40) UNIQUE, orders_id BIGINT)");
        RuntimeDataSourceAccess access = mock(RuntimeDataSourceAccess.class);
        when(access.dataSource(71L)).thenReturn(external);
        service.setRuntimeWriteTransactions(new RuntimeWriteTransactions(new RuntimeJdbcTemplateProvider(
                jdbc, new NamedParameterJdbcTemplate(external), access)));
        LowcodeRuntimeDataSourceContext externalContext = LowcodeRuntimeDataSourceContext.master("orders");
        externalContext.setMaster(false);
        externalContext.setDatasourceId(71L);
        when(runtimeSources.resolveRuntime(any())).thenReturn(externalContext);
        when(repository.insertReturningKey(eq("orders"), anyMap(), eq("id"), eq(true))).thenAnswer(call -> {
            jdbc.update("INSERT INTO orders(id,title) VALUES(10,'new')");
            return 10L;
        });
        when(repository.insert(eq("lines"), anyMap())).thenAnswer(call -> {
            Map<String, Object> row = call.getArgument(1);
            return jdbc.update("INSERT INTO lines(name,orders_id) VALUES(?,?)", row.get("name"), row.get("orders_id"));
        });
        Map<String, Object> payload = new LinkedHashMap<>(Map.of("title", "new", "children",
                Map.of("lines", List.of(Map.of("name", "duplicate"), Map.of("name", "duplicate")))));
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> service.insert("orders", payload));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM lines", Integer.class));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    private void configureSaveMode(String mode) {
        config.setPageSchema("""
                {"primaryModelCode":"orders","modelRefs":[
                  {"modelCode":"orders","tableName":"orders","primary":true},
                  {"modelCode":"lines","tableName":"lines","props":{"saveMode":"%s"},"fields":[
                    {"field":"ordersId","columnName":"orders_id"},
                    {"field":"name","required":true},
                    {"field":"quantity","min":0,"max":100},
                    {"field":"tenantId","columnName":"tenant_id"}]}]}
                """.formatted(mode));
    }
}
