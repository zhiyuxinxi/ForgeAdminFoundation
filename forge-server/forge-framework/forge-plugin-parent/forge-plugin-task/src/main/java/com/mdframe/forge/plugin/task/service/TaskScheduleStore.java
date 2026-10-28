package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.plugin.task.entity.TaskOccurrenceRow;
import com.mdframe.forge.plugin.task.entity.TaskScheduleRow;
import com.mdframe.forge.plugin.task.mapper.TaskScheduleMapper;
import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/** Only these owner-table SQL calls skip configurable business data scope. */
@Component
@RequiredArgsConstructor
public class TaskScheduleStore {
    private final TaskScheduleMapper mapper;

    TaskScheduleRow insert(TaskScheduleRow row) {
        return query(() -> {
            mapper.insert(row);
            return mapper.byKey(row.getTenantId(), row.getCode(), row.getRuleVersion());
        });
    }

    TaskScheduleRow get(long tenant, long id, boolean lock) {
        return query(() -> mapper.get(tenant, id, lock));
    }

    List<TaskScheduleRow> list(long tenant, Long after, int limit) {
        return query(() -> mapper.list(tenant, after, limit));
    }

    void update(TaskScheduleRow row) {
        if (query(() -> mapper.update(row)) != 1) {
            throw TaskMessages.error(409, "state.conflict");
        }
    }

    List<TaskScheduleRow> due(LocalDateTime now) {
        return query(() -> mapper.due(now));
    }

    TaskOccurrenceRow occurrence(long tenant, long schedule, int index) {
        return query(() -> mapper.occurrence(tenant, schedule, index));
    }

    void insertOccurrence(TaskOccurrenceRow row) {
        query(() -> mapper.insertOccurrence(row));
    }

    void generated(long tenant, long occurrence) {
        if (query(() -> mapper.markGenerated(tenant, occurrence)) != 1) {
            throw TaskMessages.error(409, "state.conflict");
        }
    }

    void link(long tenant, long occurrence, long task, long assignee) {
        query(() -> mapper.link(tenant, occurrence, task, assignee));
    }

    TaskOccurrenceRow forTask(long tenant, long task) {
        return query(() -> mapper.forTask(tenant, task));
    }

    private <T> T query(Supplier<T> operation) {
        boolean previous = DataScopeContextHolder.isSkip();
        DataScopeContextHolder.skipDataScope();
        try {
            return operation.get();
        } finally {
            if (!previous) {
                DataScopeContextHolder.clearSkip();
            }
        }
    }
}
