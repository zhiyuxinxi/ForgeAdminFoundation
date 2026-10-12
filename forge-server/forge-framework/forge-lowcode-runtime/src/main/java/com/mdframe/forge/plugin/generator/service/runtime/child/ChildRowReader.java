package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRowPolicy.firstPresent;

/** Reads child collections after parent authorization, preserving established fallback behavior. */
@Slf4j
public final class ChildRowReader {
    private final DynamicCrudRepository repository;
    private final ChildDetailRelationResolver relationResolver;

    public ChildRowReader(DynamicCrudRepository repository, ChildDetailRelationResolver relationResolver) {
        this.repository = repository;
        this.relationResolver = relationResolver;
    }

    /** Called only after the main record has passed its scoped read. */
    public Map<String, Object> readChildren(LowcodeRuntimeConfig config, Object id, Map<String, Object> record,
                                            Map<String, String> mainColumnMapping,
                                            List<RuntimeChildRelation> relations) {
        Map<String, Object> children = new LinkedHashMap<>();
        for (RuntimeChildRelation relation : relations) {
            RuntimeChildRelation queryRelation = relationResolver.resolveMasterDetailQueryRelation(config, relation, mainColumnMapping);
            Object relationValue = queryRelation.mainValue(record, id, record);
            if (relationValue == null) {
                log.info("[动态CRUD详情子表] configKey={}, mainId={}, childModel={}, table={}, childFkColumn={}, mainColumn={}, relationValue=null, rows=0",
                        config.getConfigKey(), id, queryRelation.modelCode(), queryRelation.tableName(),
                        queryRelation.childFkColumn(), queryRelation.mainColumn());
                children.put(queryRelation.modelCode(), List.of());
                continue;
            }
            List<Map<String, Object>> rows = selectMasterDetailChildRows(config, queryRelation, id, relationValue);
            log.info("[动态CRUD详情子表] configKey={}, mainId={}, childModel={}, table={}, childFkColumn={}, mainColumn={}, relationValue={}, rows={}, rowIds={}",
                    config.getConfigKey(), id, queryRelation.modelCode(), queryRelation.tableName(),
                    queryRelation.childFkColumn(), queryRelation.mainColumn(), relationValue, rows.size(),
                    summarizeRowIds(rows));
            children.put(queryRelation.modelCode(), DynamicQueryGenerator.convertListToCamelCase(rows));
        }

        return children;
    }

    private List<Map<String, Object>> selectMasterDetailChildRows(LowcodeRuntimeConfig config,
                                                                  RuntimeChildRelation relation,
                                                                  Object mainId,
                                                                  Object relationValue) {
        List<Map<String, Object>> rows = repository.selectListByColumn(
                relation.tableName(), relation.childFkColumn(), relationValue);
        if (mainId == null) {
            return rows;
        }
        List<String> fallbackColumns = relationResolver.resolveMasterDetailChildFkCandidates(config, relation);
        for (String column : fallbackColumns) {
            if (StringUtils.equals(column, relation.childFkColumn())) {
                continue;
            }
            List<Map<String, Object>> fallbackRows = repository.selectListByColumn(relation.tableName(), column, mainId);
            if (shouldUseFallbackChildRows(relation.childFkColumn(), rows, fallbackRows)) {
                log.warn("[DynamicCrudService] 主子表子表查询命中疑似错误外键，已切换候选外键: configKey={}, childModel={}, table={}, oldColumn={}, oldSize={}, newColumn={}, newSize={}",
                        config.getConfigKey(), relation.modelCode(), relation.tableName(), relation.childFkColumn(), rows.size(),
                        column, fallbackRows.size());
                log.info("[动态CRUD详情子表] fallback configKey={}, mainId={}, childModel={}, table={}, oldColumn={}, oldRows={}, newColumn={}, newRows={}, newRowIds={}",
                        config.getConfigKey(), mainId, relation.modelCode(), relation.tableName(), relation.childFkColumn(),
                        rows.size(), column, fallbackRows.size(), summarizeRowIds(fallbackRows));
                return fallbackRows;
            }
        }
        return rows;
    }

    private boolean shouldUseFallbackChildRows(String currentColumn,
                                               List<Map<String, Object>> currentRows,
                                               List<Map<String, Object>> fallbackRows) {
        if (fallbackRows == null || fallbackRows.isEmpty()) {
            return false;
        }
        int currentSize = currentRows == null ? 0 : currentRows.size();
        if (currentSize == 0) {
            return true;
        }
        if (isSuspiciousMasterDetailChildFkColumn(currentColumn)) {
            return true;
        }
        return currentSize > fallbackRows.size();
    }

    private boolean isSuspiciousMasterDetailChildFkColumn(String column) {
        if (StringUtils.isBlank(column)) {
            return true;
        }
        String normalized = column.trim().toLowerCase(Locale.ROOT);
        return Set.of("id", "tenant_id", "create_by", "create_dept", "update_by", "del_flag").contains(normalized)
                || normalized.endsWith("_name")
                || normalized.endsWith("_code");
    }

    private List<Object> summarizeRowIds(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .limit(5)
                .map(row -> firstPresent(row, "id", "ID"))
                .toList();
    }


}
