package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.entity.FlowTask;

import java.util.Set;

/**
 * 流程任务消息接收人解析服务。
 */
public interface FlowTaskReceiverResolver {

    /**
     * 根据任务处理人、候选人和候选组解析可接收消息的用户ID。
     *
     * @param flowTask 流程任务
     * @return 用户ID集合
     */
    Set<Long> resolveReceivers(FlowTask flowTask);
}
