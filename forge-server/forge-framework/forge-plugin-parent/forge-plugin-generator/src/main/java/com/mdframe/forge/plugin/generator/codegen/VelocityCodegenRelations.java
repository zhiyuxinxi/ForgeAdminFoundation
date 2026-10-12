package com.mdframe.forge.plugin.generator.codegen;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.util.GenUtils;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.RelatedTableMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.TreeCodegenMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenMetadata.Model;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenRelations.Relations;

@RequiredArgsConstructor
final class VelocityCodegenRelations {
    private final VelocityCodegenConfiguration configuration;
    private final VelocityCodegenMetadata metadata;
    private static final Set<String> MASTER_DETAIL_RELATION_TYPES = Set.of("CHILD_LIST", "DETAIL", "ONE_TO_MANY");

    Relations prepare(Model model) {
        List<RelatedTableMeta> tables = buildRelatedTables(model.config(), model.pageSchema(), model.table(),
                model.moduleName(), model.businessPath());
        TreeCodegenMeta tree = buildTreeMeta(model.table(), tables, model.treeConfig());
        List<RelatedTableMeta> children = buildMasterDetailChildren(model.masterDetailConfig(), tables,
                model.table(), model.pageSchema());
        boolean masterDetail = "master-detail-crud".equals(model.layoutType());
        if (masterDetail && children.isEmpty()) {
            throw new BusinessException("主子表代码生成失败：未解析到有效子表关系，请检查子表及主外键配置");
        }
        return new Relations(tables, tree, children, resolveInjectedRelatedTables(tree, tables, children),
                tree != null && "tree-crud".equals(model.layoutType()),
                tree != null && !"tree-crud".equals(model.layoutType()), masterDetail && !children.isEmpty());
    }

    record Relations(List<RelatedTableMeta> tables, TreeCodegenMeta tree, List<RelatedTableMeta> children,
                     List<RelatedTableMeta> injectedTables, boolean leftTreeLayout,
                     boolean treeTableLayout, boolean masterDetailLayout) {}

    @Data
    private static class MasterDetailFieldPair {
        private final String childField;
        private final String mainField;
    }

    List<RelatedTableMeta> buildRelatedTables(AiCrudConfig config,
                                                      LowcodePageSchema pageSchema,
                                                      GenTable mainTable,
                                                      String moduleName,
                                                      String businessPath) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return new ArrayList<>();
        }
        List<RelatedTableMeta> result = new ArrayList<>();
        Set<String> seenTables = new LinkedHashSet<>();
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getTableName())) {
                continue;
            }
            if (StringUtils.equals(ref.getTableName(), config.getTableName()) || !seenTables.add(ref.getTableName())) {
                continue;
            }
            GenTable table = new GenTable();
            table.setTableName(ref.getTableName());
            table.setTableComment(StringUtils.defaultIfBlank(ref.getModelName(), ref.getTableName()));
            table.setFunctionName(table.getTableComment());
        table.setFunctionNameEn(configuration.readOption(config, "functionNameEn", null));
        if (StringUtils.isNotBlank(config.getModelSchema())) {
            Map<String, Object> names = configuration.readNestedMap(configuration.parseJsonObject(config.getModelSchema()), "businessNameI18n");
            table.setFunctionNameEn(StringUtils.defaultIfBlank(configuration.text(names.get("en-US")), table.getFunctionNameEn()));
        }
            String className = LowcodeCodegenOptionUtils.buildClassName(
                    ref.getTableName(), configuration.readOption(config, "entityPrefix", ""),
                    configuration.resolveStripTablePrefixes(config));
            table.setClassName(className);
            table.setBusinessName(metadata.resolveBusinessName(businessPath));
            table.setPackageName(mainTable.getPackageName());
            table.setModuleName(moduleName);
            table.setAuthor(mainTable.getAuthor());
            List<GenTableColumn> refColumns = metadata.buildColumnsFromModelRef(ref);
            table.setColumns(refColumns);
            table.setPkColumn(GenUtils.getPkColumn(refColumns));

            RelatedTableMeta meta = new RelatedTableMeta();
            meta.setModelCode(ref.getModelCode());
            meta.setKey(StringUtils.defaultIfBlank(ref.getModelCode(), className));
            meta.setModelName(ref.getModelName());
            meta.setTableName(ref.getTableName());
            meta.setClassName(className);
            meta.setVariableName(StringUtils.uncapitalize(className));
            meta.setMapperVarName(StringUtils.uncapitalize(className) + "Mapper");
            meta.setTable(table);
            meta.setColumns(refColumns);
            meta.setPkColumn(table.getPkColumn());
            meta.setHasLogicDelete(metadata.hasColumn(refColumns, "del_flag"));
            meta.setUniqueDeleteMarker(meta.getPkColumn() != null
                    && "Long".equals(meta.getPkColumn().getJavaType())
                    && refColumns.stream().anyMatch(column -> "del_flag".equals(column.getColumnName())
                    && "Long".equals(column.getJavaType())));
            result.add(meta);
        }
        return result;
    }

    List<RelatedTableMeta> buildMasterDetailChildren(Map<String, Object> masterDetailConfig,
                                                             List<RelatedTableMeta> relatedTables,
                                                             GenTable mainTable,
                                                             LowcodePageSchema pageSchema) {
        List<RelatedTableMeta> result = buildConfiguredMasterDetailChildren(
                masterDetailConfig, relatedTables, mainTable);
        appendPageSchemaMasterDetailChildren(result, relatedTables, mainTable, pageSchema);
        return result;
    }

    List<RelatedTableMeta> buildConfiguredMasterDetailChildren(Map<String, Object> masterDetailConfig,
                                                                       List<RelatedTableMeta> relatedTables,
                                                                       GenTable mainTable) {
        Object childrenObj = masterDetailConfig.get("children");
        if (!(childrenObj instanceof List<?> childrenList)) {
            return new ArrayList<>();
        }
        List<RelatedTableMeta> result = new ArrayList<>();
        for (Object childObj : childrenList) {
            if (!(childObj instanceof Map<?, ?> rawChild)) {
                continue;
            }
            Map<String, Object> child = (Map<String, Object>) rawChild;
            RelatedTableMeta meta = findRelatedTable(relatedTables, configuration.text(child.get("modelCode")), configuration.text(child.get("tableName")));
            if (meta == null) {
                continue;
            }
            configureMasterDetailChild(
                    meta,
                    StringUtils.firstNonBlank(configuration.text(child.get("key")), configuration.text(child.get("modelCode")), meta.getKey()),
                    StringUtils.defaultIfBlank(configuration.text(child.get("sourceField")), "parentId"),
                    StringUtils.defaultIfBlank(configuration.text(child.get("targetField")), defaultMainKeyField(mainTable)),
                    mainTable);
            result.add(meta);
        }
        return result;
    }

    void appendPageSchemaMasterDetailChildren(List<RelatedTableMeta> result,
                                                       List<RelatedTableMeta> relatedTables,
                                                       GenTable mainTable,
                                                       LowcodePageSchema pageSchema) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().isEmpty()) {
            return;
        }
        LowcodePageModelRef primaryRef = resolvePrimaryModelRef(pageSchema);
        String primaryModelCode = StringUtils.firstNonBlank(
                pageSchema.getPrimaryModelCode(), primaryRef == null ? null : primaryRef.getModelCode());
        for (LowcodePageModelRef childRef : pageSchema.getModelRefs()) {
            if (childRef == null || Boolean.TRUE.equals(childRef.getPrimary())) {
                continue;
            }
            RelatedTableMeta meta = findRelatedTable(
                    relatedTables, childRef.getModelCode(), childRef.getTableName());
            if (meta == null || result.contains(meta)) {
                continue;
            }
            MasterDetailFieldPair fieldPair = resolveMasterDetailFieldPair(
                    primaryModelCode, primaryRef, childRef);
            if (fieldPair == null) {
                continue;
            }
            configureMasterDetailChild(
                    meta,
                    StringUtils.defaultIfBlank(childRef.getModelCode(), meta.getKey()),
                    fieldPair.getChildField(),
                    fieldPair.getMainField(),
                    mainTable);
            result.add(meta);
        }
    }

    LowcodePageModelRef resolvePrimaryModelRef(LowcodePageSchema pageSchema) {
        String primaryModelCode = pageSchema.getPrimaryModelCode();
        return pageSchema.getModelRefs().stream()
                .filter(Objects::nonNull)
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary())
                        || StringUtils.equals(primaryModelCode, ref.getModelCode()))
                .findFirst()
                .orElse(null);
    }

    MasterDetailFieldPair resolveMasterDetailFieldPair(String primaryModelCode,
                                                               LowcodePageModelRef primaryRef,
                                                               LowcodePageModelRef childRef) {
        if (StringUtils.isBlank(primaryModelCode) || StringUtils.isBlank(childRef.getModelCode())) {
            return null;
        }
        if (childRef.getRelations() != null) {
            for (LowcodeRelationSchema relation : childRef.getRelations()) {
                if (isMasterDetailRelation(relation)
                        && StringUtils.equals(primaryModelCode, relation.getTargetObjectCode())
                        && StringUtils.isNoneBlank(relation.getSourceField(), relation.getTargetField())) {
                    return new MasterDetailFieldPair(relation.getSourceField(), relation.getTargetField());
                }
            }
        }
        if (primaryRef != null && primaryRef.getRelations() != null) {
            for (LowcodeRelationSchema relation : primaryRef.getRelations()) {
                if (isMasterDetailRelation(relation)
                        && StringUtils.equals(childRef.getModelCode(), relation.getTargetObjectCode())
                        && StringUtils.isNoneBlank(relation.getSourceField(), relation.getTargetField())) {
                    return new MasterDetailFieldPair(relation.getTargetField(), relation.getSourceField());
                }
            }
        }
        return null;
    }

    boolean isMasterDetailRelation(LowcodeRelationSchema relation) {
        return relation != null && MASTER_DETAIL_RELATION_TYPES.contains(
                StringUtils.defaultString(relation.getRelationType()).toUpperCase(Locale.ROOT));
    }

    void configureMasterDetailChild(RelatedTableMeta meta,
                                            String childKey,
                                            String childFkField,
                                            String mainField,
                                            GenTable mainTable) {
        meta.setMasterDetailChild(true);
        meta.setChildKey(StringUtils.defaultIfBlank(childKey, meta.getKey()));
        meta.setChildFkField(metadata.normalizeJavaField(childFkField));
        meta.setChildFkFieldCap(metadata.capJavaField(meta.getChildFkField()));
        meta.setChildFkColumn(metadata.resolveColumnName(meta.getColumns(), meta.getChildFkField()));
        meta.setMainField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(mainField, defaultMainKeyField(mainTable))));
        meta.setMainFieldCap(metadata.capJavaField(meta.getMainField()));
        meta.setMainColumn(metadata.resolveColumnName(mainTable.getColumns(), meta.getMainField()));
    }

    String defaultMainKeyField(GenTable mainTable) {
        return mainTable.getPkColumn() == null ? "id" : mainTable.getPkColumn().getJavaField();
    }

    TreeCodegenMeta buildTreeMeta(GenTable mainTable,
                                          List<RelatedTableMeta> relatedTables,
                                          Map<String, Object> treeConfig) {
        if (treeConfig == null || treeConfig.isEmpty()) {
            return null;
        }
        String sourceTableName = configuration.text(treeConfig.get("sourceTableName"));
        String sourceModelCode = configuration.text(treeConfig.get("sourceModelCode"));
        RelatedTableMeta sourceMeta = findRelatedTable(relatedTables, sourceModelCode, sourceTableName);
        boolean separateSource = sourceMeta != null
                && !StringUtils.equals(sourceMeta.getTableName(), mainTable.getTableName());
        List<GenTableColumn> sourceColumns = separateSource ? sourceMeta.getColumns() : mainTable.getColumns();

        TreeCodegenMeta meta = new TreeCodegenMeta();
        meta.setSeparateSource(separateSource);
        meta.setSourceModelCode(sourceModelCode);
        meta.setSourceTableName(StringUtils.defaultIfBlank(sourceTableName, mainTable.getTableName()));
        meta.setClassName(separateSource ? sourceMeta.getClassName() : mainTable.getClassName());
        meta.setMapperVarName(separateSource ? sourceMeta.getMapperVarName() : StringUtils.uncapitalize(mainTable.getClassName()) + "Mapper");
        meta.setKeyField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("keyField")), "id")));
        meta.setParentField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("parentField")), "parentId")));
        meta.setLabelField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("labelField")), "name")));
        meta.setFilterField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("filterField")), meta.getParentField())));
        meta.setTargetField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("targetField")), meta.getKeyField())));
        meta.setChildrenField(metadata.normalizeJavaField(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("childrenField")), "children")));
        meta.setLoadMode(StringUtils.defaultIfBlank(configuration.text(treeConfig.get("loadMode")), "full"));
        meta.setKeyFieldCap(metadata.capJavaField(meta.getKeyField()));
        meta.setParentFieldCap(metadata.capJavaField(meta.getParentField()));
        meta.setLabelFieldCap(metadata.capJavaField(meta.getLabelField()));
        meta.setFilterFieldCap(metadata.capJavaField(meta.getFilterField()));
        meta.setTargetFieldCap(metadata.capJavaField(meta.getTargetField()));
        meta.setChildrenFieldCap(metadata.capJavaField(meta.getChildrenField()));
        meta.setKeyColumn(metadata.resolveColumnName(sourceColumns, meta.getKeyField()));
        meta.setParentColumn(metadata.resolveColumnName(sourceColumns, meta.getParentField()));
        meta.setTargetColumn(metadata.resolveColumnName(sourceColumns, meta.getTargetField()));
        if (sourceMeta != null) {
            sourceMeta.setTreeSource(true);
            sourceMeta.setTreeChildrenField(meta.getChildrenField());
        }
        return meta;
    }

    List<RelatedTableMeta> resolveInjectedRelatedTables(TreeCodegenMeta treeMeta,
                                                                List<RelatedTableMeta> relatedTables,
                                                                List<RelatedTableMeta> masterDetailChildren) {
        Map<String, RelatedTableMeta> result = new LinkedHashMap<>();
        if (treeMeta != null && treeMeta.isSeparateSource()) {
            for (RelatedTableMeta relatedTable : relatedTables) {
                if (StringUtils.equals(relatedTable.getClassName(), treeMeta.getClassName())) {
                    result.put(relatedTable.getClassName(), relatedTable);
                }
            }
        }
        for (RelatedTableMeta child : masterDetailChildren) {
            result.put(child.getClassName(), child);
        }
        return new ArrayList<>(result.values());
    }

    RelatedTableMeta findRelatedTable(List<RelatedTableMeta> relatedTables, String modelCode, String tableName) {
        if (relatedTables == null || relatedTables.isEmpty()) {
            return null;
        }
        for (RelatedTableMeta relatedTable : relatedTables) {
            if (StringUtils.isNotBlank(modelCode) && StringUtils.equals(modelCode, relatedTable.getModelCode())) {
                return relatedTable;
            }
            if (StringUtils.isNotBlank(tableName) && StringUtils.equals(tableName, relatedTable.getTableName())) {
                return relatedTable;
            }
        }
        return null;
    }
}
