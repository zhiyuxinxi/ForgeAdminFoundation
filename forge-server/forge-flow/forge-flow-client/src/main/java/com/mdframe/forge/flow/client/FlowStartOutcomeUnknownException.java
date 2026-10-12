package com.mdframe.forge.flow.client;

/** The request may have committed remotely. Recover the same persisted round instead of inventing a new key. */
public class FlowStartOutcomeUnknownException extends FlowClientException {
    private final String businessKey;
    private final String startRequestId;

    public FlowStartOutcomeUnknownException(String businessKey, String startRequestId, Throwable cause) {
        super("FLOW_START_OUTCOME_UNKNOWN", cause);
        this.businessKey = businessKey;
        this.startRequestId = startRequestId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getStartRequestId() {
        return startRequestId;
    }
}
