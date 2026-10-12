package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.lowcode.runtime.service.RuntimeDataScopeService;
import com.mdframe.forge.plugin.generator.service.runtime.read.RuntimeReadPipeline;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Executes interactive pages and bounded scheduler lists using authorized runtime metadata. */
@RequiredArgsConstructor
public final class RuntimePageQueries {
    private final DynamicCrudRepository repository;
    private final RuntimeQueryPlans plans;
    private final RuntimeJoinQuery joinQuery;
    private final ObjectMapper objectMapper;
    private final RuntimeDataScopeService dataScopes;
    private final RuntimeReadPipeline readPipeline;

    public Page<Map<String, Object>> page(LowcodeRuntimeConfig config, PageQuery request,
                                          DynamicCrudQuery query, LowcodePrimaryKeyStrategy primaryKey) {
        RuntimeQueryPlans.Search plan = plans.search(config, query);
        RuntimeJoinContext join = plan.join();
        RuntimeQueryPlans.PrimaryKey key = RuntimeQueryPlans.PrimaryKey.from(primaryKey);
        boolean joined = join != null && joinQuery.page(config, request, plan.params(), join);
        Page<Map<String, Object>> page;
        if (joined) {
            var scope = dataScopes.buildRuntimeCondition(config, plan.table(), "t0");
            page = repository.selectJoinedPage(plan.table(), joinQuery.select(join,
                            DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), true, key.field()),
                    join.joins(), request.getPageNum(), request.getPageSize(), plan.params(), plan.allowed(),
                    plan.types(), join.fieldColumnMapping(),
                    joinQuery.orderBy(request.getOrderByColumn(), request.getIsAsc(), join, key.descending("t0")), scope);
        } else {
            String order = DynamicQueryGenerator.buildOrderByClause(
                    request.getOrderByColumn(), request.getIsAsc(), plan.columns());
            page = repository.selectPage(plan.table(), request.getPageNum(), request.getPageSize(), plan.params(),
                    plan.allowed(), plan.types(), plan.columns(), order,
                    dataScopes.buildRuntimeCondition(config, plan.table(), null));
        }
        return project(page, config, joined);
    }

    public Page<Map<String, Object>> custom(LowcodeRuntimeConfig config, CustomQueryExecuteDTO request,
                                            LowcodePrimaryKeyStrategy primaryKey) {
        RuntimeQueryPlans.Custom plan = plans.custom(config, request);
        RuntimeJoinContext join = plan.join();
        RuntimeQueryPlans.PrimaryKey key = RuntimeQueryPlans.PrimaryKey.from(primaryKey);
        boolean joined = join != null && joinQuery.custom(request, join);
        Page<Map<String, Object>> page;
        if (joined) {
            var scope = dataScopes.buildRuntimeCondition(config, plan.table(), "t0");
            page = repository.selectJoinedCustomPage(plan.table(),
                    joinQuery.select(join, request.getFields(), true, key.field()), join.joins(),
                    pageNumber(request.getPageNum()), pageSize(request.getPageSize()), plan.conditions(), plan.allowed(),
                    join.fieldColumnMapping(),
                    joinQuery.orderBy(request.getOrderByColumn(), request.getIsAsc(), join, key.descending("t0")), scope);
        } else {
            String order = DynamicQueryGenerator.buildOrderByClause(
                    request.getOrderByColumn(), request.getIsAsc(), plan.columns());
            page = repository.selectCustomPage(plan.table(), pageNumber(request.getPageNum()),
                    pageSize(request.getPageSize()), request.getFields(), plan.conditions(), plan.allowed(),
                    plan.columns(), order, dataScopes.buildRuntimeCondition(config, plan.table(), null));
        }
        return project(page, config, joined);
    }

    public List<Map<String, Object>> scheduled(LowcodeRuntimeConfig config, String dueField,
                                               LocalDateTime windowStart, LocalDateTime windowEnd,
                                               Integer batchSize, LowcodePrimaryKeyStrategy primaryKey) {
        if (StringUtils.isBlank(dueField)) {
            throw new BusinessException("定时触发缺少到期字段");
        }
        Set<String> allowed = plans.scheduledFields(config);
        if (!allowed.contains(dueField)) {
            throw new BusinessException(UiMessages.translate("定时触发到期字段不在运行配置字段范围内: ") + dueField);
        }
        Map<String, String> columns = plans.scheduledColumns(config);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(dueField, List.of(windowStart, windowEnd));
        Map<String, String> types = new LinkedHashMap<>();
        types.put(dueField, "between");
        List<Map<String, Object>> rows = repository.selectList(config.getTableName(), params, allowed, types,
                columns, RuntimeQueryPlans.PrimaryKey.from(primaryKey).column() + " ASC",
                scheduledBatchSize(batchSize), null);
        List<Map<String, Object>> converted = DynamicQueryGenerator.convertListToCamelCase(rows);
        readPipeline.apply(converted, config);
        return converted;
    }

    private Page<Map<String, Object>> project(Page<Map<String, Object>> page, LowcodeRuntimeConfig config, boolean joined) {
        List<Map<String, Object>> rows = joined ? page.getRecords()
                : DynamicQueryGenerator.convertListToCamelCase(page.getRecords());
        readPipeline.apply(rows, config);
        if (!joined) {
            page.setRecords(rows);
        }
        return page;
    }

    private int pageNumber(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int pageSize(Integer value) {
        return value == null || value < 1 ? 10 : Math.min(value, 100);
    }

    private int scheduledBatchSize(Integer value) {
        return value == null || value < 1 ? 50 : Math.min(value, 200);
    }
}
