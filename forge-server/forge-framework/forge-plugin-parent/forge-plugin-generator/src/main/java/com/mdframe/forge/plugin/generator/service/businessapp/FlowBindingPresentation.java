package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;

/** Owns binding read views and completeness diagnostics; performs no persistence or runtime lookup. */
final class FlowBindingPresentation {
    private final FlowBindingConfiguration configuration;

    FlowBindingPresentation(FlowBindingConfiguration configuration) {
        this.configuration = configuration;
    }

    BusinessFlowBindingVO toVO(String objectCode, AiBusinessBinding binding, JSONObject config) {
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setBindingId(binding.getId());
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(StringUtils.defaultIfBlank(configuration.resolveFlowModelKey(config), binding.getBindingKey()));
        vo.setFlowModelName(StringUtils.defaultIfBlank(config.getString("flowModelName"), binding.getBindingName()));
        vo.setTitleTemplate(config.getString("titleTemplate"));
        vo.setStartMode(configuration.normalizeStartMode(config.getString("startMode")));
        vo.setBusinessBinding(FlowBindingSchema.toBusinessBindingDTO(config.getJSONObject("businessBinding")));
        vo.setVariableMapping(configuration.normalizeVariableMapping(config.getJSONArray("variableMapping")));
        vo.setNodeForms(configuration.normalizeNodeForms(configuration.readMapList(config.getJSONArray("nodeForms"))));
        vo.setConditionFlows(configuration.readMapList(config.getJSONArray("conditionFlows")));
        vo.setOptions(configuration.readOptions(config.getJSONObject("options")));
        vo.setStatus(binding.getStatus());
        enrichBindingSummary(vo, "AI_BUSINESS_BINDING");
        return vo;
    }

    BusinessFlowBindingVO legacyDocumentFlowToVO(String objectCode, AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingVO vo = new BusinessFlowBindingVO();
        vo.setObjectCode(objectCode);
        vo.setFlowModelKey(documentConfig.getDefaultFlowKey());
        vo.setFlowModelName(documentConfig.getDefaultFlowKey());
        vo.setStartMode("MANUAL");
        vo.setBusinessBinding(FlowBindingSchema.defaultBusinessBinding(null, documentConfig));
        vo.setStatus(1);
        vo.setCompatibilitySource("DOCUMENT_DEFAULT_FLOW");
        vo.setComplete(false);
        vo.setGaps(List.of("历史默认流程缺少变量映射，请在流程与自动化中保存一次主流程"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", true);
        summary.put("flowModelKey", documentConfig.getDefaultFlowKey());
        summary.put("flowModelName", documentConfig.getDefaultFlowKey());
        summary.put("startMode", "MANUAL");
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", 0);
        summary.put("complete", false);
        summary.put("gaps", vo.getGaps());
        summary.put("compatibilitySource", "DOCUMENT_DEFAULT_FLOW");
        vo.setMainFlowSummary(summary);
        return vo;
    }

    void enrichBindingSummary(BusinessFlowBindingVO vo, String compatibilitySource) {
        List<String> gaps = new ArrayList<>();
        if (StringUtils.isBlank(vo.getFlowModelKey())) {
            gaps.add("未配置主流程");
        }
        if (StringUtils.isBlank(vo.getStartMode())) {
            gaps.add("发起方式未配置");
        }
        if (vo.getVariableMapping() == null || vo.getVariableMapping().isEmpty()) {
            gaps.add("变量映射缺失");
        }
        boolean complete = gaps.isEmpty();
        vo.setComplete(complete);
        vo.setGaps(gaps);
        vo.setCompatibilitySource(compatibilitySource);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("configured", StringUtils.isNotBlank(vo.getFlowModelKey()));
        summary.put("bindingId", vo.getBindingId());
        summary.put("flowModelKey", vo.getFlowModelKey());
        summary.put("flowModelName", vo.getFlowModelName());
        summary.put("startMode", vo.getStartMode());
        summary.put("businessBinding", vo.getBusinessBinding());
        summary.put("variableMappingCount", vo.getVariableMapping() == null ? 0 : vo.getVariableMapping().size());
        summary.put("complete", complete);
        summary.put("gaps", gaps);
        summary.put("compatibilitySource", compatibilitySource);
        vo.setMainFlowSummary(summary);
    }
}
