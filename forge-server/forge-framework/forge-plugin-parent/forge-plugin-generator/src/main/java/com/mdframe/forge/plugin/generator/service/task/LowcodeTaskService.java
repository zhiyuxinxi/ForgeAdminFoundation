package com.mdframe.forge.plugin.generator.service.task;

import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskSourceHandler.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

/** One source record and binding generates one task instance; all mutations share master transaction. */
@Service
@RequiredArgsConstructor
public class LowcodeTaskService {
    private final TaskBindingSnapshots snapshots;
    private final TaskBindingRuntime runtime;
    private final BusinessTaskEngine engine;

    public List<PublishedTaskBinding> bindings(String configKey) {
        var version = snapshots.latest(configKey);
        require(com.mdframe.forge.starter.core.session.SessionHelper.hasPermission(
                "ai:business:" + version.getObjectCode() + ":query"));
        return snapshots.bindings(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessTask create(String configKey, String bindingKey, String recordId) {
        var version = snapshots.latest(configKey);
        var binding = snapshots.bindings(version).stream().filter(item -> item.key().equals(bindingKey)).findFirst()
                .orElseThrow(TaskBindingSpec::invalid);
        String key = TaskBindingRuntime.key(version.getId(), binding.id(), recordId);
        // READ first permits an idempotent retry after completion. Actual creation locks the real record below.
        var source = runtime.read(key, Operation.READ);
        Object existing = source.record().get(binding.spec().taskIdField());
        if (existing != null) { return linked(source, existing); }
        source = runtime.read(key, Operation.CREATE);
        existing = source.record().get(binding.spec().taskIdField());
        if (existing != null) { return linked(source, existing); }
        BusinessTask task = engine.create(runtime.creation(source));
        runtime.write(source, Map.of(binding.spec().taskIdField(), task.id()), null);
        return engine.dispatch(task.id());
    }

    public BusinessTask linked(String configKey, String bindingKey, String recordId) {
        var version = snapshots.latest(configKey);
        var binding = snapshots.bindings(version).stream().filter(item -> item.key().equals(bindingKey)).findFirst()
                .orElseThrow(TaskBindingSpec::invalid);
        var source = runtime.read(TaskBindingRuntime.key(version.getId(), binding.id(), recordId), Operation.READ);
        Object id = source.record().get(binding.spec().taskIdField());
        require(id != null);
        return linked(source, id);
    }

    private BusinessTask linked(TaskBindingRuntime.Source source, Object id) {
        try {
            BusinessTask task = engine.get(Long.parseLong(String.valueOf(id)));
            var original = runtime.read(task.sourceKey(), Operation.READ);
            require(task.sourceType().equals(LowcodeTaskSource.SOURCE_TYPE)
                    && original.configKey().equals(source.configKey()) && original.recordId().equals(source.recordId())
                    && original.binding().key().equals(source.binding().key()));
            return task;
        } catch (IllegalArgumentException exception) { throw TaskBindingSpec.invalid(); }
    }
}
