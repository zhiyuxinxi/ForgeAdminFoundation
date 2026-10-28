package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskSourceHandler.Operation;
import com.mdframe.forge.task.api.TaskState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

/** User-facing task metadata comes exclusively from the task's immutable published binding. */
@Service
@RequiredArgsConstructor
public class LowcodeTaskPresentation {
    private final TaskBindingSnapshots snapshots;
    private final TaskBindingRuntime runtime;
    private final BusinessTaskEngine engine;

    public record Action(String bindingKey, String title, String taskIdField) { }
    public record Form(List<Map<String, Object>> fields, Map<String, Object> values) { }

    public List<Action> actions(String configKey) {
        var version = snapshots.latest(configKey);
        require(SessionHelper.hasPermission("ai:business:" + version.getObjectCode() + ":query"));
        return snapshots.bindings(version).stream().map(binding -> {
            var definition = engine.definition(binding.spec().definitionId());
            require(LowcodeTaskSource.SOURCE_TYPE.equals(definition.sourceType()));
            return new Action(binding.key(), definition.title(), binding.spec().taskIdField());
        }).toList();
    }

    public Form form(long taskId) {
        var task = engine.get(taskId);
        require(LowcodeTaskSource.SOURCE_TYPE.equals(task.sourceType()));
        var source = runtime.read(task.sourceKey(), Operation.READ);
        require(source.spec().definitionId() == task.definitionId()
                && String.valueOf(task.id()).equals(String.valueOf(source.record().get(source.spec().taskIdField()))));
        JsonNode model = snapshots.tree(source.version().getModelSchema());
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> values = new LinkedHashMap<>();
        JsonNode result = snapshots.tree(task.state() == TaskState.COMPLETED ? task.resultJson() : null);
        source.spec().resultFields().forEach((key, name) -> {
            JsonNode field = find(model, name);
            fields.add(project(key, field));
            if (result.has(key)) {
                JsonNode value = result.get(key);
                values.put(key, value.isBoolean() ? value.booleanValue()
                        : value.isNumber() ? value.decimalValue() : value.asText());
            }
        });
        return new Form(fields, values);
    }

    private JsonNode find(JsonNode model, String name) {
        for (JsonNode field : model.path("fields")) {
            if (name.equals(field.path("field").asText())) { return field; }
        }
        throw TaskBindingSpec.invalid();
    }

    private Map<String, Object> project(String key, JsonNode field) {
        String type = component(field);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", key);
        item.put("label", field.path("label").asText(field.path("field").asText()));
        item.put("type", type);
        item.put("required", true);
        Map<String, Object> props = new LinkedHashMap<>();
        if (field.path("length").canConvertToInt() && field.path("length").intValue() > 0) {
            props.put("maxlength", field.path("length").intValue());
        }
        if ("textarea".equals(type)) { props.put("type", "textarea"); }
        if ("date".equals(type)) { props.put("valueFormat", "yyyy-MM-dd"); }
        if ("datetime".equals(type)) { props.put("valueFormat", "yyyy-MM-dd'T'HH:mm:ss"); }
        if ("dictSelect".equals(type)) {
            require(!field.path("dictType").asText().isBlank());
            props.put("dictType", field.path("dictType").asText());
            item.put("dictType", field.path("dictType").asText());
        }
        if (Set.of("select", "radio").contains(type)) {
            JsonNode options = field.path("basicProps").path("options");
            require(options.isArray() && !options.isEmpty() && options.size() <= 500);
            List<Map<String, Object>> choices = new ArrayList<>();
            for (JsonNode option : options) {
                JsonNode value = option.path("value");
                require(value.isTextual() || value.isNumber() || value.isBoolean());
                choices.add(Map.of("label", option.path("label").asText(value.asText()),
                        "value", value.isNumber() ? value.decimalValue()
                                : value.isBoolean() ? value.booleanValue() : value.textValue()));
            }
            item.put("options", choices);
        }
        item.put("props", props);
        return item;
    }

    private String component(JsonNode field) {
        String component = field.path("componentType").asText();
        if (field.path("basicProps").path("multiple").asBoolean()) { throw unsupported(); }
        if (!field.path("dictType").asText().isBlank()) { return "dictSelect"; }
        if (Set.of("input", "textarea", "number", "switch", "date", "datetime", "select", "radio").contains(component)) {
            return component;
        }
        if (!component.isBlank()) { throw unsupported(); }
        String dataType = field.path("dataType").asText().toLowerCase(Locale.ROOT);
        return switch (dataType) {
            case "varchar", "char", "text", "longtext", "mediumtext" -> "text".equals(dataType) ? "textarea" : "input";
            case "int", "integer", "smallint", "tinyint", "decimal", "double", "float", "numeric" -> "number";
            case "bigint" -> "input";
            case "boolean", "bool", "bit" -> "switch";
            case "date" -> "date";
            case "datetime", "timestamp" -> "datetime";
            default -> throw unsupported();
        };
    }

    private BusinessException unsupported() {
        return new BusinessException(409, UiMessages.label(
                "任务结果包含当前待办表单不支持的字段，请使用该业务的执行入口", "This task requires a result field supported by its business execution page"));
    }
}
