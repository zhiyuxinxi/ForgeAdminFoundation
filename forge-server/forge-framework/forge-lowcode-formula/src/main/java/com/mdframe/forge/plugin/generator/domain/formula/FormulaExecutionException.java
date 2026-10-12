package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * Exception thrown when formula execution fails at runtime.
 * <p>
 * Carries the original cause for error-handling chains to inspect.
 */
public class FormulaExecutionException extends RuntimeException {

    public FormulaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaExecutionException(String message) {
        super(message);
    }
}