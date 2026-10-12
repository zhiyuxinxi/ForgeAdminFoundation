package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeReadPipeline;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/** Executes export counts and bounded batches with the caller's explicit data-scope snapshot. */
@RequiredArgsConstructor
public final class RuntimeExportQueries {
    private static final int MAX_EXPORT_ROWS = 10000;
    private final DynamicCrudRepository repository;
    private final RuntimeQueryPlans plans;
    private final RuntimeJoinQuery joinQuery;
    private final ObjectMapper objectMapper;
    private final RuntimeDataScopeService dataScopes;
    private final RuntimeReadPipeline readPipeline;

    public long count(LowcodeRuntimeConfig config, DynamicCrudQuery query, DataScopeContext dataScope,
                      LowcodePrimaryKeyStrategy primaryKey) {
        RuntimeQueryPlans.Search plan = plans.search(config, query);
        RuntimeJoinContext join = plan.join();
        if (join != null && joinQuery.export(config, plan.params(), join)) {
            var scope = dataScopes.buildRuntimeCondition(config, plan.table(), "t0", dataScope);
            return repository.countJoined(plan.table(), joinQuery.select(join,
                            DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper),
                            true, RuntimeQueryPlans.PrimaryKey.from(primaryKey).field()),
                    join.joins(), plan.params(), plan.allowed(), plan.types(), join.fieldColumnMapping(), scope);
        }
        return repository.countList(plan.table(), plan.params(), plan.allowed(), plan.types(), plan.columns(),
                dataScopes.buildRuntimeCondition(config, plan.table(), null, dataScope));
    }

    public List<Map<String, Object>> page(LowcodeRuntimeConfig config, DynamicCrudQuery query, Integer pageNum,
                                          Integer pageSize, DataScopeContext dataScope,
                                          LowcodePrimaryKeyStrategy primaryKey) {
        RuntimeQueryPlans.Search plan = plans.search(config, query);
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 1000 : Math.min(pageSize, 5000);
        RuntimeJoinContext join = plan.join();
        RuntimeQueryPlans.PrimaryKey key = RuntimeQueryPlans.PrimaryKey.from(primaryKey);
        boolean joined = join != null && joinQuery.export(config, plan.params(), join);
        List<Map<String, Object>> rows;
        if (joined) {
            rows = repository.selectJoinedPageRecords(plan.table(), joinQuery.select(join,
                            DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), true, key.field()),
                    join.joins(), current, size, plan.params(), plan.allowed(), plan.types(), join.fieldColumnMapping(),
                    key.descending("t0"), dataScopes.buildRuntimeCondition(config, plan.table(), "t0", dataScope));
        } else {
            rows = repository.selectPageRecords(plan.table(), current, size, plan.params(), plan.allowed(),
                    plan.types(), plan.columns(), key.descending(null),
                    dataScopes.buildRuntimeCondition(config, plan.table(), null, dataScope));
            rows = DynamicQueryGenerator.convertListToCamelCase(rows);
        }
        readPipeline.apply(rows, config);
        return rows;
    }

    public static int limit(Integer maxRows) {
        return maxRows == null || maxRows < 1 ? MAX_EXPORT_ROWS : Math.min(maxRows, MAX_EXPORT_ROWS);
    }
}
