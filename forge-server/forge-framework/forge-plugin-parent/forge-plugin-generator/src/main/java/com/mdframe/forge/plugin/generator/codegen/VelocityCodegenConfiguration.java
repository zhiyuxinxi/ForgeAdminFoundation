package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
final class VelocityCodegenConfiguration {
    private final ObjectMapper objectMapper;

    String readOption(AiCrudConfig config, String key, String defaultValue) {
        if (StringUtils.isBlank(config.getOptions())) return defaultValue;
        try {
            Map<String, Object> opts = objectMapper.readValue(config.getOptions(), new TypeReference<>() {});
            Object val = opts.get(key);
            if (val == null && opts.get("codegen") instanceof Map<?, ?> codegen) {
                val = codegen.get(key);
                if (val == null && "packageName".equals(key)) {
                    val = codegen.get("domainPackage");
                }
            }
            return val != null ? String.valueOf(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    boolean readBooleanOption(AiCrudConfig config, String key, boolean defaultValue) {
        String value = readOption(config, key, null);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    List<String> resolveStripTablePrefixes(AiCrudConfig config) {
        Object value = readRawOption(config, "stripTablePrefixes");
        return LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                null, value, LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES);
    }

    Object readRawOption(AiCrudConfig config, String key) {
        if (StringUtils.isBlank(config.getOptions())) {
            return null;
        }
        try {
            Map<String, Object> options = objectMapper.readValue(config.getOptions(), new TypeReference<>() {});
            Object value = options.get(key);
            if (value == null && options.get("codegen") instanceof Map<?, ?> codegen) {
                value = codegen.get(key);
            }
            return value;
        } catch (Exception e) {
            throw new BusinessException("代码生成选项不是合法 JSON 对象", e);
        }
    }

    LowcodePageSchema parsePageSchema(AiCrudConfig config) {
        if (StringUtils.isBlank(config.getPageSchema())) {
            return null;
        }
        try {
            return objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
        } catch (Exception e) {
            log.warn("[VelocityCodegenStrategy] 解析低代码页面协议失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }

    List<Map<String, Object>> parseJsonArray(String json) {
        if (StringUtils.isBlank(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    Map<String, Object> parseJsonObject(String json) {
        if (StringUtils.isBlank(json)) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    Map<String, Object> readNestedMap(Map<String, Object> source, String key) {
        if (source == null || !(source.get(key) instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>((Map<String, Object>) map);
    }

    String toJsonLiteral(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    boolean booleanValueDefault(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return booleanValue(value);
    }
}
