package com.mdframe.forge.plugin.task.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TaskOccurrenceRow {
    private Long id;
    private Long tenantId;
    private Long scheduleId;
    private Integer occurrenceIndex;
    private LocalDateTime startsAt;
    private LocalDateTime dueAt;
    private Boolean allowLate;
    private String assignmentJson;
    private String state;
}
