package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeTaskInfo;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.engine.history.HistoricActivityInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Pure mapping of a complete task snapshot; no engine queries or writes. */
public final class ProcessNodeViewAssembler {
    private ProcessNodeViewAssembler() {
    }

    public static List<ProcessNodeInfo> assemble(BpmnModel model, ProcessNodeSnapshotLoader.Snapshot snapshot) {
        if (model.getProcesses().isEmpty()) {
            return List.of();
        }
        List<ProcessNodeInfo> result = new ArrayList<>();
        for (FlowNode node : model.getMainProcess().findFlowElementsOfType(FlowNode.class)) {
            ProcessNodeInfo info = new ProcessNodeInfo();
            info.setNodeId(node.getId());
            info.setNodeName(node.getName());
            info.setNodeType(node.getClass().getSimpleName());
            fillGraphic(info, model.getGraphicInfo(node.getId()));
            List<ProcessNodeTaskInfo> tasks = snapshot.tasksByNode().getOrDefault(node.getId(), List.of());
            info.setTaskInstances(tasks);
            fillState(info, tasks, snapshot);
            fillUsers(info, tasks, snapshot.users());
            result.add(info);
        }
        return result;
    }

    private static void fillGraphic(ProcessNodeInfo info, GraphicInfo graphic) {
        if (graphic != null) {
            info.setX(graphic.getX());
            info.setY(graphic.getY());
            info.setWidth(graphic.getWidth());
            info.setHeight(graphic.getHeight());
        }
    }

    private static void fillState(ProcessNodeInfo info, List<ProcessNodeTaskInfo> tasks,
                                  ProcessNodeSnapshotLoader.Snapshot snapshot) {
        List<ProcessNodeTaskInfo> current = tasks.stream().filter(task -> "running".equals(task.getStatus())).toList();
        info.setTaskId(current.size() == 1 ? current.get(0).getTaskId() : null);
        if (!current.isEmpty() || snapshot.activeActivityIds().contains(info.getNodeId())) {
            info.setStatus("running");
            return;
        }
        HistoricActivityInstance latest = snapshot.activities().stream()
                .filter(activity -> info.getNodeId().equals(activity.getActivityId()) && activity.getEndTime() != null)
                .max(Comparator.comparing(HistoricActivityInstance::getStartTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())).thenComparing(HistoricActivityInstance::getId))
                .orElse(null);
        info.setStatus(latest == null ? "pending" : "completed");
        if (latest != null) {
            info.setStartTime(latest.getStartTime());
            info.setEndTime(latest.getEndTime());
            info.setDuration(latest.getDurationInMillis());
        }
    }

    private static void fillUsers(ProcessNodeInfo info, List<ProcessNodeTaskInfo> tasks,
                                  Map<String, Map<String, Object>> users) {
        List<String> assignees = tasks.stream().map(ProcessNodeTaskInfo::getAssigneeId)
                .filter(id -> id != null && !id.isBlank()).distinct().toList();
        info.setAssigneeIds(assignees);
        info.setAssigneeNames(assignees.stream().map(id -> name(users, id)).toList());
        info.setAssigneeOrgs(assignees.stream().map(id -> organization(users, id)).toList());
        info.setAssigneeDetails(assignees.stream().map(id -> Map.<String, Object>of(
                "userId", id, "name", name(users, id), "orgName", organization(users, id))).toList());
        List<String> candidates = tasks.stream().filter(task -> "running".equals(task.getStatus()))
                .flatMap(task -> task.getCandidateUserIds().stream()).distinct().toList();
        info.setCandidateUserIds(candidates);
        info.setCandidateUserNames(candidates.stream().map(id -> name(users, id)).toList());
    }

    private static String name(Map<String, Map<String, Object>> users, String id) {
        return ProcessNodeSnapshotLoader.display(users.getOrDefault(id, Map.of()), id,
                "name", "realName", "nickname", "username");
    }

    private static String organization(Map<String, Map<String, Object>> users, String id) {
        return ProcessNodeSnapshotLoader.display(users.getOrDefault(id, Map.of()), "", "deptName", "orgName");
    }
}
