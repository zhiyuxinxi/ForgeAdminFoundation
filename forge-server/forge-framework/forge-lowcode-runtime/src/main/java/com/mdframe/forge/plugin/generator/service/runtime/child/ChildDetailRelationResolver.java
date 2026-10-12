package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver.resolveChildColumn;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver.resolvePrimaryColumn;

/** Resolves detail view FK overrides and legacy FK candidates; no row reads. */
@Slf4j
public final class ChildDetailRelationResolver {
    private final DynamicCrudRepository repository;
    private final ObjectMapper objectMapper;

    public ChildDetailRelationResolver(DynamicCrudRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public RuntimeChildRelation resolveMasterDetailQueryRelation(LowcodeRuntimeConfig config, RuntimeChildRelation relation, Map<String, String> mainColumnMapping) {
        JsonNode childNode = findMasterDetailChildNode(config, relation);
        if (childNode == null || !childNode.isObject()) {
            return relation;
        }
        Map<String, String> childColumnMapping = repository.getColumnMapping(relation.tableName());
        String childField = firstText(childNode, "sourceField", "childField", "foreignKey", "foreignKeyField");
        String mainField = firstText(childNode, "targetField", "parentField", "mainField", "parentKey");
        String childColumn = resolveChildColumn(childField, childColumnMapping);
        String mainColumn = resolvePrimaryColumn(mainField, mainColumnMapping);
        if (StringUtils.isBlank(childColumn) || StringUtils.isBlank(mainColumn)) {
            return relation;
        }
        return new RuntimeChildRelation(
                relation.modelCode(),
                relation.tableName(),
                relation.tableAlias(),
                childColumn,
                mainColumn,
                relation.saveMode(),
                relation.fields(),
                relation.fieldRules()
        );
    }

    private JsonNode findMasterDetailChildNode(LowcodeRuntimeConfig config, RuntimeChildRelation relation) {
        if (config == null || StringUtils.isBlank(config.getOptions()) || relation == null) {
            return null;
        }
        try {
            JsonNode children = objectMapper.readTree(config.getOptions()).path("masterDetailConfig").path("children");
            if (!children.isArray()) {
                return null;
            }
            for (JsonNode child : children) {
                if (matchesMasterDetailChildNode(child, relation)) {
                    return child;
                }
            }
        } catch (Exception e) {
            log.debug("[DynamicCrudService] 解析主子表子表配置失败, configKey={}, modelCode={}, error={}",
                    config.getConfigKey(), relation.modelCode(), e.getMessage());
        }
        return null;
    }

    private boolean matchesMasterDetailChildNode(JsonNode child, RuntimeChildRelation relation) {
        if (child == null || relation == null) {
            return false;
        }
        return StringUtils.equalsAny(relation.modelCode(),
                firstText(child, "modelCode"),
                firstText(child, "key"),
                firstText(child, "field"))
                || StringUtils.equals(relation.tableName(), firstText(child, "tableName"));
    }

    public List<String> resolveMasterDetailChildFkCandidates(LowcodeRuntimeConfig config, RuntimeChildRelation relation) {
        Set<String> candidates = new LinkedHashSet<>();
        Set<String> childColumns = repository.getTableColumns(relation.tableName());
        addMasterDetailChildFkCandidate(candidates, childColumns, relation.childFkColumn());

        JsonNode childNode = findMasterDetailChildNode(config, relation);
        if (childNode != null) {
            Map<String, String> childColumnMapping = repository.getColumnMapping(relation.tableName());
            addMasterDetailChildFkCandidate(candidates, childColumns,
                    resolveChildColumn(firstText(childNode, "sourceField", "childField", "foreignKey", "foreignKeyField"), childColumnMapping));
        }

        addMasterDetailChildFkNameCandidates(candidates, childColumns, config.getConfigKey());
        addMasterDetailChildFkNameCandidates(candidates, childColumns, config.getTableName());
        return new ArrayList<>(candidates);
    }

    private void addMasterDetailChildFkNameCandidates(Set<String> candidates, Set<String> childColumns, String baseName) {
        String normalized = normalizeIdentifierBase(baseName);
        if (StringUtils.isBlank(normalized)) {
            return;
        }
        addMasterDetailChildFkCandidate(candidates, childColumns, normalized + "_id");
        String withoutPrefix = removeShortBusinessPrefix(normalized);
        if (!StringUtils.equals(withoutPrefix, normalized)) {
            addMasterDetailChildFkCandidate(candidates, childColumns, withoutPrefix + "_id");
        }
    }

    private String normalizeIdentifierBase(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String normalized = DynamicQueryGenerator.camelToSnake(value).replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase(Locale.ROOT);
        return normalized.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
    }

    private String removeShortBusinessPrefix(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        String prefix = StringUtils.substringBefore(value, "_");
        return prefix.length() <= 4 ? StringUtils.substringAfter(value, "_") : value;
    }

    private void addMasterDetailChildFkCandidate(Set<String> candidates, Set<String> childColumns, String column) {
        if (StringUtils.isBlank(column)) {
            return;
        }
        String normalized = column.trim();
        if (childColumns.contains(normalized)) {
            candidates.add(normalized);
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && StringUtils.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }
}
