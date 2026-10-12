package com.mdframe.forge.plugin.generator.domain.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaConfig")
@Tag("dev")
class FormulaConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("legacy CALC json remains compatible")
    void legacyCalcJsonCompatible() throws Exception {
        String json = """
            {
              "type": "CALC",
              "mode": "STORED",
              "expression": "price * quantity",
              "dependsOn": ["price", "quantity"]
            }
            """;

        FormulaConfig config = objectMapper.readValue(json, FormulaConfig.class);

        assertTrue(config.isCalc());
        assertTrue(config.isStored());
        assertEquals("price * quantity", config.getExpression());
        assertEquals(List.of("price", "quantity"), config.getDependsOn());
        assertNull(config.getLookup());
        assertTrue(config.getFunctionRefs().isEmpty());
    }

    @Test
    @DisplayName("legacy AGGREGATE json remains compatible")
    void legacyAggregateJsonCompatible() throws Exception {
        String json = """
            {
              "type": "AGGREGATE",
              "mode": "STORED",
              "aggregate": {
                "function": "SUM",
                "relationCode": "items",
                "targetField": "amount",
                "filter": "amount > 0"
              }
            }
            """;

        FormulaConfig config = objectMapper.readValue(json, FormulaConfig.class);

        assertTrue(config.isAggregate());
        assertNotNull(config.getAggregate());
        assertEquals(AggregateFunction.SUM, config.getAggregate().getFunction());
        assertEquals("items", config.getAggregate().getRelationCode());
        assertEquals("amount", config.getAggregate().getTargetField());
    }

    @Test
    @DisplayName("legacy CONDITIONAL json remains compatible")
    void legacyConditionalJsonCompatible() throws Exception {
        String json = """
            {
              "type": "CONDITIONAL",
              "mode": "VIRTUAL",
              "condition": {
                "expression": "amount > 1000",
                "trueValue": "VIP",
                "falseValue": "NORMAL"
              }
            }
            """;

        FormulaConfig config = objectMapper.readValue(json, FormulaConfig.class);

        assertTrue(config.isConditional());
        assertTrue(config.isVirtual());
        assertNotNull(config.getCondition());
        assertEquals("amount > 1000", config.getCondition().getExpression());
        assertEquals("VIP", config.getCondition().getTrueValue());
        assertEquals("NORMAL", config.getCondition().getFalseValue());
    }

    @Test
    @DisplayName("LOOKUP requires lookup config")
    void lookupRequiresLookupConfig() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            FormulaConfig.builder()
                .type(FormulaType.LOOKUP)
                .mode(FormulaMode.VIRTUAL)
                .dependsOn(List.of("ownerUserId"))
                .build());

        assertTrue(error.getMessage().contains("lookup config required"));
    }

    @Test
    @DisplayName("LOOKUP config can be built and deserialized")
    void lookupConfigCanBeBuiltAndDeserialized() throws Exception {
        String json = """
            {
              "type": "LOOKUP",
              "mode": "VIRTUAL",
              "dependsOn": ["ownerUserId"],
              "lookup": {
                "relationCode": "customer_owner",
                "targetObjectCode": "sys_user",
                "sourceField": "ownerUserId",
                "targetField": "id",
                "returnField": "realName",
                "notFoundValue": "未分配"
              },
              "functionRefs": ["LOWER"],
              "rule": { "mode": "expr" }
            }
            """;

        FormulaConfig config = objectMapper.readValue(json, FormulaConfig.class);

        assertTrue(config.isLookup());
        assertEquals(List.of("ownerUserId"), config.getDependsOn());
        assertEquals("customer_owner", config.getLookup().getRelationCode());
        assertEquals("sys_user", config.getLookup().getTargetObjectCode());
        assertEquals("ownerUserId", config.getLookup().getSourceField());
        assertEquals("id", config.getLookup().getTargetField());
        assertEquals("realName", config.getLookup().getReturnField());
        assertEquals("未分配", config.getLookup().getNotFoundValue());
        assertEquals(List.of("LOWER"), config.getFunctionRefs());
        assertEquals("expr", config.getRule().get("mode"));
    }

    @Test
    @DisplayName("cross object config only accepts one-hop path")
    void crossObjectPathMustBeOneHop() {
        assertThrows(IllegalArgumentException.class, () ->
            new CrossObjectConfig("customer.owner.level", "order_customer", "crm_customer", "level", null));
        assertThrows(IllegalArgumentException.class, () ->
            new CrossObjectConfig("customer", "order_customer", "crm_customer", "level", null));
    }

    @Test
    @DisplayName("cross object config defaults recompute mode to async")
    void crossObjectDefaultsRecomputeModeToAsync() {
        CrossObjectConfig crossObject = new CrossObjectConfig(
            "customer.level", "order_customer", "crm_customer", "level", null);

        FormulaConfig config = FormulaConfig.builder()
            .type(FormulaType.CALC)
            .mode(FormulaMode.STORED)
            .expression("customer.level")
            .dependsOn(List.of("customerId"))
            .crossObject(crossObject)
            .functionRefs(List.of("LOOKUP_SAFE"))
            .rule(Map.of("operator", "EQ"))
            .build();

        assertTrue(config.hasCrossObject());
        assertEquals("customer", config.getCrossObject().getRelationAlias());
        assertEquals("level", config.getCrossObject().getPathField());
        assertEquals(CrossObjectRecomputeMode.ASYNC, config.getCrossObject().getRecomputeMode());
        assertTrue(config.hasFunctionRefs());
        assertTrue(config.hasRule());
    }
}
