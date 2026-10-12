package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AggregateEngine")
@Tag("dev")
class AggregateEngineTest {

    private final AggregateEngine engine = new AggregateEngine();

    private List<Map<String, Object>> row(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) m.put((String) kvs[i], kvs[i + 1]);
        return List.of(m);
    }

    private List<Map<String, Object>> rows(List<Map<String, Object>>... lists) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (var l : lists) all.addAll(l);
        return all;
    }

    @Nested
    @DisplayName("basic aggregation")
    class BasicAggregation {
        @Test
        @DisplayName("SUM over multiple rows")
        void sum() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", null);
            var data = rows(row("amount", 100L), row("amount", 200L), row("amount", 50L));
            AggregateResult r = engine.execute(cfg, data);
            assertTrue(r.isSuccess());
            assertEquals(350L, r.getValue());
            assertEquals(3, r.getTotalRowCount());
            assertEquals(3, r.getMatchedRowCount());
        }

        @Test
        @DisplayName("COUNT over rows")
        void count() {
            var cfg = new AggregateConfig(AggregateFunction.COUNT, "items", "id", null);
            var data = rows(row("id", 1), row("id", 2), row("id", 3));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(3L, r.getValue());
        }

        @Test
        @DisplayName("AVG over rows")
        void avg() {
            var cfg = new AggregateConfig(AggregateFunction.AVG, "items", "score", null);
            var data = rows(row("score", 80L), row("score", 90L), row("score", 100L));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(90.0, r.doubleValue(), 0.001);
        }

        @Test
        @DisplayName("MAX over rows")
        void max() {
            var cfg = new AggregateConfig(AggregateFunction.MAX, "items", "price", null);
            var data = rows(row("price", 10L), row("price", 999L), row("price", 50L));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(999L, r.getValue());
        }

        @Test
        @DisplayName("MIN over rows")
        void min() {
            var cfg = new AggregateConfig(AggregateFunction.MIN, "items", "price", null);
            var data = rows(row("price", 10L), row("price", 999L), row("price", 5L));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(5L, r.getValue());
        }
    }

    @Nested
    @DisplayName("empty rows")
    class EmptyRows {
        @Test void sumReturnsZero() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.SUM, "r", "f", null), List.of());
            assertEquals(0L, r.getValue());
        }
        @Test void countReturnsZero() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.COUNT, "r", "f", null), List.of());
            assertEquals(0L, r.getValue());
        }
        @Test void avgReturnsZero() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.AVG, "r", "f", null), List.of());
            assertEquals(0.0, r.doubleValue(), 0.001);
        }
        @Test void maxReturnsNull() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.MAX, "r", "f", null), List.of());
            assertNull(r.getValue());
        }
        @Test void minReturnsNull() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.MIN, "r", "f", null), List.of());
            assertNull(r.getValue());
        }
    }

    @Nested
    @DisplayName("filter")
    class Filter {
        @Test
        @DisplayName("filter out rows where amount < 100")
        void filterAmount() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", "amount >= 100");
            var data = rows(row("amount", 50L), row("amount", 100L), row("amount", 200L));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(300L, r.getValue());
            assertEquals(3, r.getTotalRowCount());
            assertEquals(2, r.getMatchedRowCount());
        }

        @Test
        @DisplayName("filter excludes all rows")
        void filterExcludesAll() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", "amount > 9999");
            var data = rows(row("amount", 100L));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(0L, r.getValue());
            assertEquals(0, r.getMatchedRowCount());
        }

        @Test
        @DisplayName("no filter: all rows matched")
        void noFilter() {
            var cfg = new AggregateConfig(AggregateFunction.COUNT, "items", "id", null);
            var data = rows(row("id", 1), row("id", 2));
            AggregateResult r = engine.execute(cfg, data);
            assertEquals(2L, r.getValue());
            assertEquals(2, r.getMatchedRowCount());
        }
    }

    @Nested
    @DisplayName("AggregateResult convenience")
    class AggregateResultConvenience {
        @Test void longValue() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.SUM, "r", "f", null), row("f", 42L));
            assertEquals(42L, r.longValue());
        }
        @Test void doubleValue() {
            var r = engine.execute(new AggregateConfig(AggregateFunction.AVG, "r", "f", null),
                rows(row("f", 10L), row("f", 20L)));
            assertEquals(15.0, r.doubleValue(), 0.001);
        }
    }
}
