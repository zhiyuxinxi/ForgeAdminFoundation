package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeUniqueConstraintSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import java.util.stream.Collectors;

/** Resolves unique constraints from published model and edit schemas in their existing order. */
@lombok.RequiredArgsConstructor
public final class RuntimeUniqueRules {
    private final RuntimeEditFieldReader editReader;

    List<LowcodeUniqueConstraintSchema> resolve(LowcodeRuntimeConfig config, LowcodeModelSchema model) {
        List<LowcodeUniqueConstraintSchema> constraints = resolveUniqueConstraints(model);
        appendEditSchemaUniqueConstraints(config, constraints);
        return constraints;
    }

    private List<LowcodeUniqueConstraintSchema> resolveUniqueConstraints(LowcodeModelSchema modelSchema) {
        List<LowcodeUniqueConstraintSchema> result = new ArrayList<>();
        if (modelSchema == null) {
            return result;
        }
        if (modelSchema.getUniqueConstraints() != null) {
            result.addAll(modelSchema.getUniqueConstraints());
        }
        if (modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (!isFieldUniqueEnabled(field)) {
                    continue;
                }
                LowcodeUniqueConstraintSchema constraint = new LowcodeUniqueConstraintSchema();
                constraint.setName("uk_" + StringUtils.defaultIfBlank(field.getColumnName(),
                        DynamicQueryGenerator.camelToSnake(field.getField())));
                constraint.setFields(List.of(field.getField()));
                constraint.setScope("TENANT");
                constraint.setNormalize(List.of("trim"));
                constraint.setIgnoreBlank(true);
                constraint.setMessage(StringUtils.defaultIfBlank(field.getLabel(), field.getField()) + "已存在");
                result.add(constraint);
            }
        }
        if (modelSchema.getValidationRules() != null) {
            for (Map<String, Object> rule : modelSchema.getValidationRules()) {
                if (!"UNIQUE".equalsIgnoreCase(text(rule.get("type")))) {
                    continue;
                }
                LowcodeUniqueConstraintSchema constraint = new LowcodeUniqueConstraintSchema();
                constraint.setName(text(rule.get("name")));
                constraint.setFields(toStringList(firstNonNull(rule.get("fields"), rule.get("field"))));
                constraint.setScope(text(rule.get("scope")));
                constraint.setNormalize(toStringList(firstNonNull(rule.get("normalize"), rule.get("normalizers"))));
                constraint.setIgnoreBlank(rule.get("ignoreBlank") == null || Boolean.parseBoolean(text(rule.get("ignoreBlank"))));
                constraint.setMessage(text(rule.get("message")));
                result.add(constraint);
            }
        }
        return result;
    }

    private void appendEditSchemaUniqueConstraints(LowcodeRuntimeConfig config, List<LowcodeUniqueConstraintSchema> constraints) {
        List<Map<String, Object>> editFields = editReader.read(config);
        if (editFields.isEmpty()) {
            return;
        }
        Set<String> existingKeys = constraints.stream()
                .filter(Objects::nonNull)
                .map(constraint -> uniqueConstraintKey(constraint.getFields()))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Map<String, Object> editField : editFields) {
            if (!isEditSchemaUniqueEnabled(editField)) {
                continue;
            }
            String fieldName = text(editField.get("field"));
            if (StringUtils.isBlank(fieldName)) {
                continue;
            }
            String key = uniqueConstraintKey(List.of(fieldName));
            if (!existingKeys.add(key)) {
                continue;
            }
            LowcodeUniqueConstraintSchema constraint = new LowcodeUniqueConstraintSchema();
            constraint.setName("uk_" + DynamicQueryGenerator.camelToSnake(fieldName));
            constraint.setFields(List.of(fieldName));
            constraint.setScope("TENANT");
            constraint.setNormalize(List.of("trim"));
            constraint.setIgnoreBlank(true);
            constraint.setMessage(StringUtils.defaultIfBlank(text(editField.get("label")), fieldName) + "已存在");
            constraints.add(constraint);
        }
    }

    private String uniqueConstraintKey(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        return fields.stream()
                .filter(StringUtils::isNotBlank)
                .map(DynamicQueryGenerator::camelToSnake)
                .collect(Collectors.joining("|"));
    }

    private boolean isEditSchemaUniqueEnabled(Map<String, Object> editField) {
        if (editField == null || editField.isEmpty()) {
            return false;
        }
        return isTrue(editField.get("unique"))
                || isTrue(mapFrom(editField.get("advancedProps")).get("unique"))
                || isTrue(mapFrom(editField.get("advancedProps")).get("uniqueCheck"))
                || isTrue(mapFrom(editField.get("props")).get("unique"))
                || isTrue(mapFrom(editField.get("basicProps")).get("unique"));
    }

    private boolean isFieldUniqueEnabled(LowcodeFieldSchema field) {
        if (field == null) {
            return false;
        }
        return isTrue(field.getAdvancedProps() == null ? null : field.getAdvancedProps().get("unique"))
                || isTrue(field.getAdvancedProps() == null ? null : field.getAdvancedProps().get("uniqueCheck"))
                || isTrue(field.getBasicProps() == null ? null : field.getBasicProps().get("unique"));
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::text)
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }
        String textValue = text(value);
        return StringUtils.isBlank(textValue) ? List.of() : List.of(textValue);
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapFrom(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    String message(LowcodeUniqueConstraintSchema constraint,
                                        Map<String, LowcodeFieldSchema> fieldMap) {
        if (StringUtils.isNotBlank(constraint.getMessage())) {
            return constraint.getMessage();
        }
        if (constraint.getFields() != null && constraint.getFields().size() == 1) {
            LowcodeFieldSchema field = fieldMap.get(constraint.getFields().get(0));
            if (field != null && StringUtils.isNotBlank(field.getLabel())) {
                return field.getLabel() + "已存在";
            }
        }
        return "字段值已存在";
    }
}
