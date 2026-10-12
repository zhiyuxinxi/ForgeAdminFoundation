package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateConfig;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateFunction;
import com.mdframe.forge.plugin.generator.domain.formula.ConditionConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;
import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectRecomputeMode;
import com.mdframe.forge.plugin.generator.domain.formula.ExecutionResult;
import com.mdframe.forge.plugin.generator.domain.formula.ExpressionParser;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionStep;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaExecutionTrace;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaType;
import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDebugRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaDebugResponse;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 公式调试服务。
 */
@Slf4j
@Service
public class FormulaDebugService {

    private static final String DEBUG_ERROR_FIELD = "_debug";

    private final FormulaExecutionEngine executionEngine;
    private final FormulaExecutionLogService executionLogService;
    private final ObjectMapper objectMapper;
    private final FormulaRuntimeProperties runtimeProperties;
    private final ExpressionParser expressionParser = new ExpressionParser();

    public FormulaDebugService(FormulaExecutionEngine executionEngine,
                               FormulaExecutionLogService executionLogService,
                               ObjectMapper objectMapper) {
        this(executionEngine, executionLogService, objectMapper, new FormulaRuntimeProperties());
    }

    @Autowired
    public FormulaDebugService(FormulaExecutionEngine executionEngine,
                               FormulaExecutionLogService executionLogService,
                               ObjectMapper objectMapper,
                               FormulaRuntimeProperties runtimeProperties) {
        this.executionEngine = executionEngine;
        this.executionLogService = executionLogService;
        this.objectMapper = objectMapper;
        this.runtimeProperties = runtimeProperties == null ? new FormulaRuntimeProperties() : runtimeProperties;
    }

    public FormulaDebugResponse debug(FormulaDebugRequest request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> contextBefore = sampleValues(request);

        try {
            if (request == null) {
                throw new IllegalArgumentException("调试请求不能为空");
            }
            Map<String, FormulaConfig> allFormulas = buildFormulaMap(request.getFormulas());
            Map<String, FormulaConfig> debugFormulas = selectDebugFormulas(allFormulas, request.getFieldCode());

            ExecutionResult result = executionEngine.execute(debugFormulas, contextBefore, debugTraceOptions());
            FormulaExecutionTrace trace = result.getTrace();
            Map<String, Object> contextAfter = new LinkedHashMap<>(contextBefore);
            contextAfter.putAll(result.getResults());

            recordDebugLogs(request, contextBefore, debugFormulas, result);

            return FormulaDebugResponse.builder()
                    .success(result.isSuccess())
                    .traceId(trace == null ? null : trace.getTraceId())
                    .executionPlan(trace == null ? List.of() : trace.getExecutionPlan())
                    .steps(trace == null ? List.of() : trace.getSteps())
                    .contextBefore(contextBefore)
                    .contextAfter(contextAfter)
                    .result(result.getResults())
                    .errors(result.getErrors())
                    .elapsedMs(result.getElapsedMs())
                    .build();
        } catch (Exception e) {
            Map<String, List<String>> errors = new LinkedHashMap<>();
            errors.put(DEBUG_ERROR_FIELD, List.of(e.getMessage()));
            return FormulaDebugResponse.builder()
                    .success(false)
                    .executionPlan(List.of())
                    .steps(List.of())
                    .contextBefore(contextBefore)
                    .contextAfter(contextBefore)
                    .result(Map.of())
                    .errors(errors)
                    .elapsedMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private Map<String, Object> sampleValues(FormulaDebugRequest request) {
        if (request == null || request.getSampleValues() == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(request.getSampleValues());
    }

    private Map<String, FormulaConfig> buildFormulaMap(List<FormulaDebugRequest.FormulaFieldConfig> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("调试公式不能为空");
        }
        Map<String, FormulaConfig> result = new LinkedHashMap<>();
        for (FormulaDebugRequest.FormulaFieldConfig field : fields) {
            String fieldCode = trimToNull(field == null ? null : field.resolvedFieldCode());
            if (fieldCode == null) {
                throw new IllegalArgumentException("公式字段编码不能为空");
            }
            if (result.containsKey(fieldCode)) {
                throw new IllegalArgumentException(UiMessages.translate("公式字段编码重复: ") + fieldCode);
            }
            result.put(fieldCode, buildFormulaConfig(field));
        }
        return result;
    }

    private FormulaConfig buildFormulaConfig(FormulaDebugRequest.FormulaFieldConfig field) {
        FormulaType type = parseEnum(FormulaType.class, field.getType(), FormulaType.CALC);
        FormulaMode mode = parseEnum(FormulaMode.class, field.getMode(), FormulaMode.STORED);
        FormulaConfig.Builder builder = FormulaConfig.builder()
                .type(type)
                .mode(mode)
                .expression(field.getExpression())
                .dependsOn(field.getDependsOn() == null ? List.of() : field.getDependsOn())
                .crossObject(buildCrossObjectConfig(field.getCrossObject()));

        if (type == FormulaType.AGGREGATE) {
            builder.aggregate(buildAggregateConfig(field.getAggregate()));
        }
        if (type == FormulaType.CONDITIONAL) {
            builder.condition(buildConditionConfig(field));
        }
        if (type == FormulaType.LOOKUP) {
            builder.lookup(buildLookupConfig(field.getLookup()));
        }
        return builder.build();
    }

    private AggregateConfig buildAggregateConfig(FormulaDebugRequest.AggregateDebug aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("AGGREGATE 公式缺少聚合配置");
        }
        AggregateFunction function = parseEnum(AggregateFunction.class, aggregate.getFunction(), AggregateFunction.SUM);
        return new AggregateConfig(function, aggregate.getRelationCode(), aggregate.getTargetField(), aggregate.getFilter());
    }

    private ConditionConfig buildConditionConfig(FormulaDebugRequest.FormulaFieldConfig field) {
        FormulaDebugRequest.ConditionDebug condition = field.getCondition();
        String expression = condition == null ? field.getExpression() : condition.getExpression();
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("CONDITIONAL 公式缺少条件表达式");
        }
        Object trueValue = condition == null ? Boolean.TRUE : condition.getTrueValue();
        Object falseValue = condition == null ? Boolean.FALSE : condition.getFalseValue();
        return new ConditionConfig(expression, trueValue, falseValue);
    }

    private LookupConfig buildLookupConfig(FormulaDebugRequest.LookupDebug lookup) {
        if (lookup == null) {
            throw new IllegalArgumentException("LOOKUP 公式缺少查询配置");
        }
        return new LookupConfig(
                lookup.getRelationCode(),
                lookup.getTargetObjectCode(),
                lookup.getSourceField(),
                lookup.getTargetField(),
                lookup.getReturnField(),
                lookup.getNotFoundValue());
    }

    private CrossObjectConfig buildCrossObjectConfig(FormulaDebugRequest.CrossObjectDebug crossObject) {
        if (crossObject == null) {
            return null;
        }
        return new CrossObjectConfig(
                crossObject.getPath(),
                crossObject.getRelationCode(),
                crossObject.getTargetObjectCode(),
                crossObject.getReturnField(),
                parseEnum(CrossObjectRecomputeMode.class, crossObject.getRecomputeMode(), CrossObjectRecomputeMode.ASYNC));
    }

    private Map<String, FormulaConfig> selectDebugFormulas(Map<String, FormulaConfig> allFormulas, String fieldCode) {
        String targetFieldCode = trimToNull(fieldCode);
        if (targetFieldCode == null) {
            return allFormulas;
        }
        if (!allFormulas.containsKey(targetFieldCode)) {
            throw new IllegalArgumentException(UiMessages.translate("调试字段不存在: ") + targetFieldCode);
        }
        Set<String> included = new LinkedHashSet<>();
        collectDependencies(targetFieldCode, allFormulas, included, new LinkedHashSet<>());

        Map<String, FormulaConfig> result = new LinkedHashMap<>();
        for (Map.Entry<String, FormulaConfig> entry : allFormulas.entrySet()) {
            if (included.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void collectDependencies(String fieldCode,
                                     Map<String, FormulaConfig> formulas,
                                     Set<String> included,
                                     Set<String> visiting) {
        if (!visiting.add(fieldCode)) {
            included.add(fieldCode);
            return;
        }
        included.add(fieldCode);
        FormulaConfig config = formulas.get(fieldCode);
        if (config != null) {
            for (String dependency : formulaDependencies(config)) {
                if (formulas.containsKey(dependency)) {
                    collectDependencies(dependency, formulas, included, visiting);
                }
            }
        }
        visiting.remove(fieldCode);
    }

    private Set<String> formulaDependencies(FormulaConfig config) {
        Set<String> dependencies = new LinkedHashSet<>();
        if (config.getDependsOn() != null) {
            dependencies.addAll(config.getDependsOn());
        }
        if (config.isCalc()) {
            dependencies.addAll(parseVariables(config.getExpression()));
        }
        if (config.isConditional() && config.getCondition() != null) {
            dependencies.addAll(parseVariables(config.getCondition().getExpression()));
        }
        if (config.isLookup() && config.getLookup() != null) {
            dependencies.add(config.getLookup().getSourceField());
        }
        return dependencies;
    }

    private List<String> parseVariables(String expression) {
        if (expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }
        var parseResult = expressionParser.parse(expression);
        if (!parseResult.isValid()) {
            return Collections.emptyList();
        }
        return parseResult.getVariables();
    }

    private void recordDebugLogs(FormulaDebugRequest request,
                                 Map<String, Object> contextBefore,
                                 Map<String, FormulaConfig> formulas,
                                 ExecutionResult result) {
        if (executionLogService == null || !runtimeProperties.isDebugLogEnabled() || !result.hasTrace()) {
            return;
        }
        for (FormulaExecutionStep step : result.getTrace().getSteps()) {
            FormulaConfig config = formulas.get(step.getFieldCode());
            FormulaExecutionLogResponse logEntry = FormulaExecutionLogResponse.builder()
                    .traceId(result.getTrace().getTraceId())
                    .objectCode(request.getObjectCode())
                    .recordId(recordId(request, contextBefore))
                    .fieldCode(step.getFieldCode())
                    .formulaType(config == null ? step.getFormulaType() : config.getType().name())
                    .formulaMode(config == null ? null : config.getMode().name())
                    .expression(step.getExpression())
                    .inputSnapshot(step.getInput().isEmpty() ? null : toJson(step.getInput()))
                    .outputValue(step.getOutput() == null ? null : toJson(step.getOutput()))
                    .success(step.isSuccess())
                    .errorMessage(step.getErrorMessage())
                    .elapsedMs(step.getElapsedMs())
                    .build();
            safeRecord(logEntry);
        }
    }

    private void safeRecord(FormulaExecutionLogResponse logEntry) {
        try {
            executionLogService.record(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record formula debug log: {}", e.getMessage());
        }
    }

    private String recordId(FormulaDebugRequest request, Map<String, Object> contextBefore) {
        if (request.getRecordId() != null && !request.getRecordId().isBlank()) {
            return request.getRecordId();
        }
        Object id = contextBefore.get("id");
        return id == null ? null : id.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private FormulaTraceOptions debugTraceOptions() {
        return FormulaTraceOptions.builder()
                .enabled(true)
                .debugMode(true)
                .includeInputSnapshot(runtimeProperties.isIncludeInputSnapshot())
                .includeOutputValue(runtimeProperties.isIncludeOutputValue())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(UiMessages.translate("不支持的") + enumType.getSimpleName() + ": " + rawValue);
        }
    }
}
