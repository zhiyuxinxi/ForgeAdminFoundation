package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stored document options conversion and serialization; owns compatibility parsing. */
public final class DocumentConfigJson {

    private final ObjectMapper objectMapper;

    public DocumentConfigJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> readObjectMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public Map<String, Object> readObjectMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, itemValue) -> {
                if (key != null) {
                    result.put(String.valueOf(key), itemValue);
                }
            });
            return result;
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public String writeJson(Object value, String label) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(label + UiMessages.translate("格式不正确"));
        }
    }
}
