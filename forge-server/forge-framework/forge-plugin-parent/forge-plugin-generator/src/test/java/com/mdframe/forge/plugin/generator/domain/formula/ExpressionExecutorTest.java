package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpressionExecutor")
@Tag("dev")
class ExpressionExecutorTest {

    private final ExpressionExecutor executor = new ExpressionExecutor();

    @Nested
    @DisplayName("execute")
    class Execute {
        @Test
        @DisplayName("simple arithmetic")
        void simpleArithmetic() {
            Object result = executor.execute("price * quantity",
                Map.of("price", 100L, "quantity", 3L));
            assertEquals(300L, result);
        }

        @Test
        @DisplayName("string concat via +")
        void stringConcat() {
            Object result = executor.execute("prefix + suffix",
                Map.of("prefix", "Hello", "suffix", "World"));
            assertEquals("HelloWorld", result);
        }

        @Test
        @DisplayName("ternary expression")
        void ternary() {
            Object result = executor.execute("amount > 100 ? amount * 0.9 : amount",
                Map.of("amount", 200L));
            assertEquals(180.0, ((Number)result).doubleValue(), 0.001);
        }

        @Test
        @DisplayName("ternary false branch")
        void ternaryFalse() {
            Object result = executor.execute("amount > 100 ? amount * 0.9 : amount",
                Map.of("amount", 50L));
            assertEquals(50L, result);
        }

        @Test
        @DisplayName("nested math functions")
        void mathFunctions() {
            Object result = executor.execute("math.abs(x) + math.abs(a) + math.abs(b)",
                Map.of("x", -5L, "a", 3L, "b", 7L));
            assertEquals(15L, result); // |-5| + |3| + |7| = 15;
        }

        @Test
        @DisplayName("registered string function")
        void registeredStringFunction() {
            ExpressionExecutor customExecutor = new ExpressionExecutor(
                new AviatorAdapter(), FormulaFunctionRegistry.builtin());
            Object result = customExecutor.execute("string.startsWith(name, 'For')",
                Map.of("name", "Forge"));
            assertEquals(Boolean.TRUE, result);
        }

        @Test
        @DisplayName("formula referencing computed value")
        void formulaReference() {
            // Simulates A = B + C where B and C are previously computed
            Object result = executor.execute("b + c",
                Map.of("b", 10L, "c", 20L));
            assertEquals(30L, result);
        }

        @Test
        @DisplayName("blank expression throws")
        void blankExpression() {
            assertThrows(FormulaExecutionException.class,
                () -> executor.execute("", Map.of()));
            assertThrows(FormulaExecutionException.class,
                () -> executor.execute("  ", Map.of()));
        }

        @Test
        @DisplayName("null expression throws")
        void nullExpression() {
            assertThrows(FormulaExecutionException.class,
                () -> executor.execute(null, Map.of()));
        }

        @Test
        @DisplayName("null variables throws NPE")
        void nullVariables() {
            assertThrows(NullPointerException.class,
                () -> executor.execute("1 + 1", null));
        }

        @Test
        @DisplayName("syntax error throws FormulaExecutionException")
        void syntaxError() {
            assertThrows(FormulaExecutionException.class,
                () -> executor.execute("a +", Map.of("a", 1)));
        }

        @Test
        @DisplayName("missing variable throws FormulaExecutionException")
        void missingVariable() {
            // Aviator throws when a variable is not in the context
            assertThrows(FormulaExecutionException.class,
                () -> executor.execute("a + b", Map.of("a", 1)));
        }
    }

    @Nested
    @DisplayName("executeCompiled")
    class ExecuteCompiled {
        @Test
        @DisplayName("pre-compiled expression executes correctly")
        void preCompiled() {
            AviatorAdapter adapter = new AviatorAdapter();
            var compiled = adapter.compile("x * y");
            Object result = executor.executeCompiled(compiled, Map.of("x", 5L, "y", 4L));
            assertEquals(20L, result);
        }
    }
}
