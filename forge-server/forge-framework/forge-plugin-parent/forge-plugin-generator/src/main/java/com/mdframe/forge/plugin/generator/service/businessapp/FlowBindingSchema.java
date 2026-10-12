package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;

/** Owns business-table binding fields and default merging without querying or modifying records. */
final class FlowBindingSchema {
    private FlowBindingSchema() { }

    static BusinessFlowBindingDTO.BusinessBindingDTO defaultBusinessBinding(AiCrudConfig runtimeConfig,
                                                                            AiBusinessDocumentConfig documentConfig) {
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode("LOWCODE_OBJECT");
        if (runtimeConfig != null) {
            binding.setTableName(StringUtils.firstNonBlank(runtimeConfig.getRuntimeTableName(), runtimeConfig.getTableName()));
            binding.setPrimaryKeyField(StringUtils.firstNonBlank(
                    runtimeConfig.getPrimaryKeyField(),
                    runtimeConfig.getPrimaryKeyColumn(),
                    "id"));
        } else {
            binding.setPrimaryKeyField("id");
        }
        binding.setTenantField("tenant_id");
        if (documentConfig != null) {
            binding.setStatusField(StringUtils.trimToNull(documentConfig.getStatusField()));
            binding.setOwnerField(StringUtils.trimToNull(documentConfig.getOwnerField()));
        }
        return binding;
    }

    static JSONObject normalizeBusinessBinding(BusinessFlowBindingDTO.BusinessBindingDTO binding) {
        JSONObject result = new JSONObject();
        if (binding == null) {
            return result;
        }
        putText(result, "mode", normalizeBusinessBindingMode(binding.getMode()));
        putText(result, "tableName", binding.getTableName());
        putText(result, "primaryKeyField", binding.getPrimaryKeyField());
        putText(result, "tenantField", binding.getTenantField());
        putText(result, "statusField", binding.getStatusField());
        putText(result, "titleField", binding.getTitleField());
        putText(result, "ownerField", binding.getOwnerField());
        return result;
    }

    static BusinessFlowBindingDTO.BusinessBindingDTO toBusinessBindingDTO(JSONObject source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        BusinessFlowBindingDTO.BusinessBindingDTO binding = new BusinessFlowBindingDTO.BusinessBindingDTO();
        binding.setMode(normalizeBusinessBindingMode(source.getString("mode")));
        binding.setTableName(StringUtils.trimToNull(source.getString("tableName")));
        binding.setPrimaryKeyField(StringUtils.trimToNull(source.getString("primaryKeyField")));
        binding.setTenantField(StringUtils.trimToNull(source.getString("tenantField")));
        binding.setStatusField(StringUtils.trimToNull(source.getString("statusField")));
        binding.setTitleField(StringUtils.trimToNull(source.getString("titleField")));
        binding.setOwnerField(StringUtils.trimToNull(source.getString("ownerField")));
        return binding;
    }

    static void mergeBusinessBindingDefaults(JSONObject current, JSONObject defaults) {
        if (current == null || defaults == null || defaults.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(current.getString(key)) && entry.getValue() != null) {
                current.put(key, entry.getValue());
            }
        }
    }

    static String normalizeBusinessBindingMode(String mode) {
        String normalized = StringUtils.defaultIfBlank(mode, "LOWCODE_OBJECT").trim().toUpperCase();
        if ("BUSINESS_TABLE".equals(normalized) || "ADAPTER".equals(normalized)) {
            return normalized;
        }
        return "LOWCODE_OBJECT";
    }

    static void putText(JSONObject target, String key, String value) {
        String text = StringUtils.trimToNull(value);
        if (text != null) {
            target.put(key, text);
        }
    }
}
