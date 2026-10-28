package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.AggregateEngine;
import com.mdframe.forge.plugin.generator.domain.formula.ExecutionResult;
import com.mdframe.forge.plugin.generator.domain.formula.ExpressionExecutor;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaDependencyAnalyzer;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaErrorHandler;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionStep;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaExecutionEngine LOOKUP")
@Tag("dev")
class FormulaExecutionEngineLookupTest {

    private FormulaConfig lookup(String sourceField, Object notFoundValue) {
        return FormulaConfig.builder()
                .type(FormulaType.LOOKUP)
                .mode(FormulaMode.VIRTUAL)
                .dependsOn(List.of(sourceField))
                .lookup(new LookupConfig(
                        "customer_owner",
                        "sys_user",
                        sourceField,
                        "id",
                        "realName",
                        notFoundValue))
                .build();
    }

    private FormulaExecutionEngine engine(FormulaLookupResolver resolver) {
        return new FormulaExecutionEngine(
                new ExpressionExecutor(),
                new FormulaDependencyAnalyzer(),
                new FormulaErrorHandler(),
                new AggregateEngine(),
                resolver);
    }

    private Map<String, FormulaConfig> formulas(FormulaConfig config) {
        Map<String, FormulaConfig> result = new LinkedHashMap<>();
        result.put("ownerName", config);
        return result;
    }

    @Test
    @DisplayName("LOOKUP match returns target field value")
    void lookupMatchReturnsTargetField() {
        FormulaExecutionEngine engine = engine(new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "realName", "Alice"),
                Map.of("id", "u2", "realName", "Bob"))));

        ExecutionResult result = engine.execute(
                formulas(lookup("ownerUserId", "NOT_ASSIGNED")),
                Map.of("ownerUserId", "u2"),
                FormulaTraceOptions.debug());

        assertTrue(result.isSuccess(), "Errors: " + result.getErrors());
        assertEquals("Bob", result.getResult("ownerName"));
        assertTrue(result.hasTrace());
        FormulaExecutionStep step = result.getTrace().getSteps().get(0);
        assertEquals("LOOKUP", step.getFormulaType());
        assertEquals("customer_owner", step.getMetadata().get("relationCode"));
        assertEquals("sys_user", step.getMetadata().get("targetObjectCode"));
        assertEquals("ownerUserId", step.getMetadata().get("sourceField"));
        assertEquals("id", step.getMetadata().get("targetField"));
        assertEquals("realName", step.getMetadata().get("returnField"));
        assertEquals(Boolean.TRUE, step.getMetadata().get("lookupMatched"));
    }

    @Test
    @DisplayName("LOOKUP not found returns notFoundValue without error")
    void lookupNotFoundReturnsFallback() {
        FormulaExecutionEngine engine = engine(new FormulaLookupResolver((config, context) -> List.of(
                Map.of("id", "u1", "realName", "Alice"))));

        ExecutionResult result = engine.execute(
                formulas(lookup("ownerUserId", "NOT_ASSIGNED")),
                Map.of("ownerUserId", "u9"),
                FormulaTraceOptions.debug());

        assertTrue(result.isSuccess(), "Errors: " + result.getErrors());
        assertFalse(result.hasError("ownerName"));
        assertEquals("NOT_ASSIGNED", result.getResult("ownerName"));
        assertEquals(Boolean.FALSE, result.getTrace().getSteps().get(0).getMetadata().get("lookupMatched"));
    }

    @Test
    @DisplayName("LOOKUP resolver failure is degraded and traced")
    void lookupResolverFailureIsTraced() {
        FormulaExecutionEngine engine = engine(new FormulaLookupResolver((config, context) -> {
            throw new IllegalStateException("relation path invalid");
        }));

        ExecutionResult result = engine.execute(
                formulas(lookup("ownerUserId", "NOT_ASSIGNED")),
                Map.of("ownerUserId", "u1"),
                FormulaTraceOptions.debug());

        assertFalse(result.isSuccess());
        assertTrue(result.hasError("ownerName"));
        // 引擎失败契约：失败字段不写 result（null），回退值只出现在 trace 步骤 output
        assertNull(result.getResult("ownerName"));
        assertEquals("NOT_ASSIGNED", result.getTrace().getSteps().get(0).getOutput());
        assertFalse(result.getTrace().getSteps().get(0).isSuccess());
        assertEquals("customer_owner", result.getTrace().getSteps().get(0).getMetadata().get("relationCode"));
    }
}
