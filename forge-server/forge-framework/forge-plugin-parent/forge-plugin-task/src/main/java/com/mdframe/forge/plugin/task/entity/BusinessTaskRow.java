package com.mdframe.forge.plugin.task.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_business_task")
public class BusinessTaskRow {
    private Long id;
    private Long tenantId;
    private Long definitionId;
    private String sourceType;
    private String sourceKey;
    private String occurrenceKey;
    private Long assigneeId;
    private String assigneeName;
    private String title;
    private String state;
    private LocalDateTime dueAt;
    private LocalDateTime completedAt;
    private String resultJson;
    private String completionKey;
    private String cancellationReason;
    private Long version;
    private Long createBy;
    private Long updateBy;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
