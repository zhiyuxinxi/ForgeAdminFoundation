package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Identity of caller input, excluding document fields that change after a committed start. */
record BusinessFlowStartRequestIdentity(String requestId, String digest, Long userId) {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    static BusinessFlowStartRequestIdentity from(Long tenantId, String businessKey, Long userId,
                                                 BusinessFlowStartDTO request, boolean delegated) {
        if (request.getStartRequestId() == null) return null;
        if (!request.getStartRequestId().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")
                || tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(400, UiMessages.message("flow.start.request.invalid"));
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("tenantId", tenantId);
        input.put("businessKey", businessKey);
        input.put("userId", userId);
        input.put("delegated", delegated);
        input.put("modelKey", StringUtils.trimToNull(request.getFlowModelKey()));
        input.put("title", StringUtils.isBlank(request.getTitle()) ? null : request.getTitle());
        input.put("previousProcessInstanceId", request.getPreviousProcessInstanceId());
        input.put("variables", request.getVariables() == null ? Map.of() : request.getVariables());
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(JSON.writeValueAsBytes(input)));
            return new BusinessFlowStartRequestIdentity(request.getStartRequestId(), digest, userId);
        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new BusinessException(400, UiMessages.message("flow.start.request.invalid"), exception);
        }
    }

    void requireMatch(AiBusinessFlowInstanceLink link) {
        if (!Objects.equals(userId, link.getStartUserId())) {
            throw new BusinessException(403, UiMessages.message("flow.start.request.forbidden"));
        }
        if (!Objects.equals(requestId, link.getStartRequestId())
                || !Objects.equals(digest, link.getStartRequestDigest())) {
            throw new BusinessException(409, UiMessages.message("flow.start.request.conflict"));
        }
    }

    void bind(AiBusinessFlowInstanceLink link) {
        link.setStartRequestId(requestId);
        link.setStartRequestDigest(digest);
    }
}
