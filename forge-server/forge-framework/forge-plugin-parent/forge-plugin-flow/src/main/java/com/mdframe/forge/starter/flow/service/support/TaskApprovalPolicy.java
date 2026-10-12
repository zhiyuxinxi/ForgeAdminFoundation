package com.mdframe.forge.starter.flow.service.support;

/** 已解析的节点审批动作与输入要求。 */
public record TaskApprovalPolicy(boolean allowApprove, boolean allowReject, boolean allowDelegate,
                                 boolean allowReturn, boolean allowTerminate,
                                 boolean requireSignature, boolean requireComment) {

    static TaskApprovalPolicy defaults() {
        return new TaskApprovalPolicy(true, true, true, false, false, false, true);
    }

    public boolean isAllowed(String action) {
        return switch (action) {
            case "approve" -> allowApprove;
            case "reject" -> allowReject;
            case "delegate" -> allowDelegate;
            case "return" -> allowReturn;
            case "terminate" -> allowTerminate;
            default -> false;
        };
    }
}
