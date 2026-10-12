package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaExecutionLog;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.mapper.*;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Production Spring adapters execute against controlled persistence boundaries. */
class FormulaRuntimePersistencePortsTest {
    @Test
    void installedFunctionsRefreshOnEveryExecutionAndFailurePropagates() {
        try (Fixture f = new Fixture()) {
            FormulaExecutionEngine engine = f.context.getBean(FormulaExecutionEngine.class);
            Map<String, FormulaConfig> formulas = Map.of("total", calc());
            assertEquals(6L, engine.execute(formulas, Map.of("amount", 3L)).getResult("total"));
            assertEquals(8L, engine.execute(formulas, Map.of("amount", 4L)).getResult("total"));
            verify(f.functions, times(2)).selectInstalledFunctionDefinitions(7L);
            IllegalStateException failure = new IllegalStateException("catalog unavailable");
            when(f.functions.selectInstalledFunctionDefinitions(7L)).thenThrow(failure);
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> engine.execute(formulas, Map.of("amount", 5L))));
        }
    }

    @Test
    void lookupDatabaseFailureRemainsAFormulaErrorAfterFunctionRefresh() {
        try (Fixture f = new Fixture()) {
            when(f.relations.selectRelationById(7L, 1L))
                    .thenThrow(new IllegalStateException("lookup unavailable"));
            FormulaConfig config = FormulaConfig.builder().type(FormulaType.LOOKUP).mode(FormulaMode.VIRTUAL)
                    .dependsOn(List.of("ownerId"))
                    .lookup(new LookupConfig("1", "user", "ownerId", "id", "level", null)).build();
            Map<String, Object> values = new LinkedHashMap<>(Map.of("ownerId", "u1"));
            values.put("__formulaRuntimeContext__", runtimeContext());
            ExecutionResult result = f.context.getBean(FormulaExecutionEngine.class)
                    .execute(Map.of("level", config), values, FormulaTraceOptions.debug());
            assertFalse(result.isSuccess());
            assertTrue(result.getErrors().toString().contains("lookup unavailable"));
            var order = inOrder(f.functions, f.relations);
            order.verify(f.functions).selectInstalledFunctionDefinitions(7L);
            order.verify(f.relations).selectRelationById(7L, 1L);
        }
    }

    @Test
    void virtualPrefetchKeepsExtensionOrderAndOnlyCopiesFormulaValues() {
        try (Fixture f = new Fixture()) {
            f.crossObject();
            List<String> events = new ArrayList<>();
            when(f.records.fetchDetailRecordsBatch(eq("users"), eq("id"), anyCollection(), eq(7L)))
                    .thenAnswer(call -> {
                        events.add("prefetch");
                        return List.of(Map.of("id", "u1", "level", "VIP"), Map.of("id", "u2", "level", "NORMAL"));
                    });
            when(f.functions.selectInstalledFunctionDefinitions(7L)).thenAnswer(call -> {
                events.add("refresh");
                return List.of();
            });
            VirtualFormulaRuntime virtual = new VirtualFormulaRuntime(
                    f.context.getBean(FormulaExecutionEngine.class), f.mapper,
                    f.context.getBean(FormulaExecutionLogService.class),
                    f.context.getBean(FormulaCrossObjectResolver.class), f.context.getBean(FormulaRuntimeProperties.class)) {
                @Override
                protected Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema schema) {
                    events.add("extract");
                    return super.extractFormulas(schema);
                }
            };
            List<Map<String, Object>> rows = List.of(new LinkedHashMap<>(Map.of("ownerId", "u1")),
                    new LinkedHashMap<>(Map.of("ownerId", "u2")));
            LowcodeModelSchema schema = schema("VIRTUAL", "owner.level", Map.of("path", "owner.level",
                    "relationCode", "1", "targetObjectCode", "user", "returnField", "level"));
            assertSame(rows, virtual.calculate(rows, schema, runtimeContext(), FormulaTraceOptions.disabled()));
            assertEquals("VIP", rows.get(0).get("total"));
            assertEquals("NORMAL", rows.get(1).get("total"));
            assertFalse(rows.get(0).containsKey("owner"));
            assertEquals(List.of("extract", "prefetch", "extract", "refresh", "refresh"), events);
            IllegalStateException failure = new IllegalStateException("cross-object unavailable");
            when(f.records.fetchDetailRecordsBatch(eq("users"), eq("id"), anyCollection(), eq(7L)))
                    .thenThrow(failure);
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> virtual.calculate(rows, schema, runtimeContext(), FormulaTraceOptions.disabled())));
            verify(f.functions, times(2)).selectInstalledFunctionDefinitions(7L);
        }
    }

    @Test
    void traceAdapterMasksAndScopesLogsWhileStorageFailureDoesNotStopRows() {
        try (Fixture f = new Fixture()) {
            f.context.getBean(FormulaRuntimeProperties.class).setSuccessLogEnabled(true);
            when(f.logs.insert(any(AiFormulaExecutionLog.class))).thenAnswer(call -> {
                AiFormulaExecutionLog log = call.getArgument(0);
                assertEquals(7L, log.getTenantId());
                assertTrue(log.getInputSnapshot().contains("****"));
                assertEquals(Boolean.TRUE, log.getSuccess());
                throw new IllegalStateException("log storage unavailable");
            });
            List<Map<String, Object>> rows = List.of(
                    new LinkedHashMap<>(Map.of("amount", 3L, "mobile", "13812345678")),
                    new LinkedHashMap<>(Map.of("amount", 4L, "mobile", "13812345678")));
            assertSame(rows, f.context.getBean(StoredFormulaRuntime.class).calculate(rows,
                    schema("STORED", "amount * 2", null), runtimeContext(), FormulaTraceOptions.debug()));
            assertEquals(6L, rows.get(0).get("total"));
            assertEquals(8L, rows.get(1).get("total"));
            verify(f.logs, times(2)).insert(any(AiFormulaExecutionLog.class));
        }
    }

    private static FormulaConfig calc() {
        return FormulaConfig.builder().type(FormulaType.CALC).mode(FormulaMode.STORED)
                .expression("amount * 2").dependsOn(List.of("amount")).build();
    }

    private static FormulaRuntimeContext runtimeContext() {
        return new FormulaRuntimeContext(7L, "SUITE", "record", Map.of());
    }

    private static LowcodeModelSchema schema(String mode, String expression, Map<String, Object> crossObject) {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("total");
        Map<String, Object> formula = new LinkedHashMap<>(Map.of("type", "CALC", "mode", mode,
                "expression", expression, "dependsOn", List.of(crossObject == null ? "amount" : "ownerId")));
        if (crossObject != null) formula.put("crossObject", crossObject);
        field.setFormulaConfig(formula);
        LowcodeModelSchema model = new LowcodeModelSchema();
        model.setFields(List.of(field));
        return model;
    }

    private static final class Fixture implements AutoCloseable {
        private final ObjectMapper mapper = new ObjectMapper();
        private final BusinessObjectRelationMapper relations = mock(BusinessObjectRelationMapper.class);
        private final BusinessObjectMapper objects = mock(BusinessObjectMapper.class);
        private final AiLowcodeModelMapper models = mock(AiLowcodeModelMapper.class);
        private final DetailRecordFetcher records = mock(DetailRecordFetcher.class);
        private final FormulaFunctionMapper functions = mock(FormulaFunctionMapper.class);
        private final FormulaExecutionLogMapper logs = mock(FormulaExecutionLogMapper.class);
        private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        private final MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);

        private Fixture() {
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            session.when(SessionHelper::getUserId).thenReturn(9L);
            session.when(SessionHelper::getMainOrgId).thenReturn(8L);
            when(functions.selectInstalledFunctionDefinitions(7L)).thenReturn(List.of());
            context.registerBean(ObjectMapper.class, () -> mapper);
            context.registerBean(BusinessObjectRelationMapper.class, () -> relations);
            context.registerBean(BusinessObjectMapper.class, () -> objects);
            context.registerBean(AiLowcodeModelMapper.class, () -> models);
            context.registerBean(DetailRecordFetcher.class, () -> records);
            context.registerBean(FormulaFunctionMapper.class, () -> functions);
            context.registerBean(FormulaExecutionLogMapper.class, () -> logs);
            context.registerBean(AggregateDataProvider.class, () -> (relation, record) -> List.of());
            context.register(FormulaRuntimeProperties.class, FormulaBuiltinFunctionProvider.class,
                    FormulaFunctionInvoker.class, FormulaFunctionRegistry.class, FormulaLookupResolver.class,
                    FormulaCrossObjectResolver.class, FormulaFunctionMarketService.class, FormulaExecutionLogService.class,
                    FormulaValueMasker.class, FormulaExecutionEngine.class, StoredFormulaRuntime.class, VirtualFormulaRuntime.class);
            context.refresh();
        }

        private void crossObject() {
            AiBusinessObjectRelation relation = new AiBusinessObjectRelation();
            relation.setSuiteCode("SUITE");
            relation.setSourceObjectCode("record");
            relation.setTargetObjectCode("user");
            relation.setSourceFieldCode("ownerId");
            relation.setTargetFieldCode("id");
            relation.setRelationName("owner");
            when(relations.selectRelationById(7L, 1L)).thenReturn(relation);
            AiBusinessObject object = new AiBusinessObject();
            object.setModelCode("users");
            when(objects.selectByObjectCode(7L, "SUITE", "user")).thenReturn(object);
            AiLowcodeModel model = new AiLowcodeModel();
            model.setModelSchema("{\"tableName\":\"users\",\"fields\":[{\"field\":\"id\"},{\"field\":\"level\"}]}");
            when(models.selectByModelCode(7L, "users")).thenReturn(model);
        }

        @Override
        public void close() {
            context.close();
            session.close();
        }
    }
}
