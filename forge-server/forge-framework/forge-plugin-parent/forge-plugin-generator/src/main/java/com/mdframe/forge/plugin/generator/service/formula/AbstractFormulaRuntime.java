package com.mdframe.forge.plugin.generator.service.formula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.formula.runtime.FormulaRecordRuntime;
import com.mdframe.forge.plugin.generator.domain.formula.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import java.util.*;

/** Preserves the subclass contract while calculation belongs to the runtime module. */
public abstract class AbstractFormulaRuntime {
    protected final FormulaExecutionEngine executionEngine;
    protected final ObjectMapper objectMapper;
    protected final FormulaExecutionLogService executionLogService;
    protected final FormulaRuntimeProperties runtimeProperties;
    private final String modeLabel;
    private final FormulaRecordRuntime runtime;

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel) {
        this(executionEngine, objectMapper, modeLabel, null);
    }

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel,
                                      FormulaExecutionLogService executionLogService) {
        this(executionEngine, objectMapper, modeLabel, executionLogService, new FormulaRuntimeProperties());
    }

    protected AbstractFormulaRuntime(FormulaExecutionEngine executionEngine,
                                      ObjectMapper objectMapper,
                                      String modeLabel,
                                      FormulaExecutionLogService executionLogService,
                                      FormulaRuntimeProperties runtimeProperties) {
        this.executionEngine = Objects.requireNonNull(executionEngine);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.modeLabel = Objects.requireNonNull(modeLabel);
        this.executionLogService = executionLogService;
        this.runtimeProperties = runtimeProperties == null ? new FormulaRuntimeProperties() : runtimeProperties;
        this.runtime = new FormulaRecordRuntime(executionEngine.runtime(), objectMapper, modeLabel,
                executionLogService, this.runtimeProperties);
    }

    protected abstract Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema);

    public List<Map<String, Object>> calculate(List<Map<String, Object>> records, LowcodeModelSchema modelSchema,
                                               FormulaRuntimeContext context) {
        return calculate(records, modelSchema, context, FormulaTraceOptions.disabled());
    }

    public List<Map<String, Object>> calculate(List<Map<String, Object>> records, LowcodeModelSchema modelSchema,
                                               FormulaRuntimeContext context, FormulaTraceOptions traceOptions) {
        if (records == null || records.isEmpty()) return records != null ? records : List.of();
        if (modelSchema == null || modelSchema.getFields() == null) return records;
        Map<String, FormulaConfig> formulas = extractFormulas(modelSchema);
        return runtime.calculate(records, formulas, context, traceOptions);
    }

    protected Map<String, FormulaConfig> extractByMode(LowcodeModelSchema modelSchema, FormulaMode targetMode) {
        return runtime.extractByMode(modelSchema, targetMode);
    }

    public FormulaConfig parseFormulaConfig(Map<String, Object> formula) {
        return runtime.parseFormulaConfig(formula);
    }
}
