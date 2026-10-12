package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Owns encrypted write values and exclusion of masked display values from updates. */
@lombok.RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public final class RuntimePrivacyWrite {
    private final ObjectMapper objectMapper;
    private final PersistentCryptoService persistentCryptoService;
    private final LowcodeEncryptConfigParser encryptConfigParser;
    private final RuntimeWriteFieldMapping writeMapping;

    public void encrypt(Map<String, Object> data, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || data == null || data.isEmpty()) {
            return;
        }
        try {
            for (LowcodeEncryptConfigParser.FieldRule rule : encryptConfigParser.parse(encryptConfigJson)) {
                String dataKey = resolveWriteEncryptKey(data, rule);
                if (dataKey == null || data.get(dataKey) == null) {
                    continue;
                }
                Object value = data.get(dataKey);
                if (value instanceof String) {
                    String plainText = (String) value;
                    if (StringUtils.isNotBlank(plainText)) {
                        String encryptedValue = persistentCryptoService.encrypt(plainText, rule.algorithm());
                        data.put(dataKey, encryptedValue);
                        log.debug("[DynamicCrudService] 加密字段: {}, algorithm: {}", rule.fieldName(), rule.algorithm());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 加密处理失败, exceptionType={}", e.getClass().getSimpleName());
            throw new BusinessException("低代码加密字段处理失败，请检查持久化密钥配置", e);
        }
    }

    private String resolveWriteEncryptKey(Map<String, Object> data, LowcodeEncryptConfigParser.FieldRule rule) {
        if (data.containsKey(rule.columnName())) {
            return rule.columnName();
        }
        if (data.containsKey(rule.fieldName())) {
            return rule.fieldName();
        }
        String snakeFieldName = DynamicQueryGenerator.camelToSnake(rule.fieldName());
        return data.containsKey(snakeFieldName) ? snakeFieldName : null;
    }

    public void removeMaskedColumns(Map<String, Object> data, LowcodeRuntimeConfig config, String tableName) {
        if (data == null || data.isEmpty() || config == null || StringUtils.isBlank(config.getDesensitizeConfig())) {
            return;
        }
        Set<String> sensitiveColumns = resolveDesensitizedColumns(config, tableName);
        if (sensitiveColumns.isEmpty()) {
            return;
        }
        data.entrySet().removeIf(entry -> sensitiveColumns.contains(entry.getKey()) && isMaskedValue(entry.getValue()));
    }

    private Set<String> resolveDesensitizedColumns(LowcodeRuntimeConfig config, String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            JsonNode configNode = objectMapper.readTree(config.getDesensitizeConfig());
            if (!configNode.isObject()) {
                return columns;
            }
            Map<String, String> columnMapping = writeMapping.columns(config, tableName);
            for (String fieldName : iterableFieldNames(configNode)) {
                String columnName = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
                columns.add(columnName);
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析脱敏写入字段失败", e);
        }
        return columns;
    }

    private List<String> iterableFieldNames(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private boolean isMaskedValue(Object value) {
        return value instanceof String text && text.contains("*");
    }
}
