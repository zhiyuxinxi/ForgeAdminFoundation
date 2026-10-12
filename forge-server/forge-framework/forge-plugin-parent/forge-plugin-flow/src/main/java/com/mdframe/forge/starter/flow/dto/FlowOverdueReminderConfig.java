package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批节点逾期提醒运行时配置。
 */
@Data
public class FlowOverdueReminderConfig {

    public static final String DEFAULT_TEMPLATE_CODE = "FLOW_TASK_OVERDUE";
    public static final String DEFAULT_CHANNEL = "WEB";
    public static final String REPEAT_ONCE = "once";
    public static final String REPEAT_INTERVAL = "interval";

    /**
     * 是否启用逾期提醒。
     */
    private boolean enabled;

    /**
     * 消息模板编码。
     */
    private String templateCode = DEFAULT_TEMPLATE_CODE;

    /**
     * 推送渠道列表。
     */
    private List<String> channels = new ArrayList<>(List.of(DEFAULT_CHANNEL));

    /**
     * 重复策略：once/interval。
     */
    private String repeatMode = REPEAT_ONCE;

    /**
     * 重复提醒间隔分钟。
     */
    private Integer intervalMinutes = 1440;

    /**
     * 最大提醒次数。
     */
    private Integer maxTimes = 1;
}
