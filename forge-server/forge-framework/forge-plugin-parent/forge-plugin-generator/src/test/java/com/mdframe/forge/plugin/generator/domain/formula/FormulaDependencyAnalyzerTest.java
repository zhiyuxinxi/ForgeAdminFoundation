package com.mdframe.forge.plugin.generator.domain.formula;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FormulaDependencyAnalyzer 单元测试
 */
@DisplayName("FormulaDependencyAnalyzer")
@Tag("dev")
class FormulaDependencyAnalyzerTest {

    private final FormulaDependencyAnalyzer analyzer = new FormulaDependencyAnalyzer(3);

    private FormulaConfig calcConfig(String expression, List<String> dependsOn) {
        return FormulaConfig.builder()
            .type(FormulaType.CALC)
            .mode(FormulaMode.STORED)
            .expression(expression)
            .dependsOn(dependsOn)
            .build();
    }

    private FormulaConfig aggregateConfig(String relationCode, String targetField) {
        return FormulaConfig.builder()
            .type(FormulaType.AGGREGATE)
            .mode(FormulaMode.STORED)
            .expression("SUM(" + targetField + ")")
            .aggregate(new AggregateConfig(AggregateFunction.SUM, relationCode, targetField, null))
            .build();
    }

    private FormulaConfig lookupConfig(String sourceField) {
        return FormulaConfig.builder()
            .type(FormulaType.LOOKUP)
            .mode(FormulaMode.VIRTUAL)
            .dependsOn(List.of(sourceField))
            .lookup(new LookupConfig(
                "customer_owner",
                "sys_user",
                sourceField,
                "id",
                "realName",
                "NOT_ASSIGNED"))
            .build();
    }

    private static Map<String, FormulaConfig> mapOf(Object... keysAndValues) {
        Map<String, FormulaConfig> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], (FormulaConfig) keysAndValues[i + 1]);
        }
        return map;
    }

    @Nested
    @DisplayName("合法 DAG")
    class ValidDag {
        @Test
        @DisplayName("空公式列表：分析通过")
        void emptyFormulaMap() {
            DependencyAnalysisResult result = analyzer.analyze(Collections.emptyMap());
            assertTrue(result.isValid());
            assertTrue(result.getTopologicalOrder().isEmpty());
            assertFalse(result.hasCycles());
        }

        @Test
        @DisplayName("单公式无依赖：分析通过，拓扑序含该字段")
        void singleFormulaNoDeps() {
            Map<String, FormulaConfig> map = mapOf("total",
                calcConfig("price * qty", Collections.emptyList()));

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid());
            assertFalse(result.hasCycles());
            assertEquals(List.of("total"), result.getTopologicalOrder());
            assertEquals(1, result.getDepthMap().get("total"));
        }

        @Test
        @DisplayName("链式依赖 A→B→C：分析通过，拓扑序 B,C,A")
        void chainDependency() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("C * 2", List.of("C")),
                "C", calcConfig("42", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), "链式依赖应通过: " + result.getErrors());
            assertFalse(result.hasCycles());

            List<String> order = result.getTopologicalOrder();
            assertTrue(order.indexOf("C") < order.indexOf("B"));
            assertTrue(order.indexOf("B") < order.indexOf("A"));

            assertEquals(1, result.getDepthMap().get("C"));
            assertEquals(2, result.getDepthMap().get("B"));
            assertEquals(3, result.getDepthMap().get("A"));
        }

        @Test
        @DisplayName("树形依赖 A依赖[B,C]：分析通过")
        void treeDependency() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + C", List.of("B", "C")),
                "B", calcConfig("10", Collections.emptyList()),
                "C", calcConfig("20", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), result.getErrors().toString());
            assertFalse(result.hasCycles());

            List<String> order = result.getTopologicalOrder();
            assertTrue(order.indexOf("B") < order.indexOf("A"));
            assertTrue(order.indexOf("C") < order.indexOf("A"));

            assertEquals(1, result.getDepthMap().get("B"));
            assertEquals(1, result.getDepthMap().get("C"));
            assertEquals(2, result.getDepthMap().get("A"));
        }

        @Test
        @DisplayName("孤立节点（无依赖不被依赖）：分析通过")
        void isolatedNode() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("10", Collections.emptyList()),
                "X", calcConfig("independent", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), result.getErrors().toString());
            // X is not in any dependency chain but should be in the order
            assertTrue(result.getTopologicalOrder().contains("X"));
        }
    }

    @Nested
    @DisplayName("直接循环")
    class DirectCycle {
        @Test
        @DisplayName("A↔B 两节点环：检测失败")
        void twoNodeCycle() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("A * 2", List.of("A"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertFalse(result.isValid(), "应检测到 A↔B 循环");
            assertTrue(result.hasCycles());
            System.out.println("Cycle detected: " + result.getCycles());
        }

        @Test
        @DisplayName("A→A 自引用：检测失败")
        void selfReference() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("A + 1", List.of("A"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertFalse(result.isValid(), "应检测到自引用");
            assertTrue(result.hasCycles());
        }
    }

    @Nested
    @DisplayName("间接循环")
    class IndirectCycle {
        @Test
        @DisplayName("A→B→C→A 三节点环：检测失败")
        void threeNodeCycle() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("C * 2", List.of("C")),
                "C", calcConfig("A + 5", List.of("A"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertFalse(result.isValid(), "应检测到间接循环");
            assertTrue(result.hasCycles());
            System.out.println("Cycle detected: " + result.getCycles());
        }

        @Test
        @DisplayName("A→B→C→D→B 带尾环：检测失败")
        void cycleWithTail() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "B", calcConfig("C * 2", List.of("C")),
                "C", calcConfig("D + 5", List.of("D")),
                "D", calcConfig("B - 1", List.of("B"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertFalse(result.isValid(), "应检测到 B→C→D→B 循环");
            assertTrue(result.hasCycles());
        }
    }

    @Nested
    @DisplayName("深度超限")
    class DepthExceeded {
        @Test
        @DisplayName("嵌套深度=3：通过（刚好在限内）")
        void depthExactly3() {
            // 3-node chain: C→B→A, depths: C=1, B=2, A=3 (exactly at limit)
            Map<String, FormulaConfig> map = mapOf(
                "C", calcConfig("42", Collections.emptyList()),
                "B", calcConfig("C + 1", List.of("C")),
                "A", calcConfig("B + 1", List.of("B"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), "深度=3应在限内: " + result.getErrors());
            assertEquals(1, result.getDepthMap().get("C"));
            assertEquals(2, result.getDepthMap().get("B"));
            assertEquals(3, result.getDepthMap().get("A"));
        }

        @Test
        @DisplayName("嵌套深度=5：拦截，提示超限")
        void depthExceeded4() {
            Map<String, FormulaConfig> map = mapOf(
                "E", calcConfig("42", Collections.emptyList()),
                "D", calcConfig("E + 1", List.of("E")),
                "C", calcConfig("D + 1", List.of("D")),
                "B", calcConfig("C + 1", List.of("C")),
                "A", calcConfig("B + 1", List.of("B"))
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertFalse(result.isValid(), "深度=5 应被拦截");
            assertFalse(result.getErrors().isEmpty());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("深度")),
                "错误应提到嵌套深度");
        }
    }

    @Nested
    @DisplayName("聚合公式依赖")
    class AggregateDependency {
        @Test
        @DisplayName("聚合公式无环：分析通过")
        void aggregateNoCycle() {
            Map<String, FormulaConfig> map = mapOf(
                "totalAmount", aggregateConfig("order_items", "amount"),
                "unitPrice", calcConfig("100", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), result.getErrors().toString());
            assertFalse(result.hasCycles());
        }
    }

    @Nested
    @DisplayName("LOOKUP formula dependencies")
    class LookupDependency {
        @Test
        @DisplayName("LOOKUP sourceField makes source formula run first")
        void lookupSourceFieldOrdersFormulaFirst() {
            Map<String, FormulaConfig> map = mapOf(
                "ownerUserId", calcConfig("'u1'", Collections.emptyList()),
                "ownerName", lookupConfig("ownerUserId")
            );

            DependencyAnalysisResult result = analyzer.analyze(map);

            assertTrue(result.isValid(), result.getErrors().toString());
            assertTrue(result.getTopologicalOrder().indexOf("ownerUserId")
                < result.getTopologicalOrder().indexOf("ownerName"));
            assertEquals(2, result.getDepthMap().get("ownerName"));
        }

        @Test
        @DisplayName("LOOKUP sourceField participates in missing field validation")
        void lookupSourceFieldMissingBlocksValidation() {
            Map<String, FormulaConfig> map = mapOf(
                "ownerName", lookupConfig("ownerUserId")
            );

            List<String> errors = FormulaDependencyAnalyzer.validateDependencyFields(map, Set.of("ownerName"));

            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("ownerUserId"));
        }
    }

    @Nested
    @DisplayName("边界条件")
    class EdgeCases {
        @Test
        @DisplayName("同一字段被多个公式依赖：分析通过")
        void sharedDependency() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("C + 1", List.of("C")),
                "B", calcConfig("C * 2", List.of("C")),
                "C", calcConfig("10", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), result.getErrors().toString());
            assertFalse(result.hasCycles());
            List<String> order = result.getTopologicalOrder();
            assertTrue(order.indexOf("C") < order.indexOf("A"));
            assertTrue(order.indexOf("C") < order.indexOf("B"));
        }

        @Test
        @DisplayName("多被依赖：B被3个公式依赖")
        void multipleDependents() {
            Map<String, FormulaConfig> map = mapOf(
                "A", calcConfig("B + 1", List.of("B")),
                "C", calcConfig("B * 2", List.of("B")),
                "D", calcConfig("B - 1", List.of("B")),
                "B", calcConfig("10", Collections.emptyList())
            );

            DependencyAnalysisResult result = analyzer.analyze(map);
            assertTrue(result.isValid(), result.getErrors().toString());
            assertFalse(result.hasCycles());
            List<String> order = result.getTopologicalOrder();
            assertTrue(order.indexOf("B") < order.indexOf("A"));
            assertTrue(order.indexOf("B") < order.indexOf("C"));
            assertTrue(order.indexOf("B") < order.indexOf("D"));
        }
    }
}
