package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketResponse;
import com.mdframe.forge.plugin.generator.mapper.FormulaFunctionMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("FormulaValidationService")
@Tag("dev")
class FormulaValidationServiceTest {

    private final FormulaValidationService service = new FormulaValidationService();

    private FormulaConfig calcConfig(String expr, List<String> deps) {
        return FormulaConfig.builder()
            .type(FormulaType.CALC)
            .mode(FormulaMode.STORED)
            .expression(expr)
            .dependsOn(deps)
            .build();
    }

    private FormulaConfig lookupConfig(String relationCode, List<String> dependsOn) {
        return FormulaConfig.builder()
            .type(FormulaType.LOOKUP)
            .mode(FormulaMode.VIRTUAL)
            .expression("")
            .dependsOn(dependsOn)
            .lookup(new LookupConfig(
                relationCode,
                "sys_user",
                "ownerUserId",
                "id",
                "realName",
                "NOT_ASSIGNED"))
            .build();
    }

    private Map<String, FormulaConfig> mapOf(Object... keysAndValues) {
        Map<String, FormulaConfig> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], (FormulaConfig) keysAndValues[i + 1]);
        }
        return map;
    }

    @Nested
    @DisplayName("valid formulas")
    class ValidFormulas {
        @Test
        @DisplayName("empty formula map → valid")
        void emptyMap() {
            FormulaValidationResult result = service.validate(Collections.emptyMap());
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
        }

        @Test
        @DisplayName("single valid formula → valid")
        void singleFormula() {
            Map<String, FormulaConfig> map = mapOf("total",
                calcConfig("price * quantity", List.of("price", "quantity")));
            FormulaValidationResult result = service.validate(map);
            assertTrue(result.isValid(), "Errors: " + result.getErrors());
        }

        @Test
        @DisplayName("chain dependency A→B→C → valid")
        void chainDependency() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("C * 2", List.of("C")),
                "C", calcConfig("42", Collections.emptyList())
            );
            FormulaValidationResult result = service.validate(map);
            assertTrue(result.isValid(), "Errors: " + result.getErrors());
            assertNotNull(result.getDependencyAnalysis());
            assertNotNull(result.getDependencyAnalysis().getTopologicalOrder());
        }

        @Test
        @DisplayName("LOOKUP with blank expression and lookup config is valid")
        void lookupBlankExpressionValid() {
            FormulaValidationResult result = service.validate(mapOf(
                "ownerName", lookupConfig("customer_owner", List.of("ownerUserId"))));

            assertTrue(result.isValid(), "Errors: " + result.getErrors());
            assertFalse(result.hasWarnings());
        }
    }

    @Nested
    @DisplayName("syntax errors")
    class SyntaxErrors {
        @Test
        @DisplayName("invalid expression → error")
        void invalidExpression() {
            Map<String, FormulaConfig> map = mapOf("bad",
                calcConfig("a +", List.of("a")));
            FormulaValidationResult result = service.validate(map);
            // Syntax errors should be reported
            assertTrue(result.hasErrors(), "Should have syntax error: " + result.getErrors());
        }

        @Test
        @DisplayName("blank expression for non-aggregate → error")
        void blankExpression() {
            Map<String, FormulaConfig> map = mapOf("field",
                FormulaConfig.builder()
                    .type(FormulaType.CALC)
                    .mode(FormulaMode.STORED)
                    .expression("")
                    .dependsOn(Collections.emptyList())
                    .build());
            FormulaValidationResult result = service.validate(map);
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("LOOKUP path-style relationCode is invalid")
        void lookupPathRelationCodeInvalid() {
            FormulaValidationResult result = service.validate(mapOf(
                "ownerName", lookupConfig("customer.owner", List.of("ownerUserId"))));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(error -> "CONFIG".equals(error.getCategory())));
        }

        @Test
        @DisplayName("LOOKUP config with SQL fragments is invalid")
        void lookupSqlFragmentInvalid() {
            FormulaValidationResult result = service.validate(mapOf(
                "ownerName", lookupConfig("customer_owner;drop table sys_user", List.of("ownerUserId"))));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("SQL fragments")));
        }

        @Test
        @DisplayName("cross-object config with SQL fragments is invalid")
        void crossObjectSqlFragmentInvalid() {
            FormulaConfig config = FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.VIRTUAL)
                .expression("customer.level")
                .dependsOn(List.of("customerId"))
                .crossObject(new CrossObjectConfig(
                    "customer.level",
                    "customer;drop table crm_customer",
                    "crm_customer",
                    "level",
                    CrossObjectRecomputeMode.ASYNC))
                .build();

            FormulaValidationResult result = service.validate(mapOf("customerLevel", config));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("SQL fragments")));
        }

        @Test
        @DisplayName("disabled function reference is invalid")
        void disabledFunctionReferenceInvalid() {
            FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
            registry.registerDefinition(FormulaFunctionDefinition.builder()
                .functionCode("string.contains")
                .displayName("包含判断")
                .category("String")
                .description("disabled for test")
                .returnType("BOOLEAN")
                .status(FormulaFunctionDefinition.STATUS_DISABLED)
                .beanName("formulaBuiltinFunctionProvider")
                .methodName("stringContains")
                .argument("value", "STRING", true)
                .argument("keyword", "STRING", true)
                .build());
            FormulaValidationService customService = new FormulaValidationService(
                new ExpressionParser(), new FormulaDependencyAnalyzer(), registry);
            FormulaValidationResult result = customService.validate(mapOf(
                "matched", FormulaConfig.builder()
                    .type(FormulaType.CALC)
                    .mode(FormulaMode.STORED)
                    .expression("string.contains(name, 'x')")
                    .dependsOn(List.of("name"))
                    .functionRefs(List.of("string.contains"))
                    .build()));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(error -> "FUNCTION".equals(error.getCategory())));
        }

        @Test
        @DisplayName("disabled market function reference is invalid")
        void disabledMarketFunctionReferenceInvalid() {
            FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
            FormulaFunctionMapper mapper = Mockito.mock(FormulaFunctionMapper.class);
            FormulaFunctionMarketService marketService = new FormulaFunctionMarketService(mapper, registry);
            FormulaValidationService customService = new FormulaValidationService(
                new ExpressionParser(), new FormulaDependencyAnalyzer(), registry, marketService);

            FormulaFunctionMarketResponse disabled = new FormulaFunctionMarketResponse();
            disabled.setFunctionCode("math.max");
            disabled.setStatus("ENABLED");
            disabled.setInstallStatus("INSTALLED");
            disabled.setEnabled(false);

            try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
                sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
                when(mapper.selectFormulaFunctionMarketDetail(7L, "math.max")).thenReturn(disabled);

                FormulaValidationResult result = customService.validate(mapOf(
                    "maxValue", FormulaConfig.builder()
                        .type(FormulaType.CALC)
                        .mode(FormulaMode.STORED)
                        .expression("math.max(leftValue, rightValue)")
                        .dependsOn(List.of("leftValue", "rightValue"))
                        .functionRefs(List.of("math.max"))
                        .build()));

                assertFalse(result.isValid());
                assertTrue(result.getErrors().stream()
                    .anyMatch(error -> "FUNCTION".equals(error.getCategory())
                        && error.getMessage().contains("not enabled")));
            }
        }
    }

    @Nested
    @DisplayName("cycle detection")
    class CycleDetection {
        @Test
        @DisplayName("A↔B cycle → invalid")
        void twoNodeCycle() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("A * 2", List.of("A"))
            );
            FormulaValidationResult result = service.validate(map);
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertTrue(result.getDependencyAnalysis().hasCycle());
            System.out.println("Cycle errors: " + result.getErrors());
        }
    }

    @Nested
    @DisplayName("depth exceeded")
    class DepthExceeded {
        @Test
        @DisplayName("depth 5 chain → invalid")
        void depth5Chain() {
            Map<String, FormulaConfig> map = mapOf(
                "E", calcConfig("42", Collections.emptyList()),
                "D", calcConfig("E + 1", List.of("E")),
                "C", calcConfig("D + 1", List.of("D")),
                "B", calcConfig("C + 1", List.of("C")),
                "A", calcConfig("B + 1", List.of("B"))
            );
            FormulaValidationResult result = service.validate(map);
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
        }
    }

    @Nested
    @DisplayName("dependency warnings")
    class DependencyWarnings {
        @Test
        @DisplayName("undeclared variable → warning")
        void undeclaredVariable() {
            Map<String, FormulaConfig> map = mapOf("total",
                calcConfig("price * qty", List.of("price"))); // qty not declared
            FormulaValidationResult result = service.validate(map);
            // Expression is valid, DAG is valid, but there should be a warning
            System.out.println("Warnings: " + result.getWarnings());
            assertTrue(result.hasWarnings());
        }

        @Test
        @DisplayName("LOOKUP source field omitted from dependsOn produces warning")
        void lookupSourceFieldMissingFromDependsOnWarns() {
            FormulaValidationResult result = service.validate(mapOf(
                "ownerName", lookupConfig("customer_owner", Collections.emptyList())));

            assertTrue(result.isValid(), "Errors: " + result.getErrors());
            assertTrue(result.hasWarnings());
        }
    }

    @Nested
    @DisplayName("validateExpression convenience")
    class ValidateExpression {
        @Test
        @DisplayName("valid single expression")
        void validSingle() {
            ExpressionParser.ExpressionParseResult result = service.validateExpression("a + b");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("invalid single expression")
        void invalidSingle() {
            ExpressionParser.ExpressionParseResult result = service.validateExpression("a +");
            assertFalse(result.isValid());
        }
    }
}
