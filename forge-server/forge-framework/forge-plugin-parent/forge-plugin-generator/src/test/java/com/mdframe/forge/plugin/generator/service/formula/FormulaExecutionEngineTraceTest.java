package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.ExecutionResult;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionStep;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaExecutionEngine trace")
@Tag("dev")
class FormulaExecutionEngineTraceTest {

    private final FormulaExecutionEngine engine = new FormulaExecutionEngine();

    @Test
    @DisplayName("debug trace captures plan, input and output")
    void debugTrace() {
        Map<String, FormulaConfig> formulas = formulaMap("total",
                calc("price * quantity", List.of("price", "quantity")));

        ExecutionResult result = engine.execute(formulas,
                Map.of("price", 100L, "quantity", 3L),
                FormulaTraceOptions.debug());

        assertTrue(result.isSuccess(), "Errors: " + result.getErrors());
        assertTrue(result.hasTrace());
        assertNotNull(result.getTrace().getTraceId());
        assertEquals(List.of("total"), result.getTrace().getExecutionPlan());
        assertEquals(1, result.getTrace().getSteps().size());
        FormulaExecutionStep step = result.getTrace().getSteps().get(0);
        assertEquals("total", step.getFieldCode());
        assertEquals("CALC", step.getFormulaType());
        assertEquals(100L, step.getInput().get("price"));
        assertEquals(300L, step.getOutput());
        assertTrue(step.isSuccess());
    }

    @Test
    @DisplayName("disabled trace keeps V1 result trace-free")
    void disabledTrace() {
        Map<String, FormulaConfig> formulas = formulaMap("total",
                calc("price * quantity", List.of("price", "quantity")));

        ExecutionResult result = engine.execute(formulas,
                Map.of("price", 100L, "quantity", 3L));

        assertTrue(result.isSuccess(), "Errors: " + result.getErrors());
        assertFalse(result.hasTrace());
        assertNull(result.getTrace());
    }

    private FormulaConfig calc(String expression, List<String> dependsOn) {
        return FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression(expression)
                .dependsOn(dependsOn)
                .build();
    }

    private Map<String, FormulaConfig> formulaMap(String fieldCode, FormulaConfig config) {
        Map<String, FormulaConfig> formulas = new LinkedHashMap<>();
        formulas.put(fieldCode, config);
        return formulas;
    }
}
