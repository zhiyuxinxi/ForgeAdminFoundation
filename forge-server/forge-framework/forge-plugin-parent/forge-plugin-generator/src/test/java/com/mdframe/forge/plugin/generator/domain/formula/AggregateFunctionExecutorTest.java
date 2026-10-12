package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AggregateFunctionExecutor")
@Tag("dev")
class AggregateFunctionExecutorTest {

    private final AggregateFunctionExecutor executor = new AggregateFunctionExecutor();

    private List<Map<String, Object>> rows(Object... kvs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2)
            row.put((String) kvs[i], kvs[i + 1]);
        return List.of(row);
    }

    private List<Map<String, Object>> multiRows(List<Map<String, Object>>... rows) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (List<Map<String, Object>> r : rows) all.addAll(r);
        return all;
    }

    @Nested
    @DisplayName("SUM")
    class Sum {
        @Test void singleRow() { assertEquals(100L, executor.execute(AggregateFunction.SUM, "amt", rows("amt", 100L))); }
        @Test void multipleRows() {
            List<Map<String, Object>> data = multiRows(rows("amt", 100L), rows("amt", 200L), rows("amt", 50L));
            assertEquals(350L, executor.execute(AggregateFunction.SUM, "amt", data));
        }
        @Test void emptyRows() { assertEquals(0L, executor.execute(AggregateFunction.SUM, "amt", List.of())); }
        @Test void nullValues() {
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(Map.of("amt", 100L));
            data.add(Map.of("other", 1));
            data.add(Map.of("amt", 50L));
            assertEquals(150L, executor.execute(AggregateFunction.SUM, "amt", data));
        }
    }

    @Nested
    @DisplayName("COUNT")
    class Count {
        @Test void countsAllRows() {
            List<Map<String, Object>> data = multiRows(rows("a", 1), rows("b", 2), rows("c", 3));
            assertEquals(3L, executor.execute(AggregateFunction.COUNT, "any", data));
        }
        @Test void emptyRows() { assertEquals(0L, executor.execute(AggregateFunction.COUNT, "any", List.of())); }
    }

    @Nested
    @DisplayName("AVG")
    class Avg {
        @Test void basic() {
            List<Map<String, Object>> data = multiRows(rows("v", 10L), rows("v", 20L), rows("v", 30L));
            assertEquals(20.0, ((Number) executor.execute(AggregateFunction.AVG, "v", data)).doubleValue(), 0.001);
        }
        @Test void emptyRows() {
            assertEquals(0.0, ((Number) executor.execute(AggregateFunction.AVG, "v", List.of())).doubleValue(), 0.001);
        }
    }

    @Nested
    @DisplayName("MAX")
    class Max {
        @Test void basic() {
            List<Map<String, Object>> data = multiRows(rows("v", 10L), rows("v", 100L), rows("v", 50L));
            assertEquals(100L, executor.execute(AggregateFunction.MAX, "v", data));
        }
        @Test void emptyRows() { assertNull(executor.execute(AggregateFunction.MAX, "v", List.of())); }
    }

    @Nested
    @DisplayName("MIN")
    class Min {
        @Test void basic() {
            List<Map<String, Object>> data = multiRows(rows("v", 10L), rows("v", 100L), rows("v", 5L));
            assertEquals(5L, executor.execute(AggregateFunction.MIN, "v", data));
        }
        @Test void emptyRows() { assertNull(executor.execute(AggregateFunction.MIN, "v", List.of())); }
    }
}
