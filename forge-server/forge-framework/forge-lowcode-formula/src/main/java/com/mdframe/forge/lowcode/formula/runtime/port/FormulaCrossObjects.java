package com.mdframe.forge.lowcode.formula.runtime.port;

import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import java.util.List;
import java.util.Map;

/** Persistence boundary consumed by formula evaluation. */
public interface FormulaCrossObjects {
    boolean hasCrossObject(Map<String, FormulaConfig> formulas);

    void prefetch(Map<String, FormulaConfig> formulas, List<Map<String, Object>> records, FormulaRuntimeContext context);
}
