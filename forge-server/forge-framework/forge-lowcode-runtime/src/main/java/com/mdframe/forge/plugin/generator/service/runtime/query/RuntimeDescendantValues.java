package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;

/** Resolves metadata-bound descendant values using stable breadth-first traversal. */
@RequiredArgsConstructor
public final class RuntimeDescendantValues {
    private final DynamicCrudRepository repository;
    private final RuntimeSearchTreeSource trees;

    public List<Object> resolve(LowcodeRuntimeConfig config,
                                                      String tableName,
                                                      String baseField,
                                                      Object baseValue) {
        if (baseValue == null) {
            return List.of();
        }
        Object normalizedBaseValue = normalizeIncludeChildrenBaseValue(baseValue);
        LowcodeTreeConfig treeConfig = trees.resolve(config, baseField);
        String sourceTable = treeConfig != null && StringUtils.isNotBlank(treeConfig.getSourceTableName())
                ? treeConfig.getSourceTableName()
                : tableName;
        if (StringUtils.isBlank(sourceTable) || !repository.tableExists(sourceTable)) {
            return List.of(normalizedBaseValue);
        }
        Map<String, String> columnMapping = repository.getColumnMapping(sourceTable);
        String keyField = treeConfig == null ? "id" : StringUtils.defaultIfBlank(treeConfig.getKeyField(), "id");
        String parentField = treeConfig == null
                ? DynamicQueryGenerator.camelToSnake(baseField)
                : StringUtils.defaultIfBlank(treeConfig.getParentField(), "parentId");
        String targetField = treeConfig == null
                ? "id"
                : StringUtils.defaultIfBlank(treeConfig.getTargetField(), keyField);
        String keyColumn = resolveColumnName(keyField, columnMapping);
        String parentColumn = resolveColumnName(parentField, columnMapping);
        String targetColumn = resolveColumnName(targetField, columnMapping);
        if (StringUtils.isBlank(keyColumn) || StringUtils.isBlank(parentColumn) || StringUtils.isBlank(targetColumn)) {
            return List.of(normalizedBaseValue);
        }

        Columns columns = new Columns(sourceTable, keyColumn, parentColumn, targetColumn);
        Traversal traversal = new Traversal();
        if (!seed(columns, normalizedBaseValue, traversal)) {
            return List.of(normalizedBaseValue);
        }
        traverse(columns, traversal);
        return traversal.values.isEmpty() ? List.of(normalizedBaseValue) : new ArrayList<>(traversal.values);
    }

    private Object normalizeIncludeChildrenBaseValue(Object baseValue) {
        if (baseValue instanceof String text && text.endsWith("ALL")) {
            return text.substring(0, text.length() - 3);
        }
        return baseValue;
    }

    private String resolveColumnName(String fieldName, Map<String, String> columnMapping) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        String column = columnMapping.getOrDefault(fieldName, DynamicQueryGenerator.camelToSnake(fieldName));
        return columnMapping.containsValue(column) ? column : null;
    }

    private boolean seed(Columns columns, Object baseValue, Traversal traversal) {
        if (columns.target().equals(columns.key())) {
            traversal.values.add(baseValue);
            traversal.queue.add(baseValue);
            traversal.visited.add(String.valueOf(baseValue));
            return true;
        }
        List<Map<String, Object>> seeds = repository.selectListByColumn(columns.table(), columns.target(), baseValue);
        if (seeds.isEmpty()) return false;
        for (Map<String, Object> row : seeds) {
            Object key = row.get(columns.key());
            if (key == null) continue;
            traversal.queue.add(key);
            traversal.visited.add(String.valueOf(key));
            Object target = row.get(columns.target());
            traversal.values.add(target != null ? target : baseValue);
        }
        return true;
    }

    private void traverse(Columns columns, Traversal traversal) {
        while (!traversal.queue.isEmpty()) {
            Object current = traversal.queue.removeFirst();
            List<Map<String, Object>> children = repository.selectListByColumn(columns.table(), columns.parent(), current);
            for (Map<String, Object> child : children) {
                Object key = child.get(columns.key());
                if (key != null && traversal.visited.add(String.valueOf(key))) {
                    traversal.queue.addLast(key);
                }
                Object target = child.get(columns.target());
                if (target != null) traversal.values.add(target);
            }
        }
    }

    private record Columns(String table, String key, String parent, String target) {
    }

    private static final class Traversal {
        private final LinkedHashSet<Object> values = new LinkedHashSet<>();
        private final Deque<Object> queue = new ArrayDeque<>();
        private final LinkedHashSet<String> visited = new LinkedHashSet<>();
    }

}
