package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.plugin.task.entity.TaskOccurrenceRow;

/** Internal call proof for the built-in source; ordinary HTTP create cannot impersonate a tick. */
final class TaskPeriodInvocation implements AutoCloseable {
    private static final ThreadLocal<TaskOccurrenceRow> CURRENT = new ThreadLocal<>();
    private final TaskOccurrenceRow previous;

    private TaskPeriodInvocation(TaskOccurrenceRow row) {
        previous = CURRENT.get();
        CURRENT.set(row);
    }

    static TaskPeriodInvocation open(TaskOccurrenceRow row) {
        return new TaskPeriodInvocation(row);
    }

    static boolean matches(TaskOccurrenceRow row) {
        TaskOccurrenceRow current = CURRENT.get();
        return current != null && current.getId().equals(row.getId())
                && current.getTenantId().equals(row.getTenantId());
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
