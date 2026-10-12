package com.mdframe.forge.lowcode.formula.runtime.port;

import com.mdframe.forge.plugin.generator.dto.formula.FormulaExecutionLogResponse;

/** Persistence boundary consumed by formula evaluation. */
public interface FormulaExecutionTraceSink {
    void record(FormulaExecutionLogResponse trace);
}
