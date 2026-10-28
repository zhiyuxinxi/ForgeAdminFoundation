package com.mdframe.forge.lowcode.runtime.service.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;

import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/** Normal form/import writes cannot create, replace or clear an engine-owned task instance link. */
public final class RuntimeTaskLinkFields {
    private RuntimeTaskLinkFields() { }

    public static boolean hasBindings(LowcodeRuntimeConfig config, ObjectMapper json) {
        if (config.getOptions() == null) { return false; }
        try {
            JsonNode options = json.readTree(config.getOptions());
            return options != null && !fields(options).isEmpty();
        } catch (JsonProcessingException exception) { throw denied(); }
    }

    public static void validate(LowcodeRuntimeConfig config, Map<String, Object> payload,
                                Map<String, Object> previous, ObjectMapper json) {
        if (config.getOptions() == null || payload == null) { return; }
        try {
            JsonNode options = json.readTree(config.getOptions());
            JsonNode model = json.readTree(config.getModelSchema() == null ? "{}" : config.getModelSchema());
            for (String field : fields(options)) {
                Set<String> aliases = aliases(field, model);
                Object old = previous(previous, aliases);
                for (String alias : aliases) {
                    check(payload, alias, old);
                    if (payload.get("main") instanceof Map<?, ?> main) { check(main, alias, old); }
                }
            }
        } catch (JsonProcessingException exception) { throw denied(); }
    }

    private static Set<String> aliases(String field, JsonNode model) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(field);
        aliases.add(field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT));
        for (JsonNode property : model.path("fields")) {
            if (field.equals(property.path("field").asText()) && !property.path("columnName").asText().isBlank()) {
                aliases.add(property.path("columnName").asText());
                aliases.add(DynamicQueryGenerator.snakeToCamel(property.path("columnName").asText()));
            }
        }
        return aliases;
    }

    private static Object previous(Map<String, Object> previous, Set<String> aliases) {
        Map<?, ?> main = previous != null && previous.get("main") instanceof Map<?, ?> nested ? nested : previous;
        if (main != null) {
            for (String alias : aliases) {
                if (main.containsKey(alias)) { return main.get(alias); }
            }
        }
        return null;
    }

    private static Set<String> fields(JsonNode options) {
        Set<String> names = new LinkedHashSet<>();
        for (String property : new String[]{"publicTaskBindings", "publicTaskLinkFields"}) {
            JsonNode array = options.get(property);
            if (array == null) { continue; }
            if (!array.isArray() || array.size() > 256) { throw denied(); }
            for (JsonNode item : array) {
                String name = "publicTaskBindings".equals(property) ? item.path("spec").path("taskIdField").asText() : item.asText();
                if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) { throw denied(); }
                names.add(name);
            }
        }
        return names;
    }

    private static void check(Map<?, ?> data, String field, Object previous) {
        if (data.containsKey(field) && !Objects.equals(text(data.get(field)), text(previous))) { throw denied(); }
    }

    private static String text(Object value) { return value == null ? null : String.valueOf(value); }

    private static BusinessException denied() {
        return new BusinessException(409, UiMessages.label("任务实例引用只能由公共任务引擎维护",
                "Task instance references can only be maintained by the public task engine"));
    }
}
