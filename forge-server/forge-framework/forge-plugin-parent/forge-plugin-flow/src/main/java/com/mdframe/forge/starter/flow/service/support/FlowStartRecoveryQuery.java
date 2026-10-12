package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.core.domain.FlowStartOutcome;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.dto.FlowStartRoundRequest;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.springframework.stereotype.Service;

/** Read one persisted start round without starting or compensating any process. */
@Service
public class FlowStartRecoveryQuery {
    private final FlowBusinessMapper businesses;

    public FlowStartRecoveryQuery(FlowBusinessMapper businesses) {
        this.businesses = businesses;
    }

    public FlowStartOutcome find(Long tenantId, Long userId, String businessKey, String requestId) {
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(401, "FLOW_START_QUERY_SESSION_REQUIRED");
        }
        if (businessKey == null || businessKey.isBlank() || businessKey.length() > 100) {
            throw new BusinessException(400, "FLOW_START_BUSINESS_KEY_REQUIRED");
        }
        new FlowStartRoundRequest(requestId, null);
        return TenantContextHolder.executeWithTenant(tenantId, () -> findOwned(tenantId, userId, businessKey, requestId));
    }

    private FlowStartOutcome findOwned(Long tenantId, Long userId, String businessKey, String requestId) {
        var business = businesses.selectByStartRequest(tenantId, businessKey, requestId);
        if (business == null) {
            return new FlowStartOutcome(FlowStartOutcome.State.NOT_FOUND, businessKey, requestId, null, null, null);
        }
        if (!String.valueOf(userId).equals(business.getApplyUserId())) {
            throw new BusinessException(403, "FLOW_START_QUERY_FORBIDDEN");
        }
        String instanceId = business.getProcessInstanceId();
        var state = instanceId == null || instanceId.isBlank()
                ? FlowStartOutcome.State.IN_PROGRESS : FlowStartOutcome.State.STARTED;
        return new FlowStartOutcome(state, businessKey, requestId, instanceId, business.getStatus(), business.getRoundNo());
    }
}
