package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaPublishValidator")
@Tag("dev")
class FormulaPublishValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FormulaPublishValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FormulaPublishValidator(new FormulaValidationService(), objectMapper);
    }

    private LowcodeModelSchema schemaWithFields(List<LowcodeFieldSchema> fields) {
        LowcodeModelSchema s = new LowcodeModelSchema();
        s.setFields(fields);
        return s;
    }

    private LowcodeFieldSchema plainField(String name) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        return f;
    }

    private LowcodeFieldSchema formulaField(String name, Map<String, Object> formulaConfig) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        f.setFormulaConfig(formulaConfig);
        return f;
    }

    private Map<String, Object> crossObjectFormulaConfig(String relationCode,
                                                         String targetObjectCode,
                                                         String returnField) {
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "CALC");
        fc.put("mode", "VIRTUAL");
        fc.put("expression", "1");
        fc.put("dependsOn", List.of());
        Map<String, Object> crossObject = new LinkedHashMap<>();
        crossObject.put("path", relationCode + "." + returnField);
        crossObject.put("relationCode", relationCode);
        crossObject.put("targetObjectCode", targetObjectCode);
        crossObject.put("returnField", returnField);
        fc.put("crossObject", crossObject);
        return fc;
    }

    private FormulaObjectDependencyAnalyzer.ObjectRelation relation(String sourceObjectCode,
                                                                    String targetObjectCode,
                                                                    String relationCode,
                                                                    String sourceField,
                                                                    String targetField) {
        return new FormulaObjectDependencyAnalyzer.ObjectRelation(
                null, relationCode, sourceObjectCode, targetObjectCode, sourceField, targetField);
    }

    // ---- tests ----

    @Nested
    @DisplayName("empty inputs")
    class EmptyInputs {
        @Test void nullSchema() { assertTrue(validator.validate(null).isValid()); }
        @Test void nullFields() {
            LowcodeModelSchema s = new LowcodeModelSchema();
            s.setFields(null);
            assertTrue(validator.validate(s).isValid());
        }
        @Test void emptyFields() { assertTrue(validator.validate(schemaWithFields(List.of())).isValid()); }
    }

    @Nested
    @DisplayName("plain fields")
    class PlainFields {
        @Test
        @DisplayName("fields without formulaConfig pass")
        void skipped() {
            assertTrue(validator.validate(
                schemaWithFields(List.of(plainField("name"), plainField("price")))).isValid());
        }
    }

    @Nested
    @DisplayName("formula extraction")
    class Extraction {
        @Test
        @DisplayName("extracts formula config correctly")
        void extractsFormula() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "a + b"); fc.put("dependsOn", List.of("a", "b"));

            Map<String, FormulaConfig> formulas = validator.extractFormulas(
                schemaWithFields(List.of(formulaField("result", fc))));
            assertEquals(1, formulas.size());
            FormulaConfig config = formulas.get("result");
            assertEquals(FormulaType.CALC, config.getType());
            assertEquals("a + b", config.getExpression());
        }

        @Test
        @DisplayName("skips fields without formulaConfig")
        void skipsPlainFields() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "x * 2"); fc.put("dependsOn", List.of("x"));

            Map<String, FormulaConfig> formulas = validator.extractFormulas(
                schemaWithFields(List.of(plainField("id"), formulaField("double", fc), plainField("remark"))));
            assertEquals(1, formulas.size());
            assertTrue(formulas.containsKey("double"));
        }
    }

    @Nested
    @DisplayName("validation flow")
    class Validation {
        @Test
        @DisplayName("valid CALC formula passes")
        void validCalc() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "price * quantity");
            fc.put("dependsOn", List.of("price", "quantity"));

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("price"), plainField("quantity"), formulaField("total", fc))));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("syntax error detected")
        void syntaxError() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "price * ");
            fc.put("dependsOn", List.of("price"));

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("price"), plainField("quantity"), formulaField("total", fc))));
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("chained formulas pass")
        void chainedFormulas() {
            Map<String, Object> fc1 = new LinkedHashMap<>();
            fc1.put("type", "CALC"); fc1.put("mode", "STORED");
            fc1.put("expression", "price * quantity");
            fc1.put("dependsOn", List.of("price", "quantity"));

            Map<String, Object> fc2 = new LinkedHashMap<>();
            fc2.put("type", "CALC"); fc2.put("mode", "STORED");
            fc2.put("expression", "total * 1.1");
            fc2.put("dependsOn", List.of("total"));

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("price"), plainField("quantity"), formulaField("total", fc1), formulaField("grandTotal", fc2))));
            assertTrue(result.isValid());
            assertNotNull(result.getDependencyAnalysis());
        }

        @Test
        @DisplayName("cycle detected")
        void cycleDetected() {
            Map<String, Object> fcA = new LinkedHashMap<>();
            fcA.put("type", "CALC"); fcA.put("mode", "STORED");
            fcA.put("expression", "b * 2"); fcA.put("dependsOn", List.of("b"));

            Map<String, Object> fcB = new LinkedHashMap<>();
            fcB.put("type", "CALC"); fcB.put("mode", "STORED");
            fcB.put("expression", "a + 1"); fcB.put("dependsOn", List.of("a"));

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(formulaField("a", fcA), formulaField("b", fcB))));
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertTrue(result.getDependencyAnalysis().hasCycle());
        }
    }

    @Nested
    @DisplayName("UAT-05: missing dependency fields")
    class MissingDependencies {
        @Test
        @DisplayName("single missing dependency field blocks publish")
        void singleMissingDep() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "price * quantity");
            fc.put("dependsOn", List.of("price", "quantity"));

            // quantity is NOT in the schema fields (only price and amount)
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), formulaField("amount", fc)));

            FormulaValidationResult result = validator.validate(schema);
            assertFalse(result.isValid(), "Should fail: quantity field missing");
            assertTrue(result.hasErrors());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("quantity")),
                "Error should mention missing field 'quantity'");
        }

        @Test
        @DisplayName("multiple missing dependency fields")
        void multipleMissingDeps() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "price * quantity + tax");
            fc.put("dependsOn", List.of("price", "quantity", "tax"));

            // only price exists in schema
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), formulaField("amount", fc)));

            FormulaValidationResult result = validator.validate(schema);
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("all dependencies exist - passes")
        void allDepsExist() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC"); fc.put("mode", "STORED");
            fc.put("expression", "price * quantity");
            fc.put("dependsOn", List.of("price", "quantity"));

            // both price and quantity exist in schema
            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"), formulaField("amount", fc)));

            FormulaValidationResult result = validator.validate(schema);
            assertTrue(result.isValid(), "Should pass: all deps exist");
        }

        @Test
        @DisplayName("formula depends on another formula field - passes")
        void dependsOnFormulaField() {
            Map<String, Object> fc1 = new LinkedHashMap<>();
            fc1.put("type", "CALC"); fc1.put("mode", "STORED");
            fc1.put("expression", "price * quantity");
            fc1.put("dependsOn", List.of("price", "quantity"));

            Map<String, Object> fc2 = new LinkedHashMap<>();
            fc2.put("type", "CALC"); fc2.put("mode", "STORED");
            fc2.put("expression", "total * 1.1");
            fc2.put("dependsOn", List.of("total"));  // total is a formula field

            LowcodeModelSchema schema = schemaWithFields(List.of(
                plainField("price"), plainField("quantity"),
                formulaField("total", fc1), formulaField("grandTotal", fc2)));

            FormulaValidationResult result = validator.validate(schema);
            assertTrue(result.isValid(), "Should pass: total is a formula field in schema");
        }
    }
    @Nested
    @DisplayName("R1: AGGREGATE config validation integration")
    class AggregateConfigValidation {
        @Test
        @DisplayName("missing relationCode blocks publish")
        void missingRelationCode() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE"); fc.put("mode", "STORED");
            fc.put("expression", ""); fc.put("dependsOn", List.of());
            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("function", "SUM");
            agg.put("relationCode", "");  // blank
            agg.put("targetField", "amount");
            fc.put("aggregate", agg);

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("id"), formulaField("totalAmount", fc))));
            assertFalse(result.isValid(), "Should fail: relationCode is blank");
            assertTrue(result.hasErrors());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("Relation code") || e.getMessage().contains("relationCode")),
                "Error should mention missing relationCode");
        }

        @Test
        @DisplayName("missing aggregate config blocks publish")
        void missingAggregateConfig() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE");
            fc.put("mode", "STORED");
            fc.put("expression", "");
            fc.put("dependsOn", List.of());

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("id"), formulaField("totalAmount", fc))));
            assertFalse(result.isValid(), "Should fail: aggregate config is required");
            assertTrue(result.hasErrors());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> "CONFIG".equals(e.getCategory()) && e.getMessage().contains("Invalid formulaConfig")),
                "Error should mention invalid formulaConfig");
        }

        @Test
        @DisplayName("missing targetField blocks publish (non-COUNT)")
        void missingTargetField() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE"); fc.put("mode", "STORED");
            fc.put("expression", ""); fc.put("dependsOn", List.of());
            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("function", "SUM");
            agg.put("relationCode", "order");
            agg.put("targetField", "");  // blank
            fc.put("aggregate", agg);

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("id"), formulaField("totalAmount", fc))));
            assertFalse(result.isValid(), "Should fail: targetField is blank for SUM");
            assertTrue(result.hasErrors());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("targetField") || e.getMessage().contains("Target field")),
                "Error should mention missing targetField");
        }

        @Test
        @DisplayName("COUNT without targetField passes")
        void countWithoutTargetField() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE"); fc.put("mode", "STORED");
            fc.put("expression", ""); fc.put("dependsOn", List.of());
            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("function", "COUNT");
            agg.put("relationCode", "order");
            agg.put("targetField", "");  // COUNT doesn''t require targetField
            fc.put("aggregate", agg);

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("id"), formulaField("orderCount", fc))));
            assertTrue(result.isValid(), "COUNT should pass without targetField");
        }

        @Test
        @DisplayName("valid AGGREGATE config passes")
        void validAggregate() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "AGGREGATE"); fc.put("mode", "STORED");
            fc.put("expression", ""); fc.put("dependsOn", List.of());
            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("function", "SUM");
            agg.put("relationCode", "order");
            agg.put("targetField", "amount");
            agg.put("filter", "amount > 0");
            fc.put("aggregate", agg);

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(plainField("id"), formulaField("totalAmount", fc))));
            assertTrue(result.isValid(), "Valid AGGREGATE config should pass");
        }

        @Test
        @DisplayName("mixed CALC and invalid AGGREGATE formula blocks publish")
        void mixedCalcAndInvalidAggregate() {
            Map<String, Object> calcFc = new LinkedHashMap<>();
            calcFc.put("type", "CALC"); calcFc.put("mode", "STORED");
            calcFc.put("expression", "price * quantity");
            calcFc.put("dependsOn", List.of("price", "quantity"));

            Map<String, Object> aggFc = new LinkedHashMap<>();
            aggFc.put("type", "AGGREGATE"); aggFc.put("mode", "STORED");
            aggFc.put("expression", ""); aggFc.put("dependsOn", List.of());
            Map<String, Object> aggConf = new LinkedHashMap<>();
            aggConf.put("function", "SUM");
            aggConf.put("relationCode", "");  // invalid
            aggConf.put("targetField", "amount");
            aggFc.put("aggregate", aggConf);

            FormulaValidationResult result = validator.validate(
                schemaWithFields(List.of(
                    plainField("price"), plainField("quantity"),
                    formulaField("total", calcFc),
                    formulaField("grandTotal", aggFc))));
            assertFalse(result.isValid(), "Should fail due to invalid AGGREGATE config");
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> "AGGREGATE".equals(e.getCategory()) || "CONFIG".equals(e.getCategory())),
                "Should have AGGREGATE category error");
        }
    }

    @Nested
    @DisplayName("cross-object publish validation")
    class CrossObjectPublishValidation {
        @Test
        @DisplayName("valid cross-object formula passes relation and target field checks")
        void validCrossObjectPasses() {
            LowcodeModelSchema orderSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("customerId"),
                    formulaField("customerLevel", crossObjectFormulaConfig("customer", "customer", "level"))));
            LowcodeModelSchema customerSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("level")));

            FormulaObjectDependencyAnalyzer.ObjectContext orderContext = validator.buildObjectContext(
                    "order", orderSchema, List.of(relation("order", "customer", "customer", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customerContext = validator.buildObjectContext(
                    "customer", customerSchema, List.of());

            FormulaValidationResult result = validator.validate(orderSchema, List.of(orderContext, customerContext));

            assertTrue(result.isValid(), result.getErrors().toString());
        }

        @Test
        @DisplayName("illegal relation blocks publish")
        void illegalRelationBlocksPublish() {
            LowcodeModelSchema orderSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("customerId"),
                    formulaField("customerLevel", crossObjectFormulaConfig("customer", "customer", "level"))));
            LowcodeModelSchema customerSchema = schemaWithFields(List.of(plainField("id"), plainField("level")));

            FormulaObjectDependencyAnalyzer.ObjectContext orderContext = validator.buildObjectContext(
                    "order", orderSchema, List.of(relation("order", "owner", "owner", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customerContext = validator.buildObjectContext(
                    "customer", customerSchema, List.of());

            FormulaValidationResult result = validator.validate(orderSchema, List.of(orderContext, customerContext));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                    .anyMatch(error -> "CROSS_OBJECT".equals(error.getCategory())
                            && error.getMessage().contains("Relation not found")));
        }

        @Test
        @DisplayName("missing target return field blocks publish")
        void missingTargetReturnFieldBlocksPublish() {
            LowcodeModelSchema orderSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("customerId"),
                    formulaField("customerLevel", crossObjectFormulaConfig("customer", "customer", "level"))));
            LowcodeModelSchema customerSchema = schemaWithFields(List.of(plainField("id")));

            FormulaObjectDependencyAnalyzer.ObjectContext orderContext = validator.buildObjectContext(
                    "order", orderSchema, List.of(relation("order", "customer", "customer", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customerContext = validator.buildObjectContext(
                    "customer", customerSchema, List.of());

            FormulaValidationResult result = validator.validate(orderSchema, List.of(orderContext, customerContext));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                    .anyMatch(error -> error.getMessage().contains("return field")));
        }

        @Test
        @DisplayName("cross-object cycle blocks publish")
        void crossObjectCycleBlocksPublish() {
            LowcodeModelSchema orderSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("customerId"),
                    plainField("status"),
                    formulaField("customerLevel", crossObjectFormulaConfig("customer", "customer", "level"))));
            LowcodeModelSchema customerSchema = schemaWithFields(List.of(
                    plainField("id"),
                    plainField("orderId"),
                    plainField("level"),
                    formulaField("orderStatus", crossObjectFormulaConfig("order", "order", "status"))));

            FormulaObjectDependencyAnalyzer.ObjectContext orderContext = validator.buildObjectContext(
                    "order", orderSchema, List.of(relation("order", "customer", "customer", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customerContext = validator.buildObjectContext(
                    "customer", customerSchema, List.of(relation("customer", "order", "order", "orderId", "id")));

            FormulaValidationResult result = validator.validate(orderSchema, List.of(orderContext, customerContext));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                    .anyMatch(error -> "CROSS_OBJECT_CYCLE".equals(error.getCategory())));
        }
    }

}
