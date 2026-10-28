package com.mdframe.forge.plugin.generator.service.task;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskSourceHandler;
import com.mdframe.forge.task.api.TaskState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

@Component
@RequiredArgsConstructor
public class LowcodeTaskSource implements TaskSourceHandler {
    public static final String SOURCE_TYPE = "lowcode.record";
    private final TaskBindingRuntime runtime;

    @Override
    public String sourceType() { return SOURCE_TYPE; }
    @Override
    public String transactionDataSource() { return "master"; }

    @Override
    public void authorize(String sourceKey, Actor actor, Operation operation) {
        require(actor.tenantId() == TaskBindingSnapshots.tenant() && actor.userId() == SessionHelper.getUserId());
        runtime.read(sourceKey, operation);
    }

    @Override
    public void validateCreation(TaskDefinition definition, BusinessTaskEngine.Create command, Actor actor) {
        var source = runtime.read(command.sourceKey(), Operation.CREATE);
        require(definition.id() == source.spec().definitionId()
                && SOURCE_TYPE.equals(definition.sourceType()) && command.equals(runtime.creation(source)));
    }

    @Override
    public void validateCompletion(BusinessTask task, String resultJson, Actor actor) {
        var source = runtime.read(task.sourceKey(), Operation.COMPLETE);
        linked(source, task);
        runtime.result(source, resultJson);
    }

    @Override
    public void onTerminal(BusinessTask task, Actor actor) {
        var source = runtime.read(task.sourceKey(), task.state() == TaskState.COMPLETED ? Operation.COMPLETE : Operation.CANCEL);
        linked(source, task);
        Map<String, Object> fields = new LinkedHashMap<>();
        if (task.state() == TaskState.COMPLETED) { fields.putAll(runtime.result(source, task.resultJson())); }
        fields.put(source.spec().stateField(), task.state() == TaskState.COMPLETED
                ? source.spec().completedValue() : source.spec().cancelledValue());
        runtime.write(source, fields, task.id());
    }

    private void linked(TaskBindingRuntime.Source source, BusinessTask task) {
        require(source.spec().definitionId() == task.definitionId()
                && String.valueOf(task.id()).equals(String.valueOf(source.record().get(source.spec().taskIdField()))));
    }
}
