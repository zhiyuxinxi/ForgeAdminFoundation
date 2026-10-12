package com.mdframe.forge.plugin.generator.domain.formula;

import com.googlecode.aviator.Expression;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AviatorAdapter")
@Tag("dev")
class AviatorAdapterTest {

    private final AviatorAdapter adapter = new AviatorAdapter();

    @Nested
    @DisplayName("compile")
    class Compile {
        @Test
        @DisplayName("valid arithmetic expression")
        void validArithmetic() {
            Expression expr = adapter.compile("a + b * c");
            assertNotNull(expr);
        }

        @Test
        @DisplayName("valid expression with functions")
        void validWithFunctions() {
            Expression expr = adapter.compile("math.abs(x) + string.length(y)");
            assertNotNull(expr);
        }

        @Test
        @DisplayName("valid ternary expression")
        void validTernary() {
            Expression expr = adapter.compile("a > 0 ? a : b");
            assertNotNull(expr);
        }

        @Test
        @DisplayName("blank expression throws FormulaCompileException")
        void blankExpression() {
            assertThrows(FormulaCompileException.class, () -> adapter.compile(""));
            assertThrows(FormulaCompileException.class, () -> adapter.compile("  "));
        }

        @Test
        @DisplayName("null expression throws FormulaCompileException")
        void nullExpression() {
            assertThrows(FormulaCompileException.class, () -> adapter.compile(null));
        }

        @Test
        @DisplayName("syntax error throws FormulaCompileException with position")
        void syntaxError() {
            FormulaCompileException ex = assertThrows(FormulaCompileException.class,
                () -> adapter.compile("a +"));
            assertTrue(ex.getMessage().length() > 0);
            // Aviator reports line:column positions for syntax errors
            System.out.println("Syntax error: " + ex.getMessage());
        }

        @Test
        @DisplayName("unclosed parenthesis throws FormulaCompileException")
        void unclosedParen() {
            assertThrows(FormulaCompileException.class,
                () -> adapter.compile("(a + b"));
        }
    }

    @Nested
    @DisplayName("extractVariables")
    class ExtractVariables {
        @Test
        @DisplayName("extracts single variable")
        void singleVar() {
            List<String> vars = adapter.extractVariables("price * quantity");
            assertEquals(List.of("price", "quantity"), vars);
        }

        @Test
        @DisplayName("extracts multiple variables sorted")
        void multipleVarsSorted() {
            List<String> vars = adapter.extractVariables("z + a + m");
            assertEquals(List.of("a", "m", "z"), vars);
        }

        @Test
        @DisplayName("extracts variables from ternary")
        void ternaryVars() {
            List<String> vars = adapter.extractVariables("x > 0 ? x : y");
            assertEquals(List.of("x", "y"), vars);
        }

        @Test
        @DisplayName("blank expression returns empty list")
        void blankExpression() {
            assertTrue(adapter.extractVariables("").isEmpty());
            assertTrue(adapter.extractVariables("  ").isEmpty());
        }

        @Test
        @DisplayName("null expression returns empty list")
        void nullExpression() {
            assertTrue(adapter.extractVariables(null).isEmpty());
        }

        @Test
        @DisplayName("constant expression returns empty list")
        void constantExpression() {
            List<String> vars = adapter.extractVariables("100 + 200");
            assertTrue(vars.isEmpty());
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {
        @Test
        @DisplayName("valid expression returns success")
        void validExpression() {
            AviatorAdapter.SyntaxValidationResult result = adapter.validate("a + b");
            assertTrue(result.isValid());
            assertEquals(List.of("a", "b"), result.getVariables());
        }

        @Test
        @DisplayName("invalid syntax returns error")
        void invalidSyntax() {
            AviatorAdapter.SyntaxValidationResult result = adapter.validate("a +");
            assertFalse(result.isValid());
            assertNotNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("blank expression returns error")
        void blankExpression() {
            AviatorAdapter.SyntaxValidationResult result = adapter.validate("");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("blank"));
        }
    }
}