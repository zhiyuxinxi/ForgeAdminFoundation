package com.mdframe.forge.lowcode.formula.runtime.port;

import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import java.util.List;
import java.util.Map;

/** Executes stored formulas through the production formula runtime. */
public interface StoredFormulaCalculation {
    List<Map<String, Object>> calculate(List<Map<String, Object>> records,
                                        LowcodeModelSchema modelSchema,
                                        FormulaRuntimeContext context);
}
