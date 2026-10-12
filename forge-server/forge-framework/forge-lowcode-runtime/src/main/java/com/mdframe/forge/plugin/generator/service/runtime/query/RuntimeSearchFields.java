package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.DynamicCrudQuery;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;

/** Compiles configured query fields and accepts only supported operator overrides. */
@RequiredArgsConstructor
public final class RuntimeSearchFields {
    private static final Set<String> SUPPORTED_SEARCH_TYPES = Set.of(
            "eq", "ne", "like", "left_like", "right_like", "gt", "ge", "gte", "lt", "le", "lte",
            "in", "between", "is_null", "is_not_null"
    );
    private final ObjectMapper objectMapper;
    private final RuntimeSearchTreeSource trees;

    public Set<String> custom(LowcodeRuntimeConfig config) {
        Set<String> fields = new HashSet<>();
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getSearchSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper));
        fields.addAll(DynamicQueryGenerator.extractFieldNames(config.getEditSchema(), objectMapper));
        fields.add("id");
        return fields;
    }

    public Set<String> search(LowcodeRuntimeConfig config) {
        Set<String> fields = new HashSet<>(custom(config));
        if (trees.isTreeRuntime(config)) {
            String filterField = trees.filterField(config);
            if (StringUtils.isNotBlank(filterField)) {
                fields.add(filterField);
            }
        }
        return fields;
    }

    public Map<String, String> types(LowcodeRuntimeConfig config,
                                                             DynamicCrudQuery query,
                                                             Set<String> allowedSearchFields) {
        Map<String, String> result = new LinkedHashMap<>(
                DynamicQueryGenerator.extractSearchTypeMap(config.getSearchSchema(), objectMapper));
        Map<String, String> requested = query == null ? null : query.getSearchTypeMap();
        if (requested == null || requested.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : requested.entrySet()) {
            String field = StringUtils.trimToNull(entry.getKey());
            String searchType = StringUtils.lowerCase(StringUtils.trimToNull(entry.getValue()), Locale.ROOT);
            if (field != null
                    && allowedSearchFields.contains(field)
                    && searchType != null
                    && SUPPORTED_SEARCH_TYPES.contains(searchType)) {
                result.put(field, searchType);
            }
        }
        return result;
    }
}
