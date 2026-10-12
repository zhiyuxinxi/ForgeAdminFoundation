package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaErrorHandler")
@Tag("dev")
class FormulaErrorHandlerTest {

    @Nested
    @DisplayName("LENIENT mode (default)")
    class LenientMode {
        private final FormulaErrorHandler handler = new FormulaErrorHandler();

        @Test
        @DisplayName("returns HandleResult with fallback, does not throw")
        void returnsHandleResult() {
            Exception ex = new RuntimeException("test error");
            FormulaErrorHandler.HandleResult result = handler.handleError(
                "total", "price * qty", ex, 0L);

            assertNotNull(result);
            assertEquals("total", result.fieldName);
            assertEquals(0L, result.fallbackValue);
            assertTrue(result.errorMessage.contains("total"));
            assertTrue(result.errorMessage.contains("price * qty"));
        }

        @Test
        @DisplayName("null fallback is allowed")
        void nullFallback() {
            Exception ex = new RuntimeException("division by zero");
            FormulaErrorHandler.HandleResult result = handler.handleError(
                "avg", "total / count", ex, null);

            assertNull(result.fallbackValue);
            assertTrue(result.errorMessage.contains("avg"));
        }

        @Test
        @DisplayName("summarize aggregates multiple errors")
        void summarize() {
            List<FormulaErrorHandler.HandleResult> results = List.of(
                new FormulaErrorHandler.HandleResult("A", "err1", null),
                new FormulaErrorHandler.HandleResult("B", "err2", null)
            );
            String summary = handler.summarize(results);
            assertTrue(summary.contains("2 error"));
            assertTrue(summary.contains("A"));
            assertTrue(summary.contains("B"));
        }

        @Test
        @DisplayName("summarize with empty list")
        void summarizeEmpty() {
            assertEquals("No errors", handler.summarize(List.of()));
        }

        @Test
        @DisplayName("summarize with null")
        void summarizeNull() {
            assertEquals("No errors", handler.summarize(null));
        }
    }

    @Nested
    @DisplayName("STRICT mode")
    class StrictMode {
        private final FormulaErrorHandler handler =
            new FormulaErrorHandler(FormulaErrorHandler.Mode.STRICT);

        @Test
        @DisplayName("throws FormulaExecutionException immediately")
        void throwsImmediately() {
            Exception ex = new RuntimeException("critical error");
            assertThrows(FormulaExecutionException.class,
                () -> handler.handleError("field", "expr", ex, null));
        }
    }
}