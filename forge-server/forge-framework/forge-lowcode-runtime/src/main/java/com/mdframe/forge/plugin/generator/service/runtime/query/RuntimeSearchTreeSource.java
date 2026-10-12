package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.service.runtime.tree.RuntimeTreeConfigResolver;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Resolves existing navigation and platform component metadata for descendant filters. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimeSearchTreeSource {
    private static final String TREE_LAYOUT = "tree-crud";
    private final ObjectMapper objectMapper;
    private final RuntimeTreeConfigResolver treeConfigResolver;

    public boolean isTreeRuntime(LowcodeRuntimeConfig config) {
        if (config == null) {
            return false;
        }
        if (TREE_LAYOUT.equals(config.getLayoutType())) {
            return true;
        }
        if (StringUtils.isBlank(config.getOptions())) {
            return false;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            return treeNode != null && treeNode.isObject();
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 判断树形运行时失败, configKey={}", config.getConfigKey(), e);
            return false;
        }
    }

    public LowcodeTreeConfig resolve(LowcodeRuntimeConfig config, String baseField) {
        if (config == null || StringUtils.isBlank(baseField)) {
            return null;
        }
        LowcodeTreeConfig treeConfig = treeConfigResolver.resolve(config);
        if (isTreeRuntime(config) && StringUtils.equals(baseField, treeConfig.getFilterField())) {
            return treeConfig;
        }
        LowcodeTreeConfig systemTreeConfig = resolveSystemTreeConfig(config, baseField);
        return systemTreeConfig != null ? systemTreeConfig : null;
    }

    private LowcodeTreeConfig resolveSystemTreeConfig(LowcodeRuntimeConfig config, String baseField) {
        JsonNode fieldNode = findSchemaField(config.getSearchSchema(), baseField);
        if (fieldNode == null) {
            fieldNode = findSchemaField(config.getEditSchema(), baseField);
        }
        if (fieldNode == null) {
            return null;
        }
        String fieldType = RuntimeFieldMetadata.firstText(fieldNode, "type", "componentType");
        if ("orgTreeSelect".equals(fieldType)) {
            return buildStaticTreeConfig("sys_org", "id", "parentId", "id", baseField);
        }
        if ("regionTreeSelect".equals(fieldType)) {
            return buildStaticTreeConfig("sys_region_code", "code", "parentCode", "code", baseField);
        }
        if ("treeSelect".equals(fieldType)) {
            JsonNode optionSource = fieldNode.path("props").path("optionSource");
            String api = RuntimeFieldMetadata.firstText(optionSource, "api");
            if (StringUtils.contains(api, "/ai/crud/")) {
                return treeConfigResolver.resolve(config);
            }
        }
        return null;
    }

    private LowcodeTreeConfig buildStaticTreeConfig(String sourceTable,
                                                    String keyField,
                                                    String parentField,
                                                    String targetField,
                                                    String filterField) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        treeConfig.setSourceTableName(sourceTable);
        treeConfig.setKeyField(keyField);
        treeConfig.setParentField(parentField);
        treeConfig.setTargetField(targetField);
        treeConfig.setFilterField(filterField);
        treeConfig.setChildrenField("children");
        return treeConfig;
    }

    private JsonNode findSchemaField(String schemaJson, String fieldName) {
        if (StringUtils.isBlank(schemaJson) || StringUtils.isBlank(fieldName)) {
            return null;
        }
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            if (!schemaNode.isArray()) {
                return null;
            }
            for (JsonNode item : schemaNode) {
                String currentField = RuntimeFieldMetadata.firstText(item, "field", "prop", "key", "dataIndex");
                if (fieldName.equals(currentField)) {
                    return item;
                }
            }
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 查找树形查询字段失败, field={}", fieldName, e);
        }
        return null;
    }

    public String filterField(LowcodeRuntimeConfig config) {
        return treeConfigResolver.resolve(config).getFilterField();
    }
}
