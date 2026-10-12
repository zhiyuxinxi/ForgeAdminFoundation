package com.mdframe.forge.starter.flow.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Completes engine tasks and advances repeated approvers within the caller's authorized transaction. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowTaskCompletion {
    private static final String AUTO_APPROVAL_FIRST_ONLY = "firstOnly";
    private static final String AUTO_APPROVAL_CONSECUTIVE = "consecutive";
    private static final int MAX_AUTO_APPROVALS = 30;

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowBpmnPolicyReader bpmnPolicies;
    private final FlowTaskMapper taskMapper;

    public void complete(Task task, Map<String, Object> variables, boolean approved) {
        Map<String, Object> completeVariables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        completeVariables.put("approved", approved);
        completeVariables.put("approvalResult", approved ? "approve" : "reject");
        String taskId = task.getId();
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            log.info("任务处于委派待解决状态，先 resolve 再 complete：taskId={}, assignee={}, owner={}",
                    taskId, task.getAssignee(), task.getOwner());
            taskService.resolveTask(taskId);
        }
        runtimeService.setVariables(task.getProcessInstanceId(), completeVariables);
        taskService.complete(taskId, completeVariables);
    }

    public void approveRepeated(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            return;
        }
        String mode = bpmnPolicies.readProcessString(instance.getProcessDefinitionId(), "autoApprovalMode");
        if (!AUTO_APPROVAL_FIRST_ONLY.equals(mode) && !AUTO_APPROVAL_CONSECUTIVE.equals(mode)) {
            return;
        }
        Set<String> completedAutomatically = new HashSet<>();
        int guard = 0;
        while (guard++ < MAX_AUTO_APPROVALS) {
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            Task matchedTask = null;
            for (Task activeTask : activeTasks) {
                if (completedAutomatically.contains(activeTask.getId())) {
                    continue;
                }
                if (shouldAutoApproveTask(activeTask, mode)) {
                    matchedTask = activeTask;
                    break;
                }
            }
            if (matchedTask == null) {
                return;
            }
            autoApproveTask(matchedTask, mode);
            completedAutomatically.add(matchedTask.getId());
        }
        log.warn("重复审批自动同意达到保护上限：processInstanceId={}, mode={}", processInstanceId, mode);
    }

    private boolean shouldAutoApproveTask(Task task, String mode) {
        if (task == null || isBlank(task.getAssignee())) {
            return false;
        }
        String assignee = task.getAssignee();
        if (AUTO_APPROVAL_FIRST_ONLY.equals(mode)) {
            return hasFinishedTaskByAssignee(task.getProcessInstanceId(), assignee);
        }
        HistoricTaskInstance previousTask = findLastFinishedTask(task.getProcessInstanceId());
        return previousTask != null && Objects.equals(previousTask.getAssignee(), assignee);
    }

    private boolean hasFinishedTaskByAssignee(String processInstanceId, String assignee) {
        long count = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(assignee)
                .finished()
                .count();
        return count > 0;
    }

    private HistoricTaskInstance findLastFinishedTask(String processInstanceId) {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .listPage(0, 1);
        return tasks == null || tasks.isEmpty() ? null : tasks.get(0);
    }

    private void autoApproveTask(Task task, String mode) {
        String comment = "系统自动同意（重复审批人）";
        taskService.addComment(task.getId(), task.getProcessInstanceId(), comment);
        complete(task, null, true);
        FlowTask flowTask = new FlowTask();
        flowTask.setStatus(2);
        flowTask.setComment(comment);
        flowTask.setCompleteTime(LocalDateTime.now());
        taskMapper.update(flowTask, Wrappers.<FlowTask>lambdaUpdate().eq(FlowTask::getTaskId, task.getId()));
        log.info("重复审批自动同意：taskId={}, processInstanceId={}, assignee={}, mode={}",
                task.getId(), task.getProcessInstanceId(), task.getAssignee(), mode);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
