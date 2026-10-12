package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.lowcode.formula.runtime.port.StoredFormulaCalculation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaMode;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executes STORED-mode formulas during record save/update.
 * <p>
 * Extends {@link AbstractFormulaRuntime} with STORED mode filtering.
 * Formula results are written back into the record for persistence.
 */
@Component
public class StoredFormulaRuntime extends AbstractFormulaRuntime implements StoredFormulaCalculation {

    public StoredFormulaRuntime(FormulaExecutionEngine executionEngine, ObjectMapper objectMapper) {
        super(executionEngine, objectMapper, "STORED");
    }

    public StoredFormulaRuntime(FormulaExecutionEngine executionEngine,
                                ObjectMapper objectMapper,
                                FormulaExecutionLogService executionLogService) {
        super(executionEngine, objectMapper, "STORED", executionLogService);
    }

    @Autowired
    public StoredFormulaRuntime(FormulaExecutionEngine executionEngine,
                                ObjectMapper objectMapper,
                                FormulaExecutionLogService executionLogService,
                                FormulaRuntimeProperties runtimeProperties) {
        super(executionEngine, objectMapper, "STORED", executionLogService, runtimeProperties);
    }

    @Override
    protected Map<String, FormulaConfig> extractFormulas(LowcodeModelSchema modelSchema) {
        return extractByMode(modelSchema, FormulaMode.STORED);
    }
}
