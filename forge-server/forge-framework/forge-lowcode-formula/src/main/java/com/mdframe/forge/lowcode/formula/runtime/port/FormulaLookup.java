package com.mdframe.forge.lowcode.formula.runtime.port;

import com.mdframe.forge.plugin.generator.domain.formula.LookupConfig;
import com.mdframe.forge.plugin.generator.service.formula.LookupResolveResult;
import java.util.Map;

/** Persistence boundary consumed by formula evaluation. */
public interface FormulaLookup {
    LookupResolveResult resolve(LookupConfig config, Map<String, Object> context);
}
