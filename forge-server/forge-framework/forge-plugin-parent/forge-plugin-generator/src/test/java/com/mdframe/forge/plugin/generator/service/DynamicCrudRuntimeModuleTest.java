package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.lowcode.api.datasource.RuntimeDataSourceAccess;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityReferenceService;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateDataProvider;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.*;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratorRuntimeDataSourceAccess;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePolicyService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeWriteTransactions;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The same old public entry is exercised before and after moving its runtime implementation. */
class DynamicCrudRuntimeModuleTest {
    private static final String TABLE = "module_records";

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
    }

    @Test
    void publicInsertUsesActualSpringTransactionAndRollsBackJdbcOnAggregateFailure() {
        try (Fixture fixture = new Fixture()) {
            IllegalStateException failure = new IllegalStateException("aggregate unavailable");
            doAnswer(call -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals(1, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM module_records", Integer.class));
                throw failure;
            }).when(fixture.aggregates).refreshRuntimeAfterChildInsert(any(), anyMap());

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> fixture.service.insert(TABLE, new LinkedHashMap<>(Map.of("amount", -7L)))));
            assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM module_records", Integer.class));
            assertEquals(List.of("stored", "jdbc"), fixture.events);
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    @Test
    void legacyTypeHasOneProxiedBeanAndAllTwelveExceptionRollbackMethods() {
        try (Fixture fixture = new Fixture()) {
            assertEquals(1, fixture.context.getBeanNamesForType(DynamicCrudService.class).length);
            assertTrue(AopUtils.isCglibProxy(fixture.service));
            TransactionAttributeSource attributes = fixture.context.getBean(TransactionAttributeSource.class);
            int transactionalMethods = 0;
            for (var method : DynamicCrudService.class.getMethods()) {
                var attribute = attributes.getTransactionAttribute(method, DynamicCrudService.class);
                if (attribute != null) {
                    transactionalMethods++;
                    assertTrue(attribute.rollbackOn(new Exception("checked failure")), method.toString());
                }
            }
            assertEquals(12, transactionalMethods);
        }
    }

    @Test
    void realStoredThenVirtualFormulasPreserveScopedPublicWriteAndReadOrder() {
        try (Fixture fixture = new Fixture()) {
            Map<String, Object> input = new LinkedHashMap<>(Map.of("amount", -7L));
            fixture.service.insert(TABLE, input);
            assertEquals(14D, ((Number) input.get("storedTotal")).doubleValue());
            assertFalse(input.containsKey("virtualTotal"));
            assertEquals(14D, fixture.jdbc.queryForObject("SELECT stored_total FROM module_records", Double.class));
            when(fixture.repository.selectById(eq(TABLE), eq("id"), eq(1L), any()))
                    .thenReturn(Map.of("id", 1L, "amount", -7L, "stored_total", 14L));
            Map<String, Object> result = fixture.service.selectById(TABLE, 1L);
            assertEquals(21D, ((Number) result.get("virtualTotal")).doubleValue());
            assertEquals(14L, result.get("storedTotal"));
            assertEquals(List.of("stored", "jdbc", "aggregate-insert", "virtual"), fixture.events);
            verify(fixture.repository).selectById(eq(TABLE), eq("id"), eq(1L), argThat(condition ->
                    condition != null && condition.sql().equals("owner_id = :__dsUserId")
                            && condition.params().get("__dsUserId").equals(8L)));
            verify(fixture.permissions).getCurrentUserDataScope("ai:business:RECORD");
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    @Test
    void unpublishedConfigRequiresOrdinaryAdmissionWhileAllowDraftRetainsItsOwnChecks() {
        try (Fixture fixture = new Fixture()) {
            fixture.config.setPublishStatus("DRAFT");
            assertThrows(BusinessException.class, () -> fixture.service.selectById(TABLE, 1L));
            verify(fixture.repository, never()).selectById(anyString(), anyString(), any(), any());
            when(fixture.repository.selectById(eq(TABLE), eq("id"), eq(1L), any()))
                    .thenReturn(Map.of("id", 1L, "amount", -7L, "stored_total", 14L));
            assertEquals(1L, fixture.service.selectByIdAllowDraft(TABLE, 1L).get("id"));
            verify(fixture.configs, never()).resolvePublishedRuntimeConfig(any());
            fixture.config.setStatus("1");
            assertThrows(BusinessException.class, () -> fixture.service.selectByIdAllowDraft(TABLE, 1L));
            fixture.config.setStatus("0");
            fixture.config.setMode("CODE");
            assertThrows(BusinessException.class, () -> fixture.service.selectByIdAllowDraft(TABLE, 1L));
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    @Test
    void updateAndDeleteKeepAggregateArgumentsAndPropagateOriginalFailure() {
        try (Fixture fixture = new Fixture()) {
            Map<String, Object> before = Map.of("id", 1L, "amount", -7L, "stored_total", 14L);
            Map<String, Object> after = Map.of("id", 1L, "amount", -5L, "stored_total", 10L);
            when(fixture.repository.selectById(eq(TABLE), eq("id"), eq(1L), any()))
                    .thenAnswer(call -> call.getArgument(3) == null ? after : before);
            when(fixture.repository.updateById(eq(TABLE), eq("id"), eq(1L), anyMap(), any())).thenReturn(1);
            fixture.service.updateById(TABLE, new LinkedHashMap<>(Map.of("id", 1L, "amount", -5L)));
            verify(fixture.aggregates).refreshRuntimeAfterChildUpdate(
                    argThat(value -> value.getTenantId().equals(7L)), same(before), same(after));

            when(fixture.repository.deleteById(eq(TABLE), eq("id"), eq(1L), eq(false), any())).thenReturn(1);
            IllegalStateException failure = new IllegalStateException("delete refresh failed");
            doThrow(failure).when(fixture.aggregates).refreshRuntimeAfterChildDelete(any(), same(before));
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> fixture.service.deleteById(TABLE, 1L)));
            var order = inOrder(fixture.repository, fixture.aggregates);
            order.verify(fixture.repository).deleteById(eq(TABLE), eq("id"), eq(1L), eq(false), any());
            order.verify(fixture.aggregates).refreshRuntimeAfterChildDelete(any(), same(before));
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    @Test
    void masterDetailInsertInvokesFourthAggregateEntryAfterActualChildWrite() {
        try (Fixture fixture = new Fixture()) {
            fixture.config.setLayoutType("master-detail-crud");
            fixture.config.setModelSchema("{\"object\":{\"code\":\"module_records\"},\"fields\":[{\"field\":\"amount\",\"columnName\":\"amount\"}]}");
            fixture.config.setEditSchema("[{\"field\":\"amount\"},{\"field\":\"lines__name\"}]");
            fixture.config.setPageSchema("""
                    {"primaryModelCode":"module_records","modelRefs":[
                      {"modelCode":"module_records","tableName":"module_records","primary":true},
                      {"modelCode":"lines","tableName":"module_lines","props":{"saveMode":"merge"},"fields":[
                        {"field":"moduleRecordsId","columnName":"module_records_id"},
                        {"field":"name","required":true}]}]}
                    """);
            when(fixture.repository.getColumnMapping("module_lines"))
                    .thenReturn(Map.of("id", "id", "moduleRecordsId", "module_records_id", "name", "name"));
            when(fixture.repository.getTableColumns("module_lines"))
                    .thenReturn(Set.of("id", "module_records_id", "name"));
            when(fixture.repository.tableExists("module_lines")).thenReturn(true);
            doReturn(10L).when(fixture.repository).insertReturningKey(eq(TABLE), anyMap(), eq("id"), eq(true));
            when(fixture.repository.insert(eq("module_lines"), anyMap())).thenReturn(1);
            fixture.service.insert(TABLE, new LinkedHashMap<>(Map.of(
                    "main", new LinkedHashMap<>(Map.of("amount", 2L)),
                    "children", Map.of("lines", List.of(Map.of("name", "child"))))));
            var order = inOrder(fixture.repository, fixture.aggregates);
            order.verify(fixture.repository).insert(eq("module_lines"), argThat(row ->
                    row.get("module_records_id").equals(10L) && row.get("name").equals("child")));
            order.verify(fixture.aggregates).refreshRuntimeRecord(any(LowcodeRuntimeConfig.class), eq(10L));
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
        private final AiCrudConfigService configs = mock(AiCrudConfigService.class);
        private final StoredAggregateRefreshService aggregates = mock(StoredAggregateRefreshService.class);
        private final IDataScopeService permissions = mock(IDataScopeService.class);
        private final FormulaFunctionMarketService market = mock(FormulaFunctionMarketService.class);
        private final List<String> events = new ArrayList<>();
        private final AiCrudConfig config = configuration();
        private final JdbcTemplate jdbc;
        private final DynamicCrudService service;

        private Fixture() {
            DriverManagerDataSource source = new DriverManagerDataSource(
                    "jdbc:h2:mem:runtime_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1", "sa", "");
            source.setDriverClassName("org.h2.Driver");
            jdbc = new JdbcTemplate(source);
            jdbc.execute("CREATE TABLE module_records (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, amount BIGINT, stored_total DOUBLE)");
            ObjectMapper mapper = new ObjectMapper();
            when(configs.getByConfigKey(TABLE)).thenReturn(config);
            when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
            when(repository.getColumnMapping(TABLE)).thenReturn(Map.of(
                    "id", "id", "amount", "amount", "storedTotal", "stored_total", "virtualTotal", "virtual_total", "ownerId", "owner_id"));
            when(repository.getTableColumns(TABLE)).thenReturn(Set.of("id", "amount", "stored_total", "virtual_total", "owner_id"));
            when(permissions.getCurrentUserDataScope("ai:business:RECORD")).thenReturn(
                    new DataScopeContext(8L, List.of(2L), 2L, List.of(7L), 5, Set.of(), 7L, null, null, null));
            when(repository.insertReturningKey(eq(TABLE), anyMap(), eq("id"), eq(true))).thenAnswer(call -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                Map<String, Object> row = call.getArgument(1);
                events.add("jdbc");
                jdbc.update("INSERT INTO module_records(amount, stored_total) VALUES (?, ?)", row.get("amount"), row.get("stored_total"));
                return null;
            });
            doAnswer(call -> {
                events.add("aggregate-insert");
                return null;
            }).when(aggregates).refreshRuntimeAfterChildInsert(any(), anyMap());
            doAnswer(call -> {
                events.add(events.contains("jdbc") ? "virtual" : "stored");
                return null;
            }).when(market).syncInstalledDefinitions();
            context.register(Transactions.class);
            context.registerBean("transactionManager", DataSourceTransactionManager.class,
                    () -> new DataSourceTransactionManager(source));
            context.registerBean(ObjectMapper.class, () -> mapper);
            context.registerBean(RuntimeIdentityReferenceService.class, () -> mock(RuntimeIdentityReferenceService.class));
            context.registerBean(RuntimeWriteTransactions.class, () -> new RuntimeWriteTransactions(
                    new RuntimeJdbcTemplateProvider(jdbc, new NamedParameterJdbcTemplate(source),
                            mock(RuntimeDataSourceAccess.class))));
            context.registerBean(AggregateDataProvider.class, () -> (relation, record) -> List.of());
            context.registerBean(FormulaLookupResolver.class, () -> mock(FormulaLookupResolver.class));
            context.registerBean(FormulaCrossObjectResolver.class, () -> mock(FormulaCrossObjectResolver.class));
            context.registerBean(FormulaFunctionMarketService.class, () -> market);
            context.registerBean(FormulaExecutionLogService.class, () -> mock(FormulaExecutionLogService.class));
            context.register(FormulaRuntimeProperties.class, FormulaBuiltinFunctionProvider.class,
                    FormulaFunctionInvoker.class, FormulaFunctionRegistry.class, FormulaExecutionEngine.class,
                    StoredFormulaRuntime.class, VirtualFormulaRuntime.class);
            LowcodeRuntimeDataSourceResolver resolver = new LowcodeRuntimeDataSourceResolver(
                    new GeneratorRuntimeDataSourceAccess(mock(GenDatasourceMapper.class), null), mapper);
            DynamicDataScopeService scopes = new DynamicDataScopeService(permissions, repository, mapper, new LowcodePolicyService());
            context.registerBean(DynamicCrudService.class, () -> new DynamicCrudService(repository, configs, mapper,
                    null, null, null, new LowcodeEncryptConfigParser(mapper), scopes,
                    mock(BusinessDocumentConfigService.class), null, aggregates,
                    context.getBean(StoredFormulaRuntime.class), context.getBean(VirtualFormulaRuntime.class), resolver));
            context.refresh();
            service = context.getBean(DynamicCrudService.class);
        }

        @Override
        public void close() {
            context.close();
            jdbc.execute("SHUTDOWN");
        }

        private static AiCrudConfig configuration() {
            AiCrudConfig value = new AiCrudConfig();
            value.setConfigKey(TABLE);
            value.setTableName(TABLE);
            value.setObjectCode("RECORD");
            value.setDomainCode("SUITE");
            value.setTenantId(7L);
            value.setMode("CONFIG");
            value.setBuildMode("LOWCODE");
            value.setPublishStatus("PUBLISHED");
            value.setEditSchema("[{\"field\":\"amount\"}]");
            value.setModelSchema("""
                    {"policies":{"dataScope":"FOLLOW_SYSTEM","userField":"ownerId","userColumn":"owner_id"},
                    "fields":[{"field":"amount","columnName":"amount"},
                    {"field":"storedTotal","columnName":"stored_total","formulaConfig":
                    {"type":"CALC","mode":"STORED","expression":"math.abs(amount) * 2","dependsOn":["amount"]}},
                    {"field":"virtualTotal","columnName":"virtual_total","formulaConfig":
                    {"type":"CALC","mode":"VIRTUAL","expression":"storedTotal + 7","dependsOn":["storedTotal"]}}]}
                    """);
            return value;
        }
    }
}
