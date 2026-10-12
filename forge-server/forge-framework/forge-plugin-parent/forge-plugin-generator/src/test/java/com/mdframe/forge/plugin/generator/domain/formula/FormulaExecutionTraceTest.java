package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FormulaExecutionTrace")
@Tag("dev")
class FormulaExecutionTraceTest {

    @Test
    @DisplayName("build trace with execution plan and immutable steps")
    void buildTrace() {
        FormulaExecutionStep step = FormulaExecutionStep.builder()
                .fieldCode("total")
                .formulaType("CALC")
                .expression("price * quantity")
                .input(Map.of("price", 100L, "quantity", 3L))
                .output(300L)
                .elapsedMs(4L)
                .success(true)
                .putMetadata("mode", "STORED")
                .build();

        FormulaExecutionTrace trace = FormulaExecutionTrace.builder()
                .traceId("trace-1")
                .executionPlan(List.of("total"))
                .addStep(step)
                .elapsedMs(5L)
                .build();

        assertEquals("trace-1", trace.getTraceId());
        assertEquals(List.of("total"), trace.getExecutionPlan());
        assertEquals(1, trace.getSteps().size());
        assertEquals(300L, trace.getSteps().get(0).getOutput());
        assertEquals("STORED", trace.getSteps().get(0).getMetadata().get("mode"));
        assertTrue(trace.isSuccess());
        assertThrows(UnsupportedOperationException.class, () -> trace.getSteps().add(step));
        assertThrows(UnsupportedOperationException.class, () -> step.getInput().put("x", 1));
    }

    @Test
    @DisplayName("failed step makes trace unsuccessful")
    void failedStep() {
        FormulaExecutionStep step = FormulaExecutionStep.builder()
                .fieldCode("bad")
                .formulaType("CALC")
                .expression("a +")
                .success(false)
                .errorMessage("syntax error")
                .build();

        FormulaExecutionTrace trace = FormulaExecutionTrace.builder()
                .traceId("trace-2")
                .addStep(step)
                .addError("bad syntax")
                .build();

        assertFalse(trace.isSuccess());
        assertEquals("syntax error", trace.getSteps().get(0).getErrorMessage());
        assertEquals(List.of("bad syntax"), trace.getErrors());
    }

    @Test
    @DisplayName("debug options imply trace and snapshots")
    void debugOptions() {
        FormulaTraceOptions disabled = FormulaTraceOptions.disabled();
        assertFalse(disabled.isEnabled());
        assertFalse(disabled.isIncludeInputSnapshot());
        assertFalse(disabled.isIncludeOutputValue());

        FormulaTraceOptions debug = FormulaTraceOptions.debug();
        assertTrue(debug.isEnabled());
        assertTrue(debug.isDebugMode());
        assertTrue(debug.isIncludeInputSnapshot());
        assertTrue(debug.isIncludeOutputValue());
    }
}
