package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpressionParser")
@Tag("dev")
class ExpressionParserTest {

    private final ExpressionParser parser = new ExpressionParser();

    @Nested
    @DisplayName("parse")
    class Parse {
        @Test
        @DisplayName("valid arithmetic expression")
        void validArithmetic() {
            ExpressionParser.ExpressionParseResult result = parser.parse("price * quantity");
            assertTrue(result.isValid());
            assertTrue(result.getVariables().contains("price"));
            assertTrue(result.getVariables().contains("quantity"));
        }

        @Test
        @DisplayName("blank expression returns error")
        void blankExpression() {
            ExpressionParser.ExpressionParseResult result = parser.parse("");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("blank"));
        }

        @Test
        @DisplayName("syntax error returns error with message")
        void syntaxError() {
            ExpressionParser.ExpressionParseResult result = parser.parse("a +");
            assertFalse(result.isValid());
            assertNotNull(result.getErrorMessage());
            System.out.println("Parse error: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("valid ternary returns variables")
        void validTernary() {
            ExpressionParser.ExpressionParseResult result = parser.parse("amount > 100 ? amount * 0.9 : amount");
            assertTrue(result.isValid());
            assertTrue(result.getVariables().contains("amount"));
        }
    }

    @Nested
    @DisplayName("extractDependencies")
    class ExtractDependencies {
        @Test
        @DisplayName("extracts variables from expression")
        void extractsVariables() {
            List<String> deps = parser.extractDependencies("price * qty");
            assertEquals(List.of("price", "qty"), deps);
        }

        @Test
        @DisplayName("constant expression returns empty")
        void constantExpression() {
            List<String> deps = parser.extractDependencies("3.14 * 2");
            assertTrue(deps.isEmpty());
        }

        @Test
        @DisplayName("invalid expression returns empty gracefully")
        void invalidExpression() {
            List<String> deps = parser.extractDependencies("a +");
            // Should not throw, returns empty
            assertNotNull(deps);
        }
    }

    @Nested
    @DisplayName("crossCheckDependencies")
    class CrossCheckDependencies {
        @Test
        @DisplayName("exact match returns no warnings")
        void exactMatch() {
            List<String> warnings = parser.crossCheckDependencies(
                List.of("a", "b"), List.of("a", "b"));
            assertTrue(warnings.isEmpty());
        }

        @Test
        @DisplayName("undeclared variable in expression")
        void undeclaredVariable() {
            List<String> warnings = parser.crossCheckDependencies(
                List.of("a"), List.of("a", "b"));
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).contains("b"));
            assertTrue(warnings.get(0).contains("not declared"));
        }

        @Test
        @DisplayName("declared variable not in expression")
        void unusedDeclaration() {
            List<String> warnings = parser.crossCheckDependencies(
                List.of("a", "b"), List.of("a"));
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0).contains("b"));
            assertTrue(warnings.get(0).contains("not found"));
        }

        @Test
        @DisplayName("both undeclared and unused returns multiple warnings")
        void bothMismatches() {
            List<String> warnings = parser.crossCheckDependencies(
                List.of("a", "c"), List.of("a", "b"));
            assertEquals(2, warnings.size());
        }

        @Test
        @DisplayName("null declared deps handled gracefully")
        void nullDeclared() {
            List<String> warnings = parser.crossCheckDependencies(
                null, List.of("a"));
            assertEquals(1, warnings.size());
        }

        @Test
        @DisplayName("null actual vars handled gracefully")
        void nullActual() {
            List<String> warnings = parser.crossCheckDependencies(
                List.of("a"), null);
            assertEquals(1, warnings.size());
        }
    }
}
