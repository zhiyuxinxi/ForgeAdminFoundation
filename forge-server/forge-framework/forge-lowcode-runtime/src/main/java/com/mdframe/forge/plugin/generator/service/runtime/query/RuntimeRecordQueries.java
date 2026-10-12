package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowReader;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeReadPipeline;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads records and assembles their projected results inside the caller's authorized runtime scope. */
@RequiredArgsConstructor
public final class RuntimeRecordQueries {
    private final DynamicCrudRepository repository;
    private final RuntimeJoinPlanner joinPlanner;
    private final RuntimeDataScopeService dataScopes;
    private final RuntimeReadPipeline readPipeline;
    private final ChildRowReader children;

    public Map<String, Object> byId(LowcodeRuntimeConfig config, Object id,
                                    LowcodePrimaryKeyStrategy primaryKey, boolean masterDetail) {
        String table = config.getTableName();
        RuntimeJoinContext join = joinPlanner.plan(config);
        if (masterDetail && join != null) {
            return masterDetail(config, id, join);
        }
        if (join != null) {
            Map<String, Object> record = repository.selectJoinedById(table, id, join.selectFields(),
                    join.joins(), dataScopes.buildRuntimeCondition(config, table, "t0"));
            if (record != null) {
                readPipeline.apply(Collections.singletonList(record), config);
            }
            return record;
        }
        Map<String, Object> record = repository.selectById(table,
                RuntimeQueryPlans.PrimaryKey.from(primaryKey).column(), id,
                dataScopes.buildRuntimeCondition(config, table, null));
        if (record == null) {
            return null;
        }
        Map<String, Object> projected = DynamicQueryGenerator.convertMapToCamelCase(record);
        readPipeline.apply(Collections.singletonList(projected), config);
        return projected;
    }

    private Map<String, Object> masterDetail(LowcodeRuntimeConfig config, Object id, RuntimeJoinContext join) {
        Map<String, Object> record = repository.selectById(config.getTableName(), id,
                dataScopes.buildRuntimeCondition(config, config.getTableName(), null));
        if (record == null) {
            return null;
        }
        Map<String, Object> main = DynamicQueryGenerator.convertMapToCamelCase(record);
        readPipeline.apply(Collections.singletonList(main), config);
        Map<String, Object> childRows = children.readChildren(config, id, record,
                join.primaryColumnMapping(), join.childRelations());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("main", main);
        result.put("children", childRows);
        return result;
    }

    public Map<Object, Map<String, Object>> byIds(LowcodeRuntimeConfig config, List<Object> normalizedIds,
                                                 LowcodePrimaryKeyStrategy primaryKey) {
        RuntimeQueryPlans.PrimaryKey key = RuntimeQueryPlans.PrimaryKey.from(primaryKey);
        String table = config.getTableName();
        List<Map<String, Object>> rows = repository.selectListByColumnIn(table, key.column(), normalizedIds,
                dataScopes.buildRuntimeCondition(config, table, null));
        List<Map<String, Object>> projected = DynamicQueryGenerator.convertListToCamelCase(rows);
        readPipeline.apply(projected, config);
        Map<Object, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : projected) {
            Object id = rowKey(row, key);
            if (id != null) {
                result.put(id, row);
            }
        }
        return result;
    }

    public List<Object> normalizeIds(Collection<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> StringUtils.isNotBlank(String.valueOf(id)))
                .distinct()
                .map(Object.class::cast)
                .toList();
    }

    private Object rowKey(Map<String, Object> row, RuntimeQueryPlans.PrimaryKey key) {
        for (String field : List.of(key.field(), DynamicQueryGenerator.snakeToCamel(key.column()), key.column(), "id")) {
            if (StringUtils.isNotBlank(field) && row.containsKey(field)) {
                return row.get(field);
            }
        }
        return null;
    }
}
