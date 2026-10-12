package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VirtualFormulaRuntime")
@Tag("dev")
class VirtualFormulaRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VirtualFormulaRuntime runtime =
        new VirtualFormulaRuntime(new FormulaExecutionEngine(), objectMapper);

    private LowcodeModelSchema schemaWithFields(List<LowcodeFieldSchema> fields) {
        LowcodeModelSchema s = new LowcodeModelSchema();
        s.setFields(fields);
        return s;
    }

    private LowcodeFieldSchema plainField(String name) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        return f;
    }

    private LowcodeFieldSchema formulaField(String name, String expression, List<String> dependsOn, FormulaMode mode) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "CALC");
        fc.put("mode", mode.name());
        fc.put("expression", expression);
        fc.put("dependsOn", dependsOn != null ? dependsOn : List.of());
        f.setFormulaConfig(fc);
        return f;
    }

    private LowcodeFieldSchema crossObjectFormulaField(String name) {
        LowcodeFieldSchema f = formulaField(name, "customer.level", List.of("customerId"), FormulaMode.VIRTUAL);
        Map<String, Object> crossObject = new LinkedHashMap<>();
        crossObject.put("path", "customer.level");
        crossObject.put("relationCode", "customer");
        crossObject.put("targetObjectCode", "crm_customer");
        crossObject.put("returnField", "level");
        f.getFormulaConfig().put("crossObject", crossObject);
        return f;
    }

    private FormulaRuntimeContext ctx() {
        return new FormulaRuntimeContext(1L, "default", "testObj", Map.of());
    }

    @Nested
    @DisplayName("extraction via calculate")
    class Extraction {
        @Test
        @DisplayName("only VIRTUAL formulas are calculated, STORED skipped")
        void onlyVirtualCalculated() {
            LowcodeFieldSchema f1 = formulaField("displayTotal", "price * qty", List.of("price", "qty"), FormulaMode.VIRTUAL);
            LowcodeFieldSchema f2 = formulaField("storedTotal", "price * qty", List.of("price", "qty"), FormulaMode.STORED);

            LowcodeModelSchema schema = schemaWithFields(List.of(f1, f2));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 10L); record.put("qty", 5L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(1, results.size());
            // VIRTUAL computed
            assertEquals(50L, results.get(0).get("displayTotal"));
            // STORED not computed
            assertNull(results.get(0).get("storedTotal"));
        }

        @Test
        @DisplayName("no virtual formulas returns records unchanged")
        void noVirtual() {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("name", "test");
            List<Map<String, Object>> results = runtime.calculate(
                List.of(record), schemaWithFields(List.of(plainField("a"))), ctx());
            assertEquals(1, results.size());
            assertEquals("test", results.get(0).get("name"));
        }
    }

    @Nested
    @DisplayName("calculation")
    class Calculation {
        @Test
        @DisplayName("simple virtual formula: displayTotal = price * quantity")
        void simpleVirtual() {
            LowcodeFieldSchema totalField = formulaField("displayTotal", "price * quantity",
                List.of("price", "quantity"), FormulaMode.VIRTUAL);
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"), totalField));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 100L); record.put("quantity", 5L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(1, results.size());
            assertEquals(500L, results.get(0).get("displayTotal"));
        }

        @Test
        @DisplayName("virtual formula appends to record, original fields preserved")
        void preservesOriginalFields() {
            LowcodeFieldSchema vField = formulaField("taxedPrice", "price * 1.1",
                List.of("price"), FormulaMode.VIRTUAL);
            LowcodeModelSchema schema = schemaWithFields(List.of(plainField("price"), vField));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("id", 42L); record.put("price", 100L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(42L, results.get(0).get("id"));
            assertEquals(100L, results.get(0).get("price"));
            assertEquals(110.0, ((Number)results.get(0).get("taxedPrice")).doubleValue(), 0.001);
        }

        @Test
        @DisplayName("multiple records")
        void multipleRecords() {
            LowcodeFieldSchema vField = formulaField("taxedPrice", "price * 1.1",
                List.of("price"), FormulaMode.VIRTUAL);
            LowcodeModelSchema schema = schemaWithFields(List.of(plainField("price"), vField));

            Map<String, Object> r1 = new LinkedHashMap<>(); r1.put("price", 100L);
            Map<String, Object> r2 = new LinkedHashMap<>(); r2.put("price", 200L);

            List<Map<String, Object>> results = runtime.calculate(List.of(r1, r2), schema, ctx());
            assertEquals(110.0, ((Number)results.get(0).get("taxedPrice")).doubleValue(), 0.001);
            assertEquals(220.0, ((Number)results.get(1).get("taxedPrice")).doubleValue(), 0.001);
        }

        @Test
        @DisplayName("null records returns empty")
        void nullRecords() {
            List<Map<String, Object>> result = runtime.calculate(null, schemaWithFields(List.of()), ctx());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("empty records returns empty")
        void emptyRecords() {
            List<Map<String, Object>> result = runtime.calculate(List.of(), schemaWithFields(List.of()), ctx());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("cross-object virtual formula uses batch prefetch")
        void crossObjectVirtualUsesBatchPrefetch() {
            java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
            FormulaCrossObjectResolver resolver = new FormulaCrossObjectResolver(
                "customerId",
                "id",
                (config, sourceValues, context) -> {
                    calls.incrementAndGet();
                    assertEquals(2, sourceValues.size());
                    return List.of(
                        Map.of("id", "c1", "level", "VIP"),
                        Map.of("id", "c2", "level", "NORMAL"));
                });
            VirtualFormulaRuntime crossRuntime =
                new VirtualFormulaRuntime(new FormulaExecutionEngine(), objectMapper, null, resolver);
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("customerId"), crossObjectFormulaField("customerLevel")));

            Map<String, Object> r1 = new LinkedHashMap<>(); r1.put("customerId", "c1");
            Map<String, Object> r2 = new LinkedHashMap<>(); r2.put("customerId", "c2");
            List<Map<String, Object>> results = crossRuntime.calculate(List.of(r1, r2), schema, ctx());

            assertEquals(1, calls.get());
            assertEquals("VIP", results.get(0).get("customerLevel"));
            assertEquals("NORMAL", results.get(1).get("customerLevel"));
            assertFalse(results.get(0).containsKey("customer"));
        }
    }
}
