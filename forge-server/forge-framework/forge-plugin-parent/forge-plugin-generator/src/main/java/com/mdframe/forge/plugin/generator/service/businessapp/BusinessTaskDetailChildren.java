package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mdframe.forge.plugin.generator.service.businessapp.FormAssetValues.*;

/** Selects detail-child definitions and projects their visible rows for approval forms. */
@Slf4j
final class BusinessTaskDetailChildren {
    private final DynamicCrudService dynamicCrudService;

    BusinessTaskDetailChildren(DynamicCrudService dynamicCrudService) {
        this.dynamicCrudService = dynamicCrudService;
    }

    List<Map<String, Object>> project(String configKey, Object recordId, Map<String, Object> visibleRecordData) {
        List<Map<String, Object>> childrenConfig = resolveBusinessTaskChildrenConfig(configKey);
        logBusinessTaskChildren("raw", configKey, recordId, childrenConfig, visibleRecordData);
        filterVisibleRecordChildren(visibleRecordData, childrenConfig);
        logBusinessTaskChildren("filtered", configKey, recordId, childrenConfig, visibleRecordData);
        return childrenConfig;
    }

    private List<Map<String, Object>> resolveBusinessTaskChildrenConfig(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return List.of();
        }
        try {
            LowcodeRuntimeConfig runtimeConfig = dynamicCrudService.getRuntimeValues(configKey);
            JSONObject options = runtimeConfig == null ? new JSONObject() : readJsonObject(runtimeConfig.getOptions());
            JSONObject masterDetailConfig = readNestedObject(options.get("masterDetailConfig"));
            return readMapList(readNestedArray(masterDetailConfig.get("children"))).stream()
                    .filter(this::isBusinessTaskDetailChild)
                    .toList();
        } catch (Exception e) {
            log.debug("读取业务表单子表配置失败: configKey={}, error={}", configKey, e.getMessage());
            return List.of();
        }
    }

    private boolean isBusinessTaskDetailChild(Map<String, Object> child) {
        if (child == null || child.isEmpty()) {
            return false;
        }
        if (Boolean.FALSE.equals(child.get("showInDetail"))) {
            return false;
        }
        if (readMapList(readNestedArray(child.get("fields"))).isEmpty()) {
            return false;
        }
        String relationType = StringUtils.defaultIfBlank(textValue(child.get("relationType")), "ONE_TO_MANY")
                .trim()
                .toUpperCase(Locale.ROOT);
        return !Set.of("REFERENCE", "LOOKUP", "OBJECT_REFERENCE", "OBJECTREFERENCE", "MANY_TO_ONE", "ONE_TO_ONE")
                .contains(relationType);
    }

    private void filterVisibleRecordChildren(Map<String, Object> recordData, List<Map<String, Object>> childrenConfig) {
        if (recordData == null || !recordData.containsKey("children")) {
            return;
        }
        if (childrenConfig == null || childrenConfig.isEmpty()) {
            recordData.remove("children");
            return;
        }
        Object childrenValue = recordData.get("children");
        if (!(childrenValue instanceof Map<?, ?> children)) {
            return;
        }
        Set<String> allowedKeys = childrenConfig.stream()
                .map(this::resolveBusinessTaskChildKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : allowedKeys) {
            Object value = children.get(key);
            if (value instanceof List<?>) {
                filtered.put(key, value);
            }
        }
        recordData.put("children", filtered);
    }

    private void logBusinessTaskChildren(String stage,
                                         String configKey,
                                         Object recordId,
                                         List<Map<String, Object>> childrenConfig,
                                         Map<String, Object> recordData) {
        log.info("[审批表单子表] stage={}, configKey={}, recordId={}, childrenConfig={}, children={}",
                stage, configKey, recordId, summarizeBusinessTaskChildrenConfig(childrenConfig),
                summarizeBusinessTaskChildrenData(recordData == null ? null : recordData.get("children")));
    }

    private List<Map<String, Object>> summarizeBusinessTaskChildrenConfig(List<Map<String, Object>> childrenConfig) {
        if (childrenConfig == null || childrenConfig.isEmpty()) {
            return List.of();
        }
        return childrenConfig.stream()
                .map(child -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("key", resolveBusinessTaskChildKey(child));
                    item.put("modelCode", textValue(child.get("modelCode")));
                    item.put("tableName", textValue(child.get("tableName")));
                    item.put("relationType", textValue(child.get("relationType")));
                    item.put("sourceField", textValue(child.get("sourceField")));
                    item.put("targetField", textValue(child.get("targetField")));
                    item.put("fieldCount", readMapList(readNestedArray(child.get("fields"))).size());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> summarizeBusinessTaskChildrenData(Object childrenValue) {
        if (!(childrenValue instanceof Map<?, ?> children) || children.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : children.entrySet()) {
            Object value = entry.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            if (value instanceof List<?> list) {
                item.put("rows", list.size());
                item.put("rowIds", list.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .limit(5)
                        .map(row -> ((Map<?, ?>) row).get("id"))
                        .toList());
                item.put("firstFields", list.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .findFirst()
                        .map(row -> ((Map<?, ?>) row).keySet().stream().limit(12).toList())
                        .orElse(List.of()));
            } else {
                item.put("type", value == null ? "null" : value.getClass().getSimpleName());
            }
            result.put(String.valueOf(entry.getKey()), item);
        }
        return result;
    }

    private String resolveBusinessTaskChildKey(Map<String, Object> child) {
        if (child == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(textValue(child.get("key"))),
                StringUtils.trimToNull(textValue(child.get("modelCode"))),
                StringUtils.trimToNull(textValue(child.get("tableName"))),
                "children");
    }

}
