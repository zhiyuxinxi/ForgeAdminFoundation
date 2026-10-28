package com.mdframe.forge.plugin.task.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.task.entity.TaskOccurrenceRow;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Assignment;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Occurrence;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Run;
import com.mdframe.forge.task.api.PeriodicTaskEngine.Schedule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Allocation commits once; emission and cursor movement are one ordinary master transaction. */
@Component
public class TaskPeriodGenerator {
    private final TaskScheduleStore store;
    private final TaskScheduleRules rules;
    private final TaskPeriodAllocation allocation;
    private final BusinessTaskEngine tasks;
    private final TransactionTemplate transactions;

    public TaskPeriodGenerator(TaskScheduleStore store, TaskScheduleRules rules,
                               TaskPeriodAllocation allocation, BusinessTaskEngine tasks,
                               PlatformTransactionManager manager) {
        this.store = store;
        this.rules = rules;
        this.allocation = allocation;
        this.tasks = tasks;
        transactions = new TransactionTemplate(manager);
    }

    Run generate(long id, int limit) {
        TaskInputs.actor();
        TaskInputs.positive(id);
        if (limit < 1 || limit > 60) {
            throw TaskMessages.error(400, "period.invalid");
        }
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            throw TaskMessages.error(409, "period.outer.transaction");
        }
        int count = 0;
        for (int attempt = 0; attempt < limit; attempt++) {
            TaskOccurrenceRow snapshot = transactions.execute(status -> prepare(id));
            if (snapshot == null) {
                break;
            }
            count += transactions.execute(status -> emit(id, snapshot));
        }
        TaskScheduleRow row = require(id);
        return new Run(id, count, row.getNextIndex(), row.getNextGenerateAt() == null);
    }

    private TaskOccurrenceRow prepare(long id) {
        TaskScheduleRow row = require(id);
        Schedule schedule = rules.view(row);
        rules.authorize(schedule.rule());
        if (!schedule.enabled() || row.getNextGenerateAt() == null
                || row.getNextGenerateAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            return null;
        }
        TaskOccurrenceRow existing = store.occurrence(row.getTenantId(), id, row.getNextIndex());
        if (existing != null) {
            return existing;
        }
        Occurrence date = TaskCalendar.at(schedule.rule().calendar(), row.getNextIndex());
        TaskOccurrenceRow snapshot = new TaskOccurrenceRow();
        snapshot.setId(IdWorker.getId());
        snapshot.setTenantId(row.getTenantId());
        snapshot.setScheduleId(id);
        snapshot.setOccurrenceIndex(date.index());
        snapshot.setStartsAt(date.startsAtUtc());
        snapshot.setDueAt(date.dueAtUtc());
        snapshot.setAllowLate(schedule.rule().allowLate());
        snapshot.setAssignmentJson(rules.write(allocation.resolve(schedule.rule().owner())));
        snapshot.setState("PENDING");
        store.insertOccurrence(snapshot);
        return snapshot;
    }

    private int emit(long id, TaskOccurrenceRow snapshot) {
        TaskScheduleRow row = require(id);
        Schedule schedule = rules.view(row);
        var owner = rules.authorize(schedule.rule());
        if (!schedule.enabled() || row.getNextIndex() != snapshot.getOccurrenceIndex().intValue()) {
            return 0;
        }
        List<Assignment> assignments = rules.assignments(snapshot.getAssignmentJson());
        Occurrence date = TaskCalendar.at(schedule.rule().calendar(), snapshot.getOccurrenceIndex());
        String periodKey = "period:" + id + ":" + date.index();
        String sourceKey = owner.prepareOccurrence(schedule, periodKey, date, assignments, TaskInputs.actor());
        TaskInputs.text(sourceKey, 128);
        try (var ignored = TaskPeriodInvocation.open(snapshot)) {
            for (Assignment member : assignments) {
                BusinessTask task = tasks.create(new BusinessTaskEngine.Create(schedule.rule().definitionId(),
                        sourceKey, periodKey, member.userId(), schedule.rule().title(), date.dueAtUtc()));
                store.link(row.getTenantId(), snapshot.getId(), task.id(), member.userId());
                tasks.dispatch(task.id());
            }
        }
        store.generated(row.getTenantId(), snapshot.getId());
        row.setNextIndex(row.getNextIndex() + 1);
        Occurrence next = TaskCalendar.at(schedule.rule().calendar(), row.getNextIndex());
        row.setNextGenerateAt(next == null ? null : next.generateAtUtc());
        row.setNextAttemptAt(row.getNextGenerateAt());
        row.setLastFailure(null);
        if (next == null) {
            row.setActiveSlot(null);
        }
        store.update(row);
        return 1;
    }

    private TaskScheduleRow require(long id) {
        var actor = TaskInputs.actor();
        TaskScheduleRow row = store.get(actor.tenantId(), id, true);
        if (row == null) {
            throw TaskMessages.error(404, "period.missing");
        }
        if (row.getActorId() != actor.userId()
                || !Objects.equals(row.getActorOrgId(), SessionHelper.getActiveOrgId())) {
            throw TaskMessages.error(403, "period.identity.invalid");
        }
        return row;
    }
}
