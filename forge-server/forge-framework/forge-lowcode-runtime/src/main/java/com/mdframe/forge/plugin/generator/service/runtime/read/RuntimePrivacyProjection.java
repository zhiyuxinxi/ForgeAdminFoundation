package com.mdframe.forge.plugin.generator.service.runtime.read;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.plugin.generator.service.crypto.LowcodeEncryptConfigParser;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/** Applies the existing fail-closed decryption and best-effort masking policies. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimePrivacyProjection {
    private final ObjectMapper objectMapper;
    private final PersistentCryptoService persistentCryptoService;
    private final LowcodeEncryptConfigParser encryptConfigParser;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;

    public void decrypt(List<Map<String, Object>> rows, String encryptConfigJson) {
        if (StringUtils.isBlank(encryptConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            List<LowcodeEncryptConfigParser.FieldRule> rules = encryptConfigParser.parse(encryptConfigJson);
            for (Map<String, Object> row : rows) {
                for (LowcodeEncryptConfigParser.FieldRule rule : rules) {
                    String dataKey = resolveReadEncryptKey(row, rule);
                    if (dataKey == null || row.get(dataKey) == null) {
                        continue;
                    }
                    Object value = row.get(dataKey);
                    if (value instanceof String) {
                        String cipherText = (String) value;
                        if (StringUtils.isNotBlank(cipherText)) {
                            String decryptedValue = persistentCryptoService.decrypt(cipherText, rule.algorithm());
                            row.put(dataKey, decryptedValue);
                            log.debug("[DynamicCrudService] 解密字段: {}, algorithm: {}", rule.fieldName(), rule.algorithm());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解密处理失败, exceptionType={}", e.getClass().getSimpleName());
            throw new BusinessException("低代码加密字段读取失败，请检查持久化密钥配置", e);
        }
    }

    private String resolveReadEncryptKey(Map<String, Object> row, LowcodeEncryptConfigParser.FieldRule rule) {
        if (row.containsKey(rule.fieldName())) {
            return rule.fieldName();
        }
        if (row.containsKey(rule.columnName())) {
            return rule.columnName();
        }
        String camelColumnName = DynamicQueryGenerator.snakeToCamel(rule.columnName());
        return row.containsKey(camelColumnName) ? camelColumnName : null;
    }

    public void mask(List<Map<String, Object>> rows, String desensitizeConfigJson) {
        if (StringUtils.isBlank(desensitizeConfigJson) || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            JsonNode configNode = objectMapper.readTree(desensitizeConfigJson);
            if (!configNode.isObject()) return;

            for (Map<String, Object> row : rows) {
                for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
                    String fieldName = entry.getKey(); // camelCase字段名
                    JsonNode ruleNode = entry.getValue();
                    if (!row.containsKey(fieldName) || row.get(fieldName) == null) continue;

                    String typeStr = ruleNode.has("type") ? ruleNode.get("type").asText("CUSTOM") : "CUSTOM";
                    DesensitizeType type = DesensitizeType.valueOf(typeStr);
                    DesensitizeStrategy strategy = desensitizeStrategyFactory.getStrategy(type);
                    if (strategy != null) {
                        String originalValue = String.valueOf(row.get(fieldName));
                        row.put(fieldName, strategy.desensitize(originalValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 脱敏处理失败", e);
        }
    }
}
