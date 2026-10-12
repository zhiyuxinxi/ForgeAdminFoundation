package com.mdframe.forge.plugin.generator.service.runtime.read;

import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/** Compiles existing explicit and schema-derived translation rules in precedence order. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimeTranslationConfig {
    private final ObjectMapper objectMapper;

    public String resolve(LowcodeRuntimeConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> rules = new LinkedHashMap<>();
        mergeTransConfig(rules, config.getTransConfig());
        mergeTransRulesFromSchema(rules, config.getColumnsSchema(), true);
        mergeTransRulesFromSchema(rules, config.getSearchSchema(), false);
        mergeTransRulesFromSchema(rules, config.getEditSchema(), false);
        if (rules.isEmpty()) {
            return config.getTransConfig();
        }
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 合并翻译配置失败", e);
            return config.getTransConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeTransConfig(Map<String, Object> rules, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(transConfigJson);
            if (!node.isObject()) {
                return;
            }
            Map<String, Object> source = objectMapper.convertValue(node, Map.class);
            rules.putAll(source);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析翻译配置失败", e);
        }
    }

    private void mergeTransRulesFromSchema(Map<String, Object> rules, String schemaJson, boolean overrideExisting) {
        if (StringUtils.isBlank(schemaJson)) {
            return;
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            if (!schemaNode.isArray()) {
                return;
            }
            for (JsonNode item : schemaNode) {
                mergeTransRuleFromField(rules, item, overrideExisting);
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 从Schema推导翻译配置失败", e);
        }
    }

    private void mergeTransRuleFromField(Map<String, Object> rules, JsonNode item, boolean overrideExisting) {
        String fieldName = RuntimeFieldMetadata.firstText(item, "field", "prop", "key", "dataIndex");
        if (StringUtils.isBlank(fieldName) || (!overrideExisting && rules.containsKey(fieldName))) {
            return;
        }
        String dictType = RuntimeFieldMetadata.firstText(item, "dictType");
        JsonNode renderNode = item.get("render");
        if (StringUtils.isBlank(dictType) && renderNode != null && renderNode.isObject()) {
            dictType = RuntimeFieldMetadata.firstText(renderNode, "dictType");
        }
        if (StringUtils.isNotBlank(dictType)) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("dictType", dictType);
            rule.put("targetField", fieldName + "Name");
            rules.put(fieldName, rule);
            return;
        }

        String renderType = renderNode != null && renderNode.isObject() ? RuntimeFieldMetadata.firstText(renderNode, "type") : "";
        String componentType = StringUtils.defaultIfBlank(RuntimeFieldMetadata.firstText(item, "type", "componentType"), renderType);
        String transType = resolveTransType(componentType);
        if (StringUtils.isBlank(transType)) {
            return;
        }
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("type", transType);
        String targetField = renderNode != null && renderNode.isObject()
                ? StringUtils.defaultIfBlank(RuntimeFieldMetadata.firstText(renderNode, "targetField"), fieldName + "Name")
                : fieldName + "Name";
        rule.put("targetField", targetField);
        rules.put(fieldName, rule);
    }

    private String resolveTransType(String componentType) {
        return switch (StringUtils.defaultString(componentType)) {
            case "orgTreeSelect", "orgName" -> "orgName";
            case "userSelect", "userName" -> "userName";
            case "regionTreeSelect", "regionName" -> "regionName";
            case "fileUpload", "imageUpload" -> componentType;
            default -> "";
        };
    }
}
