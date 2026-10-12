package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AggregateValidation")
@Tag("dev")
class AggregateValidationTest {

    private final AggregateValidation validation = new AggregateValidation();

    @Nested
    @DisplayName("valid configs")
    class ValidConfigs {
        @Test void validSum() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", null);
            assertTrue(validation.validate(cfg).isEmpty());
        }
        @Test void validCount() {
            var cfg = new AggregateConfig(AggregateFunction.COUNT, "items", "id", null);
            assertTrue(validation.validate(cfg).isEmpty());
        }
        @Test void validWithFilter() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", "amount > 0");
            assertTrue(validation.validate(cfg).isEmpty());
        }
    }

    @Nested
    @DisplayName("invalid configs")
    class InvalidConfigs {
        @Test void blankRelationCode() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "", "amount", null);
            assertFalse(validation.validate(cfg).isEmpty());
        }
        @Test void blankTargetField() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "", null);
            assertFalse(validation.validate(cfg).isEmpty());
        }
        @Test void invalidFilter() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", "a +");
            assertFalse(validation.validate(cfg).isEmpty());
        }
    }

    @Nested
    @DisplayName("validateOrThrow")
    class ValidateOrThrow {
        @Test void validDoesNotThrow() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "items", "amount", null);
            assertDoesNotThrow(() -> validation.validateOrThrow(cfg));
        }
        @Test void invalidThrows() {
            var cfg = new AggregateConfig(AggregateFunction.SUM, "", "", null);
            assertThrows(AggregateValidation.AggregateValidationException.class,
                () -> validation.validateOrThrow(cfg));
        }
    }

    @Nested
    @DisplayName("validateFormula")
    class ValidateFormula {
        @Test void validFormula() {
            var fc = FormulaConfig.builder()
                .type(FormulaType.AGGREGATE)
                .mode(FormulaMode.STORED)
                .expression("SUM(amount)")
                .aggregate(new AggregateConfig(AggregateFunction.SUM, "items", "amount", null))
                .build();
            assertTrue(validation.validateFormula(fc).isEmpty());
        }
        @Test void wrongType() {
            var fc = FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression("a+b")
                .dependsOn(List.of("a", "b"))
                .build();
            List<String> errors = validation.validateFormula(fc);
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("AGGREGATE"));
        }
        @Test void missingAggregateConfig() {
            // FormulaConfig.validate() rejects AGGREGATE without aggregate config
            assertThrows(IllegalArgumentException.class, () ->
                FormulaConfig.builder()
                    .type(FormulaType.AGGREGATE)
                    .mode(FormulaMode.STORED)
                    .build());
        }
    }
}
