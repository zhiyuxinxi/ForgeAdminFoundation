package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeWriteFieldMapping;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds query metadata inside the datasource scope established by the application service. */
@RequiredArgsConstructor
public final class RuntimeQueryPlans {
    private final RuntimeWriteFieldMapping fieldMapping;
    private final RuntimeSearchFields searchFields;
    private final RuntimeSearchConditions searchConditions;
    private final RuntimeJoinPlanner joinPlanner;

    public Search search(LowcodeRuntimeConfig config, DynamicCrudQuery query) {
        String table = config.getTableName();
        Map<String, String> columns = fieldMapping.columns(config, table);
        Set<String> allowed = searchFields.search(config);
        Map<String, String> types = searchFields.types(config, query, allowed);
        Map<String, Object> params = query != null ? query.getSearchParams() : null;
        params = searchConditions.expand(params, config, table, allowed, types);
        return new Search(table, columns, allowed, types, params, joinPlanner.plan(config));
    }

    public Custom custom(LowcodeRuntimeConfig config, CustomQueryExecuteDTO request) {
        String table = config.getTableName();
        Map<String, String> columns = fieldMapping.columns(config, table);
        Set<String> allowed = searchFields.custom(config);
        RuntimeJoinContext join = joinPlanner.plan(config);
        if (join != null) {
            allowed.addAll(join.fields().keySet());
        }
        List<CustomQueryConditionDTO> conditions = searchConditions.expandCustom(
                request.getConditions(), config, table, allowed);
        return new Custom(table, columns, allowed, conditions, join);
    }

    public Set<String> scheduledFields(LowcodeRuntimeConfig config) {
        return searchFields.custom(config);
    }

    public Map<String, String> scheduledColumns(LowcodeRuntimeConfig config) {
        return fieldMapping.columns(config, config.getTableName());
    }

    public record Search(String table, Map<String, String> columns, Set<String> allowed,
                         Map<String, String> types, Map<String, Object> params, RuntimeJoinContext join) {
    }

    public record Custom(String table, Map<String, String> columns, Set<String> allowed,
                         List<CustomQueryConditionDTO> conditions, RuntimeJoinContext join) {
    }

    public record PrimaryKey(String field, String column) {
        public static PrimaryKey from(LowcodePrimaryKeyStrategy key) {
            return new PrimaryKey(StringUtils.defaultIfBlank(key != null ? key.getField() : null, "id"),
                    StringUtils.defaultIfBlank(key != null ? key.getColumnName() : null, "id"));
        }

        public String descending(String alias) {
            return (StringUtils.isBlank(alias) ? column : alias + "." + column) + " DESC";
        }
    }
}
