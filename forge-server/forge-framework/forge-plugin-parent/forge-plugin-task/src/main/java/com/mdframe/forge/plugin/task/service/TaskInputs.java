package com.mdframe.forge.plugin.task.service;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.task.api.TaskSourceHandler.Actor;

final class TaskInputs {
    private TaskInputs() { }

    static Actor actor() {
        LoginUser user = SessionHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getUserId() <= 0
                || user.getTenantId() == null || user.getTenantId() <= 0) {
            throw TaskMessages.error(401, "identity.required");
        }
        if (!user.getTenantId().equals(TenantContextHolder.getTenantId())) {
            throw TaskMessages.error(403, "identity.mismatch");
        }
        String datasource = DynamicDataSourceContextHolder.peek();
        if (datasource != null && !datasource.isBlank() && !"master".equals(datasource)) {
            throw TaskMessages.error(409, "datasource.unsupported");
        }
        return new Actor(user.getTenantId(), user.getUserId());
    }

    static void permission(String permission) {
        if (!SessionHelper.hasPermission(permission)) {
            throw TaskMessages.error(403, "permission.denied");
        }
    }

    static String text(String value, int max) {
        if (value == null || value.isBlank() || value.length() > max
                || !value.equals(value.strip()) || value.chars().anyMatch(Character::isISOControl)) {
            throw TaskMessages.error(400, "input.invalid");
        }
        return value;
    }

    static String code(String value) {
        text(value, 64);
        if (!value.matches("[a-z][a-z0-9_.-]{0,63}")) {
            throw TaskMessages.error(400, "input.invalid");
        }
        return value;
    }

    static void positive(long id) {
        if (id <= 0) {
            throw TaskMessages.error(400, "input.invalid");
        }
    }

    static void page(Long after, int limit) {
        if (limit < 1 || limit > 200 || (after != null && after <= 0)) {
            throw TaskMessages.error(400, "input.invalid");
        }
    }

    static JsonNode result(ObjectMapper json, String value) {
        if (value == null || value.length() > 16384) {
            throw TaskMessages.error(400, "input.invalid");
        }
        try {
            JsonNode parsed = json.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                    .readTree(value);
            if (parsed == null || !parsed.isObject()) {
                throw TaskMessages.error(400, "input.invalid");
            }
            return parsed;
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(400, "input.invalid");
        }
    }
}
