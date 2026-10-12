package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.booleanWithDefault;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.copyOption;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.firstNonBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.firstPresent;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.intValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.integerValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.mapValue;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.putIfNotBlank;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeConfigValues.text;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.resolveEditGridCols;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.resolveGridBlockProps;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.findZone;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeMasterDetail.buildMasterDetailConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeMasterDetail.isMasterDetailRuntime;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.buildJoinConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.buildTreeConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.extractTreeConfigOverrides;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.isTreeRuntime;

/** Page presentation, workspace and action options. */
final class RuntimePageOptions {

    private RuntimePageOptions() {
    }

    static Map<String, Object> buildOptions(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, Object> options = new LinkedHashMap<>();
        boolean masterDetailRuntime = isMasterDetailRuntime(pageSchema);
        LowcodePageZone editZone = findZone(pageSchema, "edit");
        Map<String, Object> editProps = editZone == null || editZone.getProps() == null ? Map.of() : editZone.getProps();
        Map<String, Object> crudBlockProps = resolveGridBlockProps(pageSchema, List.of("AiCrudPage"));
        String formOpenMode = resolveFormOpenMode(firstNonBlank(
                editProps.get("formOpenMode"),
                crudBlockProps.get("formOpenMode"),
                editProps.get("modalType"),
                crudBlockProps.get("modalType")));
        options.put("formOpenMode", formOpenMode);
        options.put("modalType", resolveModalType(firstNonBlank(
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                formOpenMode)));
        options.put("tabWorkspace", buildTabWorkspaceOptions(editProps, crudBlockProps));
        int editGridCols = resolveEditGridCols(pageSchema);
        options.put("modalWidth", resolveRuntimeModalWidth(editProps, crudBlockProps,
                resolveDefaultModalWidth(masterDetailRuntime, editGridCols)));
        options.put("searchGridCols", integerValue(crudBlockProps.get("searchGridCols")) == null
                ? 4
                : integerValue(crudBlockProps.get("searchGridCols")));
        options.put("editGridCols", editGridCols);
        options.put("editLabelPlacement", StringUtils.defaultIfBlank(text(editProps.get("labelPlacement")),
                StringUtils.defaultIfBlank(text(crudBlockProps.get("editLabelPlacement")), "left")));
        options.put("editLabelAlign", StringUtils.defaultIfBlank(text(editProps.get("labelAlign")),
                StringUtils.defaultIfBlank(text(crudBlockProps.get("editLabelAlign")), "right")));
        options.put("editLabelWidth", editProps.getOrDefault("labelWidth",
                crudBlockProps.getOrDefault("editLabelWidth", "auto")));
        options.put("editSize", normalizeRuntimeFormSize(StringUtils.defaultIfBlank(text(editProps.get("size")),
                text(crudBlockProps.get("editSize")))));
        options.put("editShowFeedback", booleanWithDefault(firstPresent(editProps.get("showFeedback"),
                crudBlockProps.get("editShowFeedback")), true));
        copyOption(editProps, options, "editFormClass");
        copyOption(editProps, options, "editFormStyle");
        options.put("editXGap", intValue(editProps.get("columnGap"), 16));
        options.put("editYGap", intValue(editProps.get("rowGap"), 16));
        Object formLayout = editProps.get("formLayout");
        if (formLayout instanceof List<?> layout && !layout.isEmpty()) {
            options.put("editFormLayout", layout);
        }
        Object formDesignerSchema = editProps.get("formDesignerSchema");
        if (formDesignerSchema != null) {
            options.put("formDesignerSchema", formDesignerSchema);
        }

        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> tableProps = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            tableProps.putAll(tableZone.getProps());
        }
        tableProps.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        if (!tableProps.isEmpty()) {
            copyOption(tableProps, options, "showImport");
            copyOption(tableProps, options, "showExport");
            copyOption(tableProps, options, "showPagination");
            copyOption(tableProps, options, "hideAdd");
            copyOption(tableProps, options, "hideToolbar");
            copyOption(tableProps, options, "hideSelection");
            copyOption(tableProps, options, "hideBatchDelete");
            copyOption(tableProps, options, "enableCustomQuery");
            copyOption(tableProps, options, "showRenderModeSwitch");
            copyOption(tableProps, options, "renderMode");
            copyOption(tableProps, options, "tableSize");
            copyOption(tableProps, options, "bordered");
            copyOption(tableProps, options, "striped");
            copyOption(tableProps, options, "drawerPlacement");
            copyOption(tableProps, options, "tabWorkspace");
            options.put("tableRowGap", intValue(tableProps.get("rowGap"), 8));
        }
        formOpenMode = resolveFormOpenMode(firstNonBlank(
                editProps.get("formOpenMode"),
                crudBlockProps.get("formOpenMode"),
                tableProps.get("formOpenMode"),
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                tableProps.get("modalType"),
                options.get("formOpenMode")));
        options.put("formOpenMode", formOpenMode);
        options.put("modalType", resolveModalType(firstNonBlank(
                Set.of("modal", "drawer").contains(formOpenMode) ? formOpenMode : null,
                editProps.get("modalType"),
                crudBlockProps.get("modalType"),
                tableProps.get("modalType"),
                options.get("modalType"))));
        Set<String> toolbarActions = resolveToolbarStandardActions(pageSchema);
        if (!toolbarActions.isEmpty()) {
            options.put("hideAdd", !toolbarActions.contains("add"));
            options.put("showImport", toolbarActions.contains("import"));
            options.put("showExport", toolbarActions.contains("export"));
            options.put("hideBatchDelete", !toolbarActions.contains("batch-delete"));
            options.put("enableCustomQuery", toolbarActions.contains("custom-query"));
        }
        options.put("toolbarActions", resolveCustomActions(pageSchema, "toolbar"));
        options.put("rowActions", resolveCustomActions(pageSchema, "row"));
        options.put("detailActions", resolveCustomActions(pageSchema, "detail"));
        options.put("formActions", resolveCustomActions(pageSchema, "form"));
        options.put("defaultSort", buildDefaultSort(modelSchema, pageSchema));
        options.put("joinConfig", buildJoinConfig(modelSchema, pageSchema));
        if (masterDetailRuntime) {
            options.put("masterDetailConfig", buildMasterDetailConfig(modelSchema, pageSchema));
        }
        LowcodePageZone detailZone = findZone(pageSchema, "detail");
        Map<String, Object> detailProps = detailZone == null || detailZone.getProps() == null ? Map.of() : detailZone.getProps();
        Object quantityPanels = detailProps.get("quantityPanels");
        if (quantityPanels instanceof List<?> panels && !panels.isEmpty()) {
            options.put("detailPanels", panels);
        }
        if (isTreeRuntime(modelSchema, pageSchema)) {
            options.put("treeConfig", buildTreeConfig(modelSchema, pageSchema, extractTreeConfigOverrides(pageSchema)));
        }
        return options;
    }

    static String resolveDefaultModalWidth(boolean masterDetailRuntime, int editGridCols) {
        if (masterDetailRuntime) {
            return "1080px";
        }
        if (editGridCols >= 3) {
            return "1180px";
        }
        return editGridCols > 1 ? "1040px" : "800px";
    }

    static String resolveRuntimeModalWidth(Map<String, Object> editProps,
                                            Map<String, Object> crudBlockProps,
                                            String defaultWidth) {
        String editModalWidth = normalizeModalWidth(editProps.get("modalWidth"));
        if (StringUtils.isNotBlank(editModalWidth)) {
            return editModalWidth;
        }
        String crudModalWidth = normalizeModalWidth(crudBlockProps.get("modalWidth"));
        if (StringUtils.isNotBlank(crudModalWidth) && !isDefaultCrudModalWidth(crudModalWidth)) {
            return crudModalWidth;
        }
        String formStyleWidth = resolveFormStyleModalWidth(editProps.get("editFormStyle"));
        if (StringUtils.isNotBlank(formStyleWidth)) {
            return formStyleWidth;
        }
        return StringUtils.defaultIfBlank(crudModalWidth, defaultWidth);
    }

    static String resolveFormStyleModalWidth(Object style) {
        if (!(style instanceof Map<?, ?> styleMap)) {
            return null;
        }
        Object width = firstNonBlank(styleMap.get("maxWidth"), styleMap.get("width"));
        return normalizeModalWidth(width);
    }

    static String normalizeModalWidth(Object value) {
        String width = StringUtils.trimToNull(text(value));
        if (StringUtils.isBlank(width)) {
            return null;
        }
        String normalized = width.toLowerCase(Locale.ROOT);
        if ("auto".equals(normalized) || "100%".equals(normalized)) {
            return null;
        }
        if (width.matches("\\d+")) {
            return width + "px";
        }
        return width;
    }

    static boolean isDefaultCrudModalWidth(String width) {
        return "900px".equals(StringUtils.trimToEmpty(width));
    }

    static String resolveModalType(Object value) {
        String modalType = StringUtils.defaultIfBlank(text(value), "modal").toLowerCase(Locale.ROOT);
        return Set.of("modal", "drawer").contains(modalType) ? modalType : "modal";
    }

    static String resolveFormOpenMode(Object value) {
        String mode = StringUtils.defaultIfBlank(text(value), "modal");
        if ("tabworkspace".equalsIgnoreCase(mode)) {
            return "tabWorkspace";
        }
        String normalized = mode.toLowerCase(Locale.ROOT);
        return Set.of("modal", "drawer", "flat").contains(normalized) ? normalized : "modal";
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildTabWorkspaceOptions(Map<String, Object> editProps,
                                                          Map<String, Object> crudBlockProps) {
        Map<String, Object> source = new LinkedHashMap<>();
        Object editConfig = editProps.get("tabWorkspace");
        if (editConfig instanceof Map<?, ?> map) {
            source.putAll((Map<String, Object>) map);
        }
        Object crudConfig = crudBlockProps.get("tabWorkspace");
        if (crudConfig instanceof Map<?, ?> map) {
            source.putAll((Map<String, Object>) map);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxTabs", Math.max(1, intValue(source.get("maxTabs"), 8)));
        result.put("reuseRecordTab", booleanWithDefault(source.get("reuseRecordTab"), true));
        result.put("closeAfterSave", booleanWithDefault(source.get("closeAfterSave"), false));
        result.put("showDirtyMark", booleanWithDefault(source.get("showDirtyMark"), true));
        return result;
    }

    static Set<String> resolveToolbarStandardActions(LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getListGridLayout() == null) {
            return Set.of();
        }
        Object items = pageSchema.getListGridLayout().get("items");
        if (!(items instanceof List<?> list)) {
            return Set.of();
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> block) || !"toolbar".equals(text(block.get("blockType")))) {
                continue;
            }
            Object props = block.get("props");
            if (!(props instanceof Map<?, ?> propsMap)) {
                return Set.of();
            }
            Object actions = propsMap.get("actions");
            if (!(actions instanceof List<?> actionList)) {
                return Set.of();
            }
            return actionList.stream()
                    .map(RuntimeConfigValues::text)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }

    static Map<String, Object> buildDefaultSort(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> props = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            props.putAll(tableZone.getProps());
        }
        props.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        Object defaultSort = props.get("defaultSort");
        String sortField = text(props.get("defaultSortField"));
        String sortOrder = text(props.get("defaultSortOrder"));
        if (defaultSort instanceof Map<?, ?> defaultSortMap) {
            sortField = StringUtils.defaultIfBlank(sortField,
                    StringUtils.defaultIfBlank(text(defaultSortMap.get("orderByColumn")), text(defaultSortMap.get("field"))));
            sortOrder = StringUtils.defaultIfBlank(sortOrder,
                    StringUtils.defaultIfBlank(text(defaultSortMap.get("isAsc")), text(defaultSortMap.get("order"))));
        }

        Set<String> allowedFields = modelSchema == null || modelSchema.getFields() == null
                ? Set.of("id")
                : modelSchema.getFields().stream()
                .map(LowcodeFieldSchema::getField)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String orderByColumn = StringUtils.defaultIfBlank(sortField, "id");
        if (!allowedFields.contains(orderByColumn)) {
            orderByColumn = "id";
        }

        String isAsc = "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderByColumn", orderByColumn);
        result.put("isAsc", isAsc);
        return result;
    }

    static List<Map<String, Object>> buildRowActions(LowcodePageSchema pageSchema, boolean treeRuntime) {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(defaultAction("edit", "编辑", "primary"));
        actions.add(defaultAction("detail", "查看详情", "info"));
        if (treeRuntime) {
            actions.add(defaultAction("addChild", "添加下级", "success"));
        }
        actions.add(defaultAction("delete", "删除", "error"));
        List<Map<String, Object>> customActions = resolveCustomActions(pageSchema, "row");
        Set<String> existingKeys = actions.stream()
                .map(action -> text(action.get("key")))
                .collect(Collectors.toSet());
        customActions.stream()
                .filter(action -> !existingKeys.contains(text(action.get("key"))))
                .forEach(actions::add);
        return actions;
    }

    static Map<String, Object> defaultAction(String key, String label, String type) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("key", key);
        action.put("label", label);
        action.put("type", type);
        action.put("position", "row");
        return action;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> resolveCustomActions(LowcodePageSchema pageSchema, String position) {
        LowcodePageZone tableZone = findZone(pageSchema, "table");
        Map<String, Object> props = new LinkedHashMap<>();
        if (tableZone != null && tableZone.getProps() != null) {
            props.putAll(tableZone.getProps());
        }
        props.putAll(resolveGridBlockProps(pageSchema, List.of("data-table", "AiCrudPage", "AiTable")));
        Object value = props.get("customActions");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String actionPosition = StringUtils.defaultIfBlank(text(source.get("position")), "toolbar");
            if (!position.equals(actionPosition)) {
                continue;
            }
            String key = StringUtils.defaultIfBlank(text(source.get("key")), "custom_" + result.size());
            String label = StringUtils.defaultIfBlank(text(source.get("label")), "自定义按钮");
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("key", key);
            action.put("label", label);
            action.put("type", StringUtils.defaultIfBlank(text(source.get("type")), "default"));
            action.put("position", position);
            action.put("actionType", StringUtils.defaultIfBlank(text(source.get("actionType")), "route"));
            putIfNotBlank(action, "actionCode", text(source.get("actionCode")));
            putIfNotBlank(action, "suiteCode", text(source.get("suiteCode")));
            putIfNotBlank(action, "objectCode", text(source.get("objectCode")));
            putIfNotBlank(action, "businessObjectCode", text(source.get("businessObjectCode")));
            putIfNotBlank(action, "targetObjectCode", text(source.get("targetObjectCode")));
            putIfNotBlank(action, "targetEntityCode", text(source.get("targetEntityCode")));
            putIfNotBlank(action, "referenceObjectCode", text(source.get("referenceObjectCode")));
            Map<String, Object> actionConfig = mapValue(source.get("actionConfig"));
            if (!actionConfig.isEmpty()) {
                action.put("actionConfig", actionConfig);
            }
            putIfNotBlank(action, "routePath", text(source.get("routePath")));
            putIfNotBlank(action, "targetFormKey", text(source.get("targetFormKey")));
            putIfNotBlank(action, "openTarget", StringUtils.defaultIfBlank(text(source.get("openTarget")), "_self"));
            putIfNotBlank(action, "permissionCode", text(source.get("permissionCode")));
            putIfNotBlank(action, "permissionKey", StringUtils.firstNonBlank(
                    text(source.get("permissionKey")), text(source.get("permissionCode"))));
            putIfNotBlank(action, "permissionStrategy", text(source.get("permissionStrategy")));
            putIfNotBlank(action, "confirmText", text(source.get("confirmText")));
            putIfNotBlank(action, "displayCondition", text(source.get("displayCondition")));
            putIfNotBlank(action, "successMessage", text(source.get("successMessage")));
            putIfNotBlank(action, "failureMessage", text(source.get("failureMessage")));
            putIfNotBlank(action, "successBehavior", text(source.get("successBehavior")));
            List<Map<String, Object>> params = resolveActionParams(source.get("params"));
            if (!params.isEmpty()) {
                action.put("params", params);
            }
            result.add(action);
        }
        return result;
    }

    static List<Map<String, Object>> resolveActionParams(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> source)) {
                continue;
            }
            String name = text(source.get("name"));
            if (StringUtils.isBlank(name)) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", name);
            putIfNotBlank(param, "target", text(source.get("target")));
            putIfNotBlank(param, "sourceType", text(source.get("sourceType")));
            putIfNotBlank(param, "sourceField", text(source.get("sourceField")));
            param.put("value", StringUtils.defaultString(text(source.get("value"))));
            result.add(param);
        }
        return result;
    }

    static String normalizeRuntimeFormSize(String value) {
        String size = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        if ("default".equals(size) || "medium".equals(size)) {
            return "medium";
        }
        return Set.of("small", "large").contains(size) ? size : "medium";
    }
}
