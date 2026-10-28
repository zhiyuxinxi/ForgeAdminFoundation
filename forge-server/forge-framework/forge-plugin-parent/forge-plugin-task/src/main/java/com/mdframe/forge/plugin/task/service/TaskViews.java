package com.mdframe.forge.plugin.task.service;

import com.mdframe.forge.plugin.task.entity.BusinessTaskRow;
import com.mdframe.forge.plugin.task.entity.TaskDefinitionRow;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskState;

/** Public projections never expose tenant or mutation/request ownership fields. */
final class TaskViews {
    private TaskViews() { }

    static TaskDefinition definition(TaskDefinitionRow row) {
        return new TaskDefinition(row.getId(), row.getCode(), row.getDefinitionVersion(), row.getTitle(), row.getSourceType());
    }

    static BusinessTask task(BusinessTaskRow row) {
        return new BusinessTask(row.getId(), row.getDefinitionId(), row.getSourceType(), row.getSourceKey(),
                row.getOccurrenceKey(), row.getAssigneeId(), row.getAssigneeName(), row.getTitle(),
                TaskState.valueOf(row.getState()), row.getDueAt(), row.getCompletedAt(), row.getResultJson(),
                row.getCancellationReason(), row.getVersion());
    }

}
