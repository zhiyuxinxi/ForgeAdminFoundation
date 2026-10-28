package com.mdframe.forge.task.api;

/** Overdue is derived from dueAt and the unfinished state; it is not a terminal state. */
public enum TaskState {
    DRAFT, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}
