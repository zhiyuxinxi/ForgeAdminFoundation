package com.mdframe.forge.plugin.generator.domain.formula;

/**
 * STORED cross-object formula recompute strategy.
 */
public enum CrossObjectRecomputeMode {
    /** Recompute in the current transaction or request path */
    SYNC,
    /** Recompute asynchronously, default for the first release */
    ASYNC,
    /** Recompute only when manually triggered */
    MANUAL
}
