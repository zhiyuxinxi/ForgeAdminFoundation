package com.mdframe.forge.plugin.task.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.task.api.PeriodicTaskEngine;
import com.mdframe.forge.task.api.TaskSourceHandler.Actor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

@Service
public class TaskScheduleService implements PeriodicTaskEngine {
    private final TaskScheduleStore store;
    private final TaskScheduleRules rules;
    private final TaskPeriodGenerator generator;
    private final TransactionTemplate transactions;

    public TaskScheduleService(TaskScheduleStore store, TaskScheduleRules rules,
                               TaskPeriodGenerator generator, PlatformTransactionManager manager) {
        this.store = store;
        this.rules = rules;
        this.generator = generator;
        transactions = new TransactionTemplate(manager);
    }

    @Override
    public Schedule define(Define command) {
        Actor actor = TaskInputs.actor();
        rules.validate(command);
        Long org = SessionHelper.getActiveOrgId();
        if (org == null || org <= 0) {
            throw TaskMessages.error(403, "period.identity.invalid");
        }
        return transactions.execute(status -> {
            TaskScheduleRow row = new TaskScheduleRow();
            row.setId(IdWorker.getId());
            row.setTenantId(actor.tenantId());
            row.setCode(command.code());
            row.setRuleVersion(command.version());
            row.setRuleJson(rules.write(command));
            row.setActorId(actor.userId());
            row.setActorOrgId(org);
            row.setNextIndex(0);
            row.setNextGenerateAt(TaskCalendar.at(command.calendar(), 0).generateAtUtc());
            row.setNextAttemptAt(row.getNextGenerateAt());
            TaskScheduleRow saved = store.insert(row);
            if (!rules.read(saved).equals(command) || !saved.getActorId().equals(actor.userId())
                    || !saved.getActorOrgId().equals(org)) {
                throw TaskMessages.error(409, "idempotency.conflict");
            }
            return rules.view(saved);
        });
    }

    @Override
    public Schedule get(long id) {
        TaskScheduleRow row = require(id, false);
        rules.authorize(rules.read(row));
        return rules.view(row);
    }

    @Override
    public List<Schedule> list(Long after, int limit) {
        Actor actor = TaskInputs.actor();
        TaskInputs.permission("task:schedule:write");
        TaskInputs.page(after, limit);
        return store.list(actor.tenantId(), after, limit).stream().map(row -> {
            rules.authorize(rules.read(row));
            return rules.view(row);
        }).toList();
    }

    @Override
    public Schedule enable(long id, boolean enabled) {
        try {
            return transactions.execute(status -> {
                TaskScheduleRow row = require(id, true);
                rules.authorize(rules.read(row));
                if (enabled && row.getActorId() != TaskInputs.actor().userId()) {
                    throw TaskMessages.error(403, "permission.denied");
                }
                if (enabled && row.getNextGenerateAt() == null) {
                    throw TaskMessages.error(409, "period.exhausted");
                }
                row.setActiveSlot(enabled ? 1 : null);
                row.setNextAttemptAt(row.getNextGenerateAt());
                store.update(row);
                return rules.view(row);
            });
        } catch (DuplicateKeyException exception) {
            throw TaskMessages.error(409, "period.version.active");
        }
    }

    @Override
    public List<Occurrence> preview(Calendar calendar, int fromIndex, int count) {
        TaskInputs.actor();
        TaskInputs.permission("task:schedule:write");
        return TaskCalendar.preview(calendar, fromIndex, count);
    }

    @Override
    public Run generate(long id, int limit) {
        return generator.generate(id, limit);
    }

    private TaskScheduleRow require(long id, boolean lock) {
        Actor actor = TaskInputs.actor();
        TaskInputs.positive(id);
        TaskScheduleRow row = store.get(actor.tenantId(), id, lock);
        if (row == null) {
            throw TaskMessages.error(404, "period.missing");
        }
        return row;
    }
}
