package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.lowcode.runtime.service.identity.RuntimeIdentityFields;
import com.mdframe.forge.plugin.generator.service.lowcode.RuntimeCrudConfigAdapter;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.ObjectProvider;
import java.time.LocalDate;
import java.util.*;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.RelatedTableMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.TreeCodegenMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenMetadata.Model;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenRelations.Relations;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenContext.PreparedContext;

@RequiredArgsConstructor
final class VelocityCodegenContext {
    private final ObjectMapper objectMapper;
    private final VelocityCodegenConfiguration configuration;

    PreparedContext prepare(Model model, Relations relations, Map<String, Object> annotationFlags,
                            ObjectProvider<LowcodeStaticCodegenContributor> staticCodegenContributors) throws Exception {
        VelocityUtils.initVelocity();
        VelocityContext ctx = VelocityUtils.prepareContext(model.table());
        configureActionPermissions(model, ctx);
        configureIdentity(model, ctx, annotationFlags);
        configureSchemas(model, relations, ctx);
        configureReferences(model, ctx);
        OutputPaths paths = configureOutputPaths(model, ctx);
        AiCrudConfig codegenConfig = model.config();
        boolean includeBackend = paths.includeBackend();
        List<LowcodeStaticCodegenContributor> activeContributors = includeBackend
                ? staticCodegenContributors.orderedStream()
                .filter(contributor -> contributor.supports(codegenConfig))
                .toList()
                : List.of();
        for (LowcodeStaticCodegenContributor contributor : activeContributors) {
            contributor.contributeContext(codegenConfig, ctx);
        }
        ctx.put("staticCodegenContributors", activeContributors.stream()
                .map(LowcodeStaticCodegenContributor::capabilityId)
                .toList());

        return new PreparedContext(ctx, activeContributors, paths,
                Boolean.TRUE.equals(annotationFlags.get("hasDictConfig")));
    }

    record PreparedContext(VelocityContext context, List<LowcodeStaticCodegenContributor> contributors,
                           OutputPaths paths, boolean hasDictConfig) {}

    record OutputPaths(boolean includeBackend, boolean includeFrontend, String backendBasePath,
                       String mapperXmlBasePath, String frontendBasePath, String frontendApiBasePath) {}

    private void configureReferences(Model model, VelocityContext context) throws Exception {
        AiCrudConfig config = model.config();
        boolean enabled = !new RuntimeIdentityFields(objectMapper).read(RuntimeCrudConfigAdapter.fromDesign(config)).isEmpty();
        context.put("hasIdentityReferences", enabled);
        if (enabled) {
            context.put("identityModelLiteral", objectMapper.writeValueAsString(config.getModelSchema()));
            context.put("identityEditLiteral", objectMapper.writeValueAsString(config.getEditSchema()));
            context.put("identityObjectLiteral", objectMapper.writeValueAsString(config.getObjectCode()));
        }
    }

    private void configureActionPermissions(Model model, VelocityContext ctx) throws Exception {
        AiCrudConfig codegenConfig = model.config();
        Map<String, Object> runtimeOptions = model.options();
        // Use the same permission namespace as menu.sql for generated built-in actions.
        String permissionPrefix = ctx.get("moduleName") + ":" + ctx.get("businessName") + ":";
        Map<String, String> actionPermissions = new LinkedHashMap<>();
        actionPermissions.put("add", permissionPrefix + "add");
        actionPermissions.put("detail", permissionPrefix + "query");
        actionPermissions.put("edit", permissionPrefix + "edit");
        actionPermissions.put("delete", permissionPrefix + "remove");
        actionPermissions.put("batchDelete", permissionPrefix + "remove");
        actionPermissions.put("import", permissionPrefix + "add");
        actionPermissions.put("export", permissionPrefix + "query");
        runtimeOptions.put("actionPermissions", actionPermissions);
        codegenConfig.setOptions(objectMapper.writeValueAsString(runtimeOptions));

    }

    private void configureIdentity(Model model, VelocityContext ctx, Map<String, Object> annotationFlags) {
        AiCrudConfig codegenConfig = model.config();
        String outputConfigKey = model.outputConfigKey();
        String runtimeConfigKey = model.runtimeConfigKey();
        String apiBase = model.apiBase();
        // 注入注解控制变量
        annotationFlags.forEach(ctx::put);

        // 注入业务路由前缀（前端模板使用）
        ctx.put("apiBase", apiBase);
        ctx.put("businessApiBase", apiBase);
        ctx.put("date", LocalDate.now().toString());

        // 前端 configKey → 组件名/变量名
        String configKey = outputConfigKey;
        String viewPath = configKey.replace("_", "/");
        ctx.put("configKey", configKey);
        ctx.put("localeNamespace", "generated_" + configKey.replaceAll("[^a-zA-Z0-9_]", "_"));
        ctx.put("runtimeConfigKey", runtimeConfigKey);
        ctx.put("viewPath", viewPath);
        ctx.put("componentName", VelocityCodegenMetadata.toPascalCase(configKey));
        ctx.put("apiVarName", VelocityCodegenMetadata.toPascalCase(configKey));
        ctx.put("tableComment", StringUtils.isNotBlank(codegenConfig.getTableComment())
                ? codegenConfig.getTableComment() : configKey);

    }

    private void configureSchemas(Model model, Relations relations, VelocityContext ctx) {
        AiCrudConfig codegenConfig = model.config();
        GenTable genTable = model.table();
        List<GenTableColumn> columns = genTable.getColumns();
        String layoutType = model.layoutType();
        Map<String, Object> runtimeOptions = model.options();
        Map<String, Object> treeConfig = model.treeConfig();
        Map<String, Object> masterDetailConfig = model.masterDetailConfig();
        TreeCodegenMeta treeMeta = relations.tree();
        boolean hasTreeConfig = treeMeta != null;
        boolean isLeftTreeLayout = relations.leftTreeLayout();
        boolean isTreeTableLayout = relations.treeTableLayout();
        boolean isMasterDetailLayout = relations.masterDetailLayout();
        List<RelatedTableMeta> masterDetailChildren = relations.children();
        List<RelatedTableMeta> injectedRelatedTables = relations.injectedTables();
        // searchSchema / columnsSchema / editSchema / apiConfig（前端模板需要）
        Map<String, Object> transConfig = configuration.parseJsonObject(codegenConfig.getTransConfig());
        ctx.put("searchSchema", configuration.parseJsonArray(codegenConfig.getSearchSchema()));
        ctx.put("columnsSchema", preprocessColumnsSchema(
                configuration.parseJsonArray(codegenConfig.getColumnsSchema()), transConfig));
        ctx.put("editSchema", configuration.parseJsonArray(codegenConfig.getEditSchema()));
        ctx.put("apiConfig", configuration.parseJsonObject(codegenConfig.getApiConfig()));
        ctx.put("layoutType", layoutType);
        ctx.put("options", runtimeOptions);
        ctx.put("optionsJson", configuration.toJsonLiteral(runtimeOptions));
        ctx.put("searchSchemaJson", configuration.toJsonLiteral(ctx.get("searchSchema")));
        ctx.put("columnsSchemaJson", configuration.toJsonLiteral(ctx.get("columnsSchema")));
        ctx.put("editSchemaJson", configuration.toJsonLiteral(ctx.get("editSchema")));
        ctx.put("treeConfigJson", configuration.toJsonLiteral(treeConfig));
        ctx.put("masterDetailConfigJson", configuration.toJsonLiteral(masterDetailConfig));
        ctx.put("hasTreeConfig", hasTreeConfig);
        ctx.put("tree", treeMeta);
        ctx.put("isLeftTreeLayout", isLeftTreeLayout);
        ctx.put("isTreeTableLayout", isTreeTableLayout);
        ctx.put("isMasterDetailLayout", isMasterDetailLayout);
        ctx.put("masterDetailChildren", masterDetailChildren);
        ctx.put("injectedRelatedTables", injectedRelatedTables);
        ctx.put("hasEntityTreeFields", hasTreeConfig && !treeMeta.isSeparateSource());
        ctx.put("entityTreeChildrenField", hasTreeConfig && !treeMeta.isSeparateSource()
                ? treeMeta.getChildrenField() : "children");
        ctx.put("isPrimaryCodegenTable", true);
        ctx.put("hasLogicDeleteColumn", VelocityCodegenMetadata.hasColumn(columns, "del_flag"));
        ctx.put("enableServiceExtensions", true);
    }

    private OutputPaths configureOutputPaths(Model model, VelocityContext ctx) {
        AiCrudConfig codegenConfig = model.config();
        boolean includeBackend = configuration.readBooleanOption(codegenConfig, "includeBackend", true);
        boolean includeFrontend = configuration.readBooleanOption(codegenConfig, "includeFrontend", true);
        String backendBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(
                configuration.readOption(codegenConfig, "backendBasePath", null),
                LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径");
        String mapperXmlBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(
                configuration.readOption(codegenConfig, "mapperXmlBasePath", null),
                LowcodeCodegenOptionUtils.DEFAULT_MAPPER_XML_BASE_PATH, "Mapper XML 输出路径");
        String frontendBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(
                configuration.readOption(codegenConfig, "frontendBasePath", null),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_BASE_PATH, "前端页面输出路径");
        String frontendApiBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(
                configuration.readOption(codegenConfig, "frontendApiBasePath", null),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_API_BASE_PATH, "前端 API 输出路径");
        ctx.put("backendBasePath", backendBasePath);
        ctx.put("mapperXmlBasePath", mapperXmlBasePath);
        ctx.put("frontendBasePath", frontendBasePath);
        ctx.put("frontendApiBasePath", frontendApiBasePath);
        ctx.put("includeBackend", includeBackend);
        ctx.put("includeFrontend", includeFrontend);
        ctx.put("entityPrefix", LowcodeCodegenOptionUtils.normalizeEntityPrefix(
                configuration.readOption(codegenConfig, "entityPrefix", "")));
        ctx.put("stripTablePrefixes", configuration.resolveStripTablePrefixes(codegenConfig));
        return new OutputPaths(includeBackend, includeFrontend, backendBasePath,
                mapperXmlBasePath, frontendBasePath, frontendApiBasePath);
    }

    List<Map<String, Object>> preprocessColumnsSchema(List<Map<String, Object>> columns, Map<String, Object> transConfig) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            Map<String, Object> newCol = projectColumn(col, transConfig);
            result.add(newCol);
        }
        return result;
    }

    private Map<String, Object> projectColumn(Map<String, Object> col, Map<String, Object> transConfig) {
        Object key = col.get("key");
        Map<String, Object> newCol = new java.util.LinkedHashMap<>(col);
        // 提取 render.dictType
        Object renderObj = col.get("render");
        if (renderObj instanceof Map) {
            Map<String, Object> render = (Map<String, Object>) renderObj;
            if ("dictTag".equals(render.get("type")) && render.get("dictType") != null) {
                newCol.put("_dictType", render.get("dictType"));
            }
        }
        // 提取 transConfig 中的 targetField 为 _transName
        // transConfig 的 key 是数据字段名，优先用 dataIndex，其次用 key
        Object dataIndex = col.get("dataIndex");
        String fieldName = dataIndex != null && !String.valueOf(dataIndex).isEmpty()
                ? String.valueOf(dataIndex)
                : (key != null ? String.valueOf(key) : "");
        if (!fieldName.isEmpty() && transConfig.containsKey(fieldName)) {
            Object transConf = transConfig.get(fieldName);
            String targetField = fieldName + "Name"; // 默认是数据字段名 + Name
            if (transConf instanceof Map) {
                Object tf = ((Map<String, Object>) transConf).get("targetField");
                if (tf != null && !String.valueOf(tf).isEmpty()) {
                targetField = String.valueOf(tf);
                }
            }
            newCol.put("_transName", targetField);
        }
        return newCol;
    }
}
