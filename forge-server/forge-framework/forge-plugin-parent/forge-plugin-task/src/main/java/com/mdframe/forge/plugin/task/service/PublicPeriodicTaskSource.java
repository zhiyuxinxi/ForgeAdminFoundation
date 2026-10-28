package com.mdframe.forge.plugin.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.task.entity.TaskOccurrenceRow;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine.Create;
import com.mdframe.forge.task.api.PeriodicTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Assignment;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskPeriodSourceHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;

/** Built-in public recurring confirmation tasks. Customer forms remain separate source owners. */
@Component
@RequiredArgsConstructor
public class PublicPeriodicTaskSource implements TaskPeriodSourceHandler {
    private final TaskScheduleStore store;
    private final ObjectMapper json;

    @Override
    public String sourceType() { return "task.periodic"; }

    @Override
    public String transactionDataSource() { return "master"; }

    @Override
    public void validateSchedule(TaskDefinition definition, PeriodicTaskEngine.Define rule, Actor actor) {
        TaskInputs.permission("task:schedule:write");
        if (!sourceType().equals(definition.sourceType()) || !"self".equals(rule.sourceKey())) {
            throw TaskMessages.error(400, "period.invalid");
        }
    }

    @Override
    public String prepareOccurrence(PeriodicTaskEngine.Schedule schedule, String periodKey,
                                    PeriodicTaskEngine.Occurrence occurrence, List<Assignment> members, Actor actor) {
        return schedule.id() + ":" + occurrence.index();
    }

    @Override
    public void authorize(String sourceKey, Actor actor, Operation operation) {
        TaskOccurrenceRow occurrence = occurrence(sourceKey, actor);
        if (operation == Operation.CREATE || operation == Operation.DISPATCH || operation == Operation.CANCEL) {
            TaskInputs.permission("task:schedule:write");
            TaskScheduleRow schedule = store.get(actor.tenantId(), occurrence.getScheduleId(), false);
            if (schedule == null || schedule.getActorId() != actor.userId()) {
                throw TaskMessages.error(403, "permission.denied");
            }
        } else if (!assigned(occurrence, actor.userId())) {
            TaskInputs.permission("task:schedule:write");
        }
    }

    @Override
    public void validateCreation(TaskDefinition definition, Create command, Actor actor) {
        TaskOccurrenceRow occurrence = occurrence(command.sourceKey(), actor);
        TaskScheduleRow schedule = store.get(actor.tenantId(), occurrence.getScheduleId(), false);
        PeriodicTaskEngine.Define rule = rule(schedule);
        if (!TaskPeriodInvocation.matches(occurrence) || !"PENDING".equals(occurrence.getState())
                || rule.definitionId() != definition.id() || !rule.title().equals(command.title())
                || !occurrence.getDueAt().equals(command.dueAt())
                || !command.occurrenceKey().equals("period:" + schedule.getId() + ":" + occurrence.getOccurrenceIndex())
                || !assigned(occurrence, command.assigneeId())) {
            throw TaskMessages.error(403, "period.creation.denied");
        }
    }

    @Override
    public void validateCompletion(BusinessTask task, String resultJson, Actor actor) {
        if (TaskInputs.result(json, resultJson).size() != 0) {
            throw TaskMessages.error(400, "period.confirmation.only");
        }
    }

    @Override
    public void onTerminal(BusinessTask task, Actor actor) {
        // Public confirmation result is owned and persisted by the ordinary task itself.
    }

    private TaskOccurrenceRow occurrence(String sourceKey, Actor actor) {
        if (sourceKey == null || !sourceKey.matches("[1-9][0-9]{0,18}:(0|[1-9][0-9]{0,8})")) {
            throw TaskMessages.error(400, "period.invalid");
        }
        try {
            String[] parts = sourceKey.split(":");
            TaskOccurrenceRow row = store.occurrence(actor.tenantId(), Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
            if (row == null) {
                throw TaskMessages.error(404, "period.missing");
            }
            return row;
        } catch (NumberFormatException exception) {
            throw TaskMessages.error(400, "period.invalid");
        }
    }

    private boolean assigned(TaskOccurrenceRow row, long user) {
        try {
            List<Assignment> members = json.readValue(row.getAssignmentJson(), new TypeReference<List<Assignment>>() { });
            return members.stream().anyMatch(member -> member.userId() == user);
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(409, "period.snapshot.invalid");
        }
    }

    private PeriodicTaskEngine.Define rule(TaskScheduleRow row) {
        try {
            return json.readValue(row.getRuleJson(), PeriodicTaskEngine.Define.class);
        } catch (JsonProcessingException exception) {
            throw TaskMessages.error(409, "period.snapshot.invalid");
        }
    }
}
