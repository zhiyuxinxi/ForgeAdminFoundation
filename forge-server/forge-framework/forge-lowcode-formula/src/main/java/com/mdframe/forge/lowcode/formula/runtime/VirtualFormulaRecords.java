package com.mdframe.forge.lowcode.formula.runtime;

import com.mdframe.forge.lowcode.formula.runtime.port.FormulaCrossObjects;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaConfig;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import java.util.*;

/** Cross-object prefetch and projection, preserving the caller's extraction extension point. */
public final class VirtualFormulaRecords {
    private final FormulaCrossObjects crossObjectResolver;

    public VirtualFormulaRecords(FormulaCrossObjects crossObjectResolver) {
        this.crossObjectResolver = crossObjectResolver;
    }

    public Prepared prepare(List<Map<String, Object>> records, Map<String, FormulaConfig> formulas,
                            FormulaRuntimeContext context) {
        if (crossObjectResolver == null || !crossObjectResolver.hasCrossObject(formulas)) {
            return new Prepared(records, records, formulas, false);
        }
        List<Map<String, Object>> workingRecords = copyRecords(records);
        crossObjectResolver.prefetch(formulas, workingRecords, context);
        return new Prepared(records, workingRecords, formulas, true);
    }

    public List<Map<String, Object>> finish(Prepared prepared, List<Map<String, Object>> calculated) {
        if (!prepared.project()) return calculated;
        copyFormulaValues(calculated, prepared.originalRecords(), prepared.formulas().keySet());
        return prepared.originalRecords();
    }

    public record Prepared(List<Map<String, Object>> originalRecords, List<Map<String, Object>> workingRecords,
                           Map<String, FormulaConfig> formulas, boolean project) {
    }

    private List<Map<String, Object>> copyRecords(List<Map<String, Object>> records) {
        List<Map<String, Object>> copies = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            copies.add(record == null ? new LinkedHashMap<>() : new LinkedHashMap<>(record));
        }
        return copies;
    }

    private void copyFormulaValues(List<Map<String, Object>> sourceRecords,
                                   List<Map<String, Object>> targetRecords,
                                   Set<String> formulaFields) {
        int size = Math.min(sourceRecords.size(), targetRecords.size());
        for (int i = 0; i < size; i++) {
            Map<String, Object> source = sourceRecords.get(i);
            Map<String, Object> target = targetRecords.get(i);
            if (source == null || target == null) {
                continue;
            }
            for (String field : formulaFields) {
                if (source.containsKey(field)) {
                    target.put(field, source.get(field));
                }
            }
        }
    }
}
