package com.mdframe.forge.task.api;

import java.util.List;

/**
 * Explicit opt-in by an installed source owner. No client callback or reflective target.
 * Both methods run under the authenticated publisher's current permissions. The owner
 * must authorize the template, responsibility selector and actual business source state.
 */
public interface TaskPeriodSourceHandler extends TaskSourceHandler {
    void validateSchedule(TaskDefinition definition, PeriodicTaskEngine.Define rule, Actor actor);

    /**
     * Prepare the actual source record inside the same master transaction as task creation.
     * The stable periodKey and allocation snapshot identify retries. Return an existing or
     * newly created source key whose ordinary authorize/validateCreation/onTerminal methods
     * enforce the real business state. Business checklist schemas remain with this owner.
     */
    String prepareOccurrence(PeriodicTaskEngine.Schedule schedule, String periodKey,
                             PeriodicTaskEngine.Occurrence occurrence,
                             List<PeriodicTaskEngine.Assignment> assignments, Actor actor);
}
