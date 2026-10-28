package com.mdframe.forge.plugin.generator.codegen;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.RelatedTableMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.TreeCodegenMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenMetadata.Model;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenRelations.Relations;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenContext.PreparedContext;

@Slf4j
@RequiredArgsConstructor
final class VelocityCodegenArtifacts {
    private final ObjectMapper objectMapper;
    private final LowcodeProtocolSnapshotBuilder protocolSnapshotBuilder;
    private final VelocityCodegenConfiguration configuration;
    private static final Pattern UNRESOLVED_VELOCITY_REFERENCE = Pattern.compile(
            "\\$\\{(?:packageName|moduleName|className|classname|businessName|functionName|author|date|datetime|"
                    + "tableName|businessApiBase|apiBase|configKey|runtimeConfigKey|componentName|apiVarName|pkColumn|"
                    + "entityTreeChildrenField|tree|child|table|column)[^}\\r\\n]*}|"
                    + "\\$(?:pkColumn|tree|child|table|column)\\.[A-Za-z_]");


    Map<String, String> render(Model model, Relations relations, PreparedContext prepared) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        VelocityContext context = prepared.context();
        VelocityCodegenContext.OutputPaths paths = prepared.paths();
        String extensionSample = renderBackend(files, model, relations, context, paths);
        renderSql(files, model, relations, context, prepared.hasDictConfig());
        renderFrontend(files, model, context, paths);
        if (paths.includeBackend()) {
            contributeStaticCodegenFiles(files, model.config(), context, prepared.contributors());
        }
        renderProtocol(files, model, context, extensionSample, paths);
        return files;
    }

    private String renderBackend(Map<String, String> files, Model model, Relations relations, VelocityContext ctx,
                                 VelocityCodegenContext.OutputPaths paths) {
        GenTable genTable = model.table();
        String moduleName = model.moduleName();
        String className = genTable.getClassName();
        String backendBasePath = paths.backendBasePath();
        String javaRoot = backendBasePath + "/" + genTable.getPackageName().replace(".", "/") + "/" + moduleName + "/";
        String mapperXmlRoot = paths.mapperXmlBasePath() + "/";
        boolean includeBackend = paths.includeBackend();
        boolean isMasterDetailLayout = relations.masterDetailLayout();
        List<RelatedTableMeta> relatedTables = relations.tables();
        TreeCodegenMeta treeMeta = relations.tree();
        String extensionSamplePath = null;
        if (includeBackend) {
            renderTo(files, "templates/vm/entity.java.vm",      ctx, javaRoot + "entity/" + className + ".java");
            renderTo(files, "templates/vm/vo.java.vm",          ctx, javaRoot + "vo/" + className + "VO.java");
            renderTo(files, "templates/vm/mapper.java.vm",      ctx, javaRoot + "mapper/" + className + "Mapper.java");
            renderTo(files, "templates/vm/mapper.xml.vm",       ctx, mapperXmlRoot + className + "Mapper.xml");
            renderTo(files, "templates/vm/service.java.vm",     ctx, javaRoot + "service/I" + className + "Service.java");
            renderTo(files, "templates/vm/serviceImpl.java.vm", ctx, javaRoot + "service/impl/" + className + "ServiceImpl.java");
            renderTo(files, "templates/vm/serviceExtension.java.vm", ctx,
                    javaRoot + "service/extension/" + className + "ServiceExtension.java");
            renderTo(files, "templates/vm/controller.java.vm",  ctx, javaRoot + "controller/" + className + "Controller.java");
            renderTo(files, "templates/vm/dto.java.vm",         ctx, javaRoot + "dto/" + className + "DTO.java");
            renderTo(files, "templates/vm/query.java.vm",       ctx, javaRoot + "dto/" + className + "Query.java");
            if (isMasterDetailLayout) {
                renderTo(files, "templates/vm/masterDetailDTO.java.vm", ctx,
                        javaRoot + "dto/" + className + "MasterDetailDTO.java");
            }
            renderRelatedTableFiles(files, relatedTables, javaRoot, mapperXmlRoot, treeMeta);
            extensionSamplePath = "examples/" + javaRoot
                    + "service/extension/custom/" + className + "BusinessExtension.java.example";
            renderTo(files, "templates/vm/businessExtension.java.vm.example", ctx, extensionSamplePath);
        }

        return extensionSamplePath;
    }

    private void renderSql(Map<String, String> files, Model model, Relations relations, VelocityContext ctx, boolean hasDictConfig) {
        AiCrudConfig codegenConfig = model.config();
        List<RelatedTableMeta> relatedTables = relations.tables();
        TreeCodegenMeta treeMeta = relations.tree();
        boolean includeSql = configuration.readBooleanOption(codegenConfig, "includeSql", true);
        boolean includeMenuSql = configuration.readBooleanOption(codegenConfig, "includeMenuSql", true);
        boolean includeDictSql = configuration.readBooleanOption(codegenConfig, "includeDictSql", true);
        boolean includeExcelSql = configuration.readBooleanOption(codegenConfig, "includeExcelSql", true);
        ctx.put("includeSql", includeSql);
        ctx.put("includeMenuSql", includeMenuSql);
        ctx.put("includeDictSql", includeDictSql);
        ctx.put("includeExcelSql", includeExcelSql);
        if (includeSql) {
            renderTo(files, "templates/vm/sql/table.sql.vm", ctx,
                    "sql/schema/" + codegenConfig.getTableName() + ".sql");
            renderRelatedTableSql(files, relatedTables, treeMeta);
        }
        if (includeSql && includeMenuSql) {
            renderTo(files, "templates/vm/sql/menu.sql.vm", ctx,
                    "sql/" + codegenConfig.getTableName() + "_menu.sql");
        }
        if (includeSql && includeDictSql && hasDictConfig) {
            renderTo(files, "templates/vm/sql/dict.sql.vm", ctx,
                    "sql/" + codegenConfig.getTableName() + "_dict.sql");
        }
        if (includeSql && includeExcelSql) {
            renderTo(files, "templates/vm/sql/excel.sql.vm", ctx,
                    "sql/" + codegenConfig.getTableName() + "_excel.sql");
        }

    }

    private void renderFrontend(Map<String, String> files, Model model, VelocityContext ctx,
                                VelocityCodegenContext.OutputPaths paths) throws Exception {
        GenTable genTable = model.table();
        List<GenTableColumn> columns = genTable.getColumns();
        String configKey = model.outputConfigKey();
        String viewPath = configKey.replace("_", "/");
        String frontendBasePath = paths.frontendBasePath();
        String frontendApiBasePath = paths.frontendApiBasePath();
        boolean includeFrontend = paths.includeFrontend();
        if (includeFrontend) {
            renderTo(files, "templates/vm/ai-crud/index.vue.vm", ctx,
                    frontendBasePath + "/" + viewPath + "/index.vue");
            renderTo(files, "templates/vm/ai-crud/api.js.vm", ctx,
                    frontendApiBasePath + "/" + configKey + ".js");
            Map<String, String> chineseLabels = new LinkedHashMap<>();
            Map<String, String> englishLabels = new LinkedHashMap<>();
            chineseLabels.put("title", genTable.getFunctionName());
            englishLabels.put("title", String.valueOf(ctx.get("functionNameEn")));
            com.mdframe.forge.plugin.generator.util.CodegenI18n display = new com.mdframe.forge.plugin.generator.util.CodegenI18n();
            for (GenTableColumn column : columns) {
                chineseLabels.put(column.getJavaField(), column.getColumnComment());
                englishLabels.put(column.getJavaField(), display.column(column));
            }
            String namespace = String.valueOf(ctx.get("localeNamespace"));
            files.put(frontendBasePath + "/" + viewPath + "/locales/zh-CN.js",
                    "export default " + objectMapper.writeValueAsString(Map.of(namespace, chineseLabels)) + "\n");
            files.put(frontendBasePath + "/" + viewPath + "/locales/en-US.js",
                    "export default " + objectMapper.writeValueAsString(Map.of(namespace, englishLabels)) + "\n");
        }
    }

    private void renderProtocol(Map<String, String> files, Model model, VelocityContext ctx, String extensionSamplePath,
                                VelocityCodegenContext.OutputPaths paths) throws Exception {
        AiCrudConfig codegenConfig = model.config();
        GenTable genTable = model.table();
        String moduleName = model.moduleName();
        String configKey = model.outputConfigKey();
        String runtimeConfigKey = model.runtimeConfigKey();
        String viewPath = configKey.replace("_", "/");
        String backendBasePath = paths.backendBasePath();
        String javaRoot = backendBasePath + "/" + genTable.getPackageName().replace(".", "/") + "/" + moduleName + "/";
        String frontendBasePath = paths.frontendBasePath();
        boolean includeBackend = paths.includeBackend();
        boolean includeFrontend = paths.includeFrontend();
        LowcodeProtocolSnapshotBuilder.ProtocolArtifacts artifacts = protocolSnapshotBuilder.build(codegenConfig);
        String frontendRuntime = artifacts.frontendRuntimeConfig();
        if (Boolean.TRUE.equals(ctx.get("hasIdentityReferences"))) {
            var runtime = objectMapper.readTree(frontendRuntime);
            ((com.fasterxml.jackson.databind.node.ObjectNode) runtime)
                    .put("identityReferenceApiBase", model.apiBase() + "/references");
            frontendRuntime = objectMapper.writeValueAsString(runtime);
        }
        if (includeFrontend) {
            files.put(frontendBasePath + "/" + viewPath + "/runtime-config.json",
                    frontendRuntime);
        }
        files.put("config/" + runtimeConfigKey + "-config.json", frontendRuntime);
        files.put("config/" + runtimeConfigKey + "-protocol.json", artifacts.protocolSnapshot());
        files.put("config/" + runtimeConfigKey + "-coverage.json", artifacts.coverageReport());
        renderTo(files, "templates/vm/README.md.vm", ctx, "README.md");
        String ownershipPath = "config/" + runtimeConfigKey + "-ownership.json";
        files.put(ownershipPath, buildOwnershipManifest(
                files.keySet(), extensionSamplePath, ownershipPath,
                includeBackend ? javaRoot + "service/extension/custom/**/*.java" : null));

    }

    void renderTo(Map<String, String> files, String templatePath,
                          VelocityContext ctx, String outputPath) {
        try {
            String content = VelocityUtils.renderTemplate(templatePath, ctx);
            assertNoUnresolvedVelocityReference(content, outputPath);
            String existing = files.putIfAbsent(outputPath, content);
            if (existing != null && !StringUtils.equals(existing, content)) {
                throw new BusinessException(UiMessages.translate("代码生成文件路径冲突：") + outputPath
                        + UiMessages.translate("，请检查实体前缀、表前缀和对象表名设置"));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[VelocityCodegenStrategy] 渲染模板失败: {}, outputPath={}", templatePath, outputPath, e);
            throw new BusinessException(UiMessages.translate("代码模板渲染失败：") + outputPath);
        }
    }

    void assertNoUnresolvedVelocityReference(String content, String outputPath) {
        if (!StringUtils.endsWithIgnoreCase(outputPath, ".java")) {
            return;
        }
        Matcher matcher = UNRESOLVED_VELOCITY_REFERENCE.matcher(StringUtils.defaultString(content));
        if (matcher.find()) {
            throw new BusinessException(UiMessages.translate("代码模板渲染失败：") + outputPath
                    + UiMessages.translate(" 存在未解析模板变量 ") + matcher.group());
        }
    }

    void renderRelatedTableFiles(Map<String, String> files,
                                         List<RelatedTableMeta> relatedTables,
                                         String javaRoot,
                                         String mapperXmlRoot,
                                         TreeCodegenMeta treeMeta) {
        if (relatedTables == null || relatedTables.isEmpty()) {
            return;
        }
        for (RelatedTableMeta meta : relatedTables) {
            VelocityContext childCtx = prepareRelatedTableContext(meta, treeMeta);
            renderTo(files, "templates/vm/entity.java.vm", childCtx, javaRoot + "entity/" + meta.getClassName() + ".java");
            renderTo(files, "templates/vm/vo.java.vm", childCtx, javaRoot + "vo/" + meta.getClassName() + "VO.java");
            renderTo(files, "templates/vm/mapper.java.vm", childCtx, javaRoot + "mapper/" + meta.getClassName() + "Mapper.java");
            renderTo(files, "templates/vm/mapper.xml.vm", childCtx,
                    mapperXmlRoot + meta.getClassName() + "Mapper.xml");
            renderTo(files, "templates/vm/dto.java.vm", childCtx, javaRoot + "dto/" + meta.getClassName() + "DTO.java");
            renderTo(files, "templates/vm/query.java.vm", childCtx, javaRoot + "dto/" + meta.getClassName() + "Query.java");
            renderTo(files, "templates/vm/service.java.vm", childCtx,
                    javaRoot + "service/I" + meta.getClassName() + "Service.java");
            renderTo(files, "templates/vm/serviceImpl.java.vm", childCtx,
                    javaRoot + "service/impl/" + meta.getClassName() + "ServiceImpl.java");
        }
    }

    void renderRelatedTableSql(Map<String, String> files,
                                       List<RelatedTableMeta> relatedTables,
                                       TreeCodegenMeta treeMeta) {
        if (relatedTables == null || relatedTables.isEmpty()) {
            return;
        }
        for (RelatedTableMeta meta : relatedTables) {
            VelocityContext childCtx = prepareRelatedTableContext(meta, treeMeta);
            renderTo(files, "templates/vm/sql/table.sql.vm", childCtx,
                    "sql/schema/" + meta.getTableName() + ".sql");
        }
    }

    VelocityContext prepareRelatedTableContext(RelatedTableMeta meta, TreeCodegenMeta treeMeta) {
        meta.getColumns().forEach(column -> column.setDesensitizeType(
                VelocityCodegenAnnotations.normalizeDesensitizeType(column.getDesensitizeType())));
        VelocityContext childCtx = VelocityUtils.prepareContext(meta.getTable());
        childCtx.put("hasDictConfig", GenUtils.hasDictTrans(meta.getColumns()));
        childCtx.put("hasDictTrans", GenUtils.hasDictTrans(meta.getColumns()));
        childCtx.put("hasDesensitize", meta.getColumns().stream()
                .anyMatch(column -> StringUtils.isNotBlank(column.getDesensitizeType())));
        childCtx.put("hasEncrypt", false);
        childCtx.put("enableDecrypt", false);
        childCtx.put("enableEncrypt", false);
        childCtx.put("hasEntityTreeFields", meta.isTreeSource());
        childCtx.put("entityTreeChildrenField", StringUtils.defaultIfBlank(meta.getTreeChildrenField(), "children"));
        childCtx.put("tree", meta.isTreeSource() ? treeMeta : null);
        childCtx.put("hasTreeConfig", meta.isTreeSource() && treeMeta != null);
        childCtx.put("isPrimaryCodegenTable", true);
        childCtx.put("isMasterDetailLayout", false);
        childCtx.put("masterDetailChildren", List.of());
        childCtx.put("injectedRelatedTables", List.of());
        childCtx.put("hasLogicDeleteColumn", meta.isHasLogicDelete());
        childCtx.put("enableServiceExtensions", false);
        return childCtx;
    }

    String buildOwnershipManifest(Collection<String> paths,
                                          String createOnceSamplePath,
                                          String manifestPath,
                                          String userOwnedPattern) throws Exception {
        List<Map<String, Object>> files = new ArrayList<>();
        Set<String> sortedPaths = new TreeSet<>(paths);
        sortedPaths.add(manifestPath);
        for (String path : sortedPaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("path", path);
            item.put("ownership", StringUtils.equals(path, createOnceSamplePath)
                    ? "CREATE_ONCE_SAMPLE" : "GENERATED");
            files.add(item);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("ownershipVersion", "forge-codegen-file-ownership-v1");
        manifest.put("generatedPolicy", "REPLACE_WITH_REVIEW");
        manifest.put("createOncePolicy", "COPY_AND_RENAME_ONCE");
        manifest.put("userOwnedPatterns", StringUtils.isBlank(userOwnedPattern)
                ? List.of() : List.of(userOwnedPattern));
        manifest.put("userOwnedPolicy", "NEVER_GENERATED_NEVER_OVERWRITTEN");
        manifest.put("files", files);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
    }

    void contributeStaticCodegenFiles(Map<String, String> target,
                                              AiCrudConfig config,
                                              VelocityContext context,
                                              List<LowcodeStaticCodegenContributor> contributors) {
        for (LowcodeStaticCodegenContributor contributor : contributors) {
            Map<String, String> contributed = new LinkedHashMap<>();
            contributor.contributeFiles(config, context, contributed);
            for (Map.Entry<String, String> entry : contributed.entrySet()) {
                String existing = target.putIfAbsent(entry.getKey(), entry.getValue());
                if (existing != null) {
                    throw new BusinessException(UiMessages.translate("静态代码编译贡献器不能覆盖已有文件: ")
                            + contributor.capabilityId() + " -> " + entry.getKey());
                }
            }
        }
    }
}
