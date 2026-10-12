package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.port.VirtualFormulaCalculation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.formula.runtime.VirtualFormulaRecords;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaTraceOptions;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes VIRTUAL-mode formulas during record query.
 * <p>
 * Extends {@link AbstractFormulaRuntime} with VIRTUAL mode filtering.
 * Formula results are appended to the record in memory only (not persisted).
 */
@Component
public class VirtualFormulaRuntime extends AbstractFormulaRuntime implements VirtualFormulaCalculation {

    private final VirtualFormulaRecords virtualRecords;

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine, ObjectMapper objectMapper) {
        super(executionEngine, objectMapper, "VIRTUAL");
        this.virtualRecords = new VirtualFormulaRecords(null);
    }

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService);
        this.virtualRecords = new VirtualFormulaRecords(null);
    }

    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService,
                                 FormulaCrossObjectResolver crossObjectResolver) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService);
        this.virtualRecords = new VirtualFormulaRecords(Objects.requireNonNull(crossObjectResolver));
    }

    @Autowired
    public VirtualFormulaRuntime(FormulaExecutionEngine executionEngine,
                                 ObjectMapper objectMapper,
                                 FormulaExecutionLogService executionLogService,
                                 FormulaCrossObjectResolver crossObjectResolver,
                                 FormulaRuntimeProperties runtimeProperties) {
        super(executionEngine, objectMapper, "VIRTUAL", executionLogService, runtimeProperties);
        this.virtualRecords = new VirtualFormulaRecords(Objects.requireNonNull(crossObjectResolver));
    }

    @Override
    protected Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema) {
        return extractByMode(modelSchema, FormulaMode.VIRTUAL);
    }

    @Override
    public List<Map<String, Object>> calculate(List<Map<String, Object>> records,
                                               LowcodeModelSchema modelSchema,
                                               FormulaRuntimeContext context,
                                               FormulaTraceOptions traceOptions) {
        if (records == null || records.isEmpty()) {
            return records != null ? records : List.of();
        }
        Map<String, FormulaConfig> formulas = extractFormulas(modelSchema);
        VirtualFormulaRecords.Prepared prepared = virtualRecords.prepare(records, formulas, context);
        List<Map<String, Object>> calculated = super.calculate(
                prepared.workingRecords(), modelSchema, context, traceOptions);
        return virtualRecords.finish(prepared, calculated);
    }
}
