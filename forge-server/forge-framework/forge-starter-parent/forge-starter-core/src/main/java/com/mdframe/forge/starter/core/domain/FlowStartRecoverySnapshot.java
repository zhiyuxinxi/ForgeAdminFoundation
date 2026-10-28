package com.mdframe.forge.starter.core.domain;

/** Exact start round and its original terminal event, if that round has ended. */
public record FlowStartRecoverySnapshot(FlowStartOutcome outcome, FlowEventMessage terminalEvent) {
}
