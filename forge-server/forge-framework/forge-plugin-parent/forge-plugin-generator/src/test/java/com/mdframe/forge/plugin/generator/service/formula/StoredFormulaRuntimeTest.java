package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StoredFormulaRuntime")
@Tag("dev")
class StoredFormulaRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StoredFormulaRuntime runtime =
        new StoredFormulaRuntime(new FormulaExecutionEngine(), objectMapper);

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

    private FormulaRuntimeContext ctx() {
        return new FormulaRuntimeContext(1L, "default", "testObj", Map.of());
    }

    @Nested
    @DisplayName("extraction via calculate")
    class Extraction {
        @Test
        @DisplayName("only STORED formulas are calculated, VIRTUAL skipped")
        void onlyStoredCalculated() {
            LowcodeFieldSchema f1 = formulaField("total", "price * qty", List.of("price", "qty"), FormulaMode.STORED);
            LowcodeFieldSchema f2 = formulaField("taxDisplay", "tax * 100", List.of("tax"), FormulaMode.VIRTUAL);
            LowcodeFieldSchema f3 = plainField("name");

            LowcodeModelSchema schema = schemaWithFields(List.of(f1, f2, f3));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 10L); record.put("qty", 5L); record.put("tax", 0L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(1, results.size());
            // STORED formula computed
            assertEquals(50L, results.get(0).get("total"));
            // VIRTUAL formula NOT computed (no displayTax in result from stored runtime)
            assertNull(results.get(0).get("taxDisplay"));
        }

        @Test
        @DisplayName("no formulas returns records unchanged")
        void noFormulas() {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("name", "test");
            List<Map<String, Object>> results = runtime.calculate(
                List.of(record), schemaWithFields(List.of(plainField("a"), plainField("b"))), ctx());
            assertEquals(1, results.size());
            assertEquals("test", results.get(0).get("name"));
        }
    }

    @Nested
    @DisplayName("calculation")
    class Calculation {
        @Test
        @DisplayName("simple stored formula: total = price * quantity")
        void simpleCalc() {
            LowcodeFieldSchema totalField = formulaField("total", "price * quantity",
                List.of("price", "quantity"), FormulaMode.STORED);
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"), totalField));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 100L); record.put("quantity", 3L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(1, results.size());
            assertEquals(300L, results.get(0).get("total"));
        }

        @Test
        @DisplayName("multiple records")
        void multipleRecords() {
            LowcodeFieldSchema totalField = formulaField("total", "price * quantity",
                List.of("price", "quantity"), FormulaMode.STORED);
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"), totalField));

            Map<String, Object> r1 = new LinkedHashMap<>();
            r1.put("price", 10L); r1.put("quantity", 2L);
            Map<String, Object> r2 = new LinkedHashMap<>();
            r2.put("price", 5L); r2.put("quantity", 4L);

            List<Map<String, Object>> results = runtime.calculate(List.of(r1, r2), schema, ctx());
            assertEquals(2, results.size());
            assertEquals(20L, results.get(0).get("total"));
            assertEquals(20L, results.get(1).get("total"));
        }

        @Test
        @DisplayName("chained stored formulas: subtotal -> tax -> grandTotal")
        void chainedFormulas() {
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"),
                formulaField("subtotal", "price * quantity", List.of("price", "quantity"), FormulaMode.STORED),
                formulaField("tax", "subtotal * 0.1", List.of("subtotal"), FormulaMode.STORED),
                formulaField("grandTotal", "subtotal + tax", List.of("subtotal", "tax"), FormulaMode.STORED)
            ));

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 100L); record.put("quantity", 2L);

            List<Map<String, Object>> results = runtime.calculate(List.of(record), schema, ctx());
            assertEquals(200L, results.get(0).get("subtotal"));
            assertEquals(20.0, ((Number)results.get(0).get("tax")).doubleValue(), 0.001);
            assertEquals(220.0, ((Number)results.get(0).get("grandTotal")).doubleValue(), 0.001);
        }

        @Test
        @DisplayName("null records returns empty list")
        void nullRecords() {
            List<Map<String, Object>> result = runtime.calculate(null, schemaWithFields(List.of()), ctx());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("empty records returns empty list")
        void emptyRecords() {
            List<Map<String, Object>> result = runtime.calculate(List.of(), schemaWithFields(List.of()), ctx());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null schema returns records unchanged")
        void nullSchema() {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("price", 100L);
            List<Map<String, Object>> results = runtime.calculate(List.of(record), null, ctx());
            assertEquals(1, results.size());
            assertEquals(100L, results.get(0).get("price"));
        }
    }

    @Nested
    @DisplayName("parseFormulaConfig")
    class ParseConfig {
        @Test
        @DisplayName("parses CALC formula correctly")
        void parseCalc() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "a + b"); fc.put("dependsOn", List.of("a", "b"));
            FormulaConfig config = runtime.parseFormulaConfig(fc);
            assertNotNull(config);
            assertEquals(FormulaType.CALC, config.getType());
            assertEquals("a + b", config.getExpression());
        }

        @Test
        @DisplayName("parses AGGREGATE formula correctly")
        void parseAggregate() {
            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("function", "SUM"); agg.put("relationCode", "10"); agg.put("targetField", "amount");
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE"); fc.put("mode", "STORED");
            fc.put("expression", ""); fc.put("dependsOn", List.of());
            fc.put("aggregate", agg);

            FormulaConfig config = runtime.parseFormulaConfig(fc);
            assertNotNull(config);
            assertEquals(FormulaType.AGGREGATE, config.getType());
            assertNotNull(config.getAggregate());
        }
    }
}