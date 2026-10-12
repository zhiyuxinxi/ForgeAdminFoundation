package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;

/**
 * Aggregate formula engine - orchestrates aggregate computation over detail records.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Fetch detail records via {@link AggregateDataProvider}</li>
 *   <li>Apply optional filter (Aviator expression) via {@link ExpressionExecutor}</li>
 *   <li>Execute aggregate function via {@link AggregateFunctionExecutor}</li>
 *   <li>Return {@link AggregateResult}</li>
 * </ol>
 * <p>
 * Phase 3B: works with in-memory {@code List<Map<String,Object>>} for testing.
 * Phase 4: {@code AggregateDataProvider} will be backed by DB queries.
 * <p>
 * Pure domain logic - no DB, no Spring.
 */
public class AggregateEngine {

    private final AggregateFunctionExecutor functionExecutor;
    private final ExpressionExecutor expressionExecutor;
    private final AggregateDataProvider dataProvider;

    /**
     * Full constructor with data provider.
     * Phase 4 will inject a DB-backed {@link AggregateDataProvider}.
     */
    public AggregateEngine(AggregateFunctionExecutor functionExecutor,
                            ExpressionExecutor expressionExecutor,
                            AggregateDataProvider dataProvider) {
        this.functionExecutor = Objects.requireNonNull(functionExecutor, "functionExecutor");
        this.expressionExecutor = Objects.requireNonNull(expressionExecutor, "expressionExecutor");
        this.dataProvider = Objects.requireNonNull(dataProvider, "dataProvider");
    }

    /**
     * Test constructor - works with in-memory data.
     * Uses a no-op data provider; callers should use {@link #execute(AggregateConfig, List)}.
     */
    public AggregateEngine() {
        this(new AggregateFunctionExecutor(), new ExpressionExecutor(),
             (rc, ctx) -> Collections.emptyList());
    }

    /**
     * Execute an aggregate formula with in-memory detail records.
     * <p>
     * This is the primary entry point for Phase 3B testing.
     * Phase 4 will replace calls with {@link #execute(AggregateConfig, Map)}
     * which uses the injected {@link AggregateDataProvider}.
     *
     * @param config aggregate configuration
     * @param rows   detail records (List of field-to-value maps)
     * @return aggregate result
     */
    public AggregateResult execute(AggregateConfig config, List<Map<String, Object>> rows) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(rows, "rows must not be null");

        AggregateResult.Builder result = AggregateResult.builder()
            .totalRowCount(rows.size());

        if (rows.isEmpty()) {
            return result.value(defaultValue(config.getFunction())).matchedRowCount(0).build();
        }

        // Apply filter if configured
        List<Map<String, Object>> filteredRows = rows;
        if (config.hasFilter()) {
            filteredRows = applyFilter(config.getFilter(), rows, result);
        }
        result.matchedRowCount(filteredRows.size());

        if (filteredRows.isEmpty()) {
            return result.value(defaultValue(config.getFunction())).build();
        }

        // Execute aggregate function
        try {
            Object value = functionExecutor.execute(
                config.getFunction(), config.getTargetField(), filteredRows);
            return result.value(value).build();
        } catch (AggregateExecutionException e) {
            result.addError(e.getMessage());
            return result.value(defaultValue(config.getFunction())).build();
        }
    }

    /**
     * Execute using the injected data provider (Phase 4 entry point).
     * Catches AggregateDataException and returns error result instead of propagating.
     *
     * @param config  aggregate configuration
     * @param context current row context (main record values)
     * @return aggregate result
     */
    public AggregateResult execute(AggregateConfig config, Map<String, Object> context) {
        List<Map<String, Object>> rows;
        try {
            rows = dataProvider.getDetailRecords(config.getRelationCode(), context);
        } catch (AggregateDataException e) {
            return AggregateResult.builder()
                .value(defaultValue(config.getFunction()))
                .addError("Data provider error: " + e.getMessage())
                .build();
        }
        return execute(config, rows);
    }

    private List<Map<String, Object>> applyFilter(String filterExpr,
                                                    List<Map<String, Object>> rows,
                                                    AggregateResult.Builder result) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                Object flag = expressionExecutor.execute(filterExpr, row);
                if (isTruthy(flag)) {
                    filtered.add(row);
                }
            } catch (FormulaExecutionException e) {
                result.addError("Filter evaluation failed for row: " + e.getMessage());
            }
        }
        return filtered;
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).doubleValue() != 0;
        return true;
    }

    private Object defaultValue(AggregateFunction function) {
        switch (function) {
            case COUNT:
            case SUM:
                return 0L;
            case AVG:
                return 0.0;
            case MAX:
            case MIN:
                return null;
            default:
                return null;
        }
    }
}