package com.mdframe.forge.starter.core.domain;

/** A committed start-round snapshot; NOT_FOUND does not prove that an in-flight request failed. */
public record FlowStartOutcome(State state, String businessKey, String startRequestId,
                               String processInstanceId, String processStatus, Integer roundNo) {
    public enum State {
        NOT_FOUND, IN_PROGRESS, STARTED
    }
}
