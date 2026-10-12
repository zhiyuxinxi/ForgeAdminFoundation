package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.resolveEditFieldSetting;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.resolveRuntimeFieldSetting;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeDesignerSettings.sortByCanvasOrder;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.buildDesensitizeConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.buildDictConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.buildEncryptConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldCatalog.resolveFields;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildEditField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildSearchField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.buildTableColumn;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeFieldSchemas.isEditFieldVisibleAtDesignTime;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeMasterDetail.buildChildFieldRefs;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeMasterDetail.isMasterDetailRuntime;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageOptions.buildOptions;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimePageOptions.buildRowActions;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeRelations.buildTransConfig;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.appendTreeRuntimeField;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.decorateTreeRuntimeFields;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.isEmbeddedTreeTableRuntime;
import static com.mdframe.forge.plugin.generator.service.lowcode.RuntimeTreeConfig.isTreeRuntime;

/** Converts lowcode protocols to runtime consumer configuration. */
@Service
@RequiredArgsConstructor
public class LowcodeRuntimeConfigBuilder {

    private final ObjectMapper objectMapper;
    private final LowcodeSchemaValidator schemaValidator;
    private final LowcodePolicyService policyService;

    public LowcodeRuntimeConfig buildRuntimeConfig(String configKey,
                                                   LowcodeModelSchema modelSchema,
                                                   LowcodePageSchema pageSchema) {
        if (StringUtils.isBlank(configKey)) {
            throw new BusinessException("configKey不能为空");
        }
        policyService.normalizeModelSchema(modelSchema);
        schemaValidator.validatePage(pageSchema, modelSchema);

        LowcodeRuntimeConfig runtimeConfig = new LowcodeRuntimeConfig();
        runtimeConfig.setConfigKey(configKey);
        runtimeConfig.setObjectCode(StringUtils.firstNonBlank(
                modelSchema.getObject() == null ? null : modelSchema.getObject().getCode(),
                configKey,
                modelSchema.getTableName()));
        runtimeConfig.setTableName(modelSchema.getTableName());
        runtimeConfig.setTableComment(modelSchema.getBusinessName());
        runtimeConfig.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));

        try {
            runtimeConfig.setSearchSchema(objectMapper.writeValueAsString(buildSearchSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setColumnsSchema(objectMapper.writeValueAsString(buildColumnsSchema(modelSchema, pageSchema)));
            runtimeConfig.setEditSchema(objectMapper.writeValueAsString(buildEditSchema(configKey, modelSchema, pageSchema)));
            runtimeConfig.setApiConfig(objectMapper.writeValueAsString(buildApiConfig(configKey, modelSchema, pageSchema)));
            runtimeConfig.setOptions(objectMapper.writeValueAsString(buildOptions(modelSchema, pageSchema)));
            runtimeConfig.setDictConfig(objectMapper.writeValueAsString(buildDictConfig(modelSchema)));
            runtimeConfig.setDesensitizeConfig(objectMapper.writeValueAsString(buildDesensitizeConfig(modelSchema)));
            runtimeConfig.setEncryptConfig(objectMapper.writeValueAsString(buildEncryptConfig(modelSchema)));
            runtimeConfig.setTransConfig(objectMapper.writeValueAsString(buildTransConfig(modelSchema, pageSchema)));
            return runtimeConfig;
        } catch (Exception e) {
            throw new BusinessException(UiMessages.translate("低代码运行时配置生成失败: ") + e.getMessage());
        }
    }

    private List<Map<String, Object>> buildSearchSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> fields = resolveFields(modelSchema, pageSchema, "search", field -> Boolean.TRUE.equals(field.getSearchable()))
                .stream()
                .map(field -> buildSearchField(field, resolveRuntimeFieldSetting(pageSchema, "search", field.getField()), modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "search");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private List<Map<String, Object>> buildColumnsSchema(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        List<Map<String, Object>> columns = resolveFields(modelSchema, pageSchema, "table",
                field -> field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible()))
                .stream()
                .map(field -> buildTableColumn(field, resolveRuntimeFieldSetting(pageSchema, "table", field.getField()),
                        modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("key", "actions");
        actions.put("title", "操作");
        actions.put("titleI18n", Map.of("en-US", "Actions"));
        actions.put("dataIndex", "actions");
        List<Map<String, Object>> rowActions = buildRowActions(pageSchema, isEmbeddedTreeTableRuntime(modelSchema, pageSchema));
        actions.put("width", Math.max(180, rowActions.size() * 58));
        actions.put("fixed", "right");
        actions.put("actions", rowActions);
        actions.put("maxActionButtons", 3);
        columns.add(actions);
        return columns;
    }

    private List<Map<String, Object>> buildEditSchema(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> childFieldRefs = isMasterDetailRuntime(pageSchema) ? buildChildFieldRefs(pageSchema) : Set.of();
        List<LowcodeFieldSchema> orderedFields = sortByCanvasOrder(
                resolveFields(modelSchema, pageSchema, "edit",
                        field -> isEditFieldVisibleAtDesignTime(pageSchema, field)),
                pageSchema,
                "edit"
        );
        List<Map<String, Object>> fields = orderedFields
                .stream()
                .filter(field -> !childFieldRefs.contains(field.getField()))
                .map(field -> buildEditField(field, resolveEditFieldSetting(pageSchema, field.getField()),
                        modelSchema, pageSchema))
                .collect(Collectors.toCollection(ArrayList::new));
        appendTreeRuntimeField(fields, modelSchema, pageSchema, "edit");
        decorateTreeRuntimeFields(fields, configKey, modelSchema, pageSchema);
        return fields;
    }

    private Map<String, String> buildApiConfig(String configKey, LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Map<String, String> apiConfig = new LinkedHashMap<>();
        apiConfig.put("list", "get@/ai/crud/" + configKey + "/page");
        if (isTreeRuntime(modelSchema, pageSchema)) {
            apiConfig.put("tree", "get@/ai/crud/" + configKey + "/tree");
        }
        apiConfig.put("detail", "get@/ai/crud/" + configKey + "/:id");
        apiConfig.put("create", "post@/ai/crud/" + configKey);
        apiConfig.put("update", "put@/ai/crud/" + configKey);
        apiConfig.put("delete", "delete@/ai/crud/" + configKey + "/:id");
        apiConfig.put("import", "post@/ai/crud/" + configKey + "/import");
        apiConfig.put("export", "post@/ai/crud/" + configKey + "/export");
        apiConfig.put("importTemplate", "get@/ai/crud/" + configKey + "/import-template");
        return apiConfig;
    }
}
