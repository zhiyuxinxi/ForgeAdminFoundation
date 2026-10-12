package com.mdframe.forge.plugin.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateDataProvider;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.service.formula.*;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDataSourceResolution;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual public CRUD consumers use the production Spring constructors and formula engine. */
class DynamicCrudFormulaKernelTest {
    @Test
    void storedKernelComputesBeforePublicInsertFiltersAndPersists() {
        try (Fixture fixture = new Fixture()) {
            Map<String, Object> data = new LinkedHashMap<>(Map.of("amount", -7L));
            fixture.service.insert("formula_records", data);
            assertEquals(14D, ((Number) data.get("storedTotal")).doubleValue());
            assertFalse(data.containsKey("virtualTotal"));
            verify(fixture.repository).insertReturningKey(eq("formula_records"), argThat(row ->
                    row.size() == 2 && ((Number) row.get("amount")).longValue() == -7L
                            && ((Number) row.get("stored_total")).doubleValue() == 14D), eq("id"), eq(true));
            verify(fixture.market).syncInstalledDefinitions();
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    @Test
    void virtualKernelComputesThroughPublicScopedReadWithoutWriting() {
        try (Fixture fixture = new Fixture()) {
            when(fixture.repository.selectById("formula_records", "id", 1L, fixture.scope))
                    .thenReturn(Map.of("id", 1L, "amount", -7L, "stored_total", 14L));
            Map<String, Object> result = fixture.service.selectById("formula_records", 1L);
            assertEquals(21D, ((Number) result.get("virtualTotal")).doubleValue());
            assertEquals(14L, result.get("storedTotal"));
            verify(fixture.repository, never()).insertReturningKey(anyString(), anyMap(), anyString(), anyBoolean());
            verify(fixture.market).syncInstalledDefinitions();
            assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final DynamicCrudRepository repository = mock(DynamicCrudRepository.class);
        private final FormulaFunctionMarketService market = mock(FormulaFunctionMarketService.class);
        private final DynamicCrudRepository.SqlCondition scope =
                new DynamicCrudRepository.SqlCondition("owner_id = :owner", Map.of("owner", 8L));
        private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        private final DynamicCrudService service;

        private Fixture() {
            ObjectMapper mapper = new ObjectMapper();
            context.registerBean(ObjectMapper.class, () -> mapper);
            context.registerBean(AggregateDataProvider.class, () -> (relation, record) -> List.of());
            context.registerBean(FormulaLookupResolver.class, () -> mock(FormulaLookupResolver.class));
            context.registerBean(FormulaCrossObjectResolver.class, () -> mock(FormulaCrossObjectResolver.class));
            context.registerBean(FormulaFunctionMarketService.class, () -> market);
            context.registerBean(FormulaExecutionLogService.class, () -> mock(FormulaExecutionLogService.class));
            context.register(FormulaRuntimeProperties.class, FormulaBuiltinFunctionProvider.class,
                    FormulaFunctionInvoker.class, FormulaFunctionRegistry.class, FormulaExecutionEngine.class,
                    StoredFormulaRuntime.class, VirtualFormulaRuntime.class);
            context.refresh();
            assertNotNull(context.getBean("formulaBuiltinFunctionProvider"));
            AiCrudConfig config = new AiCrudConfig();
            config.setConfigKey("formula_records");
            config.setTableName("formula_records");
            config.setObjectCode("RECORD");
            config.setDomainCode("SUITE");
            config.setTenantId(7L);
            config.setMode("CONFIG");
            config.setBuildMode("LOWCODE");
            config.setPublishStatus("PUBLISHED");
            config.setEditSchema("[{\"field\":\"amount\"}]");
            config.setModelSchema("""
                    {"fields":[{"field":"amount","columnName":"amount"},
                    {"field":"storedTotal","columnName":"stored_total","formulaConfig":
                    {"type":"CALC","mode":"STORED","expression":"math.abs(amount) * 2","dependsOn":["amount"]}},
                    {"field":"virtualTotal","columnName":"virtual_total","formulaConfig":
                    {"type":"CALC","mode":"VIRTUAL","expression":"math.abs(amount) * 3","dependsOn":["amount"]}}]}
                    """);
            AiCrudConfigService configs = mock(AiCrudConfigService.class);
            when(configs.getByConfigKey("formula_records")).thenReturn(config);
            when(configs.resolvePublishedRuntimeConfig(config)).thenReturn(config);
            LowcodeRuntimeDataSourceResolver sources = mock(LowcodeRuntimeDataSourceResolver.class);
            RuntimeDataSourceResolution runtimeSources = mock(RuntimeDataSourceResolution.class);
            when(sources.getResolution()).thenReturn(runtimeSources);
            when(runtimeSources.resolveRuntime(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual))))
                    .thenReturn(LowcodeRuntimeDataSourceContext.master("formula_records"));
            when(repository.getColumnMapping("formula_records")).thenReturn(Map.of(
                    "amount", "amount", "storedTotal", "stored_total", "virtualTotal", "virtual_total"));
            when(repository.getTableColumns("formula_records"))
                    .thenReturn(Set.of("id", "amount", "stored_total", "virtual_total"));
            DynamicDataScopeService dataScope = mock(DynamicDataScopeService.class);
            when(dataScope.buildRuntimeCondition(argThat(actual -> RuntimeCrudConfigAdapter.fromDesign(config).equals(actual)),
                    eq("formula_records"), isNull())).thenReturn(scope);
            service = new DynamicCrudService(repository, configs, mapper, null, null, null,
                    new LowcodeEncryptConfigParser(mapper), dataScope,
                    mock(com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService.class), null,
                    mock(StoredAggregateRefreshService.class), context.getBean(StoredFormulaRuntime.class),
                    context.getBean(VirtualFormulaRuntime.class), sources);
        }

        @Override
        public void close() {
            context.close();
        }
    }
}
