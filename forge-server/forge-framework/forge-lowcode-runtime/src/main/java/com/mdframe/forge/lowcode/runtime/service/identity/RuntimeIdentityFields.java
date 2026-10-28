package com.mdframe.forge.lowcode.runtime.service.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads identity-reference semantics from the trusted published model and edit schema. */
@RequiredArgsConstructor
public final class RuntimeIdentityFields {
    private static final Set<String> USERS = Set.of("userselect", "userpicker", "user", "username", "sysuserselect", "forgeuserselect");
    private static final Set<String> ORGS = Set.of("orgtreeselect", "orgselect", "organizationselect", "departmentselect",
            "departmenttreeselect", "deptselect", "depttreeselect", "eltreeselect", "orgname", "deptname", "forgeorgtreeselect");
    private static final Set<String> POSTS = Set.of("postselect");
    private final ObjectMapper json;

    public enum Kind { ORGANIZATION, MEMBER, POST }

    public record Field(LowcodeFieldSchema model, Kind kind, boolean writable) {
        public boolean user() { return kind == Kind.MEMBER; }
        public boolean post() { return kind == Kind.POST; }
        public String name() { return model.getField(); }
        public String column() { return model.getColumnName(); }
    }

    public List<Field> read(LowcodeRuntimeConfig config) {
        if (config == null || StringUtils.isBlank(config.getModelSchema())) {
            return List.of();
        }
        try {
            LowcodeModelSchema model = json.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            JsonNode edit = StringUtils.isBlank(config.getEditSchema()) ? null : json.readTree(config.getEditSchema());
            List<Field> result = new ArrayList<>();
            for (LowcodeFieldSchema field : model.getFields() == null ? List.<LowcodeFieldSchema>of() : model.getFields()) {
                if (field == null || StringUtils.isBlank(field.getField())) {
                    continue;
                }
                JsonNode node = find(edit, field.getField());
                String component = node != null && node.hasNonNull("type") ? node.get("type").asText() : field.getComponentType();
                Kind kind = kind(component);
                if (kind == null || external(field, node)) {
                    continue;
                }
                result.add(new Field(field, kind, writable(field, node)));
            }
            return result;
        } catch (Exception error) {
            throw new BusinessException(400, UiMessages.label("组织/人员/岗位字段配置无效", "Invalid organization/member/post field configuration"));
        }
    }

    private Kind kind(String component) {
        String normalized = StringUtils.defaultString(component).replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        if (USERS.contains(normalized)) {
            return Kind.MEMBER;
        }
        if (POSTS.contains(normalized)) {
            return Kind.POST;
        }
        return ORGS.contains(normalized) ? Kind.ORGANIZATION : null;
    }

    private boolean writable(LowcodeFieldSchema field, JsonNode node) {
        if (Boolean.TRUE.equals(field.getReadonly()) || Boolean.TRUE.equals(field.getSystemField())
                || Boolean.TRUE.equals(field.getPrimaryKey()) || Boolean.TRUE.equals(field.getAutoIncrement())
                || Boolean.FALSE.equals(field.getFormVisible())
                || Set.of("DISABLED", "HIDDEN").contains(StringUtils.defaultString(field.getFieldStatus()).toUpperCase(Locale.ROOT))) {
            return false;
        }
        return node == null || (!node.path("readonly").asBoolean() && !node.path("disabled").asBoolean()
                && !node.path("props").path("readonly").asBoolean() && !node.path("props").path("disabled").asBoolean());
    }

    public Field require(LowcodeRuntimeConfig config, String name) {
        return read(config).stream().filter(field -> field.name().equals(name) && field.writable()).findFirst()
                .orElseThrow(() -> new BusinessException(403, UiMessages.label(
                        "该字段不允许选择内部组织、人员或岗位", "This field does not allow internal organization, member or post selection")));
    }

    public boolean hasPayload(LowcodeRuntimeConfig config, Map<String, Object> data) {
        Map<?, ?> values = main(data);
        return read(config).stream().anyMatch(field -> contains(values, field));
    }

    public static Map<?, ?> main(Map<String, Object> data) {
        if (data == null) {
            return Map.of();
        }
        return data.get("main") instanceof Map<?, ?> nested ? nested : data;
    }

    public static boolean contains(Map<?, ?> values, Field field) {
        return values.containsKey(field.name()) || (field.column() != null && values.containsKey(field.column()));
    }

    public static Object value(Map<?, ?> values, Field field) {
        return values.containsKey(field.name()) ? values.get(field.name()) : values.get(field.column());
    }

    private boolean external(LowcodeFieldSchema field, JsonNode edit) {
        return externalSource(json.valueToTree(field.getBasicProps()))
                || externalSource(json.valueToTree(field.getAdvancedProps())) || externalSource(edit);
    }

    private boolean externalSource(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        String owner = node.path("referenceSource").asText("");
        if (!owner.isBlank() && !"SYSTEM".equalsIgnoreCase(owner)) {
            return true;
        }
        if (node.path("options").isArray() && !node.path("options").isEmpty()) {
            return true;
        }
        JsonNode source = node.get("optionSource");
        if (source != null && !source.isNull()) {
            String api = source.isTextual() ? source.asText() : source.path("api").asText(source.path("url").asText(""));
            if (!api.isBlank() && !api.endsWith("/system/org/tree") && !api.endsWith("/system/user/page")) {
                return true;
            }
            if (source.path("options").isArray() || source.path("data").isArray()) {
                return true;
            }
        }
        return externalSource(node.get("props"));
    }

    private JsonNode find(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        if (node.isObject() && field.equals(node.path("field").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = find(child, field);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
