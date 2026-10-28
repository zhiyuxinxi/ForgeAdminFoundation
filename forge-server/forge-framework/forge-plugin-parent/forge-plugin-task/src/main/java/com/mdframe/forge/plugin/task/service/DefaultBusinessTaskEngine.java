package com.mdframe.forge.plugin.task.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.task.entity.BusinessTaskRow;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskSourceHandler;
import com.mdframe.forge.task.api.TaskSourceHandler.Actor;
import com.mdframe.forge.task.api.TaskSourceHandler.Operation;
import com.mdframe.forge.task.api.TaskState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/** Unique persistent owner. The real transaction manager owns task and source write-back atomically. */
@Service
public class DefaultBusinessTaskEngine implements BusinessTaskEngine {
    private final TaskPersistence store;
    private final TaskSources sources;
    private final TaskCatalog catalog;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final TaskPeriodConstraints periods;

    public DefaultBusinessTaskEngine(TaskPersistence store, TaskSources sources, TaskCatalog catalog,
                                     ObjectMapper json, PlatformTransactionManager transactionManager,
                                     TaskPeriodConstraints periods) {
        this.store = store;
        this.sources = sources;
        this.catalog = catalog;
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
        this.periods = periods;
    }

    @Override
    public TaskDefinition define(Define command) {
        return catalog.define(command);
    }

    @Override
    public TaskDefinition definition(long id) {
        return catalog.definition(id);
    }

    public List<TaskDefinition> definitions(String sourceType, Long afterId, int limit) {
        return catalog.definitions(sourceType, afterId, limit);
    }

    @Override
    public BusinessTask create(Create command) {
        return catalog.create(command);
    }

    @Override
    public BusinessTask dispatch(long taskId) {
        return advance(taskId, TaskState.DRAFT, TaskState.ASSIGNED, Operation.DISPATCH, false);
    }

    @Override
    public BusinessTask start(long taskId) {
        return advance(taskId, TaskState.ASSIGNED, TaskState.IN_PROGRESS, Operation.START, true);
    }

    private BusinessTask advance(long taskId, TaskState before, TaskState after, Operation operation, boolean ownOnly) {
        Actor actor = TaskInputs.actor();
        return transaction(() -> {
            BusinessTaskRow row = load(taskId, actor, true, operation);
            if (ownOnly) {
                requireAssignee(row, actor);
                periods.start(actor.tenantId(), taskId);
            } else {
                catalog.member(row.getAssigneeId());
            }
            if (after.name().equals(row.getState())) {
                return TaskViews.task(row);
            }
            requireState(row, before);
            row.setState(after.name());
            row.setUpdateBy(actor.userId());
            store.transition(row);
            return TaskViews.task(row);
        });
    }

    @Override
    public BusinessTask complete(long taskId, Complete command) {
        Actor actor = TaskInputs.actor();
        if (command == null) {
            throw TaskMessages.error(400, "input.invalid");
        }
        TaskInputs.text(command.requestKey(), 96);
        JsonNode result = TaskInputs.result(json, command.resultJson());
        return transaction(() -> {
            BusinessTaskRow row = load(taskId, actor, true, Operation.COMPLETE);
            requireAssignee(row, actor);
            if (TaskState.COMPLETED.name().equals(row.getState())) {
                if (command.requestKey().equals(row.getCompletionKey())
                        && result.equals(TaskInputs.result(json, row.getResultJson()))) {
                    return TaskViews.task(row);
                }
                throw TaskMessages.error(409, "idempotency.conflict");
            }
            requireState(row, TaskState.IN_PROGRESS);
            periods.completion(actor.tenantId(), taskId);
            TaskSourceHandler source = sources.require(row.getSourceType());
            source.validateCompletion(TaskViews.task(row), result.toString(), actor);
            row.setState(TaskState.COMPLETED.name());
            row.setResultJson(result.toString());
            row.setCompletionKey(command.requestKey());
            row.setCompletedAt(LocalDateTime.now());
            row.setUpdateBy(actor.userId());
            store.transition(row);
            BusinessTask completed = TaskViews.task(row);
            source.onTerminal(completed, actor);
            return completed;
        });
    }

    @Override
    public BusinessTask cancel(long taskId, String reason) {
        Actor actor = TaskInputs.actor();
        TaskInputs.text(reason, 500);
        return transaction(() -> {
            BusinessTaskRow row = load(taskId, actor, true, Operation.CANCEL);
            if (TaskState.CANCELLED.name().equals(row.getState())) {
                if (reason.equals(row.getCancellationReason())) {
                    return TaskViews.task(row);
                }
                throw TaskMessages.error(409, "idempotency.conflict");
            }
            if (TaskState.COMPLETED.name().equals(row.getState())) {
                throw TaskMessages.error(409, "state.conflict");
            }
            row.setState(TaskState.CANCELLED.name());
            row.setCancellationReason(reason);
            row.setUpdateBy(actor.userId());
            store.transition(row);
            BusinessTask cancelled = TaskViews.task(row);
            sources.require(row.getSourceType()).onTerminal(cancelled, actor);
            return cancelled;
        });
    }

    @Override
    public BusinessTask get(long taskId) {
        Actor actor = TaskInputs.actor();
        return TaskViews.task(load(taskId, actor, false, Operation.READ));
    }

    @Override
    public List<BusinessTask> assigned(TaskState state, Long afterId, int limit) {
        Actor actor = TaskInputs.actor();
        TaskInputs.page(afterId, limit);
        return store.assigned(actor.tenantId(), actor.userId(), state == null ? null : state.name(), afterId, limit)
                .stream().map(row -> {
                    sources.require(row.getSourceType()).authorize(row.getSourceKey(), actor, Operation.READ);
                    return TaskViews.task(row);
                }).toList();
    }

    private BusinessTaskRow load(long id, Actor actor, boolean lock, Operation operation) {
        TaskInputs.positive(id);
        BusinessTaskRow row = store.task(actor.tenantId(), id, lock);
        if (row == null) {
            throw TaskMessages.error(404, "task.missing");
        }
        // Draft obligations are visible only through their authorized source, not assigned inboxes.
        sources.require(row.getSourceType()).authorize(row.getSourceKey(), actor, operation);
        return row;
    }

    private void requireAssignee(BusinessTaskRow row, Actor actor) {
        if (row.getAssigneeId() != actor.userId()) {
            throw TaskMessages.error(403, "permission.denied");
        }
        catalog.member(actor.userId());
    }

    private void requireState(BusinessTaskRow row, TaskState expected) {
        if (!expected.name().equals(row.getState())) {
            throw TaskMessages.error(409, "state.conflict");
        }
    }

    private <T> T transaction(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }
}
