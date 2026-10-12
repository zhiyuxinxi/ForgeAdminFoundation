package com.mdframe.forge.plugin.generator.service.runtime.write;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Resolves explicit and conventional numbering plans in published schema order. */
@lombok.RequiredArgsConstructor
public final class RuntimeGenerationRules {
    private final RuntimeFieldMetadata fieldMetadata;
    private final RuntimeEditFieldReader editFields;

    List<RuntimeGenerationField> explicit(LowcodeRuntimeConfig config) {
        if (config == null) {
            return List.of();
        }
        Map<String, RuntimeGenerationField> fields = new LinkedHashMap<>();
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                RuntimeGenerationField autoField = RuntimeGenerationField.explicit(
                        field.getField(), field.getColumnName(), field.getLabel(),
                        generation(field.getBasicProps(), field.getAdvancedProps()));
                putRuntimeGenerationField(fields, autoField);
            }
        }
        for (Map<String, Object> editField : editFields.read(config)) {
            Map<String, Object> props = mapFrom(editField.get("props"));
            Map<String, Object> advancedProps = mapFrom(editField.get("advancedProps"));
            RuntimeGenerationField autoField = RuntimeGenerationField.explicit(
                    text(editField.get("field")),
                    StringUtils.firstNonBlank(text(editField.get("columnName")), text(editField.get("fieldCode"))),
                    StringUtils.firstNonBlank(text(editField.get("label")), text(editField.get("title"))),
                    firstMap(props.get("generation"), editField.get("generation"), advancedProps.get("generation"))
            );
            putRuntimeGenerationField(fields, autoField);
        }
        return new ArrayList<>(fields.values());
    }

    private void putRuntimeGenerationField(Map<String, RuntimeGenerationField> fields, RuntimeGenerationField field) {
        if (field == null || !field.enabled() || StringUtils.isBlank(field.ruleCode())) {
            return;
        }
        String key = StringUtils.firstNonBlank(field.fieldName(), field.columnName());
        if (StringUtils.isBlank(key)) {
            return;
        }
        fields.putIfAbsent(key, field);
    }

    List<RuntimeGenerationField> conventional(LowcodeRuntimeConfig config) {
        if (config == null) {
            return List.of();
        }
        Map<String, RuntimeGenerationField> fields = new LinkedHashMap<>();
        Set<String> generationConfiguredAliases = new LinkedHashSet<>();
        LowcodeModelSchema modelSchema = fieldMetadata.model(config);
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field != null && hasGenerationConfig(field.getBasicProps(), field.getAdvancedProps())) {
                    addFieldAliases(generationConfiguredAliases, field.getField(), field.getColumnName());
                    continue;
                }
                RuntimeGenerationField candidate = buildConventionalCodeRuleField(field);
                putRuntimeGenerationField(fields, candidate);
            }
        }
        for (Map<String, Object> editField : editFields.read(config)) {
            String fieldName = text(editField.get("field"));
            String columnName = StringUtils.firstNonBlank(text(editField.get("columnName")), text(editField.get("fieldCode")));
            Map<String, Object> props = mapFrom(editField.get("props"));
            Map<String, Object> advancedProps = mapFrom(editField.get("advancedProps"));
            if (!firstMap(props.get("generation"), editField.get("generation"), advancedProps.get("generation")).isEmpty()) {
                addFieldAliases(generationConfiguredAliases, fieldName, columnName);
                continue;
            }
            if (RuntimeWriteFieldMapping.matches(fieldName, generationConfiguredAliases)
                    || RuntimeWriteFieldMapping.matches(columnName, generationConfiguredAliases)) {
                continue;
            }
            RuntimeGenerationField candidate = buildConventionalCodeRuleField(
                    fieldName,
                    columnName,
                    StringUtils.firstNonBlank(text(editField.get("label")), text(editField.get("title"))),
                    props,
                    advancedProps
            );
            putRuntimeGenerationField(fields, candidate);
        }
        return new ArrayList<>(fields.values());
    }

    private RuntimeGenerationField buildConventionalCodeRuleField(LowcodeFieldSchema field) {
        if (field == null || Boolean.TRUE.equals(field.getSystemField())
                || "DISABLED".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()))
                || "HIDDEN".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()))) {
            return null;
        }
        return buildConventionalCodeRuleField(
                field.getField(),
                field.getColumnName(),
                field.getLabel(),
                field.getBasicProps(),
                field.getAdvancedProps()
        );
    }

    private RuntimeGenerationField buildConventionalCodeRuleField(String fieldName,
                                                               String columnName,
                                                               String label,
                                                               Map<String, Object> basicProps,
                                                               Map<String, Object> advancedProps) {
        if (hasGenerationConfig(basicProps, advancedProps)) {
            return null;
        }
        if (!isConventionalCodeField(fieldName, columnName, label)) {
            return null;
        }
        String ruleCode = StringUtils.firstNonBlank(columnName, DynamicQueryGenerator.camelToSnake(fieldName));
        if (StringUtils.isBlank(ruleCode)) {
            return null;
        }
        return new RuntimeGenerationField(
                StringUtils.trimToNull(fieldName),
                StringUtils.trimToNull(columnName),
                StringUtils.trimToNull(label),
                ruleCode.trim(),
                "ON_CREATE",
                "EMPTY_ONLY",
                true
        );
    }

    private boolean hasGenerationConfig(Map<String, Object> basicProps, Map<String, Object> advancedProps) {
        return !firstMap(
                basicProps == null ? null : basicProps.get("generation"),
                advancedProps == null ? null : advancedProps.get("generation")
        ).isEmpty();
    }

    private void addFieldAliases(Set<String> aliases, String fieldName, String columnName) {
        aliases.addAll(RuntimeWriteFieldMapping.aliases(fieldName));
        aliases.addAll(RuntimeWriteFieldMapping.aliases(columnName));
    }

    private boolean isConventionalCodeField(String fieldName, String columnName, String label) {
        String field = StringUtils.defaultString(fieldName);
        String column = StringUtils.defaultString(columnName);
        String fieldLower = field.toLowerCase(Locale.ROOT);
        String columnLower = column.toLowerCase(Locale.ROOT);
        return fieldLower.endsWith("code")
                || fieldLower.endsWith("no")
                || columnLower.endsWith("_code")
                || columnLower.endsWith("_no")
                || StringUtils.containsAny(StringUtils.defaultString(label), "编号", "单号");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstMap(Object... values) {
        if (values == null) {
            return Map.of();
        }
        for (Object value : values) {
            if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                return (Map<String, Object>) map;
            }
        }
        return Map.of();
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

    boolean isConfigured(LowcodeRuntimeConfig config, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return false;
        }
        Set<String> requestedAliases = RuntimeWriteFieldMapping.aliases(fieldName);
        return explicit(config).stream()
                .anyMatch(field -> RuntimeWriteFieldMapping.matches(field.fieldName(), requestedAliases)
                        || RuntimeWriteFieldMapping.matches(field.columnName(), requestedAliases));
    }

    private Map<String, Object> generation(Map<String, Object> basic, Map<String, Object> advanced) {
        return firstMap(basic == null ? null : basic.get("generation"),
                advanced == null ? null : advanced.get("generation"));
    }
}
