package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * Thrown when the {@link AggregateDataProvider} cannot resolve a relation,
 * target object, or table name at runtime.
 * <p>
 * Fix for R1 (UAT-AGG-05/06): replaces silent empty-list returns
 * with explicit exceptions so callers can surface configuration errors.
 */
public class AggregateDataException extends RuntimeException {

    public AggregateDataException(String message) {
        super(message);
    }

    public AggregateDataException(String message, Throwable cause) {
        super(message, cause);
    }
}