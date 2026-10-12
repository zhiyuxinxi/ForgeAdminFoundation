package com.mdframe.forge.starter.flow.dto;

/** A stable request identifies one round; a restart explicitly names its predecessor. */
public record FlowStartRoundRequest(String startRequestId, String previousProcessInstanceId) {
    public FlowStartRoundRequest {
        if (startRequestId == null || !startRequestId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")) {
            throw new IllegalArgumentException("FLOW_START_REQUEST_ID_INVALID");
        }
        if (previousProcessInstanceId != null && (previousProcessInstanceId.isBlank()
                || previousProcessInstanceId.length() > 64
                || !previousProcessInstanceId.equals(previousProcessInstanceId.trim()))) {
            throw new IllegalArgumentException("FLOW_START_PREVIOUS_INSTANCE_INVALID");
        }
    }
}
