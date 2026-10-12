package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
final class VelocityCodegenAnnotations {
    private final ObjectMapper objectMapper;

    Map<String, Object> resolveAnnotationFlags(AiCrudConfig config, List<GenTableColumn> columns) {
        Map<String, Object> flags = new LinkedHashMap<>();
        populateDictionaryFlags(config, columns, flags);
        populateEncryptionFlags(config, flags);
        populateMaskingFlags(config, columns, flags);
        return flags;
    }

    private void populateDictionaryFlags(AiCrudConfig config, List<GenTableColumn> columns, Map<String, Object> flags) {
        boolean hasDictConfig = false;
        Set<String> dictFields = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(config.getDictConfig()) && StringUtils.isNotBlank(config.getColumnsSchema())) {
            try {
                // 建立 dictType -> javaField 反查 Map（从 columnsSchema + editSchema）
                Map<String, String> dictTypeToField = dictionaryFields(config);
                // dictConfig 格式： [{dictType: "sys_status", dictName: "...", items: [...]}]
                List<Map<String, Object>> dictList = objectMapper.readValue(
                        config.getDictConfig(), new TypeReference<>() {});
                for (Map<String, Object> item : dictList) {
                    String dictType = (String) item.get("dictType");
                    if (StringUtils.isBlank(dictType)) continue;
                    String field = dictTypeToField.get(dictType);
                    if (StringUtils.isNotBlank(field)) {
                        dictFields.add(field);
                        hasDictConfig = true;
                        final String finalDictType = dictType;
                        final String finalField = field;
                        columns.stream()
                                .filter(c -> finalField.equals(c.getJavaField()))
                                .findFirst()
                                .ifPresent(c -> c.setDictType(finalDictType));
                    }
                }
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 dictConfig 失败", e);
            }
        }
        flags.put("hasDictConfig", hasDictConfig);
        flags.put("hasDictTrans", hasDictConfig || GenUtils.hasDictTrans(columns));
        flags.put("dictFields", dictFields);

    }

    private Map<String, String> dictionaryFields(AiCrudConfig config) throws Exception {
        Map<String, String> dictTypeToField = new LinkedHashMap<>();
        for (String schemaJson : new String[]{config.getColumnsSchema(), config.getEditSchema()}) {
            if (StringUtils.isBlank(schemaJson)) continue;
            List<Map<String, Object>> schemaList = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            for (Map<String, Object> col : schemaList) {
                Object dtObj = col.get("dictType");
                Object fObj = col.get("field") != null ? col.get("field")
                : col.get("dataIndex") != null ? col.get("dataIndex") : col.get("key");
                if (dtObj != null && fObj != null) {
                    dictTypeToField.putIfAbsent(String.valueOf(dtObj), String.valueOf(fObj));
                }
            }
        }
        return dictTypeToField;
    }

    private void populateEncryptionFlags(AiCrudConfig config, Map<String, Object> flags) {
        boolean hasEncrypt = false;
        boolean enableDecrypt = false;
        boolean enableEncrypt = false;
        if (StringUtils.isNotBlank(config.getEncryptConfig())) {
            try {
                Map<String, Object> encConf = objectMapper.readValue(
                        config.getEncryptConfig(), new TypeReference<>() {});
                // 格式：{enableEncrypt: true, enableDecrypt: true, operations: [...]}
                Object encryptVal = encConf.get("enableEncrypt");
                Object decryptVal = encConf.get("enableDecrypt");
                enableEncrypt = Boolean.TRUE.equals(encryptVal) || "true".equals(String.valueOf(encryptVal));
                enableDecrypt = Boolean.TRUE.equals(decryptVal) || "true".equals(String.valueOf(decryptVal));
                hasEncrypt = enableEncrypt || enableDecrypt;
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 encryptConfig 失败", e);
            }
        }
        flags.put("hasEncrypt", hasEncrypt);
        flags.put("enableDecrypt", enableDecrypt);
        flags.put("enableEncrypt", enableEncrypt);

    }

    private void populateMaskingFlags(AiCrudConfig config, List<GenTableColumn> columns, Map<String, Object> flags) {
        columns.forEach(column -> column.setDesensitizeType(
                normalizeDesensitizeType(column.getDesensitizeType())));
        if (StringUtils.isNotBlank(config.getDesensitizeConfig())) {
            try {
                Map<String, Object> desMap = objectMapper.readValue(
                        config.getDesensitizeConfig(), new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : desMap.entrySet()) {
                    String field = entry.getKey();
                    String strategy = maskingStrategy(entry.getValue());
                    if (StringUtils.isNotBlank(field)) {
                        final String finalField = field;
                        final String finalStrategy = normalizeDesensitizeType(strategy);
                        columns.stream()
                                .filter(c -> finalField.equals(c.getJavaField()))
                                .findFirst()
                                .ifPresent(c -> c.setDesensitizeType(finalStrategy));
                    }
                }
            } catch (Exception e) {
                log.warn("[VelocityCodegenStrategy] 解析 desensitizeConfig 失败", e);
            }
        }
        boolean hasDesensitize = columns.stream()
                .anyMatch(column -> StringUtils.isNotBlank(column.getDesensitizeType()));
        flags.put("hasDesensitize", hasDesensitize);

    }

    private String maskingStrategy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object type = map.get("type");
            return type == null ? null : String.valueOf(type);
        }
        return value instanceof String text ? text : null;
    }

    static String normalizeDesensitizeType(String value) {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return "NONE".equals(normalized) ? null : normalized;
    }
}
