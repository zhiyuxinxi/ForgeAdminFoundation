package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;

/** Normalizes the two existing query input shapes without mutating caller inputs. */
@RequiredArgsConstructor
public final class RuntimeSearchConditions {
    private final RuntimeDescendantValues descendants;

    public Map<String, Object> expand(Map<String, Object> searchParams,
                                                            LowcodeRuntimeConfig config,
                                                            String tableName,
                                                            Set<String> allowedSearchFields,
                                                            Map<String, String> searchTypeMap) {
        if (searchParams == null || searchParams.isEmpty()) {
            return searchParams;
        }
        Map<String, Object> expanded = new LinkedHashMap<>(searchParams);
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, Object> entry : searchParams.entrySet()) {
            String key = entry.getKey();
            if (!key.endsWith("_includeChildren")) continue;
            if (!isTruthy(entry.getValue())) continue;
            String baseField = key.substring(0, key.length() - "_includeChildren".length());
            if (!allowedSearchFields.contains(baseField)) continue;
            Object baseValue = searchParams.get(baseField);
            if (baseValue == null) continue;
            keysToRemove.add(key);
            List<Object> values = normalizeIncludeChildrenValues(baseValue);
            if (values == null) {
                values = descendants.resolve(config, tableName, baseField, baseValue);
            }
            if (values == null || values.isEmpty()) {
                continue;
            }
            expanded.put(baseField, values);
            if (searchTypeMap != null) {
                searchTypeMap.put(baseField, "in");
            }
        }
        for (String key : keysToRemove) {
            expanded.remove(key);
        }
        return expanded;
    }

    public List<CustomQueryConditionDTO> expandCustom(List<CustomQueryConditionDTO> conditions,
                                                                                LowcodeRuntimeConfig config,
                                                                                String tableName,
                                                                                Set<String> allowedFields) {
        if (conditions == null || conditions.isEmpty()) {
            return conditions;
        }
        List<CustomQueryConditionDTO> expanded = new ArrayList<>(conditions.size());
        boolean changed = false;
        for (CustomQueryConditionDTO condition : conditions) {
            if (condition == null || !Boolean.TRUE.equals(condition.getIncludeChildren())
                    || StringUtils.isBlank(condition.getField()) || !allowedFields.contains(condition.getField())) {
                expanded.add(condition);
                continue;
            }
            List<Object> values = normalizeIncludeChildrenValues(condition.getValue());
            if (values == null) {
                values = descendants.resolve(config, tableName, condition.getField(), condition.getValue());
            }
            if (values == null || values.isEmpty()) {
                expanded.add(condition);
                continue;
            }
            CustomQueryConditionDTO next = new CustomQueryConditionDTO();
            next.setRelation(condition.getRelation());
            next.setField(condition.getField());
            next.setOperator("in");
            next.setValue(values);
            next.setValueEnd(null);
            next.setIncludeChildren(true);
            expanded.add(next);
            changed = true;
        }
        return changed ? expanded : conditions;
    }

    private boolean isTruthy(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return true;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text) || "1".equals(text);
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return false;
    }

    private List<Object> normalizeIncludeChildrenValues(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !(item instanceof String text) || StringUtils.isNotBlank(text))
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        if (value instanceof Object[] array) {
            return Arrays.stream(array)
                    .filter(Objects::nonNull)
                    .filter(item -> !(item instanceof String text) || StringUtils.isNotBlank(text))
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        if (value instanceof String text && text.contains(",")) {
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .map(item -> (Object) item)
                    .toList();
        }
        return null;
    }
}
