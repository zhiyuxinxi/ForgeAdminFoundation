package com.mdframe.forge.task.api;

import java.time.LocalDateTime;

/** Stable IDs are identities; assigneeName is an assignment-time display snapshot. */
public record BusinessTask(long id, long definitionId, String sourceType, String sourceKey,
                           String occurrenceKey, long assigneeId, String assigneeName, String title,
                           TaskState state, LocalDateTime dueAt, LocalDateTime completedAt,
                           String resultJson, String cancellationReason, long version) { }
