package com.mdframe.forge.plugin.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Assignment;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Define;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Schedule;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskPeriodSourceHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskScheduleRules {
    private final ObjectMapper json;
    private final TaskSources sources;
    private final TaskCatalog catalog;

    void validate(Define rule) {
        if (rule == null || rule.owner() == null || rule.owner().type() == null) {
            throw TaskMessages.error(400, "period.invalid");
        }
        TaskInputs.code(rule.code());
        TaskInputs.positive(rule.version());
        TaskInputs.positive(rule.definitionId());
        TaskInputs.positive(rule.owner().id());
        TaskInputs.text(rule.sourceKey(), 128);
        TaskInputs.text(rule.title(), 200);
        TaskCalendar.validate(rule.calendar());
        authorize(rule);
    }

    TaskPeriodSourceHandler authorize(Define rule) {
        TaskInputs.permission("task:schedule:write");
        TaskDefinition definition = catalog.definition(rule.definitionId());
        if (!(sources.require(definition.sourceType()) instanceof TaskPeriodSourceHandler owner)) {
            throw TaskMessages.error(409, "period.source.unsupported");
        }
        owner.validateSchedule(definition, rule, TaskInputs.actor());
        return owner;
    }

    Define read(TaskScheduleRow row) {
        try {
            return json.readValue(row.getRuleJson(), Define.class);
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(409, "period.snapshot.invalid");
        }
    }

    Schedule view(TaskScheduleRow row) {
        return new Schedule(row.getId(), read(row), Integer.valueOf(1).equals(row.getActiveSlot()), row.getNextIndex());
    }

    String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(400, "period.invalid");
        }
    }

    List<Assignment> assignments(String value) {
        try {
            return json.readValue(value, new TypeReference<List<Assignment>>() { });
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(409, "period.snapshot.invalid");
        }
    }
}
