package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/** Reads the published edit schema fields for numbering and uniqueness rules. */
@lombok.extern.slf4j.Slf4j
@lombok.RequiredArgsConstructor
public final class RuntimeEditFieldReader {
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> read(LowcodeRuntimeConfig config) {
        if (config == null || StringUtils.isBlank(config.getEditSchema())) {
            return List.of();
        }
        try {
            Object schema = objectMapper.readValue(config.getEditSchema(), new TypeReference<Object>() {
            });
            List<Map<String, Object>> result = new ArrayList<>();
            collectEditSchemaFields(schema, result);
            return result;
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析editSchema唯一校验失败, configKey={}", config.getConfigKey(), e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private void collectEditSchemaFields(Object node, List<Map<String, Object>> result) {
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectEditSchemaFields(item, result);
            }
            return;
        }
        if (!(node instanceof Map<?, ?> rawMap)) {
            return;
        }
        Map<String, Object> map = (Map<String, Object>) rawMap;
        if (StringUtils.isNotBlank(text(map.get("field")))) {
            result.add(map);
        }
        collectEditSchemaFields(map.get("children"), result);
        collectEditSchemaFields(map.get("items"), result);
        collectEditSchemaFields(map.get("components"), result);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
