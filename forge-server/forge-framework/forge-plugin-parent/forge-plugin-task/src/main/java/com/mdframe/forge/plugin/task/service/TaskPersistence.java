package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.plugin.task.entity.BusinessTaskRow;
import com.mdframe.forge.plugin.task.entity.TaskDefinitionRow;
import com.mdframe.forge.plugin.task.mapper.BusinessTaskMapper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Internal task tables use explicit tenant/assignee predicates and source-owner authorization.
 * Skip only configurable administrative data-scope rewriting for these exact SQL calls.
 * Source handlers are never invoked while this narrow skip scope is active.
 */
@Component
@RequiredArgsConstructor
public class TaskPersistence {
    private final BusinessTaskMapper mapper;

    TaskDefinitionRow define(TaskDefinitionRow row) {
        return query(() -> {
            mapper.insertDefinition(row);
            return mapper.definitionKey(row.getTenantId(), row.getSourceType(), row.getCode(), row.getDefinitionVersion());
        });
    }

    TaskDefinitionRow definition(long tenant, long id) {
        return query(() -> mapper.definition(tenant, id));
    }

    List<TaskDefinitionRow> definitions(long tenant, String source, Long afterId, int limit) {
        return query(() -> mapper.definitions(tenant, source, afterId, limit));
    }

    BusinessTaskRow create(BusinessTaskRow row) {
        return query(() -> {
            mapper.insertTask(row);
            return mapper.taskKey(row);
        });
    }

    BusinessTaskRow task(long tenant, long id, boolean lock) {
        return query(() -> mapper.task(tenant, id, lock));
    }

    List<BusinessTaskRow> assigned(long tenant, long assignee, String state, Long after, int limit) {
        return query(() -> mapper.assigned(tenant, assignee, state, after, limit));
    }

    void transition(BusinessTaskRow row) {
        int changed = query(() -> mapper.transition(row));
        if (changed != 1) {
            throw TaskMessages.error(409, "state.conflict");
        }
        row.setVersion(row.getVersion() + 1);
    }

    private <T> T query(Supplier<T> operation) {
        boolean prior = DataScopeContextHolder.isSkip();
        DataScopeContextHolder.skipDataScope();
        try {
            return operation.get();
        } finally {
            if (!prior) {
                DataScopeContextHolder.clearSkip();
            }
        }
    }
}
