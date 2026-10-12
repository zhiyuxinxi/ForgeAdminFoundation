package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/** One concrete task occurrence; task/activity identities distinguish re-entry and parallel instances. */
@Data
public class ProcessNodeTaskInfo {
    private String taskId;
    private String nodeId;
    private String executionId;
    private String activityInstanceId;
    private String status;
    private String assigneeId;
    private String assigneeName;
    private String assigneeOrg;
    private List<String> candidateUserIds = List.of();
    private Date startTime;
    private Date endTime;
    private Long duration;
    private String deleteReason;
}
