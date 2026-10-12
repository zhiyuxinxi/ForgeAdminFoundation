package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeFieldRef;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.starter.core.domain.PageQuery;

/** Chooses joined query routing, projections and ordering from a compiled join plan. */
@lombok.RequiredArgsConstructor
public final class RuntimeJoinQuery {
    private final ObjectMapper objectMapper;

    public String orderBy(String orderByColumn, String isAsc, RuntimeJoinContext joinContext, String defaultOrderBy) {
        if (StringUtils.isBlank(orderByColumn)) {
            return defaultOrderBy;
        }
        List<String> columns = Arrays.stream(orderByColumn.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(column -> joinContext.fieldColumnMapping().get(column))
                .filter(StringUtils::isNotBlank)
                .toList();
        if (columns.isEmpty()) {
            return defaultOrderBy;
        }
        String direction = "asc".equalsIgnoreCase(isAsc) ? "ASC" : "DESC";
        return String.join(", ", columns) + " " + direction;
    }

    public boolean page(LowcodeRuntimeConfig config,
                                            PageQuery pageQuery,
                                            Map<String, Object> searchParams,
                                            RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsRelationDisplayField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext)
                || containsChildField(splitFields(pageQuery.getOrderByColumn()), joinContext);
    }

    public boolean export(LowcodeRuntimeConfig config,
                                              Map<String, Object> searchParams,
                                              RuntimeJoinContext joinContext) {
        return containsChildField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsRelationDisplayField(DynamicQueryGenerator.extractFieldNames(config.getColumnsSchema(), objectMapper), joinContext)
                || containsActiveChildSearchField(searchParams, joinContext);
    }

    public boolean custom(CustomQueryExecuteDTO request, RuntimeJoinContext joinContext) {
        if (request == null) {
            return false;
        }
        if (containsChildField(request.getFields(), joinContext)) {
            return true;
        }
        if (containsRelationDisplayField(request.getFields(), joinContext)) {
            return true;
        }
        if (containsChildField(splitFields(request.getOrderByColumn()), joinContext)) {
            return true;
        }
        if (request.getConditions() == null) {
            return false;
        }
        for (var condition : request.getConditions()) {
            if (condition != null && isChildRuntimeField(condition.getField(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    public List<DynamicCrudRepository.JoinField> select(RuntimeJoinContext joinContext,
                                                                           Collection<String> requestedFields,
                                                                           boolean defaultPrimaryFields, String primaryKeyField) {
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
        fieldNames.add(primaryKeyField);
        if (requestedFields != null) {
            requestedFields.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(fieldNames::add);
        }
        if (fieldNames.size() == 1 && defaultPrimaryFields) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(RuntimeFieldRef::fieldName)
                    .forEach(fieldNames::add);
        }
        List<String> relationAliases = fieldNames.stream()
                .map(fieldName -> joinContext.relationDisplayAliases().get(fieldName))
                .filter(StringUtils::isNotBlank)
                .toList();
        fieldNames.addAll(relationAliases);

        List<DynamicCrudRepository.JoinField> selectFields = new ArrayList<>();
        for (String fieldName : fieldNames) {
            RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
            if (fieldRef == null) {
                continue;
            }
            selectFields.add(new DynamicCrudRepository.JoinField(
                    fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()));
        }
        if (selectFields.isEmpty()) {
            joinContext.fields().values().stream()
                    .filter(RuntimeFieldRef::primary)
                    .map(fieldRef -> new DynamicCrudRepository.JoinField(
                            fieldRef.fieldName(), fieldRef.tableAlias(), fieldRef.columnName()))
                    .forEach(selectFields::add);
        }
        return selectFields;
    }

    private boolean containsChildField(Collection<String> fieldNames, RuntimeJoinContext joinContext) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (isChildRuntimeField(fieldName, joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRelationDisplayField(Collection<String> fieldNames, RuntimeJoinContext joinContext) {
        if (fieldNames == null || fieldNames.isEmpty() || joinContext == null || joinContext.relationDisplayAliases().isEmpty()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (joinContext.relationDisplayAliases().containsKey(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsActiveChildSearchField(Map<String, Object> searchParams, RuntimeJoinContext joinContext) {
        if (searchParams == null || searchParams.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            if (hasQueryValue(entry.getValue()) && isChildRuntimeField(entry.getKey(), joinContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isChildRuntimeField(String fieldName, RuntimeJoinContext joinContext) {
        if (StringUtils.isBlank(fieldName) || joinContext == null) {
            return false;
        }
        RuntimeFieldRef fieldRef = joinContext.fields().get(fieldName);
        return fieldRef != null && !fieldRef.primary();
    }

    private List<String> splitFields(String fields) {
        if (StringUtils.isBlank(fields)) {
            return List.of();
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private boolean hasQueryValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return StringUtils.isNotBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }
}
