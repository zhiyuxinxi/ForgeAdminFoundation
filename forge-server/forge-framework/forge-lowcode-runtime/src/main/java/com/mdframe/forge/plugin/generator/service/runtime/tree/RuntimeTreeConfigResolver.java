package com.mdframe.forge.plugin.generator.service.runtime.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/** Resolves navigation metadata, with options overriding model defaults. */
@Slf4j
@RequiredArgsConstructor
public final class RuntimeTreeConfigResolver {
    private final ObjectMapper objectMapper;

    public LowcodeTreeConfig resolve(LowcodeRuntimeConfig config) {
        LowcodeTreeConfig treeConfig = new LowcodeTreeConfig();
        applyTreeConfigFromModel(config, treeConfig);
        applyTreeConfigFromOptions(config, treeConfig);
        if (StringUtils.isBlank(treeConfig.getKeyField())) {
            treeConfig.setKeyField("id");
        }
        if (StringUtils.isBlank(treeConfig.getParentField())) {
            treeConfig.setParentField("parentId");
        }
        if (StringUtils.isBlank(treeConfig.getLabelField())) {
            treeConfig.setLabelField("name");
        }
        if (StringUtils.isBlank(treeConfig.getFilterField())) {
            treeConfig.setFilterField(treeConfig.getParentField());
        }
        if (StringUtils.isBlank(treeConfig.getTargetField())) {
            treeConfig.setTargetField(treeConfig.getKeyField());
        }
        if (StringUtils.isBlank(treeConfig.getChildrenField())) {
            treeConfig.setChildrenField("children");
        }
        if (StringUtils.isBlank(treeConfig.getLoadMode())) {
            treeConfig.setLoadMode("full");
        }
        return treeConfig;
    }

    private void applyTreeConfigFromModel(LowcodeRuntimeConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getModelSchema())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getModelSchema()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析modelSchema.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigFromOptions(LowcodeRuntimeConfig config, LowcodeTreeConfig target) {
        if (StringUtils.isBlank(config.getOptions())) {
            return;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(config.getOptions()).get("treeConfig");
            applyTreeConfigNode(treeNode, target);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析options.treeConfig失败, configKey={}", config.getConfigKey(), e);
        }
    }

    private void applyTreeConfigNode(JsonNode treeNode, LowcodeTreeConfig target) {
        if (treeNode == null || !treeNode.isObject()) {
            return;
        }
        if (StringUtils.isNotBlank(text(treeNode, "keyField"))) {
            target.setKeyField(text(treeNode, "keyField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelCode"))) {
            target.setSourceModelCode(text(treeNode, "sourceModelCode"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceModelName"))) {
            target.setSourceModelName(text(treeNode, "sourceModelName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "sourceTableName"))) {
            target.setSourceTableName(text(treeNode, "sourceTableName"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "parentField"))) {
            target.setParentField(text(treeNode, "parentField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "labelField"))) {
            target.setLabelField(text(treeNode, "labelField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "filterField"))) {
            target.setFilterField(text(treeNode, "filterField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "targetField"))) {
            target.setTargetField(text(treeNode, "targetField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "childrenField"))) {
            target.setChildrenField(text(treeNode, "childrenField"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "treeTitle"))) {
            target.setTreeTitle(text(treeNode, "treeTitle"));
        }
        if (StringUtils.isNotBlank(text(treeNode, "loadMode"))) {
            target.setLoadMode(text(treeNode, "loadMode"));
        } else if (treeNode.has("lazy") && treeNode.get("lazy").asBoolean(false)) {
            target.setLoadMode("lazy");
        }
        if (treeNode.has("enabled") && !treeNode.get("enabled").isNull()) {
            target.setEnabled(treeNode.get("enabled").asBoolean(false));
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
