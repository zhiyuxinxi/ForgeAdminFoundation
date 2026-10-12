package com.mdframe.forge.plugin.generator.domain.formula;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Error handler for formula execution — implements graceful degradation.
 * <p>
 * Strategy: when a formula fails during execution, log the error, record it,
 * and continue executing remaining formulas. The failed field gets a null value
 * (or keeps its original value if provided).
 * <p>
 * Two modes:
 * <ul>
 *   <li><b>LENIENT</b> (default): continue on error, collect all errors</li>
 *   <li><b>STRICT</b>: abort on first error (fail-fast)</li>
 * </ul>
 */
public class FormulaErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(FormulaErrorHandler.class);

    public enum Mode { LENIENT, STRICT }

    private final Mode mode;

    public FormulaErrorHandler() {
        this(Mode.LENIENT);
    }

    public FormulaErrorHandler(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    /**
     * Handle a formula execution error.
     * <p>
     * In LENIENT mode: logs the error, returns a {@link HandleResult} with the fallback value.
     * In STRICT mode: re-throws as {@link FormulaExecutionException}.
     *
     * @param fieldName   the field whose formula failed
     * @param expression  the expression that failed
     * @param exception   the caught exception
     * @param fallbackValue the value to use for this field (null or original value)
     * @return handle result with error details (LENIENT mode only)
     * @throws FormulaExecutionException in STRICT mode
     */
    public HandleResult handleError(String fieldName, String expression,
                                     Exception exception, Object fallbackValue) {
        return handleError(fieldName, expression, exception, fallbackValue, null);
    }

    /**
     * Handle a formula execution error and include traceId in ERROR logs when available.
     */
    public HandleResult handleError(String fieldName, String expression,
                                     Exception exception, Object fallbackValue,
                                     String traceId) {
        String errorMsg = buildErrorMessage(fieldName, expression, exception);
        if (traceId == null || traceId.isBlank()) {
            log.error(errorMsg, exception);
        } else {
            log.error("Formula traceId={} {}", traceId, errorMsg, exception);
        }

        if (mode == Mode.STRICT) {
            throw new FormulaExecutionException(errorMsg, exception);
        }

        return new HandleResult(fieldName, errorMsg, fallbackValue);
    }

    /**
     * Aggregate multiple per-field errors into a summary.
     * Useful for logging/reporting after all formulas have executed.
     */
    public String summarize(List<HandleResult> handleResults) {
        if (handleResults == null || handleResults.isEmpty()) {
            return "No errors";
        }
        StringBuilder sb = new StringBuilder("Formula execution completed with ")
            .append(handleResults.size()).append(" error(s):");
        for (HandleResult r : handleResults) {
            sb.append("\n  - [").append(r.fieldName).append("] ").append(r.errorMessage);
        }
        return sb.toString();
    }

    public Mode getMode() { return mode; }

    private String buildErrorMessage(String fieldName, String expression, Exception e) {
        return "Formula [" + fieldName + "] expr='" + truncate(expression, 80)
            + "' failed: " + e.getMessage();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // ─── inner types ───

    /**
     * Result of handling a single formula error.
     */
    public static class HandleResult {
        public final String fieldName;
        public final String errorMessage;
        public final Object fallbackValue;

        public HandleResult(String fieldName, String errorMessage, Object fallbackValue) {
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
            this.fallbackValue = fallbackValue;
        }

        @Override
        public String toString() {
            return "HandleResult{field=" + fieldName + ", error='" + errorMessage + "'}";
        }
    }
}
