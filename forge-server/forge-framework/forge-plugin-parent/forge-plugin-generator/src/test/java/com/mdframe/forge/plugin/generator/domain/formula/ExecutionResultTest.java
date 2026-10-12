package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ExecutionResult")
@Tag("dev")
class ExecutionResultTest {

    @Test
    @DisplayName("default result remains trace-free for V1 compatibility")
    void defaultTraceFree() {
        ExecutionResult result = ExecutionResult.builder()
                .success(true)
                .putResult("total", 300L)
                .elapsedMs(2L)
                .build();

        assertTrue(result.isSuccess());
        assertEquals(300L, result.getResult("total"));
        assertEquals(List.of("total"), result.getExecutedFields());
        assertNull(result.getTrace());
        assertFalse(result.hasTrace());
    }

    @Test
    @DisplayName("builder can attach optional trace")
    void attachTrace() {
        FormulaExecutionTrace trace = FormulaExecutionTrace.builder()
                .traceId("trace-1")
                .executionPlan(List.of("total"))
                .addStep(FormulaExecutionStep.builder()
                        .fieldCode("total")
                        .formulaType("CALC")
                        .expression("price * quantity")
                        .output(300L)
                        .success(true)
                        .build())
                .elapsedMs(3L)
                .build();

        ExecutionResult result = ExecutionResult.builder()
                .success(true)
                .putResult("total", 300L)
                .trace(trace)
                .build();

        assertTrue(result.hasTrace());
        assertSame(trace, result.getTrace());
        assertEquals("trace-1", result.getTrace().getTraceId());
    }
}
