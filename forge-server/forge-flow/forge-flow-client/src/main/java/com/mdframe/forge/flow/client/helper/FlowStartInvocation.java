package com.mdframe.forge.flow.client.helper;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.flow.client.FlowStartOutcomeUnknownException;
import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import org.springframework.util.StringUtils;

import java.util.Map;

/** One immutable flow start invocation, followed only by a same-round query when its outcome is unknown. */
record FlowStartInvocation(String modelKey, String businessKey, String businessType, String title,
                           Map<String, Object> variables, String userId, String userName,
                           String deptId, String deptName, String requestId, String previousId) {
    void execute(FlowClient flowClient) {
        FlowResult<String> response;
        try {
            response = flowClient.startProcess(modelKey(), businessKey(), businessType(),
                    title(), variables(), userId(), userName(),
                    deptId(), deptName(), requestId(), previousId());
        } catch (FlowStartOutcomeUnknownException unknown) {
            recover(flowClient, unknown);
            return;
        }
        if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getData())) {
            throw new FlowClientException(response == null ? "FLOW_START_RESPONSE_MISSING" : response.getMsg());
        }
    }

    private void recover(FlowClient flowClient, FlowStartOutcomeUnknownException unknown) {
        FlowResult<FlowStartOutcome> response;
        try {
            response = flowClient.getProcessStartStatus(businessKey(), requestId());
        } catch (RuntimeException failure) {
            unknown.addSuppressed(failure);
            throw unknown;
        }
        FlowStartOutcome outcome = response == null ? null : response.getData();
        if (response != null && response.isSuccess() && outcome != null
                && outcome.state() == FlowStartOutcome.State.STARTED
                && businessKey().equals(outcome.businessKey())
                && requestId().equals(outcome.startRequestId())
                && StringUtils.hasText(outcome.processInstanceId())) {
            return;
        }
        // NOT_FOUND is not proof of failure: preserve the original round and never resend here.
        throw unknown;
    }

}
