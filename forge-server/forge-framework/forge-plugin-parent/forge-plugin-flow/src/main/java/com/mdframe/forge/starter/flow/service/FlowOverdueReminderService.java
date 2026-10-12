package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.dto.FlowOverdueReminderConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;

/**
 * 流程任务逾期提醒服务。
 */
public interface FlowOverdueReminderService {

    /**
     * 扫描并发送逾期提醒。
     */
    void scanAndSendOverdueReminders();

    /**
     * 发送单个任务逾期提醒。
     *
     * @param task 流程任务
     * @param config 逾期提醒配置
     */
    void sendOverdueReminder(FlowTask task, FlowOverdueReminderConfig config);
}
