package com.mdframe.forge.plugin.generator.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.GeneratedLowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.*;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.RelatedTableMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy.TreeCodegenMeta;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenMetadata.Model;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenRelations.Relations;
import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenContext.PreparedContext;

@Component
@RequiredArgsConstructor
public class VelocityCodegenStrategy implements CodegenStrategy {
    private final GenTableColumnMapper genTableColumnMapper;
    private final ObjectMapper objectMapper;
    private final GeneratedLowcodeRuntimeConfigBuilder generatedRuntimeConfigBuilder;
    private final LowcodeProtocolSnapshotBuilder protocolSnapshotBuilder;
    private final ObjectProvider<LowcodeStaticCodegenContributor> staticCodegenContributors;

    @Override
    public boolean supports(String codegenType) {
        // TEMPLATE 类型，或 null/空（向前兼容，默认走模板生成）
        return codegenType == null || "TEMPLATE".equalsIgnoreCase(codegenType);
    }

    @Override
    public Map<String, String> generate(AiCrudConfig config, AiPageTemplate template) throws Exception {
        VelocityCodegenConfiguration configuration = new VelocityCodegenConfiguration(objectMapper);
        VelocityCodegenMetadata metadata = new VelocityCodegenMetadata(genTableColumnMapper, objectMapper, configuration);
        Model model = metadata.prepare(config, generatedRuntimeConfigBuilder);
        Map<String, Object> annotations = new VelocityCodegenAnnotations(objectMapper)
                .resolveAnnotationFlags(model.config(), model.table().getColumns());
        Relations relations = new VelocityCodegenRelations(configuration, metadata).prepare(model);
        PreparedContext context = new VelocityCodegenContext(objectMapper, configuration)
                .prepare(model, relations, annotations, staticCodegenContributors);
        return new VelocityCodegenArtifacts(objectMapper, protocolSnapshotBuilder, configuration)
                .render(model, relations, context);
    }

    @Data
    public static class RelatedTableMeta {
        private String modelCode;
        private String key;
        private String modelName;
        private String tableName;
        private String className;
        private String variableName;
        private String mapperVarName;
        private GenTable table;
        private List<GenTableColumn> columns = new ArrayList<>();
        private GenTableColumn pkColumn;
        private boolean treeSource;
        private String treeChildrenField;
        private boolean masterDetailChild;
        private String childKey;
        private String childFkField;
        private String childFkFieldCap;
        private String childFkColumn;
        private String mainField;
        private String mainFieldCap;
        private String mainColumn;
        private boolean hasLogicDelete;
        private boolean uniqueDeleteMarker;
    }

    @Data
    public static class TreeCodegenMeta {
        private boolean separateSource;
        private String sourceModelCode;
        private String sourceTableName;
        private String className;
        private String mapperVarName;
        private String keyField;
        private String keyFieldCap;
        private String keyColumn;
        private String parentField;
        private String parentFieldCap;
        private String parentColumn;
        private String labelField;
        private String labelFieldCap;
        private String filterField;
        private String filterFieldCap;
        private String targetField;
        private String targetFieldCap;
        private String targetColumn;
        private String childrenField;
        private String childrenFieldCap;
        private String loadMode;
    }
}
