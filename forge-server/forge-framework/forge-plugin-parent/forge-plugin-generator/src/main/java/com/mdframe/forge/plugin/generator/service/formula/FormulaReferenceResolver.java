package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.formula.CrossObjectConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Test seam for resolving target records used by cross-object formulas.
 */
@FunctionalInterface
public interface FormulaReferenceResolver {

    List<Map<String, Object>> fetchTargetRecords(CrossObjectConfig config,
                                                 Collection<?> sourceValues,
                                                 Map<String, Object> context);
}
