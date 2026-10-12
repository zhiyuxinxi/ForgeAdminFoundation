package com.mdframe.forge.plugin.generator.service.formula;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime switches for formula execution, logs and function invocation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "forge.formula.runtime")
public class FormulaRuntimeProperties {

    /**
     * Master switch for formula execution log persistence.
     */
    private boolean executionLogEnabled = true;

    /**
     * Record failed formula executions by default.
     */
    private boolean failureLogEnabled = true;

    /**
     * Record successful executions only when explicitly enabled.
     */
    private boolean successLogEnabled = false;

    /**
     * Record debugger executions by default.
     */
    private boolean debugLogEnabled = true;

    /**
     * Capture input snapshots in debug traces.
     */
    private boolean includeInputSnapshot = true;

    /**
     * Capture output values in debug traces.
     */
    private boolean includeOutputValue = true;

    /**
     * Upper bound for Java Bean formula function execution.
     */
    private long functionTimeoutMs = 1000L;

    public long effectiveFunctionTimeoutMs(long definitionTimeoutMs) {
        long configuredTimeout = functionTimeoutMs > 0 ? functionTimeoutMs : 1000L;
        if (definitionTimeoutMs <= 0) {
            return configuredTimeout;
        }
        return Math.min(configuredTimeout, definitionTimeoutMs);
    }
}
