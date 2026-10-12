package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaCrossObjectResolver")
@Tag("dev")
class FormulaCrossObjectResolverTest {

    private CrossObjectConfig config() {
        return new CrossObjectConfig("customer.level", "customer", "crm_customer", "level", null);
    }

    private FormulaConfig formula(CrossObjectConfig config) {
        return FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.VIRTUAL)
                .expression("customer.level")
                .dependsOn(List.of("customerId"))
                .crossObject(config)
                .build();
    }

    @Test
    @DisplayName("one-hop path parses relation alias and field")
    void oneHopPathParses() {
        CrossObjectPath path = CrossObjectPath.parse("customer.level");

        assertEquals("customer", path.getRelationAlias());
        assertEquals("level", path.getFieldCode());
        assertEquals("customer.level", path.asExpressionPath());
    }

    @Test
    @DisplayName("multi-hop path is rejected")
    void multiHopPathRejected() {
        assertThrows(IllegalArgumentException.class, () -> CrossObjectPath.parse("customer.owner.level"));
        assertThrows(IllegalArgumentException.class, () -> CrossObjectPath.parse("customer"));
    }

    @Test
    @DisplayName("single record resolve returns target field")
    void singleRecordResolveReturnsTargetField() {
        FormulaCrossObjectResolver resolver = new FormulaCrossObjectResolver(
                "customerId",
                "id",
                (config, sourceValues, context) -> List.of(Map.of("id", "c1", "level", "VIP")));

        Object value = resolver.resolve(config(), Map.of("customerId", "c1"), ctx());

        assertEquals("VIP", value);
    }

    @Test
    @DisplayName("batch prefetch uses one provider call for multiple records")
    @SuppressWarnings("unchecked")
    void batchPrefetchUsesOneProviderCall() {
        AtomicInteger calls = new AtomicInteger();
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

        Map<String, Object> first = new LinkedHashMap<>(Map.of("customerId", "c1"));
        Map<String, Object> second = new LinkedHashMap<>(Map.of("customerId", "c2"));
        resolver.prefetch(Map.of("customerLevel", formula(config())), List.of(first, second), ctx());

        assertEquals(1, calls.get());
        assertEquals("VIP", ((Map<String, Object>) first.get("customer")).get("level"));
        assertEquals("NORMAL", ((Map<String, Object>) second.get("customer")).get("level"));
    }

    private FormulaRuntimeContext ctx() {
        return new FormulaRuntimeContext(1L, "default", "order", Map.of());
    }
}
