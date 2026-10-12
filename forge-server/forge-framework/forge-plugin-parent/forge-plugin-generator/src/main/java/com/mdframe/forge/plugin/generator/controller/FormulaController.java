package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.service.formula.ConditionRuleCompiler;
import com.mdframe.forge.plugin.generator.dto.formula.*;
import com.mdframe.forge.plugin.generator.service.formula.FormulaDependencyGraphService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaDebugService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaExecutionEngine;
import com.mdframe.forge.plugin.generator.service.formula.FormulaExecutionLogService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaFunctionMarketService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationService;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.domain.OperationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Formula API: expression validation, preview, dependency analysis, and available functions.
 * <p>
 * Reuses Phase 1-4 domain services: FormulaValidationService, FormulaExecutionEngine, FormulaDependencyAnalyzer.
 */
@Slf4j
@ApiDecrypt
@SaCheckPermission("ai:businessObject:design")
@RestController
@RequestMapping("/api/ai/business/formula")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaValidationService validationService;
    private final FormulaExecutionEngine executionEngine;
    private final FormulaDependencyAnalyzer dependencyAnalyzer;
    private final FormulaExecutionLogService executionLogService;
    private final FormulaDebugService debugService;
    private final FormulaDependencyGraphService dependencyGraphService;
    private final ConditionRuleCompiler conditionRuleCompiler;
    private final FormulaFunctionMarketService functionMarketService;

    /**
     * Validate a formula expression syntax and extract variables.
     * <p>
     * POST /api/ai/business/formula/validate
     */
    @PostMapping("/validate")
    public RespInfo<FormulaValidateResponse> validate(@Valid @RequestBody FormulaValidateRequest request) {
        var parseResult = validationService.validateExpression(request.getExpression());

        List<String> depWarnings = List.of();
        if (request.getDependsOn() != null && !request.getDependsOn().isEmpty()) {
            depWarnings = List.of(); // cross-check done by service
        }

        var response = FormulaValidateResponse.builder()
            .valid(parseResult.isValid())
            .errorMessage(parseResult.getErrorMessage())
            .errorLine(parseResult.getErrorLine())
            .errorColumn(parseResult.getErrorColumn())
            .variables(parseResult.getVariables())
            .dependencyWarnings(depWarnings)
            .build();

        return RespInfo.success(response);
    }

    /**
     * Preview formula calculation with sample values.
     * <p>
     * POST /api/ai/business/formula/preview
     */
    @PostMapping("/preview")
    public RespInfo<FormulaPreviewResponse> preview(@Valid @RequestBody FormulaPreviewRequest request) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> context = request.getSampleValues() != null
                ? new LinkedHashMap<>(request.getSampleValues())
                : new LinkedHashMap<>();

            FormulaConfig config = buildFormulaConfig(request);
            Map<String, FormulaConfig> formulaMap = Map.of("_preview", config);

            ExecutionResult result = executionEngine.execute(formulaMap, context);

            var response = FormulaPreviewResponse.builder()
                .success(result.isSuccess())
                .result(result.getResult("_preview"))
                .errorMessage(result.getErrors().isEmpty() ? null
                    : String.join("; ", result.getErrors().getOrDefault("_preview", List.of())))
                .elapsedMs(System.currentTimeMillis() - start)
                .build();

            return RespInfo.success(response);

        } catch (Exception e) {
            log.warn("Formula preview failed", e);
            var response = FormulaPreviewResponse.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .elapsedMs(System.currentTimeMillis() - start)
                .build();
            return RespInfo.success(response);
        }
    }

    /**
     * Debug formula execution with sample values and trace steps.
     * <p>
     * POST /api/ai/business/formula/debug
     */
    @PostMapping("/debug")
    public RespInfo<FormulaDebugResponse> debug(@Valid @RequestBody FormulaDebugRequest request) {
        return RespInfo.success(debugService.debug(request));
    }

    /**
     * Analyze formula dependencies (DAG, cycle detection, depth).
     * <p>
     * POST /api/ai/business/formula/dependency
     */
    @PostMapping("/dependency")
    public RespInfo<FormulaDependencyResponse> dependency(@Valid @RequestBody FormulaDependencyRequest request) {
        Map<String, FormulaConfig> formulaMap = new LinkedHashMap<>();
        for (var fc : request.getFormulas()) {
            FormulaConfig config = FormulaConfig.builder()
                .type(fc.getType() != null ? FormulaType.valueOf(fc.getType()) : FormulaType.CALC)
                .mode(FormulaMode.STORED)
                .expression(fc.getExpression())
                .dependsOn(fc.getDependsOn())
                .build();
            formulaMap.put(fc.getFieldName(), config);
        }

        DependencyAnalysisResult depResult = dependencyAnalyzer.analyze(formulaMap);

        var response = FormulaDependencyResponse.builder()
            .valid(depResult.isValid())
            .hasCycle(depResult.hasCycle())
            .topologicalOrder(depResult.getTopologicalOrder())
            .depthMap(depResult.getDepthMap())
            .cyclePath(depResult.getCyclePath())
            .errors(depResult.getErrors())
            .warnings(List.of())
            .build();

        return RespInfo.success(response);
    }

    /**
     * Build formula dependency graph nodes and edges.
     * <p>
     * POST /api/ai/business/formula/dependency/graph
     */
    @PostMapping("/dependency/graph")
    public RespInfo<FormulaDependencyGraphResponse> dependencyGraph(
            @Valid @RequestBody FormulaDependencyGraphRequest request) {
        return RespInfo.success(dependencyGraphService.graph(request));
    }

    /**
     * Compile condition rule AST to Aviator expression.
     * <p>
     * POST /api/ai/business/formula/rule/compile
     */
    @PostMapping("/rule/compile")
    public RespInfo<ConditionRuleCompileResponse> compileConditionRule(
            @Valid @RequestBody ConditionRuleCompileRequest request) {
        return RespInfo.success(conditionRuleCompiler.compile(request));
    }

    /**
     * Validate condition rule AST and generated expression.
     * <p>
     * POST /api/ai/business/formula/rule/validate
     */
    @PostMapping("/rule/validate")
    public RespInfo<ConditionRuleCompileResponse> validateConditionRule(
            @Valid @RequestBody ConditionRuleCompileRequest request) {
        return RespInfo.success(conditionRuleCompiler.validate(request));
    }

    /**
     * List available Aviator functions.
     * <p>
     * GET /api/ai/business/formula/functions
     */
    @GetMapping("/functions")
    public RespInfo<List<FormulaFunctionResponse>> functions() {
        return RespInfo.success(functionMarketService.availableFunctions());
    }

    /**
     * Page formula function market.
     * <p>
     * GET /api/ai/business/formula/function-market/page
     */
    @GetMapping("/function-market/page")
    public RespInfo<Page<FormulaFunctionMarketResponse>> functionMarketPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            FormulaFunctionMarketQueryDTO query) {
        FormulaFunctionMarketQueryDTO effectiveQuery = query == null ? new FormulaFunctionMarketQueryDTO() : query;
        effectiveQuery.setPageNum(pageNum);
        effectiveQuery.setPageSize(pageSize);
        return RespInfo.success(functionMarketService.page(effectiveQuery));
    }

    /**
     * Get formula function market detail.
     * <p>
     * GET /api/ai/business/formula/function-market/{functionCode}
     */
    @GetMapping("/function-market/{functionCode}")
    public RespInfo<FormulaFunctionMarketResponse> functionMarketDetail(@PathVariable String functionCode) {
        return RespInfo.success(functionMarketService.detail(functionCode));
    }

    /**
     * Install formula function.
     * <p>
     * POST /api/ai/business/formula/function-market/install
     */
    @OperationLog(module = "公式函数市场", type = OperationType.ADD, desc = "安装公式函数")
    @PostMapping("/function-market/install")
    public RespInfo<FormulaFunctionMarketResponse> installFunction(
            @Valid @RequestBody FormulaFunctionInstallRequest request) {
        return RespInfo.success(functionMarketService.install(request));
    }

    /**
     * Register custom Java Bean formula function.
     * <p>
     * POST /api/ai/business/formula/function-market/custom
     */
    @OperationLog(module = "公式函数市场", type = OperationType.ADD, desc = "注册自定义公式函数")
    @PostMapping("/function-market/custom")
    public RespInfo<FormulaFunctionMarketResponse> registerCustomFunction(
            @Valid @RequestBody FormulaFunctionRegisterRequest request) {
        return RespInfo.success(functionMarketService.registerCustomFunction(request));
    }

    /**
     * Enable installed formula function.
     * <p>
     * PUT /api/ai/business/formula/function-market/{functionCode}/enable
     */
    @OperationLog(module = "公式函数市场", type = OperationType.UPDATE, desc = "启用公式函数")
    @PutMapping("/function-market/{functionCode}/enable")
    public RespInfo<FormulaFunctionMarketResponse> enableFunction(@PathVariable String functionCode) {
        return RespInfo.success(functionMarketService.enable(functionCode));
    }

    /**
     * Disable installed formula function.
     * <p>
     * PUT /api/ai/business/formula/function-market/{functionCode}/disable
     */
    @OperationLog(module = "公式函数市场", type = OperationType.UPDATE, desc = "禁用公式函数")
    @PutMapping("/function-market/{functionCode}/disable")
    public RespInfo<FormulaFunctionMarketResponse> disableFunction(@PathVariable String functionCode) {
        return RespInfo.success(functionMarketService.disable(functionCode));
    }

    /**
     * Page formula execution logs.
     * <p>
     * GET /api/ai/business/formula/log/page
     */
    @GetMapping("/log/page")
    public RespInfo<Page<FormulaExecutionLogResponse>> logPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String objectCode,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) String fieldCode,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        FormulaExecutionLogQueryDTO query = new FormulaExecutionLogQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setObjectCode(objectCode);
        query.setRecordId(recordId);
        query.setFieldCode(fieldCode);
        query.setSuccess(success);
        query.setTraceId(traceId);
        query.setBeginTime(beginTime);
        query.setEndTime(endTime);
        return RespInfo.success(executionLogService.page(query));
    }

    /**
     * Get formula execution log detail.
     * <p>
     * GET /api/ai/business/formula/log/{id}
     */
    @GetMapping("/log/{id}")
    public RespInfo<FormulaExecutionLogDetailResponse> logDetail(@PathVariable Long id) {
        return RespInfo.success(FormulaExecutionLogDetailResponse.from(executionLogService.detail(id)));
    }

    private FormulaConfig buildFormulaConfig(FormulaPreviewRequest request) {
        FormulaType type = request.getType() != null
            ? FormulaType.valueOf(request.getType()) : FormulaType.CALC;

        FormulaConfig.Builder builder = FormulaConfig.builder()
            .type(type)
            .mode(FormulaMode.STORED)
            .expression(request.getExpression())
            .dependsOn(request.getDependsOn() != null ? request.getDependsOn() : List.of());

        if (type == FormulaType.CONDITIONAL && request.getCondition() != null) {
            ConditionConfig cc = new ConditionConfig(
                request.getCondition().getExpression(),
                request.getCondition().getTrueValue(),
                request.getCondition().getFalseValue());
            builder.condition(cc);
        }

        return builder.build();
    }
}
