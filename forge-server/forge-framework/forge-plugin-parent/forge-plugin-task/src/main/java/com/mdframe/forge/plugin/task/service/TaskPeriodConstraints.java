package com.mdframe.forge.plugin.task.service;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Persisted period deadlines apply even through the generic completion API. */
@Component
@RequiredArgsConstructor
public class TaskPeriodConstraints {
    private final TaskScheduleStore store;

    void start(long tenant, long task) {
        var occurrence = store.forTask(tenant, task);
        if (occurrence != null && LocalDateTime.now(ZoneOffset.UTC).isBefore(occurrence.getStartsAt())) {
            throw TaskMessages.error(409, "period.not.started");
        }
    }

    void completion(long tenant, long task) {
        start(tenant, task);
        var occurrence = store.forTask(tenant, task);
        if (occurrence != null && !occurrence.getAllowLate()
                && !LocalDateTime.now(ZoneOffset.UTC).isBefore(occurrence.getDueAt())) {
            throw TaskMessages.error(409, "period.late.denied");
        }
    }
}
