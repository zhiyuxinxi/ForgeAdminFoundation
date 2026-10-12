package com.mdframe.forge.starter.flow.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Assembles active/historic form context while preserving engine and metadata read precedence. */
@Slf4j
@Component
public class FlowFormContextReader {
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowModelService flowModelService;
    private final FlowProcessDefinitions definitions;
    private final FlowUserDisplayNames userNames;
    private final FlowFormConfiguration configuration;
    private final FlowFormSnapshots snapshots;

    public FlowFormContextReader(TaskService taskService, RuntimeService runtimeService, HistoryService historyService,
                                 FlowBusinessMapper flowBusinessMapper, FlowTaskMapper flowTaskMapper,
                                 FlowModelService flowModelService, FlowProcessDefinitions definitions,
                                 FlowUserDisplayNames userNames, FlowFormConfiguration configuration,
                                 FlowFormSnapshots snapshots) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.flowBusinessMapper = flowBusinessMapper;
        this.flowTaskMapper = flowTaskMapper;
        this.flowModelService = flowModelService;
        this.definitions = definitions;
        this.userNames = userNames;
        this.configuration = configuration;
        this.snapshots = snapshots;
    }

    public record TaskContext(Task task, TaskFormInfo formInfo, FlowModel model, FlowNode node) { }

    public TaskContext readTask(String taskId) {
        // 1. 获取任务信息
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException(UiMessages.message("customer.taskMissing", taskId));
        }

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(task.getName());
        formInfo.setTaskDefKey(task.getTaskDefinitionKey());
        formInfo.setProcessInstanceId(task.getProcessInstanceId());

        // 2. 获取流程定义Key
        String processDefKey = definitions.resolveProcessDefinitionKey(task.getProcessDefinitionId(), null);
        formInfo.setProcessDefKey(processDefKey);

        // 3. 获取流程变量
        Map<String, Object> variables = taskService.getVariables(taskId);
        formInfo.setVariables(variables);

        // 4. 获取业务信息
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
        if (business != null) {
            formInfo.setBusinessKey(business.getBusinessKey());
            formInfo.setTitle(business.getTitle());
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(userNames.resolve(
                    business.getApplyUserId(), business.getApplyUserName()));
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        // 5. 读取流程模型和 BPMN 节点表单配置，同一次请求内复用给审批策略解析。
        FlowModel flowModel = !isBlank(processDefKey) ? flowModelService.getModelByKey(processDefKey) : null;
        FlowNode flowNode = configuration.resolveFormFlowNode(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        configuration.applyFormConfiguration(formInfo, flowModel, flowNode);
        snapshots.hydrateFormInstanceSnapshotIfNecessary(formInfo, task.getProcessInstanceId());
        return new TaskContext(task, formInfo, flowModel, flowNode);
    }
    public TaskFormInfo readProcess(String processInstanceId, String businessKey, String processDefKey,
                                    String taskId, String taskDefKey) {
        FlowTask sourceTask = null;
        if (!isBlank(taskId)) {
            sourceTask = flowTaskMapper.selectByIdOrTaskId(taskId);
        }

        FlowBusiness business = resolveFlowBusiness(processInstanceId, businessKey);
        String effectiveProcessInstanceId = firstNonBlank(processInstanceId,
                business != null ? business.getProcessInstanceId() : null,
                sourceTask != null ? sourceTask.getProcessInstanceId() : null);
        String effectiveBusinessKey = firstNonBlank(businessKey,
                business != null ? business.getBusinessKey() : null,
                sourceTask != null ? sourceTask.getBusinessKey() : null);
        String rawProcessDefKey = firstNonBlank(processDefKey,
                business != null ? business.getProcessDefKey() : null,
                sourceTask != null ? sourceTask.getProcessDefKey() : null);
        String processDefinitionId = firstNonBlank(
                sourceTask != null ? sourceTask.getProcessDefId() : null,
                business != null ? business.getProcessDefId() : null,
                definitions.resolveProcessDefinitionId(effectiveProcessInstanceId, rawProcessDefKey));
        String effectiveProcessDefKey = definitions.resolveProcessDefinitionKey(processDefinitionId, rawProcessDefKey);
        String effectiveTaskDefKey = firstNonBlank(
                taskDefKey,
                sourceTask != null ? sourceTask.getTaskDefKey() : null,
                findActiveTaskDefinitionKey(effectiveProcessInstanceId),
                findFirstHistoricTaskDefinitionKey(effectiveProcessInstanceId));

        TaskFormInfo formInfo = new TaskFormInfo();
        formInfo.setTaskId(taskId);
        formInfo.setTaskName(sourceTask != null ? sourceTask.getTaskName() : null);
        formInfo.setTaskDefKey(effectiveTaskDefKey);
        formInfo.setProcessInstanceId(effectiveProcessInstanceId);
        formInfo.setProcessDefKey(effectiveProcessDefKey);
        formInfo.setBusinessKey(effectiveBusinessKey);
        formInfo.setTitle(business != null ? business.getTitle() : sourceTask != null ? sourceTask.getTitle() : null);
        if (business != null) {
            formInfo.setStartUserId(business.getApplyUserId());
            formInfo.setStartUserName(userNames.resolve(
                    business.getApplyUserId(), business.getApplyUserName()));
            formInfo.setStartDeptId(business.getApplyDeptId());
            formInfo.setStartDeptName(business.getApplyDeptName());
        }

        Map<String, Object> variables = readProcessVariablesForForm(effectiveProcessInstanceId);
        if (!isBlank(effectiveBusinessKey)) {
            variables.putIfAbsent("businessKey", effectiveBusinessKey);
        }
        formInfo.setVariables(variables);
        FlowModel model = !isBlank(effectiveProcessDefKey) ? flowModelService.getModelByKey(effectiveProcessDefKey) : null;
        FlowNode node = configuration.resolveFormFlowNode(processDefinitionId, effectiveTaskDefKey);
        configuration.applyFormConfiguration(formInfo, model, node);
        snapshots.hydrateFormInstanceSnapshotIfNecessary(formInfo, effectiveProcessInstanceId);
        formInfo.setAllowApprove(false);
        formInfo.setAllowReject(false);
        formInfo.setAllowDelegate(false);
        formInfo.setAllowReturn(false);
        formInfo.setAllowTerminate(false);
        formInfo.setRequireComment(false);
        formInfo.setRequireSignature(false);
        formInfo.setAllowRejectToStart(false);
        return formInfo;
    }
    private String findActiveTaskDefinitionKey(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return null;
        }
        try {
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .orderByTaskCreateTime()
                    .asc()
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
            return task == null ? null : task.getTaskDefinitionKey();
        } catch (Exception e) {
            log.debug("读取运行中任务定义Key失败: processInstanceId={}", processInstanceId);
            return null;
        }
    }

    private FlowBusiness resolveFlowBusiness(String processInstanceId, String businessKey) {
        FlowBusiness business = null;
        if (!isBlank(processInstanceId)) {
            business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);
        }
        if (business == null && !isBlank(businessKey)) {
            business = flowBusinessMapper.selectByBusinessKey(businessKey);
        }
        return business;
    }

    private Map<String, Object> readProcessVariablesForForm(String processInstanceId) {
        Map<String, Object> variables = new HashMap<>();
        if (isBlank(processInstanceId)) {
            return variables;
        }
        try {
            Map<String, Object> runtimeVariables = runtimeService.getVariables(processInstanceId);
            if (runtimeVariables != null) {
                variables.putAll(runtimeVariables);
            }
        } catch (Exception e) {
            log.debug("读取运行流程变量失败，继续读取历史变量: processInstanceId={}", processInstanceId);
        }
        try {
            List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            if (historicVariables != null) {
                for (HistoricVariableInstance variable : historicVariables) {
                    if (variable != null && variable.getVariableName() != null) {
                        variables.putIfAbsent(variable.getVariableName(), variable.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取历史流程变量失败: processInstanceId={}", processInstanceId, e);
        }
        return variables;
    }

    private String findFirstHistoricTaskDefinitionKey(String processInstanceId) {
        if (isBlank(processInstanceId)) {
            return null;
        }
        try {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceStartTime()
                    .asc()
                    .list();
            if (historicTasks != null && !historicTasks.isEmpty()) {
                return historicTasks.get(0).getTaskDefinitionKey();
            }
        } catch (Exception e) {
            log.debug("读取历史任务定义Key失败: processInstanceId={}", processInstanceId);
        }
        return null;
    }
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
