package com.mdframe.forge.plugin.generator.service.runtime.tree;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTreeConfig;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.stream.Collectors;

/** Owns navigation queries; callers apply their shared read pipeline before node projection. */
@RequiredArgsConstructor
public final class RuntimeTreeReader {
    private static final int MAX_TREE_ROWS = 5000;
    private final DynamicCrudRepository repository;
    private final RuntimeDataScopeService dataScope;
    private final RuntimeTreeProjection projection = new RuntimeTreeProjection();

    public record Request(String parentValue, String parentId, String loadMode,
                          String orderByColumn, String isAsc) {
    }

    public record Rows(List<Map<String, Object>> values, String tableName, String parentColumn,
                       LowcodeTreeConfig config, DynamicCrudRepository.SqlCondition scope, boolean lazy) {
    }

    public Rows read(LowcodeRuntimeConfig config, String tableName, LowcodeTreeConfig treeConfig,
                     Map<String, String> columnMapping, Request request) {
        String keyColumn = columnMapping.getOrDefault(treeConfig.getKeyField(),
                DynamicQueryGenerator.camelToSnake(treeConfig.getKeyField()));
        String parentColumn = columnMapping.getOrDefault(treeConfig.getParentField(),
                DynamicQueryGenerator.camelToSnake(treeConfig.getParentField()));
        String orderColumn = keyColumn;
        if (!repository.getTableColumns(tableName).contains(keyColumn)) {
            throw new BusinessException(UiMessages.translate("树形主键字段不存在: ") + treeConfig.getKeyField());
        }
        if (!repository.getTableColumns(tableName).contains(parentColumn)) {
            throw new BusinessException(UiMessages.translate("树形父级字段不存在: ") + treeConfig.getParentField());
        }
        if (!repository.getTableColumns(tableName).contains(orderColumn)) {
            orderColumn = repository.getTableColumns(tableName).contains("id") ? "id" : parentColumn;
        }
        String orderBy = StringUtils.isBlank(request.orderByColumn())
                ? orderColumn + " ASC"
                : DynamicQueryGenerator.buildOrderByClause(request.orderByColumn(), request.isAsc(), columnMapping);
        DynamicCrudRepository.SqlCondition scope = dataScope.buildRuntimeCondition(config, tableName, null);
        String effectiveLoadMode = StringUtils.defaultIfBlank(request.loadMode(), treeConfig.getLoadMode());
        boolean lazy = "lazy".equalsIgnoreCase(effectiveLoadMode);
        Set<String> ancestorKeys = new LinkedHashSet<>();
        List<Map<String, Object>> selected;
        if (lazy) {
            selected = repository.selectTreeChildren(tableName, parentColumn,
                    StringUtils.defaultIfBlank(request.parentValue(), request.parentId()), orderBy, MAX_TREE_ROWS, scope);
        } else {
            selected = repository.selectList(tableName, null, Collections.emptySet(), Collections.emptyMap(),
                    columnMapping, orderBy, MAX_TREE_ROWS, scope);
            selected = appendTreeAncestorRows(selected, tableName, keyColumn, parentColumn, ancestorKeys);
        }
        List<Map<String, Object>> values = DynamicQueryGenerator.convertListToCamelCase(selected);
        projection.markAncestors(values, treeConfig, ancestorKeys);
        return new Rows(values, tableName, parentColumn, treeConfig, scope, lazy);
    }

    public List<Map<String, Object>> nodes(Rows rows) {
        if (!rows.lazy()) {
            return projection.full(rows.values(), rows.config());
        }
        Set<Object> parentsWithChildren = new HashSet<>();
        for (Map<String, Object> row : rows.values()) {
            Object key = row.get(rows.config().getKeyField());
            if (StringUtils.isNotBlank(TreeNodeKey.normalize(key))
                    && repository.existsByColumn(rows.tableName(), rows.parentColumn(), key, rows.scope())) {
                parentsWithChildren.add(key);
            }
        }
        return projection.lazy(rows.values(), rows.config(), parentsWithChildren);
    }

    private List<Map<String, Object>> appendTreeAncestorRows(List<Map<String, Object>> rows,
                                                             String tableName,
                                                             String keyColumn,
                                                             String parentColumn,
                                                             Set<String> ancestorKeys) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> result = new ArrayList<>(rows);
        Set<String> knownKeys = result.stream()
                .map(row -> TreeNodeKey.normalize(row.get(keyColumn)))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Deque<Object> parentQueue = new ArrayDeque<>();
        for (Map<String, Object> row : result) {
            Object parentValue = row.get(parentColumn);
            String parentKey = TreeNodeKey.normalize(parentValue);
            if (StringUtils.isNotBlank(parentKey) && !TreeNodeKey.isRoot(parentKey) && !knownKeys.contains(parentKey)) {
                parentQueue.add(parentValue);
            }
        }

        int guard = 0;
        while (!parentQueue.isEmpty() && guard < MAX_TREE_ROWS) {
            guard++;
            Object parentValue = parentQueue.removeFirst();
            String parentKey = TreeNodeKey.normalize(parentValue);
            if (StringUtils.isBlank(parentKey) || TreeNodeKey.isRoot(parentKey) || knownKeys.contains(parentKey)) {
                continue;
            }
            List<Map<String, Object>> parents = repository.selectListByColumn(tableName, keyColumn, parentValue);
            if (parents.isEmpty()) {
                continue;
            }
            Map<String, Object> parent = parents.get(0);
            String key = TreeNodeKey.normalize(parent.get(keyColumn));
            if (StringUtils.isBlank(key) || !knownKeys.add(key)) {
                continue;
            }
            ancestorKeys.add(key);
            result.add(parent);
            String nextParentKey = TreeNodeKey.normalize(parent.get(parentColumn));
            if (StringUtils.isNotBlank(nextParentKey) && !TreeNodeKey.isRoot(nextParentKey) && !knownKeys.contains(nextParentKey)) {
                parentQueue.add(parent.get(parentColumn));
            }
        }
        return result;
    }
}
