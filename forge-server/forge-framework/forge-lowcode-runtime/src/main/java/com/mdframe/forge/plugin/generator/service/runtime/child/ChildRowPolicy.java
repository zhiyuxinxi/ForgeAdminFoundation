package com.mdframe.forge.plugin.generator.service.runtime.child;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Child input projection, immutable field protection and row validation; no database access. */
public final class ChildRowPolicy {
    public static final Set<String> IMMUTABLE_WRITE_FIELDS = Set.of(
            "id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
            "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time", "delFlag", "del_flag"
    );

    public static boolean isImmutableWriteField(String key) {
        return IMMUTABLE_WRITE_FIELDS.contains(key);
    }

    private ChildRowPolicy() {
    }

    public static Map<String, Object> filterChildWriteData(Map<String, Object> data, RuntimeChildRelation relation) {
        Map<String, Object> childData = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return childData;
        }
        for (RuntimeFieldRef fieldRef : relation.fields().values()) {
            if (fieldRef.primary() || isImmutableWriteField(fieldRef.fieldName()) || isImmutableWriteField(fieldRef.sourceField())) {
                continue;
            }
            if (fieldRef.columnName().equals(relation.childFkColumn())) {
                continue;
            }
            Object value = firstPresent(data, fieldRef.sourceField(), fieldRef.fieldName(), fieldRef.columnName());
            if (value != null) {
                childData.put(fieldRef.columnName(), value);
            }
        }
        return childData;
    }

    public static Object resolveChildRowId(Map<String, Object> row) {
        return firstPresent(row, "id");
    }

    public static boolean isDeletedChildRow(Map<String, Object> row) {
        Object deleted = firstPresent(row, "_deleted", "__deleted");
        if (deleted instanceof Boolean flag) {
            return flag;
        }
        return deleted != null && Set.of("true", "1", "yes", "y")
                .contains(String.valueOf(deleted).trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isMergeChildSaveMode(RuntimeChildRelation relation) {
        return relation != null && "merge".equalsIgnoreCase(StringUtils.defaultString(relation.saveMode()));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractChildrenPayload(Map<String, Object> data) {
        if (data != null && data.get("children") instanceof Map<?, ?> children) {
            return (Map<String, Object>) children;
        }
        return Map.of();
    }

    /** Validate the complete envelope before any parent or child write. */
    public static Map<String, Object> validatedChildrenPayload(Map<String, Object> data,
                                                              List<RuntimeChildRelation> relations) {
        if (data == null || !data.containsKey("children")) {
            return Map.of();
        }
        if (!(data.get("children") instanceof Map<?, ?> children)) {
            throw new BusinessException(UiMessages.message("lowcode.children.invalid"));
        }
        Set<String> allowed = relations.stream().map(RuntimeChildRelation::modelCode).collect(Collectors.toSet());
        for (Map.Entry<?, ?> entry : children.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !allowed.contains(key)) {
                throw new BusinessException(UiMessages.message("lowcode.children.unknown", entry.getKey()));
            }
            Object value = entry.getValue();
            // A single row object is a supported legacy input; [] explicitly clears replacement children.
            if (value instanceof Map<?, ?>) {
                continue;
            }
            if (!(value instanceof List<?> rows) || rows.stream().anyMatch(row -> !(row instanceof Map<?, ?>))) {
                throw new BusinessException(UiMessages.message("lowcode.children.rows.invalid", entry.getKey()));
            }
        }
        return extractChildrenPayload(data);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> normalizeChildRows(Object value) {
        if (value instanceof List<?> rows) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }
        return List.of();
    }

    public static boolean hasWritableChildData(Map<String, Object> childData) {
        if (childData == null || childData.isEmpty()) {
            return false;
        }
        return childData.values().stream().anyMatch(value -> value != null && !(value instanceof String text && StringUtils.isBlank(text)));
    }

    public static Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    public static WritePartition partition(Map<String, Object> data,
                                       Set<String> allowedFields,
                                       Map<String, RuntimeFieldRef> fields,
                                       List<RuntimeChildRelation> relations) {
        Map<String, Object> primaryData = new LinkedHashMap<>();
        Map<String, Map<String, Object>> childDataMap = new LinkedHashMap<>();
        if (data == null || data.isEmpty()) {
            return new WritePartition(primaryData, childDataMap);
        }
        Map<String, RuntimeChildRelation> relationMap = relations.stream()
                .collect(Collectors.toMap(RuntimeChildRelation::modelCode, relation -> relation, (left, right) -> left, LinkedHashMap::new));
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (isImmutableWriteField(key) || !allowedFields.contains(key)) {
                continue;
            }
            RuntimeFieldRef fieldRef = fields.get(key);
            if (fieldRef == null) {
                continue;
            }
            if (fieldRef.primary()) {
                primaryData.put(fieldRef.columnName(), entry.getValue());
                continue;
            }
            RuntimeChildRelation relation = relationMap.get(fieldRef.modelCode());
            if (relation == null) {
                continue;
            }
            childDataMap.computeIfAbsent(fieldRef.modelCode(), ignored -> new LinkedHashMap<>())
                    .put(fieldRef.columnName(), entry.getValue());
        }
        return new WritePartition(primaryData, childDataMap);
    }

    public record WritePartition(Map<String, Object> primaryData, Map<String, Map<String, Object>> childData) {
    }

}
