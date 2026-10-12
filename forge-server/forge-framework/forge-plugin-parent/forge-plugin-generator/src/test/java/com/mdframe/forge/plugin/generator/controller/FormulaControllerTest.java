package com.mdframe.forge.plugin.generator.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.service.formula.ConditionRuleCompiler;
import com.mdframe.forge.plugin.generator.service.formula.FormulaDependencyGraphService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaDebugService;
import com.mdframe.forge.plugin.generator.dto.formula.*;
import com.mdframe.forge.plugin.generator.service.formula.FormulaExecutionEngine;
import com.mdframe.forge.plugin.generator.service.formula.FormulaExecutionLogService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionMarketService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionRegistry;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormulaController")
@Tag("dev")
class FormulaControllerTest {

    private FormulaController controller;
    private CapturingLogService logService;

    @BeforeEach
    void setUp() {
        logService = new CapturingLogService();
        FormulaFunctionRegistry functionRegistry = FormulaFunctionRegistry.builtin();
        controller = new FormulaController(
            new FormulaValidationService(new ExpressionParser(), new FormulaDependencyAnalyzer(), functionRegistry),
            new FormulaExecutionEngine(),
            new FormulaDependencyAnalyzer(),
            logService,
            new FormulaDebugService(new FormulaExecutionEngine(), logService, new ObjectMapper()),
            new FormulaDependencyGraphService(new FormulaDependencyAnalyzer()),
            new ConditionRuleCompiler(),
            new FormulaFunctionMarketService(functionRegistry)
        );
    }

    @Nested
    @DisplayName("POST /validate")
    class Validate {
        @Test
        @DisplayName("valid expression returns success")
        void validExpression() {
            var req = new FormulaValidateRequest();
            req.setExpression("price * quantity");
            req.setType("CALC");
            RespInfo<FormulaValidateResponse> resp = controller.validate(req);
            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isValid());
            assertTrue(resp.getData().getVariables().contains("price"));
            assertTrue(resp.getData().getVariables().contains("quantity"));
        }

        @Test
        @DisplayName("invalid expression returns error")
        void invalidExpression() {
            var req = new FormulaValidateRequest();
            req.setExpression("price * ");
            RespInfo<FormulaValidateResponse> resp = controller.validate(req);
            assertTrue(resp.getCode() == 200);
            assertFalse(resp.getData().isValid());
            assertNotNull(resp.getData().getErrorMessage());
        }
    }

    @Nested
    @DisplayName("POST /preview")
    class Preview {
        @Test
        @DisplayName("preview with sample values")
        void withSampleValues() {
            var req = new FormulaPreviewRequest();
            req.setExpression("price * quantity");
            req.setType("CALC");
            req.setSampleValues(Map.of("price", 100L, "quantity", 3L));
            RespInfo<FormulaPreviewResponse> resp = controller.preview(req);
            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isSuccess());
            assertEquals(300L, resp.getData().getResult());
        }

        @Test
        @DisplayName("preview without values")
        void noSampleValues() {
            var req = new FormulaPreviewRequest();
            req.setExpression("1 + 2");
            RespInfo<FormulaPreviewResponse> resp = controller.preview(req);
            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isSuccess());
            assertEquals(3L, resp.getData().getResult());
        }
    }

    @Nested
    @DisplayName("POST /dependency")
    class Dependency {
        @Test
        @DisplayName("valid DAG")
        void validDag() {
            var req = new FormulaDependencyRequest();
            var f1 = new FormulaDependencyRequest.FormulaFieldConfig();
            f1.setFieldName("total"); f1.setExpression("price * quantity");
            f1.setType("CALC"); f1.setDependsOn(List.of("price", "quantity"));
            var f2 = new FormulaDependencyRequest.FormulaFieldConfig();
            f2.setFieldName("grandTotal"); f2.setExpression("total * 1.1");
            f2.setType("CALC"); f2.setDependsOn(List.of("total"));
            req.setFormulas(List.of(f1, f2));
            RespInfo<FormulaDependencyResponse> resp = controller.dependency(req);
            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isValid());
            assertEquals(2, resp.getData().getTopologicalOrder().size());
            assertFalse(resp.getData().isHasCycle());
        }

        @Test
        @DisplayName("cycle detected")
        void cycleDetected() {
            var req = new FormulaDependencyRequest();
            var f1 = new FormulaDependencyRequest.FormulaFieldConfig();
            f1.setFieldName("a"); f1.setExpression("b * 2");
            f1.setType("CALC"); f1.setDependsOn(List.of("b"));
            var f2 = new FormulaDependencyRequest.FormulaFieldConfig();
            f2.setFieldName("b"); f2.setExpression("a + 1");
            f2.setType("CALC"); f2.setDependsOn(List.of("a"));
            req.setFormulas(List.of(f1, f2));
            RespInfo<FormulaDependencyResponse> resp = controller.dependency(req);
            assertTrue(resp.getCode() == 200);
            assertFalse(resp.getData().isValid());
            assertTrue(resp.getData().isHasCycle());
        }
    }

    @Nested
    @DisplayName("POST /dependency/graph")
    class DependencyGraph {
        @Test
        @DisplayName("returns graph nodes and edges")
        void returnsGraph() {
            FormulaDependencyGraphRequest req = new FormulaDependencyGraphRequest();
            FormulaDependencyGraphRequest.FormulaFieldConfig formula = new FormulaDependencyGraphRequest.FormulaFieldConfig();
            formula.setFieldCode("total");
            formula.setType("CALC");
            formula.setExpression("price * quantity");
            formula.setDependsOn(List.of("price", "quantity"));
            req.setFormulas(List.of(formula));

            RespInfo<FormulaDependencyGraphResponse> resp = controller.dependencyGraph(req);

            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isValid(), "Errors: " + resp.getData().getErrors());
            assertEquals(3, resp.getData().getNodes().size());
            assertEquals(2, resp.getData().getEdges().size());
            assertTrue(resp.getData().getEdges().stream()
                .allMatch(edge -> "DEPENDS_ON".equals(edge.getType())));
        }
    }

    @Nested
    @DisplayName("POST /rule")
    class Rule {
        @Test
        @DisplayName("compile returns expression")
        void compileRule() {
            ConditionRuleCompileRequest req = new ConditionRuleCompileRequest();
            req.setRule(ConditionRuleNode.group("AND", List.of(
                ConditionRuleNode.condition("amount", "GT", 1000),
                ConditionRuleNode.condition("status", "EQ", "ACTIVE")
            )));

            RespInfo<ConditionRuleCompileResponse> resp = controller.compileConditionRule(req);

            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isValid(), "Errors: " + resp.getData().getErrors());
            assertEquals("(amount > 1000 && status == 'ACTIVE')", resp.getData().getExpression());
        }

        @Test
        @DisplayName("validate returns structured errors")
        void validateRule() {
            ConditionRuleCompileRequest req = new ConditionRuleCompileRequest();
            req.setRule(ConditionRuleNode.group("AND", List.of()));

            RespInfo<ConditionRuleCompileResponse> resp = controller.validateConditionRule(req);

            assertTrue(resp.getCode() == 200);
            assertFalse(resp.getData().isValid());
            assertTrue(resp.getData().getErrors().get(0).contains("分组不能为空"));
        }
    }

    @Nested
    @DisplayName("GET /functions")
    class Functions {
        @Test
        @DisplayName("returns function list")
        void returnsFunctions() {
            RespInfo<List<FormulaFunctionResponse>> resp = controller.functions();
            assertTrue(resp.getCode() == 200);
            assertFalse(resp.getData().isEmpty());
            assertTrue(resp.getData().size() > 10);
        }

        @Test
        @DisplayName("each function has required fields")
        void functionFields() {
            RespInfo<List<FormulaFunctionResponse>> resp = controller.functions();
            for (var fn : resp.getData()) {
                assertNotNull(fn.getName());
                assertNotNull(fn.getCategory());
                assertNotNull(fn.getDescription());
            }
        }
    }

    @Nested
    @DisplayName("GET /function-market")
    class FunctionMarket {
        @Test
        @DisplayName("page returns function market records")
        void page() {
            RespInfo<Page<FormulaFunctionMarketResponse>> resp = controller.functionMarketPage(
                1,
                10,
                new FormulaFunctionMarketQueryDTO()
            );

            assertTrue(resp.getCode() == 200);
            assertEquals(1, resp.getData().getCurrent());
            assertEquals(10, resp.getData().getSize());
            assertTrue(resp.getData().getTotal() > 10);
            assertFalse(resp.getData().getRecords().isEmpty());
        }

        @Test
        @DisplayName("detail returns function metadata")
        void detail() {
            RespInfo<FormulaFunctionMarketResponse> resp = controller.functionMarketDetail("math.max");

            assertTrue(resp.getCode() == 200);
            assertEquals("math.max", resp.getData().getFunctionCode());
            assertEquals("Math", resp.getData().getCategory());
            assertEquals(Boolean.TRUE, resp.getData().getEnabled());
        }

        @Test
        @DisplayName("install enables selected version")
        void install() {
            FormulaFunctionInstallRequest request = new FormulaFunctionInstallRequest();
            request.setFunctionCode("math.max");
            request.setVersion("1.0.0");
            request.setEnabled(true);

            RespInfo<FormulaFunctionMarketResponse> resp = controller.installFunction(request);

            assertTrue(resp.getCode() == 200);
            assertEquals("math.max", resp.getData().getFunctionCode());
            assertEquals("INSTALLED", resp.getData().getInstallStatus());
            assertEquals("1.0.0", resp.getData().getInstalledVersion());
            assertEquals(Boolean.TRUE, resp.getData().getEnabled());
        }

        @Test
        @DisplayName("enable and disable return updated status")
        void enableDisable() {
            RespInfo<FormulaFunctionMarketResponse> enabled = controller.enableFunction("math.max");
            RespInfo<FormulaFunctionMarketResponse> disabled = controller.disableFunction("math.max");

            assertTrue(enabled.getCode() == 200);
            assertEquals(Boolean.TRUE, enabled.getData().getEnabled());
            assertTrue(disabled.getCode() == 200);
            assertEquals(Boolean.FALSE, disabled.getData().getEnabled());
        }
    }

    @Nested
    @DisplayName("GET /log")
    class ExecutionLog {
        @Test
        @DisplayName("page binds query filters")
        void pageBindsQueryFilters() {
            LocalDateTime beginTime = LocalDateTime.of(2026, 6, 13, 10, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2026, 6, 13, 11, 0, 0);

            RespInfo<Page<FormulaExecutionLogResponse>> resp = controller.logPage(
                2,
                20,
                "order",
                "1001",
                "totalAmount",
                false,
                "FML-001",
                beginTime,
                endTime
            );

            assertTrue(resp.getCode() == 200);
            assertEquals(2, resp.getData().getCurrent());
            assertEquals(20, resp.getData().getSize());
            assertEquals(1, resp.getData().getRecords().size());
            assertEquals("FML-001", logService.lastQuery.getTraceId());
            assertEquals("order", logService.lastQuery.getObjectCode());
            assertEquals("1001", logService.lastQuery.getRecordId());
            assertEquals("totalAmount", logService.lastQuery.getFieldCode());
            assertEquals(Boolean.FALSE, logService.lastQuery.getSuccess());
            assertEquals(beginTime, logService.lastQuery.getBeginTime());
            assertEquals(endTime, logService.lastQuery.getEndTime());
        }

        @Test
        @DisplayName("detail returns masked log detail")
        void detailReturnsMaskedLogDetail() {
            RespInfo<FormulaExecutionLogDetailResponse> resp = controller.logDetail(99L);

            assertTrue(resp.getCode() == 200);
            assertEquals(99L, logService.detailId);
            assertEquals("FML-DETAIL", resp.getData().getTraceId());
            assertEquals("order", resp.getData().getObjectCode());
            assertTrue(resp.getData().getInputSnapshot().contains("138****5678"));
        }
    }

    @Nested
    @DisplayName("POST /debug")
    class Debug {
        @Test
        @DisplayName("debug formula returns trace steps")
        void debugFormula() {
            FormulaDebugRequest req = new FormulaDebugRequest();
            req.setObjectCode("order");
            req.setRecordId("1001");
            req.setFieldCode("totalAmount");
            req.setSampleValues(Map.of("price", 100L, "quantity", 3L));

            FormulaDebugRequest.FormulaFieldConfig formula = new FormulaDebugRequest.FormulaFieldConfig();
            formula.setFieldCode("totalAmount");
            formula.setType("CALC");
            formula.setExpression("price * quantity");
            formula.setDependsOn(List.of("price", "quantity"));
            req.setFormulas(List.of(formula));

            RespInfo<FormulaDebugResponse> resp = controller.debug(req);

            assertTrue(resp.getCode() == 200);
            assertTrue(resp.getData().isSuccess(), "Errors: " + resp.getData().getErrors());
            assertNotNull(resp.getData().getTraceId());
            assertEquals(List.of("totalAmount"), resp.getData().getExecutionPlan());
            assertEquals(300L, resp.getData().getResult().get("totalAmount"));
            assertEquals(1, resp.getData().getSteps().size());
            assertFalse(logService.records.isEmpty());
            assertEquals(resp.getData().getTraceId(), logService.records.get(0).getTraceId());
        }
    }

    private static class CapturingLogService extends FormulaExecutionLogService {
        private FormulaExecutionLogQueryDTO lastQuery;
        private Long detailId;
        private final List<FormulaExecutionLogResponse> records = new ArrayList<>();

        CapturingLogService() {
            super(null);
        }

        @Override
        public void record(FormulaExecutionLogResponse log) {
            records.add(log);
        }

        @Override
        public Page<FormulaExecutionLogResponse> page(FormulaExecutionLogQueryDTO query) {
            this.lastQuery = query;
            Page<FormulaExecutionLogResponse> page = new Page<>(query.getPageNum(), query.getPageSize(), 1);
            page.setRecords(List.of(log(query.getTraceId())));
            return page;
        }

        @Override
        public FormulaExecutionLogResponse detail(Long id) {
            this.detailId = id;
            return log("FML-DETAIL");
        }

        private FormulaExecutionLogResponse log(String traceId) {
            return FormulaExecutionLogResponse.builder()
                .id(99L)
                .tenantId(1L)
                .traceId(traceId)
                .objectCode("order")
                .recordId("1001")
                .fieldCode("totalAmount")
                .formulaType("CALC")
                .formulaMode("STORED")
                .expression("price * quantity")
                .inputSnapshot("{\"mobile\":\"138****5678\"}")
                .outputValue("300")
                .success(true)
                .elapsedMs(5L)
                .build();
        }
    }
}
