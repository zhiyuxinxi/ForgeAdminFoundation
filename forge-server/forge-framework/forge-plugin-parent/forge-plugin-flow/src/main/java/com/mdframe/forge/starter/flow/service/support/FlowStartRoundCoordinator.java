package com.mdframe.forge.starter.flow.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mdframe.forge.starter.flow.dto.FlowStartRoundRequest;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Coordinates explicit start rounds inside the caller's existing database transaction. */
@Component
public class FlowStartRoundCoordinator {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private final FlowBusinessMapper businesses;
    private final RuntimeService runtime;

    public FlowStartRoundCoordinator(FlowBusinessMapper businesses, RuntimeService runtime) {
        this.businesses = businesses;
        this.runtime = runtime;
    }

    /** Returns the replayed instance ID, or prepares the new row for insertion. */
    public String prepare(FlowBusiness proposed, Map<String, Object> variables, FlowStartRoundRequest request) {
        validateIdentity(proposed);
        proposed.setStartRequestId(request.startRequestId());
        proposed.setPreviousProcessInstanceId(request.previousProcessInstanceId());
        proposed.setStartRequestDigest(fingerprint(proposed, variables));
        FlowBusiness previous = lockPrevious(proposed);
        FlowBusiness existing = previous == null
                ? businesses.selectByStartRequest(proposed.getTenantId(), proposed.getBusinessKey(), request.startRequestId())
                : businesses.selectByStartRequestForUpdate(
                        proposed.getTenantId(), proposed.getBusinessKey(), request.startRequestId());
        if (existing != null) {
            return replay(existing, proposed);
        }
        if (previous == null) {
            if (businesses.selectByBusinessKeyAndTenantId(proposed.getTenantId(), proposed.getBusinessKey()) != null) {
                throw new IllegalStateException("FLOW_START_PREVIOUS_INSTANCE_REQUIRED");
            }
            proposed.setRoundNo(1);
        } else {
            validatePrevious(previous, proposed);
            proposed.setRoundNo(Math.addExact(previous.getRoundNo(), 1));
        }
        return null;
    }

    /** A locking read sees the winner after a concurrent INSERT's unique-key conflict. */
    public String resolveDuplicate(FlowBusiness proposed) {
        FlowBusiness existing = businesses.selectByStartRequestForUpdate(
                proposed.getTenantId(), proposed.getBusinessKey(), proposed.getStartRequestId());
        if (existing == null) {
            throw new IllegalStateException("FLOW_START_ROUND_CONFLICT");
        }
        return replay(existing, proposed);
    }

    private FlowBusiness lockPrevious(FlowBusiness proposed) {
        if (proposed.getPreviousProcessInstanceId() == null) {
            return null;
        }
        FlowBusiness previous = businesses.selectByProcessInstanceIdAndTenantIdForUpdate(
                proposed.getPreviousProcessInstanceId(), proposed.getTenantId());
        if (previous == null || !Objects.equals(previous.getBusinessKey(), proposed.getBusinessKey())) {
            throw new IllegalArgumentException("FLOW_START_PREVIOUS_INSTANCE_MISMATCH");
        }
        return previous;
    }

    private void validatePrevious(FlowBusiness previous, FlowBusiness proposed) {
        if (!Objects.equals(previous.getApplyUserId(), proposed.getApplyUserId())
                || !Objects.equals(previous.getProcessDefKey(), proposed.getProcessDefKey())) {
            throw new IllegalArgumentException("FLOW_START_PREVIOUS_IDENTITY_MISMATCH");
        }
        FlowBusiness latest = businesses.selectLatestByBusinessKeyForUpdate(
                proposed.getTenantId(), proposed.getBusinessKey());
        if (latest == null || !Objects.equals(latest.getId(), previous.getId())) {
            throw new IllegalStateException("FLOW_START_PREVIOUS_INSTANCE_STALE");
        }
        String status = previous.getStatus() == null ? "" : previous.getStatus().trim().toLowerCase(Locale.ROOT);
        if (!("rejected".equals(status) || "canceled".equals(status))
                || runtime.createProcessInstanceQuery().processInstanceId(previous.getProcessInstanceId())
                        .singleResult() != null) {
            throw new IllegalStateException("FLOW_START_PREVIOUS_INSTANCE_NOT_RESTARTABLE");
        }
    }

    private String replay(FlowBusiness existing, FlowBusiness proposed) {
        if (!Objects.equals(existing.getStartRequestDigest(), proposed.getStartRequestDigest())) {
            throw new IllegalArgumentException("FLOW_START_REQUEST_CONFLICT");
        }
        if (existing.getProcessInstanceId() == null || existing.getProcessInstanceId().isBlank()) {
            throw new IllegalStateException("FLOW_START_REQUEST_IN_PROGRESS");
        }
        return existing.getProcessInstanceId();
    }

    private void validateIdentity(FlowBusiness proposed) {
        if (proposed.getTenantId() == null || proposed.getBusinessKey() == null
                || proposed.getBusinessKey().isBlank() || proposed.getApplyUserId() == null
                || proposed.getApplyUserId().isBlank()) {
            throw new IllegalArgumentException("FLOW_START_IDENTITY_REQUIRED");
        }
    }

    private String fingerprint(FlowBusiness proposed, Map<String, Object> variables) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenantId", proposed.getTenantId());
        request.put("businessKey", proposed.getBusinessKey());
        request.put("businessType", proposed.getBusinessType());
        request.put("modelKey", proposed.getProcessDefKey());
        request.put("title", proposed.getTitle());
        request.put("userId", proposed.getApplyUserId());
        request.put("deptId", proposed.getApplyDeptId());
        request.put("previousProcessInstanceId", proposed.getPreviousProcessInstanceId());
        request.put("variables", variables == null ? Map.of() : variables);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(request)));
        } catch (Exception e) {
            throw new IllegalArgumentException("FLOW_START_REQUEST_INVALID", e);
        }
    }
}
