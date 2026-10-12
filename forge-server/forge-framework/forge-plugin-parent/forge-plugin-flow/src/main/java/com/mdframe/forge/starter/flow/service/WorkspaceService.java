package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.vo.WorkspaceSummaryVO;

/**
 * 我的工作台聚合服务。
 */
public interface WorkspaceService {

    /**
     * 查询当前用户工作台首页聚合统计。
     */
    WorkspaceSummaryVO summary(String userId);

    /**
     * 查询当前用户待办徽标数。
     */
    Long todoCount(String userId);
}
