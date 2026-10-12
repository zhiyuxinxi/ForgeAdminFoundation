package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Owns code-application metadata reads and updates within an existing flow binding. */
@RequiredArgsConstructor
final class CodeAppMetadataStore {
    private final BusinessBindingMapper bindingMapper;
    private final BusinessFlowRuntimeMetadata runtimeMetadata;

    Map<String, Object> get(String objectCode) {
        if (StringUtils.isBlank(objectCode)) {
            return Map.of();
        }
        JSONObject metadata = read(BusinessFlowRuntimeMetadata.resolveTenantId(), objectCode);
        return new LinkedHashMap<>(CodeFormMetadata.sanitizeCodeAppMetadata(metadata, true));
    }

    JSONObject read(Long tenantId, String objectCode) {
        if (tenantId == null || StringUtils.isBlank(objectCode)) {
            return new JSONObject();
        }
        AiBusinessBinding binding = runtimeMetadata.selectMainFlowBindingForConfig(tenantId, objectCode);
        if (binding == null) {
            return new JSONObject();
        }
        JSONObject config = BusinessFlowRuntimeMetadata.readJsonObject(binding.getBindingConfig());
        JSONObject options = FormAssetValues.readNestedObject(config.get("options"));
        return FormAssetValues.readNestedObject(options.get("codeAppMetadata"));
    }

    boolean save(String objectCode, Object metadata) {
        if (StringUtils.isBlank(objectCode) || !(metadata instanceof Map<?, ?> || metadata instanceof JSONObject)) {
            return false;
        }
        Long tenantId = BusinessFlowRuntimeMetadata.resolveTenantId();
        AiBusinessBinding binding = runtimeMetadata.selectMainFlowBindingForConfig(tenantId, objectCode);
        boolean created = binding == null;
        if (created) {
            binding = newBinding(tenantId, objectCode);
        }
        JSONObject config = BusinessFlowRuntimeMetadata.readJsonObject(binding.getBindingConfig());
        JSONObject options = FormAssetValues.readNestedObject(config.get("options"));
        options.put("codeAppMetadata", FormAssetValues.readNestedObject(metadata));
        config.put("options", options);
        binding.setBindingConfig(config.toJSONString());
        if (created) {
            bindingMapper.insert(binding);
        } else {
            bindingMapper.updateById(binding);
        }
        return true;
    }

    private AiBusinessBinding newBinding(Long tenantId, String objectCode) {
        AiBusinessBinding binding = new AiBusinessBinding();
        binding.setTenantId(tenantId);
        binding.setTargetType("OBJECT");
        binding.setTargetCode(objectCode);
        binding.setBindingType("FLOW");
        binding.setBindingKey(objectCode);
        binding.setBindingName(objectCode + "业务表单资产配置");
        binding.setStatus(1);
        binding.setSortOrder(0);
        return binding;
    }
}
