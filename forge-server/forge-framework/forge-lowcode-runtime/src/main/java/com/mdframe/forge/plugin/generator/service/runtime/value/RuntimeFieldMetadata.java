package com.mdframe.forge.plugin.generator.service.runtime.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Reads the existing model schema and field-name metadata for runtime consumers. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimeFieldMetadata {
    private final ObjectMapper objectMapper;

    public LowcodeModelSchema model(LowcodeRuntimeConfig config) {
        String json = config.getModelSchema();
        if (org.apache.commons.lang3.StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LowcodeModelSchema.class);
        } catch (Exception e) {
            log.warn("Failed to parse modelSchema for {}: {}", config.getConfigKey(), e.getMessage());
            return null;
        }
    }

    public static String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && StringUtils.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }
}
