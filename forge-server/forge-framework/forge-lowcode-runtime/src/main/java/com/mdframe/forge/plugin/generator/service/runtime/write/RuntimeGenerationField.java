package com.mdframe.forge.plugin.generator.service.runtime.write;

import org.apache.commons.lang3.StringUtils;
import java.util.*;

/** Immutable numbering plan for one runtime field. */
record RuntimeGenerationField(String fieldName, String columnName, String label,
                              String ruleCode, String trigger, String fillPolicy, boolean enabled) {
    static RuntimeGenerationField explicit(String fieldName,
                                                         String columnName,
                                                         String label,
                                                         Map<String, Object> generation) {
        if (generation == null || generation.isEmpty() || !readBoolean(generation.get("enabled"), false)) {
            return null;
        }
        String ruleCode = StringUtils.firstNonBlank(
                text(generation.get("ruleCode")),
                text(generation.get("codeRuleCode")),
                text(generation.get("generatorCode"))
        );
        if (StringUtils.isBlank(ruleCode)) {
            return null;
        }
        String mode = StringUtils.defaultIfBlank(
                StringUtils.firstNonBlank(text(generation.get("mode")), text(generation.get("type"))),
                "CODE_RULE");
        if (!"CODE_RULE".equalsIgnoreCase(mode) && !"AUTO_CODE".equalsIgnoreCase(mode)) {
            return null;
        }
        return new RuntimeGenerationField(
                StringUtils.trimToNull(fieldName),
                StringUtils.trimToNull(columnName),
                StringUtils.trimToNull(label),
                ruleCode.trim(),
                StringUtils.defaultIfBlank(text(generation.get("trigger")), "ON_CREATE").trim(),
                StringUtils.defaultIfBlank(text(generation.get("fillPolicy")), "EMPTY_ONLY").trim(),
                true
        );
    }

    private static boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String textValue = String.valueOf(value).trim();
        if (StringUtils.isBlank(textValue)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(textValue)
                || "1".equals(textValue)
                || "yes".equalsIgnoreCase(textValue)
                || "Y".equalsIgnoreCase(textValue);
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
