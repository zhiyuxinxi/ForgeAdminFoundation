package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Projects already-selected workflow history without changing task state or query scope. */
@Component
public class FlowTaskHistoryProjection {
    private final FlowUserDisplayNames userDisplayNames;

    public FlowTaskHistoryProjection(FlowUserDisplayNames userDisplayNames) {
        this.userDisplayNames = userDisplayNames;
    }

    public List<Map<String, Object>> assemble(List<FlowTask> tasks, FlowBusiness business) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 3. 加入“发起”节点
        if (business != null) {
            Map<String, Object> startNode = new HashMap<>();
            startNode.put("taskName", "发起流程");
            startNode.put("assigneeName", userDisplayNames.resolve(
                    business.getApplyUserId(), business.getApplyUserName()));
            startNode.put("assigneeId", business.getApplyUserId());
            startNode.put("action", "start");
            startNode.put("comment", "");
            startNode.put("createTime", business.getApplyTime() != null
                    ? business.getApplyTime().toString() : business.getCreateTime() != null
                    ? business.getCreateTime().toString() : null);
            startNode.put("completeTime", startNode.get("createTime"));
            result.add(startNode);
        }

        // 4. 加入每个任务节点
        // 状态到 action 的映射
        Map<Integer, String> statusActionMap = new HashMap<>();
        statusActionMap.put(0, "pending");
        statusActionMap.put(1, "claim");
        statusActionMap.put(2, "approve");
        statusActionMap.put(3, "reject");
        statusActionMap.put(4, "delegate");
        statusActionMap.put(5, "delegate");
        statusActionMap.put(6, "withdraw");
        statusActionMap.put(7, "return");
        statusActionMap.put(8, "terminate");

        for (FlowTask task : tasks) {
            // 待办且未完成的节点也要展示（表示当前处理中）
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", task.getTaskId());
            node.put("taskName", task.getTaskName());
            // 安全处理：assignee 可能为空
            String assigneeName = userDisplayNames.resolve(task.getAssignee(), task.getAssigneeName());
            node.put("assigneeName", assigneeName);
            node.put("assigneeId", task.getAssignee());
            node.put("action", statusActionMap.getOrDefault(task.getStatus(), "pending"));
            node.put("comment", task.getComment() != null ? task.getComment() : "");
            node.put("signature", task.getSignature());
            node.put("createTime", task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            node.put("completeTime", task.getCompleteTime() != null ? task.getCompleteTime().toString() : null);
            result.add(node);
        }

        return result;
    }

}
