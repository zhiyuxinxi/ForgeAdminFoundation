package com.mdframe.forge.plugin.generator.service.runtime.query;

import com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.runtime.child.RuntimeChildRelation;
import com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import org.apache.commons.lang3.StringUtils;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.service.runtime.value.RuntimeFieldMetadata;
import com.mdframe.forge.plugin.generator.service.runtime.write.RuntimeWriteFieldMapping;
import static com.mdframe.forge.plugin.generator.service.runtime.child.ChildRelationResolver.mergeRelations;

/** Plans published model joins without querying records, authorizing writes or generating SQL. */
@lombok.extern.slf4j.Slf4j
@lombok.RequiredArgsConstructor
public final class RuntimeJoinPlanner {
    private final DynamicCrudRepository repository;
    private final ObjectMapper objectMapper;
    private final RuntimeFieldMetadata metadata;
    private final RuntimeWriteFieldMapping writeMapping;
    private final ChildRelationResolver childRelationResolver;

    public RuntimeJoinContext plan(LowcodeRuntimeConfig config) {
        if (!"LOWCODE".equals(config.getBuildMode())
                || StringUtils.isBlank(config.getPageSchema())
                || StringUtils.isBlank(config.getModelSchema())) {
            return null;
        }
        LowcodePageSchema pageSchema = readPageSchema(config);
        if (pageSchema == null || pageSchema.getModelRefs() == null || pageSchema.getModelRefs().size() <= 1) {
            return null;
        }
        LowcodeModelSchema modelSchema = metadata.model(config);
        if (modelSchema == null) {
            return null;
        }

        LowcodePageModelRef primaryRef = pageSchema.getModelRefs().stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getPrimary()))
                .findFirst()
                .orElse(pageSchema.getModelRefs().get(0));
        String primaryModelCode = StringUtils.defaultIfBlank(pageSchema.getPrimaryModelCode(), primaryRef.getModelCode());
        if (StringUtils.isBlank(primaryModelCode)) {
            primaryModelCode = modelSchema.getObject() == null ? null : modelSchema.getObject().getCode();
        }
        if (StringUtils.isBlank(primaryModelCode)) {
            return null;
        }

        RuntimeJoinProjection projection = new RuntimeJoinProjection(repository);
        projection.addPrimaryRuntimeFields(config.getTableName(), primaryModelCode);

        List<RuntimeChildRelation> childRelations = new ArrayList<>();
        List<DynamicCrudRepository.JoinSpec> joins = new ArrayList<>();
        List<LowcodeRelationSchema> primaryRelations = mergeRelations(modelSchema.getRelations(), primaryRef.getRelations());
        Map<String, String> primaryColumnMapping = writeMapping.columns(config, config.getTableName());
        int aliasIndex = 1;
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            if (StringUtils.isBlank(ref.getTableName()) || !repository.tableExists(ref.getTableName())) {
                log.warn("[DynamicCrudService] 引用模型缺少可用表名，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            ChildRelationResolver.ChildResolution resolved = childRelationResolver.buildChildRelation(primaryModelCode, ref, primaryRelations,
                    "t" + aliasIndex, primaryColumnMapping);
            if (resolved == null) {
                log.warn("[DynamicCrudService] 未找到引用模型关联关系，跳过左连接, configKey={}, modelCode={}",
                        config.getConfigKey(), ref.getModelCode());
                continue;
            }
            RuntimeChildRelation relation = resolved.relation();
            projection.addChildRuntimeProjections(resolved);
            childRelations.add(relation);
            joins.add(new DynamicCrudRepository.JoinSpec(
                    relation.tableName(), relation.tableAlias(), relation.childFkColumn(), relation.mainColumn()));
            projection.addRelationDisplayField(modelSchema, primaryRelations, ref, relation);
            aliasIndex++;
        }

        if (childRelations.isEmpty()) {
            return null;
        }
        return projection.finish(childRelations, joins, primaryColumnMapping);
    }

    private LowcodePageSchema readPageSchema(LowcodeRuntimeConfig config) {
        try {
            return objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
        } catch (Exception e) {
            log.warn("[DynamicCrudService] 解析pageSchema失败, configKey={}", config.getConfigKey(), e);
            return null;
        }
    }
}
