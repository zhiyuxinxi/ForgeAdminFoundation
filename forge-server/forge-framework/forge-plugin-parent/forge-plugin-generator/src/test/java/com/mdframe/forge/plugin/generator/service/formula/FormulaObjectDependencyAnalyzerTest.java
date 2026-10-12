package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaObjectDependencyAnalyzer")
@Tag("dev")
class FormulaObjectDependencyAnalyzerTest {

    private final FormulaObjectDependencyAnalyzer analyzer = new FormulaObjectDependencyAnalyzer();

    private FormulaConfig crossObjectFormula(String relationCode,
                                             String targetObjectCode,
                                             String returnField,
                                             FormulaMode mode) {
        return FormulaConfig.builder()
                .type(FormulaType.CALC)
                .mode(mode)
                .expression("1")
                .crossObject(new CrossObjectConfig(
                        relationCode + "." + returnField,
                        relationCode,
                        targetObjectCode,
                        returnField,
                        null))
                .build();
    }

    private FormulaObjectDependencyAnalyzer.ObjectRelation relation(String sourceObjectCode,
                                                                    String targetObjectCode,
                                                                    String relationCode,
                                                                    String sourceField,
                                                                    String targetField) {
        return new FormulaObjectDependencyAnalyzer.ObjectRelation(
                null, relationCode, sourceObjectCode, targetObjectCode, sourceField, targetField);
    }

    private FormulaObjectDependencyAnalyzer.ObjectContext context(String objectCode,
                                                                  Set<String> fields,
                                                                  Map<String, FormulaConfig> formulas,
                                                                  List<FormulaObjectDependencyAnalyzer.ObjectRelation> relations) {
        return new FormulaObjectDependencyAnalyzer.ObjectContext(objectCode, fields, formulas, relations);
    }

    @Nested
    @DisplayName("object graph")
    class ObjectGraph {
        @Test
        @DisplayName("cross-object formula creates object dependency edge")
        void createsDependencyEdge() {
            FormulaObjectDependencyAnalyzer.ObjectContext order = context(
                    "order",
                    Set.of("id", "customerId", "customerLevel"),
                    Map.of("customerLevel", crossObjectFormula("customer", "customer", "level", FormulaMode.VIRTUAL)),
                    List.of(relation("order", "customer", "customer", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customer = context(
                    "customer", Set.of("id", "level"), Map.of(), List.of());

            FormulaObjectDependencyAnalyzer.ObjectDependencyAnalysisResult result =
                    analyzer.analyze(List.of(order, customer));

            assertTrue(result.isValid(), result.getErrors().toString());
            assertEquals(1, result.getEdges().size());
            assertEquals("order", result.getEdges().get(0).getSourceObjectCode());
            assertEquals("customer", result.getEdges().get(0).getTargetObjectCode());
            assertEquals("order.customerLevel->customer.level@customer",
                    result.getEdges().get(0).getDependencyTrace());
        }

        @Test
        @DisplayName("A depends on B and B depends on A is blocked")
        void detectsObjectCycle() {
            FormulaObjectDependencyAnalyzer.ObjectContext order = context(
                    "order",
                    Set.of("id", "customerId", "status"),
                    Map.of("customerLevel", crossObjectFormula("customer", "customer", "level", FormulaMode.VIRTUAL)),
                    List.of(relation("order", "customer", "customer", "customerId", "id")));
            FormulaObjectDependencyAnalyzer.ObjectContext customer = context(
                    "customer",
                    Set.of("id", "orderId", "level"),
                    Map.of("orderStatus", crossObjectFormula("order", "order", "status", FormulaMode.VIRTUAL)),
                    List.of(relation("customer", "order", "order", "orderId", "id")));

            FormulaObjectDependencyAnalyzer.ObjectDependencyAnalysisResult result =
                    analyzer.analyze(List.of(order, customer));

            assertFalse(result.isValid());
            assertTrue(result.hasCycle());
            assertEquals(List.of("order", "customer", "order"), result.getCyclePath());
            assertTrue(result.getErrors().stream()
                    .anyMatch(error -> "CROSS_OBJECT_CYCLE".equals(error.getCategory())));
        }

        @Test
        @DisplayName("missing relation is invalid")
        void missingRelationInvalid() {
            FormulaObjectDependencyAnalyzer.ObjectContext order = context(
                    "order",
                    Set.of("id", "customerId", "customerLevel"),
                    Map.of("customerLevel", crossObjectFormula("customer", "customer", "level", FormulaMode.VIRTUAL)),
                    List.of());
            FormulaObjectDependencyAnalyzer.ObjectContext customer = context(
                    "customer", Set.of("id", "level"), Map.of(), List.of());

            FormulaObjectDependencyAnalyzer.ObjectDependencyAnalysisResult result =
                    analyzer.analyze(List.of(order, customer));

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                    .anyMatch(error -> error.getMessage().contains("Relation not found")));
        }
    }

    @Nested
    @DisplayName("recompute tasks")
    class RecomputeTasks {
        @Test
        @DisplayName("stored cross-object formula generates pending task with idempotency key")
        void storedCrossObjectGeneratesPendingTask() {
            FormulaObjectDependencyAnalyzer.ObjectContext order = context(
                    "order",
                    Set.of("id", "customerId", "customerLevel"),
                    Map.of(
                            "customerLevel", crossObjectFormula("customer", "customer", "level", FormulaMode.STORED),
                            "customerName", crossObjectFormula("customer", "customer", "name", FormulaMode.VIRTUAL)),
                    List.of(relation("order", "customer", "customer", "customerId", "id")));

            CrossObjectRecomputeTaskService service = new CrossObjectRecomputeTaskService();
            List<CrossObjectRecomputeTaskService.PendingRecomputeTask> tasks =
                    service.buildPendingTasks(order, 100L);

            assertEquals(1, tasks.size());
            CrossObjectRecomputeTaskService.PendingRecomputeTask task = tasks.get(0);
            assertEquals("PENDING", task.getStatus());
            assertEquals("order", task.getObjectCode());
            assertEquals("100", task.getRecordId());
            assertEquals("customerLevel", task.getFieldCode());
            assertEquals("order.customerLevel->customer.level@customer", task.getDependencyTrace());
            assertEquals("order:100:customerLevel:order.customerLevel->customer.level@customer",
                    task.getIdempotencyKey());
        }
    }
}
