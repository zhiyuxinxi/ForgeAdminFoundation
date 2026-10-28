package com.mdframe.forge.plugin.task.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.task.entity.BusinessTaskRow;
import com.mdframe.forge.plugin.task.entity.TaskDefinitionRow;
import com.mdframe.forge.starter.core.identity.TenantMember;
import com.mdframe.forge.starter.core.identity.TenantMemberDirectory;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine.Create;
import com.mdframe.forge.task.api.BusinessTaskEngine.Define;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskSourceHandler.Actor;
import com.mdframe.forge.task.api.TaskSourceHandler.Operation;
import com.mdframe.forge.task.api.TaskState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Immutable definition versions and idempotent obligation generation, before execution starts. */
@Component
public class TaskCatalog {
    private final TaskPersistence store;
    private final TaskSources sources;
    private final TenantMemberDirectory members;
    private final TransactionTemplate transactions;

    public TaskCatalog(TaskPersistence store, TaskSources sources, TenantMemberDirectory members,
                       PlatformTransactionManager transactionManager) {
        this.store = store;
        this.sources = sources;
        this.members = members;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public TaskDefinition define(Define command) {
        Actor actor = TaskInputs.actor();
        TaskInputs.permission("task:definition:write");
        if (command == null) {
            throw TaskMessages.error(400, "input.invalid");
        }
        TaskInputs.code(command.code());
        TaskInputs.code(command.sourceType());
        TaskInputs.positive(command.version());
        TaskInputs.text(command.title(), 200);
        sources.require(command.sourceType());
        return transaction(() -> {
            TaskDefinitionRow row = new TaskDefinitionRow();
            row.setId(IdWorker.getId());
            row.setTenantId(actor.tenantId());
            row.setCode(command.code());
            row.setDefinitionVersion(command.version());
            row.setTitle(command.title());
            row.setSourceType(command.sourceType());
            row.setCreateBy(actor.userId());
            TaskDefinitionRow saved = store.define(row);
            if (!saved.getTitle().equals(command.title())) {
                throw TaskMessages.error(409, "idempotency.conflict");
            }
            return TaskViews.definition(saved);
        });
    }

    public TaskDefinition definition(long id) {
        Actor actor = TaskInputs.actor();
        TaskInputs.positive(id);
        TaskDefinitionRow row = store.definition(actor.tenantId(), id);
        if (row == null) {
            throw TaskMessages.error(404, "definition.missing");
        }
        return TaskViews.definition(row);
    }

    public List<TaskDefinition> definitions(String sourceType, Long afterId, int limit) {
        Actor actor = TaskInputs.actor();
        TaskInputs.code(sourceType);
        TaskInputs.page(afterId, limit);
        sources.require(sourceType);
        return store.definitions(actor.tenantId(), sourceType, afterId, limit).stream()
                .map(TaskViews::definition).toList();
    }

    public BusinessTask create(Create command) {
        Actor actor = TaskInputs.actor();
        if (command == null) {
            throw TaskMessages.error(400, "input.invalid");
        }
        TaskInputs.positive(command.definitionId());
        TaskInputs.positive(command.assigneeId());
        TaskInputs.text(command.sourceKey(), 128);
        TaskInputs.text(command.occurrenceKey(), 96);
        TaskInputs.text(command.title(), 200);
        if (command.dueAt() != null && command.dueAt().getNano() % 1000 != 0) {
            throw TaskMessages.error(400, "input.invalid");
        }
        return transaction(() -> {
            TaskDefinitionRow definition = store.definition(actor.tenantId(), command.definitionId());
            if (definition == null) {
                throw TaskMessages.error(404, "definition.missing");
            }
            sources.require(definition.getSourceType()).authorize(command.sourceKey(), actor, Operation.CREATE);
            sources.require(definition.getSourceType()).validateCreation(TaskViews.definition(definition), command, actor);
            TenantMember assignee = member(command.assigneeId());
            BusinessTaskRow row = new BusinessTaskRow();
            row.setId(IdWorker.getId());
            row.setTenantId(actor.tenantId());
            row.setDefinitionId(command.definitionId());
            row.setSourceType(definition.getSourceType());
            row.setSourceKey(command.sourceKey());
            row.setOccurrenceKey(command.occurrenceKey());
            row.setAssigneeId(command.assigneeId());
            row.setAssigneeName(assignee.displayName());
            row.setTitle(command.title());
            row.setState(TaskState.DRAFT.name());
            row.setDueAt(command.dueAt());
            row.setCreateBy(actor.userId());
            row.setUpdateBy(actor.userId());
            BusinessTaskRow saved = store.create(row);
            if (!saved.getTitle().equals(command.title()) || !Objects.equals(saved.getDueAt(), command.dueAt())) {
                throw TaskMessages.error(409, "idempotency.conflict");
            }
            return TaskViews.task(saved);
        });
    }

    TenantMember member(long userId) {
        return members.findActiveMembers(List.of(userId), null).stream()
                .filter(member -> member.userId() == userId).findFirst()
                .orElseThrow(() -> TaskMessages.error(400, "assignee.invalid"));
    }

    private <T> T transaction(Supplier<T> action) {
        return transactions.execute(status -> action.get());
    }
}
