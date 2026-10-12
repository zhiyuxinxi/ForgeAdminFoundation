package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns saved flow configuration normalization, including legacy aliases and node form syntax. */
final class FlowBindingConfiguration {
    private final NodeFormConfiguration nodeFormConfiguration = new NodeFormConfiguration();

    BusinessFlowBindingDTO toDTO(JSONObject config) {
        JSONObject source = config == null ? new JSONObject() : config;
        BusinessFlowBindingDTO dto = new BusinessFlowBindingDTO();
        dto.setFlowModelKey(resolveFlowModelKey(source));
        dto.setFlowModelName(source.getString("flowModelName"));
        dto.setTitleTemplate(source.getString("titleTemplate"));
        dto.setStartMode(normalizeStartMode(source.getString("startMode")));
        dto.setBusinessBinding(FlowBindingSchema.toBusinessBindingDTO(source.getJSONObject("businessBinding")));
        dto.setVariableMapping(normalizeVariableMapping(source.getJSONArray("variableMapping")));
        dto.setNodeForms(nodeFormConfiguration.normalizeNodeForms(readMapList(source.getJSONArray("nodeForms"))));
        dto.setConditionFlows(readMapList(source.getJSONArray("conditionFlows")));
        dto.setOptions(readOptions(source.getJSONObject("options")));
        return dto;
    }

    JSONObject normalizeBindingConfig(BusinessFlowBindingDTO dto) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", StringUtils.trimToNull(dto.getFlowModelKey()));
        config.put("flowModelName", StringUtils.trimToNull(dto.getFlowModelName()));
        config.put("titleTemplate", StringUtils.trimToNull(dto.getTitleTemplate()));
        config.put("startMode", normalizeStartMode(dto.getStartMode()));
        JSONObject businessBinding = FlowBindingSchema.normalizeBusinessBinding(dto.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        JSONArray variableMapping = new JSONArray();
        if (dto.getVariableMapping() != null) {
            for (BusinessFlowBindingDTO.VariableMappingDTO item : dto.getVariableMapping()) {
                if (item == null || StringUtils.isBlank(item.getFormField()) || StringUtils.isBlank(item.getFlowVariable())) {
                    continue;
                }
                JSONObject mapping = new JSONObject();
                mapping.put("formField", item.getFormField().trim());
                mapping.put("flowVariable", item.getFlowVariable().trim());
                mapping.put("label", StringUtils.trimToNull(item.getLabel()));
                variableMapping.add(mapping);
            }
        }
        config.put("variableMapping", variableMapping);
        config.put("nodeForms", nodeFormConfiguration.normalizeNodeForms(dto.getNodeForms()));
        config.put("conditionFlows", dto.getConditionFlows() == null ? new ArrayList<>() : dto.getConditionFlows());
        config.put("options", dto.getOptions() == null ? new LinkedHashMap<>() : dto.getOptions());
        return config;
    }

    String normalizeStartMode(String startMode) {
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

    List<BusinessFlowBindingDTO.VariableMappingDTO> normalizeVariableMapping(JSONArray variableMapping) {
        List<BusinessFlowBindingDTO.VariableMappingDTO> result = new ArrayList<>();
        if (variableMapping == null) {
            return result;
        }
        for (int i = 0; i < variableMapping.size(); i++) {
            JSONObject mapping = variableMapping.getJSONObject(i);
            if (mapping == null) {
                continue;
            }
            String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
            String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
            if (StringUtils.isBlank(formField) || StringUtils.isBlank(flowVariable)) {
                continue;
            }
            BusinessFlowBindingDTO.VariableMappingDTO item = new BusinessFlowBindingDTO.VariableMappingDTO();
            item.setFormField(formField.trim());
            item.setFlowVariable(flowVariable.trim());
            item.setLabel(StringUtils.trimToNull(mapping.getString("label")));
            result.add(item);
        }
        return result;
    }

    String resolveFlowModelKey(JSONObject config) {
        if (config == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                config.getString("flowModelKey"),
                config.getString("flowKey"),
                config.getString("processDefinitionKey"),
                config.getString("modelKey")
        );
    }

    JSONObject toConfigJson(BusinessFlowBindingVO binding) {
        JSONObject config = new JSONObject();
        config.put("flowModelKey", binding.getFlowModelKey());
        config.put("flowModelName", binding.getFlowModelName());
        config.put("titleTemplate", binding.getTitleTemplate());
        config.put("startMode", binding.getStartMode());
        JSONObject businessBinding = FlowBindingSchema.normalizeBusinessBinding(binding.getBusinessBinding());
        if (!businessBinding.isEmpty()) {
            config.put("businessBinding", businessBinding);
        }
        config.put("variableMapping", binding.getVariableMapping());
        config.put("nodeForms", binding.getNodeForms());
        config.put("conditionFlows", binding.getConditionFlows());
        config.put("options", binding.getOptions());
        return config;
    }

    List<Map<String, Object>> readMapList(JSONArray array) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) {
            return result;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(new LinkedHashMap<>(item));
            }
        }
        return result;
    }

    Map<String, Object> readOptions(JSONObject options) {
        if (options == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(options);
    }

    List<Map<String, Object>> normalizeNodeForms(List<Map<String, Object>> nodeForms) {
        return nodeFormConfiguration.normalizeNodeForms(nodeForms);
    }
}
