package com.mdframe.forge.plugin.generator.service.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

/** Save and publication use the same definition/field checks. No client callback URL is accepted. */
@Component
@RequiredArgsConstructor
public class TaskBindingAuthoring {
    private final TaskBindingSnapshots snapshots;
    private final BusinessObjectMapper objects;
    private final BusinessBindingMapper bindings;
    private final BusinessTaskEngine engine;

    public void validate(AiBusinessBinding binding) {
        long tenant = TaskBindingSnapshots.tenant();
        require("OBJECT".equals(binding.getTargetType()) && binding.getTargetId() != null);
        AiBusinessObject object = objects.selectById(binding.getTargetId());
        require(object != null && Long.valueOf(tenant).equals(object.getTenantId())
                && object.getObjectCode().equals(binding.getTargetCode()) && Integer.valueOf(1).equals(object.getStatus()));
        var version = snapshots.latest(object.getConfigKey());
        validateSpec(binding, version.getModelSchema());
    }

    private TaskBindingSpec validateSpec(AiBusinessBinding binding, String schema) {
        TaskBindingSpec spec = snapshots.spec(binding.getBindingConfig());
        var definition = engine.definition(spec.definitionId());
        require(definition != null && LowcodeTaskSource.SOURCE_TYPE.equals(definition.sourceType()));
        spec.validate(snapshots.tree(schema));
        return spec;
    }

    /** Called before the normal publish snapshot is persisted; rollback restores the old options. */
    public void publish(AiCrudConfig config) {
        AiBusinessObject object = objects.selectByConfigKey(TaskBindingSnapshots.tenant(), config.getConfigKey());
        List<PublishedTaskBinding> published = new ArrayList<>();
        if (object != null) {
            List<AiBusinessBinding> candidates = bindings.selectList(new QueryWrapper<AiBusinessBinding>()
                    .eq("tenant_id", TaskBindingSnapshots.tenant()).eq("target_type", "OBJECT")
                    .eq("target_id", object.getId()).eq("binding_type", "TASK").eq("status", 1).orderByAsc("id"));
            require(candidates.size() <= 32);
            Set<String> instanceFields = new HashSet<>();
            for (AiBusinessBinding binding : candidates) {
                require(config.getRuntimeDatasourceId() == null
                        && (config.getRuntimeDatasourceCode() == null || config.getRuntimeDatasourceCode().isBlank()));
                TaskBindingSpec spec = validateSpec(binding, config.getModelSchema());
                require(instanceFields.add(spec.taskIdField()));
                published.add(new PublishedTaskBinding(binding.getId(), binding.getBindingKey(), spec));
            }
        }
        ObjectNode options = (ObjectNode) snapshots.tree(config.getOptions());
        options.set(TaskBindingSnapshots.OPTION, snapshots.tree("{\"items\":" + snapshots.write(published) + "}").get("items"));
        options.remove(TaskBindingSnapshots.LINK_FIELDS);
        config.setOptions(snapshots.write(options));
        retainPublishedLinks(config);
    }

    /** Also called after restoring an older published version, before persisting the rollback. */
    public void retainPublishedLinks(AiCrudConfig config) {
        var previous = snapshots.previousOptions(config);
        ObjectNode options = (ObjectNode) snapshots.tree(config.getOptions());
        Set<String> protectedFields = new LinkedHashSet<>();
        for (var source : List.of(previous, options)) {
            source.path(TaskBindingSnapshots.LINK_FIELDS).forEach(field -> protectedFields.add(field.asText()));
            source.path(TaskBindingSnapshots.OPTION).forEach(binding -> protectedFields.add(binding.path("spec").path("taskIdField").asText()));
        }
        require(protectedFields.size() <= 256);
        options.set(TaskBindingSnapshots.LINK_FIELDS, snapshots.tree("{\"items\":" + snapshots.write(protectedFields) + "}").get("items"));
        config.setOptions(snapshots.write(options));
    }
}
