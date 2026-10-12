package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;

/**
 * Runtime context for formula execution.
 * <p>
 * Carries the execution environment needed by {@code DbAggregateDataProvider}
 * and other runtime formula components to resolve data sources.
 */
public class FormulaRuntimeContext {
    private final Long tenantId;
    private final String suiteCode;
    private final String sourceObjectCode;
    private final Map<String, Object> currentRow;

    public FormulaRuntimeContext(Long tenantId, String suiteCode,
                                  String sourceObjectCode, Map<String, Object> currentRow) {
        this.tenantId = tenantId;
        this.suiteCode = Objects.requireNonNull(suiteCode, "suiteCode");
        this.sourceObjectCode = Objects.requireNonNull(sourceObjectCode, "sourceObjectCode");
        this.currentRow = currentRow != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(currentRow))
            : Collections.emptyMap();
    }

    public Long getTenantId() { return tenantId; }
    public String getSuiteCode() { return suiteCode; }
    public String getSourceObjectCode() { return sourceObjectCode; }
    public Map<String, Object> getCurrentRow() { return currentRow; }

    /** Get a field value from the current row, or null. */
    public Object getFieldValue(String fieldName) {
        return currentRow.get(fieldName);
    }

    @Override
    public String toString() {
        return "FormulaRuntimeContext{tenant=" + tenantId
            + ", suite=" + suiteCode
            + ", sourceObject=" + sourceObjectCode + "}";
    }
}
