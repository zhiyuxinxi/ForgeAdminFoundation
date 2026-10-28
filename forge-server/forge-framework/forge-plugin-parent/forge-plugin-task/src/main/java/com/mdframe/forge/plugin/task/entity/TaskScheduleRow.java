package com.mdframe.forge.plugin.task.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TaskScheduleRow {
    private Long id;
    private Long tenantId;
    private String code;
    private Integer ruleVersion;
    private String ruleJson;
    private Long actorId;
    private Long actorOrgId;
    private Integer activeSlot;
    private Integer nextIndex;
    private LocalDateTime nextGenerateAt;
    private LocalDateTime nextAttemptAt;
    private String lastFailure;
}
