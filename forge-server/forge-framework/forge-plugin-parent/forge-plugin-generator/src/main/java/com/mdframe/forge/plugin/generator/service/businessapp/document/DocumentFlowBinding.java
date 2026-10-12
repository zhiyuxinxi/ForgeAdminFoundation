package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Document main-flow selection, compatibility binding and summary projection. */
public final class DocumentFlowBinding {

    private final BusinessBindingMapper bindingMapper;
    private final ObjectMapper objectMapper;
    private final DocumentConfigJson json;

    public DocumentFlowBinding(BusinessBindingMapper bindingMapper, ObjectMapper objectMapper, DocumentConfigJson json) {
        this.bindingMapper = bindingMapper;
        this.objectMapper = objectMapper;
        this.json = json;
    }

    public Map<String, Object> buildMainFlowSummary(Long tenantId, String objectCode, String legacyDefaultFlowKey) {
        Map<String, Object> summary = new LinkedHashMap<>();
        AiBusinessBinding binding = selectMainFlowBinding(tenantId, objectCode);
        if (binding != null) {
            Map<String, Object> config = readBindingConfig(binding.getBindingConfig());
            String flowModelKey = StringUtils.defaultIfBlank(resolveFlowModelKey(config), binding.getBindingKey());
            String startMode = normalizeStartMode(text(config.get("startMode")));
            List<?> variableMapping = config.get("variableMapping") instanceof List<?> list ? list : List.of();
            List<String> gaps = new ArrayList<>();
            if (StringUtils.isBlank(flowModelKey)) {
                gaps.add("未配置主流程");
            }
            if (StringUtils.isBlank(startMode)) {
                gaps.add("发起方式未配置");
            }
            if (variableMapping.isEmpty()) {
                gaps.add("变量映射缺失");
            }
            if (Integer.valueOf(0).equals(binding.getStatus())) {
                gaps.add("主流程绑定已停用");
            }
            summary.put("configured", StringUtils.isNotBlank(flowModelKey));
            summary.put("bindingId", binding.getId());
            summary.put("bindingType", binding.getBindingType());
            summary.put("bindingStatus", binding.getStatus());
            summary.put("flowModelKey", flowModelKey);
            summary.put("flowModelName", StringUtils.defaultIfBlank(text(config.get("flowModelName")), binding.getBindingName()));
            summary.put("startMode", startMode);
            summary.put("variableMappingCount", variableMapping.size());
            summary.put("complete", gaps.isEmpty());
            summary.put("gaps", gaps);
            summary.put("compatibilitySource", "APPROVAL".equalsIgnoreCase(binding.getBindingType())
                    ? "LEGACY_APPROVAL_BINDING"
                    : "AI_BUSINESS_BINDING");
            return summary;
        }
        if (StringUtils.isNotBlank(legacyDefaultFlowKey)) {
            summary.put("configured", true);
            summary.put("flowModelKey", legacyDefaultFlowKey);
            summary.put("flowModelName", legacyDefaultFlowKey);
            summary.put("startMode", "MANUAL");
            summary.put("variableMappingCount", 0);
            summary.put("complete", false);
            summary.put("gaps", List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
            summary.put("compatibilitySource", "DOCUMENT_DEFAULT_FLOW");
            return summary;
        }
        summary.put("configured", false);
        summary.put("complete", false);
        summary.put("gaps", List.of("未配置主流程"));
        summary.put("compatibilitySource", "NONE");
        return summary;
    }

    public AiBusinessBinding selectMainFlowBinding(Long tenantId, String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return null;
        }
        Long effectiveTenantId = tenantId;
        AiBusinessBinding flowBinding = bindingMapper.selectBindingByTypeAndCode(
                effectiveTenantId, "OBJECT", objectCode, "FLOW");
        if (isBindingEnabled(flowBinding)) {
            return flowBinding;
        }
        AiBusinessBinding legacyApprovalBinding = bindingMapper.selectBindingByTypeAndCode(
                effectiveTenantId, "OBJECT", objectCode, "APPROVAL");
        if (isBindingEnabled(legacyApprovalBinding)) {
            return legacyApprovalBinding;
        }
        return flowBinding != null ? flowBinding : legacyApprovalBinding;
    }

    private boolean isBindingEnabled(AiBusinessBinding binding) {
        return binding != null && !Integer.valueOf(0).equals(binding.getStatus());
    }

    private String normalizeStartMode(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        if ("MANUAL_AND_TRIGGER".equals(normalized) || "MANUAL_TRIGGER".equals(normalized) || "BOTH".equals(normalized)) {
            return "BOTH";
        }
        if ("AUTO".equals(normalized) || "AUTOMATIC".equals(normalized)) {
            return "TRIGGER";
        }
        if ("TRIGGER".equals(normalized)) {
            return "TRIGGER";
        }
        return "MANUAL";
    }

    public void syncLegacyFlowBindingIfNeeded(AiBusinessObject object, String requestedFlowKey, String documentName, Long tenantId) {
        if (object == null || StringUtils.isBlank(object.getObjectCode()) || StringUtils.isBlank(requestedFlowKey)) {
            return;
        }
        AiBusinessBinding existing = selectMainFlowBinding(tenantId, object.getObjectCode());
        if (existing != null) {
            return;
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("flowModelKey", requestedFlowKey);
        config.put("flowModelName", requestedFlowKey);
        config.put("titleTemplate", StringUtils.defaultIfBlank(documentName, object.getObjectName()) + "-${id}");
        config.put("startMode", "MANUAL");
        config.put("variableMapping", new ArrayList<>());
        config.put("conditionFlows", new ArrayList<>());
        config.put("options", Map.of("compatibilitySource", "DOCUMENT_DEFAULT_FLOW"));

        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setTenantId(tenantId);
        binding.setTargetType("OBJECT");
        binding.setTargetId(object.getId());
        binding.setTargetCode(object.getObjectCode());
        binding.setBindingType("FLOW");
        binding.setBindingKey(requestedFlowKey);
        binding.setBindingName(requestedFlowKey + " 流程");
        binding.setBindingConfig(json.writeJson(config, "流程绑定兼容配置"));
        binding.setStatus(1);
        binding.setSortOrder(0);
        bindingMapper.insert(binding);
    }

    public Map<String, Object> readBindingConfig(String bindingConfig) {
        if (StringUtils.isBlank(bindingConfig)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(bindingConfig, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public String resolveFlowModelKey(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                text(config.get("flowModelKey")),
                text(config.get("flowKey")),
                text(config.get("processDefinitionKey")),
                text(config.get("modelKey"))
        );
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
