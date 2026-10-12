package com.mdframe.forge.plugin.generator.domain.formula;

import java.util.*;

/**
 * Executes a single aggregate function over a list of data rows.
 * <p>
 * Supports: SUM, COUNT, AVG, MAX, MIN.
 * <p>
 * Input is {@code List<Map<String, Object>>} - each map represents one detail record
 * with column names as keys. This is the format returned by dynamic CRUD queries.
 * <p>
 * Pure computation - no DB, no Spring.
 */
public class AggregateFunctionExecutor {

    /**
     * Execute an aggregate function on the given rows.
     *
     * @param function    the aggregate function to apply
     * @param targetField the field name to aggregate (ignored for COUNT)
     * @param rows        detail records as list of field-to-value maps
     * @return the computed aggregate value
     * @throws AggregateExecutionException if computation fails
     */
    public Object execute(AggregateFunction function, String targetField,
                           List<Map<String, Object>> rows) {
        Objects.requireNonNull(function, "function must not be null");
        Objects.requireNonNull(targetField, "targetField must not be null");
        Objects.requireNonNull(rows, "rows must not be null");

        switch (function) {
            case COUNT:
                return (long) rows.size();
            case SUM:
                return computeSum(targetField, rows);
            case AVG:
                return computeAvg(targetField, rows);
            case MAX:
                return computeMax(targetField, rows);
            case MIN:
                return computeMin(targetField, rows);
            default:
                throw new AggregateExecutionException(
                    "Unsupported aggregate function: " + function);
        }
    }

    private long computeSum(String field, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return 0L;
        long sum = 0L;
        int nonNullCount = 0;
        for (Map<String, Object> row : rows) {
            Object val = row.get(field);
            if (val != null) {
                sum += toLong(val, field);
                nonNullCount++;
            }
        }
        if (nonNullCount == 0) {
            throw new AggregateExecutionException(
                "Target field '" + field + "' not found in any detail row");
        }
        return sum;
    }

    private double computeAvg(String field, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Map<String, Object> row : rows) {
            Object val = row.get(field);
            if (val != null) {
                sum += toDouble(val, field);
                count++;
            }
        }
        if (count == 0) {
            throw new AggregateExecutionException(
                "Target field '" + field + "' not found in any detail row");
        }
        return sum / count;
    }

    private Object computeMax(String field, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return null;
        Comparable<Object> max = null;
        for (Map<String, Object> row : rows) {
            Object val = row.get(field);
            if (val == null) continue;
            if (max == null || compare(val, max) > 0) {
                @SuppressWarnings("unchecked")
                Comparable<Object> c = (Comparable<Object>) val;
                max = c;
            }
        }
        return max;
    }

    private Object computeMin(String field, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return null;
        Comparable<Object> min = null;
        for (Map<String, Object> row : rows) {
            Object val = row.get(field);
            if (val == null) continue;
            if (min == null || compare(val, min) < 0) {
                @SuppressWarnings("unchecked")
                Comparable<Object> c = (Comparable<Object>) val;
                min = c;
            }
        }
        return min;
    }

    @SuppressWarnings("unchecked")
    private int compare(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        return a.toString().compareTo(b.toString());
    }

    private long toLong(Object val, String field) {
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            throw new AggregateExecutionException(
                "Cannot convert field '" + field + "' value '" + val + "' to number");
        }
    }

    private double toDouble(Object val, String field) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            throw new AggregateExecutionException(
                "Cannot convert field '" + field + "' value '" + val + "' to number");
        }
    }
}