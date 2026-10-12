package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.copyRuleList;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.intValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.isRequiredSwitchEnabled;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findZone;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldComponents.normalizeAlign;

/** Zone, grid, canvas and form-create settings in their original precedence. */
final class RuntimeDesignerSettings {

    private RuntimeDesignerSettings() {
    }

    static int resolveEditGridCols(LowcodePageSchema pageSchema) {
        int cols = 1;
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone != null && editZone.getProps() != null) {
            Integer configuredCols = integerValue(editZone.getProps().get("editGridCols"));
            if (configuredCols != null && configuredCols > 0) {
                return Math.max(1, Math.min(3, configuredCols));
            }
            cols = Math.max(cols, resolveCanvasGridCols(editZone));
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            Object col = rule.get("col");
            if (!(col instanceof Map<?, ?> colMap)) {
                continue;
            }
            Integer span = integerValue(colMap.get("span"));
            if (span == null || span <= 0 || span >= 24) {
                continue;
            }
            cols = Math.max(cols, Math.min(3, Math.max(1, (int) Math.ceil(24.0 / span))));
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    static int resolveCanvasGridCols(LowcodePageZone editZone) {
        List<Map<String, Object>> items = extractCanvasItems(editZone);
        if (items.isEmpty()) {
            return 1;
        }
        List<Integer> columns = new ArrayList<>();
        items.stream()
                .filter(item -> StringUtils.isNotBlank(text(item.get("fieldRef"))))
                .sorted(Comparator.comparingInt(item -> intValue(item.get("x"), 0)))
                .forEach(item -> {
                    int x = intValue(item.get("x"), 0);
                    boolean exists = columns.stream().anyMatch(columnX -> Math.abs(columnX - x) < 80);
                    if (!exists) {
                        columns.add(x);
                    }
                });
        return Math.max(1, Math.min(3, columns.isEmpty() ? 1 : columns.size()));
    }

    static List<LowcodeFieldSchema> sortByCanvasOrder(List<LowcodeFieldSchema> fields,
                                                       LowcodePageSchema pageSchema,
                                                       String zoneKey) {
        if (fields == null || fields.size() <= 1) {
            return fields == null ? List.of() : fields;
        }
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone != null && zone.getProps() != null
                && "formDesignerSchema".equals(text(zone.getProps().get("compiledFrom")))) {
            return fields;
        }
        List<Map<String, Object>> items = extractCanvasItems(zone);
        if (items.isEmpty()) {
            return fields;
        }
        Map<String, LowcodeFieldSchema> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        LowcodeFieldSchema::getField,
                        field -> field,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<LowcodeFieldSchema> ordered = new ArrayList<>();
        items.stream()
                .sorted(RuntimeDesignerSettings::compareCanvasItemPosition)
                .map(item -> text(item.get("fieldRef")))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(fieldRef -> {
                    LowcodeFieldSchema field = fieldMap.remove(fieldRef);
                    if (field != null) {
                        ordered.add(field);
                    }
                });
        ordered.addAll(fieldMap.values());
        return ordered;
    }

    static int compareCanvasItemPosition(Map<String, Object> left, Map<String, Object> right) {
        int leftRow = Math.round(intValue(left.get("y"), 0) / 16.0f);
        int rightRow = Math.round(intValue(right.get("y"), 0) / 16.0f);
        if (leftRow != rightRow) {
            return Integer.compare(leftRow, rightRow);
        }
        int xCompare = Integer.compare(intValue(left.get("x"), 0), intValue(right.get("x"), 0));
        if (xCompare != 0) {
            return xCompare;
        }
        return Integer.compare(intValue(left.get("zIndex"), 0), intValue(right.get("zIndex"), 0));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> resolveFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        LowcodePageZone zone = findZone(pageSchema, zoneKey);
        if (zone == null || zone.getProps() == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        Object settings = zone.getProps().get("fieldSettings");
        if (!(settings instanceof Map<?, ?> settingsMap)) {
            return Map.of();
        }
        Object value = settingsMap.get(fieldName);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    static Map<String, Object> resolveRuntimeFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        Map<String, Object> result = new LinkedHashMap<>(resolveFieldSetting(pageSchema, zoneKey, fieldName));
        Map<String, Object> gridSetting = resolveGridFieldSetting(pageSchema, zoneKey, fieldName);
        result.putAll(gridSetting);
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> resolveGridFieldSetting(LowcodePageSchema pageSchema, String zoneKey, String fieldName) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Map.of();
        }
        for (String blockType : runtimeSettingBlockTypes(zoneKey)) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> block)) {
                    continue;
                }
                if (!blockType.equals(text(block.get("blockType")))) {
                    continue;
                }
                Object propsValue = block.get("props");
                if (!(propsValue instanceof Map<?, ?> props)) {
                    continue;
                }
                String globalAlign = "table".equals(zoneKey) ? normalizeAlign(text(props.get("globalAlign"))) : null;
                Object settingsValue = props.get("fieldSettings");
                if (!(settingsValue instanceof Map<?, ?> settings)) {
                    if (StringUtils.isNotBlank(globalAlign)) {
                        return Map.of("align", globalAlign);
                    }
                    continue;
                }
                Object value = settings.get(fieldName);
                if (value instanceof Map<?, ?> map) {
                    Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
                    if (StringUtils.isNotBlank(globalAlign)
                            && StringUtils.isBlank(normalizeAlign(StringUtils.defaultIfBlank(
                            text(result.get("align")), text(result.get("textAlign")))))) {
                        result.put("align", globalAlign);
                    }
                    return result;
                }
                if (StringUtils.isNotBlank(globalAlign)) {
                    return Map.of("align", globalAlign);
                }
            }
        }
        return Map.of();
    }

    static List<String> runtimeSettingBlockTypes(String zoneKey) {
        if ("search".equals(zoneKey)) {
            return List.of("search-form", "AiCrudPage");
        }
        if ("table".equals(zoneKey)) {
            return List.of("data-table", "AiCrudPage", "AiTable");
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> resolveGridBlockProps(LowcodePageSchema pageSchema, List<String> blockTypes) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null || blockTypes == null || blockTypes.isEmpty()) {
            return Map.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Map.of();
        }
        for (String blockType : blockTypes) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> block)) {
                    continue;
                }
                if (!blockType.equals(text(block.get("blockType")))) {
                    continue;
                }
                Object props = block.get("props");
                if (props instanceof Map<?, ?> map) {
                    return new LinkedHashMap<>((Map<String, Object>) map);
                }
            }
        }
        return Map.of();
    }

    static Map<String, Object> resolveEditFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        Map<String, Object> setting = new LinkedHashMap<>(resolveFieldSetting(pageSchema, "edit", fieldName));
        Map<String, Object> designerSetting = resolveFormRuleSetting(pageSchema, fieldName);
        setting.putAll(designerSetting);
        Map<String, Object> canvasSetting = resolveCanvasFieldSetting(pageSchema, fieldName);
        setting.putAll(canvasSetting);
        return setting;
    }

    static Map<String, Object> resolveCanvasFieldSetting(LowcodePageSchema pageSchema, String fieldName) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        int gridCols = Math.max(1, resolveEditGridCols(pageSchema));
        int canvasWidth = 1040;
        if (editZone.getProps() != null) {
            Object canvas = editZone.getProps().get("canvas");
            if (canvas instanceof Map<?, ?> canvasMap) {
                canvasWidth = intValue(canvasMap.get("width"), canvasWidth);
            }
        }
        int colWidth = Math.max(1, (canvasWidth - 64) / gridCols);
        for (Map<String, Object> item : extractCanvasItems(editZone)) {
            if (!fieldName.equals(text(item.get("fieldRef")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            int itemWidth = intValue(item.get("w"), 280);
            int span = Math.max(1, Math.min(gridCols, Math.round((float) itemWidth / colWidth)));
            setting.put("span", span);
            Object style = item.get("style");
            if (style instanceof Map<?, ?> styleMap && styleMap.get("labelWidth") != null) {
                setting.put("labelWidth", styleMap.get("labelWidth"));
            }
            return setting;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> resolveFormRuleSetting(LowcodePageSchema pageSchema, String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return Map.of();
        }
        for (Map<String, Object> rule : extractFormRules(pageSchema)) {
            if (!fieldName.equals(text(rule.get("field")))) {
                continue;
            }
            Map<String, Object> setting = new LinkedHashMap<>();
            Object props = rule.get("props");
            if (props instanceof Map<?, ?> propsMap) {
                setting.put("props", new LinkedHashMap<>((Map<String, Object>) propsMap));
            }
            String componentType = extractForgeComponentType(rule);
            if (StringUtils.isNotBlank(componentType)) {
                setting.put("componentType", componentType);
            }
            String dictType = text(getNestedValue(rule, "props.dictType"));
            if (StringUtils.isNotBlank(dictType)) {
                setting.put("dictType", dictType);
            }
            Object requiredSwitch = rule.get("$required");
            List<Map<String, Object>> validationRules = copyRuleList(rule.get("validate"));
            boolean requiredFromSwitch = isRequiredSwitchEnabled(requiredSwitch);
            boolean required = requiredFromSwitch
                    || validationRules.stream().anyMatch(item -> booleanWithDefault(item.get("required"), false));
            if (required) {
                setting.put("required", true);
                String requiredMessage = requiredFromSwitch && requiredSwitch instanceof String message && StringUtils.isNotBlank(message)
                        ? message
                        : validationRules.stream()
                        .filter(item -> booleanWithDefault(item.get("required"), false))
                        .map(item -> text(item.get("message")))
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse("");
                if (StringUtils.isNotBlank(requiredMessage)) {
                    setting.put("requiredMessage", requiredMessage);
                }
                if (requiredFromSwitch && validationRules.stream().noneMatch(item -> booleanWithDefault(item.get("required"), false))) {
                    Map<String, Object> requiredRule = new LinkedHashMap<>();
                    requiredRule.put("required", true);
                    requiredRule.put("message", StringUtils.defaultIfBlank(requiredMessage, "该字段为必填项"));
                    requiredRule.put("trigger", List.of("blur", "change"));
                    validationRules.add(0, requiredRule);
                }
            } else if (requiredSwitch != null) {
                setting.put("required", false);
                validationRules.removeIf(item -> booleanWithDefault(item.get("required"), false));
            }
            if (!validationRules.isEmpty()) {
                setting.put("rules", validationRules);
            }
            Object style = rule.get("style");
            if (style != null) {
                setting.put("componentStyle", style);
            }
            Object className = firstPresent(rule.get("className"), rule.get("class"));
            if (className != null) {
                setting.put("formItemClass", className);
            }
            Object forgeLayout = getNestedValue(rule, "_forge.layout");
            if (forgeLayout instanceof Map<?, ?> layoutMap) {
                Object align = layoutMap.get("align");
                if (align != null) {
                    setting.put("align", align);
                }
                Object forgeLabelWidth = layoutMap.get("labelWidth");
                if (forgeLabelWidth != null) {
                    setting.put("labelWidth", forgeLabelWidth);
                }
            }
            Object col = rule.get("col");
            if (col instanceof Map<?, ?> colMap) {
                int gridCols = resolveEditGridCols(pageSchema);
                Integer span = integerValue(colMap.get("span"));
                if (span != null && span > 0) {
                    int gridSpan = (int) Math.ceil(gridCols * Math.min(24, span) / 24.0);
                    setting.put("span", Math.max(1, Math.min(gridCols, gridSpan)));
                }
                Object gridStyle = colMap.get("style");
                if (gridStyle != null) {
                    setting.put("gridStyle", gridStyle);
                }
            }
            Object labelWidth = rule.get("labelWidth");
            if (labelWidth != null) {
                setting.put("labelWidth", labelWidth);
            }
            return setting;
        }
        return Map.of();
    }

    static String extractForgeComponentType(Map<String, Object> rule) {
        String componentKey = text(getNestedValue(rule, "_forge.componentKey"));
        if (StringUtils.isNotBlank(componentKey)) {
            return componentKey;
        }
        String dragTag = text(rule.get("_fc_drag_tag"));
        if (StringUtils.isBlank(dragTag)) {
            return null;
        }
        return switch (dragTag) {
            case "forgeDictSelect" -> "dictSelect";
            case "forgeRegionTreeSelect" -> "regionTreeSelect";
            case "forgeOrgTreeSelect" -> "orgTreeSelect";
            case "forgeUserSelect" -> "userSelect";
            case "forgeFileUpload" -> "fileUpload";
            case "forgeImageUpload" -> "imageUpload";
            case "forgeObjectReference" -> "objectReference";
            case "forgeRecordSelector" -> "recordSelector";
            case "forgeSubTable" -> "subTable";
            default -> null;
        };
    }

    static Object getNestedValue(Map<String, Object> source, String path) {
        if (source == null || StringUtils.isBlank(path)) {
            return null;
        }
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> extractFormRules(LowcodePageSchema pageSchema) {
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        if (editZone == null || editZone.getProps() == null) {
            return List.of();
        }
        Object rules = editZone.getProps().get("formCreateRule");
        if (!(rules instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        collectFormRules(list, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> extractCanvasItems(LowcodePageZone zone) {
        if (zone == null || zone.getProps() == null) {
            return List.of();
        }
        Object canvas = zone.getProps().get("canvas");
        if (!(canvas instanceof Map<?, ?> canvasMap)) {
            return List.of();
        }
        Object items = canvasMap.get("items");
        if (!(items instanceof List<?> itemList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : itemList) {
            if (item instanceof Map<?, ?> itemMap) {
                result.add((Map<String, Object>) itemMap);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static void collectFormRules(List<?> rules, List<Map<String, Object>> result) {
        for (Object item : rules) {
            if (!(item instanceof Map<?, ?> rule)) {
                continue;
            }
            Map<String, Object> typedRule = (Map<String, Object>) rule;
            result.add(typedRule);
            Object children = typedRule.get("children");
            if (children instanceof List<?> childRules) {
                collectFormRules(childRules, result);
            }
        }
    }
}
