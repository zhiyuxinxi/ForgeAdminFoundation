package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.lowcode.runtime.service.RuntimeCrudService;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskSourceHandler.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

/** Source owner reads and writes through the existing published CRUD/data-scope path. */
@Component
@RequiredArgsConstructor
public class TaskBindingRuntime {
    private final TaskBindingSnapshots snapshots;
    private final RuntimeCrudService crud;

    public record Source(AiCrudConfigVersion version, PublishedTaskBinding binding, String recordId,
                         Map<String, Object> record, String sourceKey) {
        String configKey() { return version.getConfigKey(); }
        TaskBindingSpec spec() { return binding.spec(); }
    }

    Source read(String key, Operation operation) {
        try {
            String[] parts = key.split(":", -1);
            require(parts.length == 3);
            var version = snapshots.version(Long.parseLong(parts[0]));
            var binding = snapshots.binding(version, Long.parseLong(parts[1]));
            String recordId = new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
            require(key.equals(key(version.getId(), binding.id(), recordId)));
            var current = snapshots.latest(version.getConfigKey());
            require(Objects.equals(version.getRuntimeTableName(), current.getRuntimeTableName())
                    && Objects.equals(version.getPrimaryKeyColumn(), current.getPrimaryKeyColumn())
                    && Objects.equals(version.getObjectCode(), current.getObjectCode()));
            binding.spec().validateCompatibility(snapshots.tree(version.getModelSchema()), snapshots.tree(current.getModelSchema()));
            if (operation == Operation.CREATE) { require(version.getId().equals(current.getId())); }
            binding.spec().validate(snapshots.tree(current.getModelSchema()));
            require(SessionHelper.hasPermission("ai:business:" + current.getObjectCode()
                    + (operation == Operation.READ ? ":query" : ":edit")));
            crud.assertLocalTransactionConfig(version.getConfigKey());
            if (operation != Operation.READ) {
                boolean ready = operation == Operation.CREATE || operation == Operation.DISPATCH || operation == Operation.START;
                crud.assertCommandRecord(version.getConfigKey(), recordId,
                        ready ? Map.of(binding.spec().stateField(), binding.spec().readyValue()) : Map.of());
            }
            Map<String, Object> row = crud.selectById(version.getConfigKey(), recordId);
            require(row != null && !row.isEmpty());
            return new Source(version, binding, recordId, project(row, snapshots.tree(current.getModelSchema())), key);
        } catch (IllegalArgumentException exception) { throw TaskBindingSpec.invalid(); }
    }

    private Map<String, Object> project(Map<String, Object> row, JsonNode model) {
        Map<?, ?> main = row.get("main") instanceof Map<?, ?> nested ? nested : row;
        Map<String, Object> projected = new LinkedHashMap<>();
        for (JsonNode field : model.path("fields")) {
            String name = field.path("field").asText();
            String column = field.path("columnName").asText(name);
            for (String alias : new String[]{name, column, DynamicQueryGenerator.snakeToCamel(column)}) {
                if (main.containsKey(alias)) {
                    projected.put(name, main.get(alias));
                    break;
                }
            }
        }
        return projected;
    }

    static String key(long version, long binding, String recordId) {
        require(version > 0 && binding > 0 && recordId != null && recordId.matches("[A-Za-z0-9_-]{1,64}"));
        return version + ":" + binding + ":" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(recordId.getBytes(StandardCharsets.UTF_8));
    }

    BusinessTaskEngine.Create creation(Source source) {
        var spec = source.spec();
        require(spec.readyValue().equals(String.valueOf(source.record().get(spec.stateField()))));
        require(source.record().get(spec.taskIdField()) == null);
        Object assignee = source.record().get(spec.assigneeField());
        Object title = source.record().get(spec.titleField());
        require(assignee != null && title instanceof String && !((String) title).isBlank());
        try {
            long userId = Long.parseLong(String.valueOf(assignee));
            Object due = spec.dueAtField() == null ? null : source.record().get(spec.dueAtField());
            LocalDateTime dueAt = due == null ? null : due instanceof LocalDateTime time ? time
                    : LocalDateTime.parse(String.valueOf(due).replace(' ', 'T'));
            return new BusinessTaskEngine.Create(spec.definitionId(), source.sourceKey(), "once", userId, (String) title, dueAt);
        } catch (IllegalArgumentException exception) { throw TaskBindingSpec.invalid(); }
    }

    Map<String, Object> result(Source source, String value) {
        JsonNode result = snapshots.tree(value);
        var mappings = source.spec().resultFields();
        require(result.size() == mappings.size());
        Map<String, Object> fields = new LinkedHashMap<>();
        mappings.forEach((key, field) -> {
            JsonNode item = result.get(key);
            require(item != null && item.isValueNode() && !item.isNull());
            fields.put(field, item.isBoolean() ? item.booleanValue() : item.isNumber() ? item.decimalValue() : item.textValue());
        });
        return fields;
    }

    void write(Source source, Map<String, Object> fields, Object taskId) {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put(source.spec().stateField(), source.spec().readyValue());
        expected.put(source.spec().taskIdField(), taskId);
        crud.updateCommandFields(source.configKey(), source.recordId(), fields, expected);
    }
}
