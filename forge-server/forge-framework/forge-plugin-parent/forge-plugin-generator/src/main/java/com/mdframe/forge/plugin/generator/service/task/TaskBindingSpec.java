package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** A definition reference is configuration; taskIdField stores the resulting instance reference. */
public record TaskBindingSpec(long definitionId, String assigneeField, String titleField, String dueAtField,
                              String taskIdField, String stateField, String readyValue, String completedValue,
                              String cancelledValue, Map<String, String> resultFields) {
    public TaskBindingSpec {
        resultFields = resultFields == null ? Map.of() : Map.copyOf(resultFields);
    }

    void validate(JsonNode model) {
        require(definitionId > 0 && resultFields.size() <= 32);
        require(value(readyValue) && value(completedValue) && value(cancelledValue));
        require(!readyValue.equals(completedValue) && !readyValue.equals(cancelledValue)
                && !completedValue.equals(cancelledValue));
        Set<String> reads = new HashSet<>();
        reads.add(assigneeField);
        reads.add(titleField);
        if (dueAtField != null) { reads.add(dueAtField); }
        Set<String> writes = new HashSet<>();
        require(writes.add(taskIdField) && writes.add(stateField));
        resultFields.forEach((key, field) -> {
            require(identifier(key) && writes.add(field));
        });
        require(reads.stream().noneMatch(writes::contains));
        Set<String> all = new HashSet<>(reads);
        all.addAll(writes);
        for (String name : all) {
            require(identifier(name));
            JsonNode field = null;
            for (JsonNode candidate : model.path("fields")) {
                if (name.equals(candidate.path("field").asText())) {
                    field = candidate;
                    break;
                }
            }
            require(field != null && !field.path("primaryKey").asBoolean()
                    && !field.path("systemField").asBoolean()
                    && !"DISABLED".equals(field.path("fieldStatus").asText()));
            if (writes.contains(name)) {
                require(!field.hasNonNull("formulaConfig") || field.path("formulaConfig").isEmpty());
            }
        }
    }

    private static boolean identifier(String value) {
        return value != null && value.matches("[A-Za-z][A-Za-z0-9_]{0,63}");
    }

    void validateCompatibility(JsonNode original, JsonNode current) {
        require(Objects.equals(original.path("tableName"), current.path("tableName")));
        Set<String> used = new HashSet<>(resultFields.values());
        used.add(assigneeField);
        used.add(titleField);
        used.add(taskIdField);
        used.add(stateField);
        if (dueAtField != null) { used.add(dueAtField); }
        for (String name : used) {
            JsonNode before = find(original, name);
            JsonNode after = find(current, name);
            require(before != null && after != null);
            require(Objects.equals(before.path("columnName"), after.path("columnName"))
                    && Objects.equals(before.path("dataType"), after.path("dataType")));
        }
    }

    private JsonNode find(JsonNode model, String name) {
        for (JsonNode field : model.path("fields")) {
            if (name.equals(field.path("field").asText())) { return field; }
        }
        return null;
    }

    private static boolean value(String value) {
        return value != null && !value.isBlank() && value.length() <= 64;
    }

    static void require(boolean condition) {
        if (!condition) { throw invalid(); }
    }

    static BusinessException invalid() {
        return new BusinessException(409, UiMessages.label(
                "任务绑定、来源状态或字段映射无效，请检查已发布版本", "Invalid task binding, source state or field mapping; check the published version"));
    }
}
