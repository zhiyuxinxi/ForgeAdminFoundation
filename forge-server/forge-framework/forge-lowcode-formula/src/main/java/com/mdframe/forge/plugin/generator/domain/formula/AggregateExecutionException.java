package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * Exception thrown when aggregate computation fails.
 */
public class AggregateExecutionException extends RuntimeException {

    public AggregateExecutionException(String message) {
        super(message);
    }

    public AggregateExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
