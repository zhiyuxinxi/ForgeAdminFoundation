package com.mdframe.forge.plugin.ai.agent.engine.permission;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agenttool.mapper.AiAgentToolPermissionMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/** Reads the current persisted policy under the verified session and binding scope. */
@Service
@RequiredArgsConstructor
public class AgentToolPermissionService {
    private final AiAgentToolPermissionMapper permissionMapper;

    public PermissionDecision getDecision(ReactContext context, String source, String key) {
        if (context.getUserId() == null || context.getTenantId() == null || context.getAgentId() == null
                || context.getSessionId() == null || context.getSessionId().isBlank()) {
            return PermissionDecision.DENY;
        }
        return TenantContextHolder.executeWithTenant(context.getTenantId(), () -> readPolicy(context, source, key));
    }

    private PermissionDecision readPolicy(ReactContext context, String source, String key) {
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        try {
            TenantContextHolder.setIgnore(false);
            List<String> decisions = permissionMapper.selectRuntimeDecisions(context.getTenantId(),
                    context.getUserId(), context.getSessionId(), context.getAgentId(), source, key);
            return combine(decisions);
        } finally {
            if (previousIgnore == null) {
                TenantContextHolder.clearIgnore();
            } else {
                TenantContextHolder.setIgnore(previousIgnore);
            }
        }
    }

    private PermissionDecision combine(List<String> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return PermissionDecision.DENY;
        }
        PermissionDecision result = null;
        for (String value : decisions) {
            // The SQL sentinel distinguishes an absent row from a malformed persisted decision.
            if ("__unconfigured__".equals(value)) {
                continue;
            }
            PermissionDecision decision = PermissionDecision.fromString(value != null && value.startsWith("configured:")
                    ? value.substring("configured:".length()) : null);
            if (decision == PermissionDecision.DENY) {
                return decision;
            }
            if (result != PermissionDecision.ASK) {
                result = decision;
            }
        }
        return result;
    }
}
