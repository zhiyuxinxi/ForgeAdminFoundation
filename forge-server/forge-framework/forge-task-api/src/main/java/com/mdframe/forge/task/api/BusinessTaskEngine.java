package com.mdframe.forge.task.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ordinary business obligations, independent of approval/workflow tasks.
 * Tenant and actor always come from trusted execution context, never command parameters.
 * All mutations and source write-back currently require the same master datasource transaction.
 */
public interface BusinessTaskEngine {
    TaskDefinition define(Define command);
    TaskDefinition definition(long id);
    List<TaskDefinition> definitions(String sourceType, Long afterId, int limit);
    BusinessTask create(Create command);
    BusinessTask dispatch(long taskId);
    BusinessTask start(long taskId);
    BusinessTask complete(long taskId, Complete command);
    BusinessTask cancel(long taskId, String reason);
    BusinessTask get(long taskId);
    List<BusinessTask> assigned(TaskState state, Long afterId, int limit);

    /** Definitions are immutable versions; repeating a key with changed content is rejected. */
    record Define(String code, int version, String title, String sourceType) { }

    /**
     * occurrenceKey identifies one obligation/period, not a random request attempt.
     * Same definition/source/occurrence/assignee returns the original task if content matches.
     * Times follow the existing Foundation LocalDateTime contract.
     */
    record Create(long definitionId, String sourceKey, String occurrenceKey,
                  long assigneeId, String title, LocalDateTime dueAt) { }

    /** Stable request key plus a bounded JSON object owned/validated by the source handler. */
    record Complete(String requestKey, String resultJson) { }
}
