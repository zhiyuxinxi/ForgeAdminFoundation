package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/**
 * 我的工作台聚合统计。
 */
@Data
public class WorkspaceSummaryVO {

    /**
     * 我的待办数。
     */
    private Long todoCount;

    /**
     * 本周已办数。
     */
    private Long doneWeekCount;

    /**
     * 我发起且仍在流转中的流程数。
     */
    private Long startedRunningCount;

    /**
     * 抄送我的未读数。
     */
    private Long ccUnreadCount;
}
