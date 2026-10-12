package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.flow.dto.ProcessNodeTaskInfo;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Read the engine once per node-view request before pure view assembly. */
@Component
@Slf4j
public class ProcessNodeSnapshotLoader {
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ObjectProvider<FlowOrgIntegrationService> organizations;

    public ProcessNodeSnapshotLoader(RuntimeService runtimeService, TaskService taskService,
                                     HistoryService historyService,
                                     ObjectProvider<FlowOrgIntegrationService> organizations) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.organizations = organizations;
    }

    public Snapshot load(String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list();
        Map<String, ProcessNodeTaskInfo> tasks = new LinkedHashMap<>();
        for (HistoricTaskInstance task : historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            tasks.put(task.getId(), historic(task));
        }
        for (Task task : taskService.createTaskQuery().processInstanceId(processInstanceId).list()) {
            tasks.put(task.getId(), current(task));
        }
        for (HistoricActivityInstance activity : activities) {
            ProcessNodeTaskInfo task = tasks.get(activity.getTaskId());
            if (task != null) {
                task.setActivityInstanceId(activity.getId());
            }
        }
        Map<String, Map<String, Object>> users = loadUsers(tasks.values().stream().toList());
        tasks.values().forEach(task -> fillAssignee(task, users));
        Map<String, List<ProcessNodeTaskInfo>> byNode = tasks.values().stream()
                .sorted(Comparator.comparing(ProcessNodeTaskInfo::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(ProcessNodeTaskInfo::getTaskId))
                .collect(Collectors.groupingBy(ProcessNodeTaskInfo::getNodeId, LinkedHashMap::new, Collectors.toList()));
        Set<String> active = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId)
                .singleResult() == null ? Set.of() : new HashSet<>(runtimeService.getActiveActivityIds(processInstanceId));
        return new Snapshot(activities, active, byNode, users);
    }

    private ProcessNodeTaskInfo historic(HistoricTaskInstance task) {
        ProcessNodeTaskInfo info = new ProcessNodeTaskInfo();
        info.setTaskId(task.getId());
        info.setNodeId(task.getTaskDefinitionKey());
        info.setExecutionId(task.getExecutionId());
        info.setAssigneeId(task.getAssignee());
        info.setStartTime(task.getCreateTime());
        info.setEndTime(task.getEndTime());
        info.setDuration(task.getDurationInMillis());
        info.setDeleteReason(task.getDeleteReason());
        info.setStatus(task.getEndTime() == null ? "unknown"
                : task.getDeleteReason() == null ? "completed" : "canceled");
        return info;
    }

    private ProcessNodeTaskInfo current(Task task) {
        ProcessNodeTaskInfo info = new ProcessNodeTaskInfo();
        info.setTaskId(task.getId());
        info.setNodeId(task.getTaskDefinitionKey());
        info.setExecutionId(task.getExecutionId());
        info.setAssigneeId(task.getAssignee());
        info.setStartTime(task.getCreateTime());
        info.setStatus("running");
        if (task.getAssignee() == null) {
            info.setCandidateUserIds(taskService.getIdentityLinksForTask(task.getId()).stream()
                    .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                    .map(org.flowable.identitylink.api.IdentityLink::getUserId).distinct().sorted().toList());
        }
        return info;
    }

    private Map<String, Map<String, Object>> loadUsers(List<ProcessNodeTaskInfo> tasks) {
        Set<String> ids = new HashSet<>();
        tasks.forEach(task -> {
            if (task.getAssigneeId() != null) {
                ids.add(task.getAssigneeId());
            }
            ids.addAll(task.getCandidateUserIds());
        });
        FlowOrgIntegrationService service = organizations.getIfAvailable();
        Map<String, Map<String, Object>> users = new HashMap<>();
        for (String id : ids) {
            Map<String, Object> user = null;
            try {
                user = service == null ? null : service.getUserInfo(id);
            } catch (RuntimeException ignored) {
                // Display lookup failure must preserve the task and its stable user ID.
                log.warn("Node-view user display lookup failed; retaining stable user ID");
            }
            users.put(id, user == null ? Map.of() : user);
        }
        return users;
    }

    private void fillAssignee(ProcessNodeTaskInfo task, Map<String, Map<String, Object>> users) {
        if (task.getAssigneeId() != null) {
            Map<String, Object> user = users.getOrDefault(task.getAssigneeId(), Map.of());
            task.setAssigneeName(display(user, task.getAssigneeId(), "name", "realName", "nickname", "username"));
            task.setAssigneeOrg(display(user, "", "deptName", "orgName"));
        }
    }

    static String display(Map<String, Object> user, String fallback, String... keys) {
        for (String key : keys) {
            Object value = user.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return fallback;
    }

    public record Snapshot(List<HistoricActivityInstance> activities, Set<String> activeActivityIds,
                           Map<String, List<ProcessNodeTaskInfo>> tasksByNode,
                           Map<String, Map<String, Object>> users) {
    }
}
