package com.mdframe.forge.plugin.generator.service.runtime.read;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/** Translates dictionaries and batches platform lookup labels with the original degradation boundaries. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimeTranslationProjection {
    private final ObjectMapper objectMapper;
    private final DictValueProvider dictValueProvider;

    public void apply(List<Map<String, Object>> rows, String transConfigJson) {
        if (StringUtils.isBlank(transConfigJson) || rows == null || rows.isEmpty() || dictValueProvider == null) return;
        try {
            JsonNode configNode = objectMapper.readTree(transConfigJson);
            if (!configNode.isObject()) return;
            Map<String, Map<String, List<String>>> buckets = new LinkedHashMap<>();
            for (String type : List.of("orgName", "userName", "regionName", "fileUpload")) {
                buckets.put(type, new LinkedHashMap<>());
            }
            Map<String, String> targets = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                translateRow(row, configNode, buckets, targets);
            }
            applyBatchTranslation(rows, buckets.get("orgName"), "orgName", dictValueProvider::batchGetOrgNames, targets);
            applyBatchTranslation(rows, buckets.get("userName"), "userName", dictValueProvider::batchGetUserNames, targets);
            applyBatchTranslation(rows, buckets.get("regionName"), "regionName", dictValueProvider::batchGetRegionNames, targets);
            applyBatchTranslation(rows, buckets.get("fileUpload"), "fileUpload", dictValueProvider::batchGetFileNames, targets);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 翻译处理失败", e);
        }
    }

    private void translateRow(Map<String, Object> row, JsonNode configNode,
                              Map<String, Map<String, List<String>>> buckets, Map<String, String> targets) {
        for (Map.Entry<String, JsonNode> entry : configNode.properties()) {
            translateField(row, entry.getKey(), entry.getValue(), buckets, targets);
        }
    }

    private void translateField(Map<String, Object> row, String field, JsonNode rule,
                                Map<String, Map<String, List<String>>> buckets, Map<String, String> targets) {
            if (!row.containsKey(field) || row.get(field) == null) return;
            String type = rule.has("type") ? rule.get("type").asText("") : "";
            String dictionary = rule.has("dictType") ? rule.get("dictType").asText("") : "";
            String target = rule.has("targetField") ? rule.get("targetField").asText() : field + "Name";
            if (StringUtils.isNotBlank(dictionary)) {
                String label = dictValueProvider.getLabel(dictionary, String.valueOf(row.get(field)));
                if (label != null) row.put(target, label);
            } else {
                Map<String, List<String>> bucket = buckets.get("imageUpload".equals(type) ? "fileUpload" : type);
                if (bucket != null) {
                    bucket.computeIfAbsent(field, ignored -> new ArrayList<>()).add(String.valueOf(row.get(field)));
                    targets.put(field, target);
                }
            }
    }
    private void applyBatchTranslation(List<Map<String, Object>> rows,
                                       Map<String, List<String>> fieldBuckets,
                                       String transType,
                                       java.util.function.Function<List<String>, Map<String, String>> batchLoader,
                                       Map<String, String> targetFieldMap) {
        for (Map.Entry<String, List<String>> bucket : fieldBuckets.entrySet()) {
            String sourceField = bucket.getKey();
            List<String> ids = bucket.getValue().stream().distinct().toList();
            if (ids.isEmpty()) continue;
            Map<String, String> nameMap;
            try {
                nameMap = batchLoader.apply(ids);
            } catch (Exception e) {
                log.warn("[DynamicCrudService] 批量翻译失败, type={}, field={}", transType, sourceField, e);
                continue;
            }
            if (nameMap == null || nameMap.isEmpty()) continue;
            String targetField = targetFieldMap.getOrDefault(sourceField, sourceField + "Name");
            for (Map<String, Object> row : rows) {
                Object value = row.get(sourceField);
                if (value == null) continue;
                String key = String.valueOf(value);
                String name = nameMap.get(key);
                if (name != null) {
                    row.put(targetField, name);
                }
            }
        }
    }
}
