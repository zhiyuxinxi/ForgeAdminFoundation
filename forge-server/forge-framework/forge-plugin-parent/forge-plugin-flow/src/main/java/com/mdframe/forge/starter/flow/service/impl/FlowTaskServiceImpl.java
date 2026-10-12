package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramReader;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramInfoReader;
import com.mdframe.forge.starter.flow.service.support.FlowFormContextReader;
import com.mdframe.forge.starter.flow.service.support.FlowProcessDefinitions;
import com.mdframe.forge.starter.flow.service.support.FlowBpmnPolicyReader;
import com.mdframe.forge.starter.flow.service.support.FlowTaskApprovalPolicies;
import com.mdframe.forge.starter.flow.service.support.TaskApprovalPolicy;
import com.mdframe.forge.starter.flow.service.support.FlowTaskCompletion;
import com.mdframe.forge.starter.flow.service.support.FlowTaskListPresentation;
import com.mdframe.forge.starter.flow.service.support.FlowTaskHistoryProjection;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程任务服务实现
 */
@Slf4j
@Service
public class FlowTaskServiceImpl extends ServiceImpl<FlowTaskMapper, FlowTask> implements FlowTaskService {

    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_DELEGATE = "delegate";
    private static final String ACTION_RETURN = "return";
    private static final String ACTION_TERMINATE = "terminate";

    @Autowired
    private ProcessDiagramInfoReader processDiagramInfoReader;

    @Autowired
    private FlowTaskCompletion taskCompletion;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessDiagramReader processDiagramReader;

    /**
     * 消息服务（可选注入）
     */
    @Autowired(required = false)
    private MessageService messageService;

    /**
     * 组织架构集成服务（可选注入）
     */
    @Autowired
    private FlowTaskHistoryProjection taskHistoryProjection;

    @Autowired
    private FlowBpmnPolicyReader bpmnPolicies;

    @Autowired
    private FlowTaskApprovalPolicies approvalPolicies;

    /**
     * 流程业务Mapper
     */
    @Autowired
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired
    private FlowProcessDefinitions processDefinitions;

    @Autowired
    private FlowErrorLogService flowErrorLogService;

    @Autowired
    private FlowFormContextReader formContexts;

    @Autowired
    private FlowTaskListPresentation taskListPresentation;

    @Override
    public IPage<FlowTask> todoTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return taskListPresentation.enrich(this.getBaseMapper().selectTodoTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> doneTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return taskListPresentation.enrich(this.getBaseMapper().selectDoneTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> startedTasks(Page<FlowTask> page, String userId, String title, String category, Integer status) {
        return taskListPresentation.enrich(this.getBaseMapper().selectStartedTasks(page, userId, title, category, status));
    }

    @Override
    public IPage<FlowTask> candidateTasks(Page<FlowTask> page, String userId, String groupId, String title) {
        // 1. 查询 Flowable 候选任务
        List<Task> flowableTasks = new ArrayList<>();

        // 按候选人查询
        if (userId != null && !userId.isEmpty()) {
            List<Task> userCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateUser(userId)
                    .taskUnassigned()
                    .list();
            flowableTasks.addAll(userCandidateTasks);
        }

        // 按候选组查询
        if (groupId != null && !groupId.isEmpty()) {
            List<Task> groupCandidateTasks = taskService.createTaskQuery()
                    .taskCandidateGroup(groupId)
                    .taskUnassigned()
                    .list();
            // 去重合并
            Set<String> existingTaskIds = flowableTasks.stream()
                    .map(Task::getId)
                    .collect(Collectors.toSet());
            for (Task task : groupCandidateTasks) {
                if (!existingTaskIds.contains(task.getId())) {
                    flowableTasks.add(task);
                }
            }
        }

        // 2. 获取任务ID列表
        if (flowableTasks.isEmpty()) {
            return page; // 返回空页
        }

        List<String> taskIds = flowableTasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());

        // 3. 从本地表查询任务详情（带分页）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FlowTask::getTaskId, taskIds)
                .like(title != null, FlowTask::getTitle, title)
                .orderByDesc(FlowTask::getCreateTime);

        return taskListPresentation.enrich(page(page, wrapper));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);

        FlowTask task = new FlowTask();
        task.setTaskId(taskId);
        task.setAssignee(userId);
        task.setStatus(1);
        task.setClaimTime(LocalDateTime.now());

        lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(task);
        log.info("签收任务：taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature, Map<String, Object> variables) {
        approve(taskId, userId, comment, signature, variables, SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String userId, String comment, String signature,
                        Map<String, Object> variables, Long tenantId,
                        String idempotencyKey, String requestDigest) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "APPROVE", idempotencyKey, requestDigest, 2);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateTaskAction(task, ACTION_APPROVE, comment, signature);
        validateRequiredVariables(task, variables);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            taskCompletion.complete(task, variables, true);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(2);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "APPROVE");
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批通过：taskId={}, userId={}", taskId, userId);
            taskCompletion.approveRepeated(task.getProcessInstanceId());
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_APPROVE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment, String signature) {
        reject(taskId, userId, comment, signature, SessionHelper.getTenantId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String userId, String comment, String signature,
                       Long tenantId, String idempotencyKey, String requestDigest) {
        FlowTask storedTask = authorizeTaskAction(
                taskId, userId, tenantId, "REJECT", idempotencyKey, requestDigest, 3);
        if (storedTask == null) {
            return;
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateFlowableAssignee(task, userId);
        validateTaskAction(task, ACTION_REJECT, comment, signature);

        try {
            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            taskCompletion.complete(task, null, false);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(3);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            flowTask.setActionIdempotencyKey(idempotencyKey);
            flowTask.setActionRequestDigest(requestDigest);
            flowTask.setActionType(idempotencyKey == null ? null : "REJECT");
            updateTaskActionResultRequired(taskId, flowTask);

            log.info("审批驳回：taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_REJECT", e);
            throw e;
        }
    }

    /**
     * 在 Flow 服务最终副作用边界重新校验租户、签收人与任务状态。
     * 返回 null 表示命中已成功的同请求幂等结果。
     */
    private FlowTask authorizeTaskAction(String taskId, String userId, Long tenantId,
                                         String actionType, String idempotencyKey,
                                         String requestDigest, int completedStatus) {
        FlowTask storedTask = baseMapper.selectByTaskIdForUpdate(taskId);
        if (FlowTaskActionAuthorization.authorize(
                storedTask, userId, tenantId, actionType,
                idempotencyKey, requestDigest, completedStatus)) {
            return null;
        }
        return storedTask;
    }

    private void validateFlowableAssignee(Task task, String userId) {
        if (!userId.equals(task.getAssignee())) {
            throw new RuntimeException("FLOW_TASK_ASSIGNEE_MISMATCH");
        }
    }

    private void updateTaskActionResultRequired(String taskId, FlowTask flowTask) {
        if (!lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask)) {
            throw new IllegalStateException("FLOW_TASK_STATE_UPDATE_FAILED");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(String taskId, String userId, String targetUserId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_DELEGATE, comment, signature);

        try {
            String owner = task.getAssignee() != null && !task.getAssignee().isEmpty()
                    ? task.getAssignee()
                    : userId;
            if (owner != null && !owner.isEmpty()) {
                taskService.setOwner(taskId, owner);
            }
            taskService.setAssignee(taskId, targetUserId);

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(0);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setAssignee(targetUserId);
            flowTask.setOwner(owner);
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("转办任务：taskId={}, from={}, to={}", taskId, userId, targetUserId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_DELEGATE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTask(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_RETURN, comment, signature);

        try {
            String targetActivityId = findPreviousUserTaskActivityId(task);
            if (targetActivityId == null || targetActivityId.isEmpty()) {
                throw new RuntimeException("当前任务没有可退回的上一审批节点");
            }

            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "退回：" + comment);
            }

            List<String> currentActivityIds = runtimeService.getActiveActivityIds(task.getProcessInstanceId());
            if (currentActivityIds == null || currentActivityIds.isEmpty()) {
                currentActivityIds = Collections.singletonList(task.getTaskDefinitionKey());
            }
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdsToSingleActivityId(currentActivityIds, targetActivityId)
                    .changeState();

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(7);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("退回任务：taskId={}, userId={}, targetActivityId={}", taskId, userId, targetActivityId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_RETURN", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateTask(String taskId, String userId, String comment, String signature) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("任务不存在或已处理");
        }
        validateTaskAction(task, ACTION_TERMINATE, comment, signature);

        try {
            String reason = comment != null && !comment.isBlank() ? comment : "审批人终结流程";
            taskService.addComment(taskId, task.getProcessInstanceId(), "终结流程：" + reason);
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), reason);

            FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(task.getProcessInstanceId());
            if (business != null) {
                business.setStatus("terminated");
                business.setEndTime(LocalDateTime.now());
                business.setUpdateTime(LocalDateTime.now());
                flowBusinessMapper.updateById(business);
            }

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(8);
            flowTask.setComment(comment);
            flowTask.setSignature(signature);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getTaskId, taskId).update(flowTask);

            log.info("审批人终结流程：taskId={}, processInstanceId={}, userId={}",
                    taskId, task.getProcessInstanceId(), userId);
        } catch (Exception e) {
            recordTaskError(task.getProcessInstanceId(), taskId, task.getTaskDefinitionKey(),
                    task.getName(), "TASK_TERMINATE", e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String userId, String delegateUserId, String comment) {
        delegate(taskId, userId, delegateUserId, comment, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String processInstanceId, String userId) {
        try {
            assertSubmitterWithdrawAllowed(processInstanceId, userId);
            runtimeService.deleteProcessInstance(processInstanceId, "用户撤回");

            FlowTask flowTask = new FlowTask();
            flowTask.setStatus(6);
            flowTask.setCompleteTime(LocalDateTime.now());
            lambdaUpdate().eq(FlowTask::getProcessInstanceId, processInstanceId).update(flowTask);

            log.info("撤回流程：processInstanceId={}, userId={}", processInstanceId, userId);
        } catch (Exception e) {
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(processInstanceId);
            errorLog.setErrorStage("TASK_WITHDRAW");
            flowErrorLogService.recordError(errorLog, e);
            throw e;
        }
    }

    private void assertSubmitterWithdrawAllowed(String processInstanceId, String userId) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            throw new RuntimeException("流程实例不存在或已结束");
        }

        Boolean allowed = bpmnPolicies.readProcessBoolean(instance.getProcessDefinitionId(), "allowSubmitterWithdraw");
        if (Boolean.FALSE.equals(allowed)) {
            throw new RuntimeException("当前流程不允许提交人撤回审批中的申请");
        }

        if (!isProcessSubmitter(processInstanceId, userId)) {
            throw new RuntimeException("只有提交人可以撤回该申请");
        }
    }

    private boolean isProcessSubmitter(String processInstanceId, String userId) {
        if (isBlank(userId)) {
            return false;
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);
        if (business != null && !isBlank(business.getApplyUserId())) {
            return Objects.equals(String.valueOf(business.getApplyUserId()), String.valueOf(userId));
        }
        Object initiator = runtimeService.getVariable(processInstanceId, "initiator");
        if (initiator != null && !isBlank(String.valueOf(initiator))) {
            return Objects.equals(String.valueOf(initiator), String.valueOf(userId));
        }
        log.warn("撤回申请未找到提交人信息，按历史兼容逻辑放行：processInstanceId={}, userId={}",
                processInstanceId, userId);
        return true;
    }

    @Override
    public FlowTask getTaskDetail(String taskId) {
        FlowTask task = getBaseMapper().selectByIdOrTaskId(taskId);
        if (task != null) {
            task.setProcessDefKey(processDefinitions.resolveProcessDefinitionKey(
                    firstNonBlank(task.getProcessDefId(), task.getProcessDefKey()),
                    task.getProcessDefKey()));
        }
        return task;
    }

    @Override
    public byte[] getProcessDiagram(String processInstanceId) {
        return processDiagramReader.read(processInstanceId);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId) {
        return getProcessDiagramInfo(processInstanceId, false);
    }

    @Override
    public ProcessDiagramInfo getProcessDiagramInfo(String processInstanceId, boolean includeImage) {
        return processDiagramInfoReader.read(processInstanceId, includeImage);
    }

    @Override
    public void remind(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("催办失败：任务不存在，taskId={}", taskId);
            return;
        }

        log.info("催办任务：taskId={}, taskName={}", taskId, task.getName());

        // 发送催办消息通知
        if (messageService != null) {
            try {
                // 获取任务处理人
                String assignee = task.getAssignee();
                if (assignee == null || assignee.isEmpty()) {
                    // 如果任务未签收，尝试获取候选人
                    log.info("任务未签收，跳过消息通知：taskId={}", taskId);
                    return;
                }

                // 构建消息
                com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO request =
                    new com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO();
                request.setTitle("流程催办提醒");
                request.setContent(String.format(
                    "您有一个待办任务需要处理：%s，请及时处理。",
                    task.getName()
                ));
                request.setType("SYSTEM");
                request.setChannel("WEB");
                request.setSendScope("USERS");

                // 设置接收人
                Set<Long> userIds = new HashSet<>();
                try {
                    userIds.add(Long.parseLong(assignee));
                } catch (NumberFormatException e) {
                    log.warn("无法解析处理人ID：{}", assignee);
                    return;
                }
                request.setUserIds(userIds);

                // 发送消息
                messageService.send(request);
                log.info("催办消息发送成功：taskId={}, assignee={}", taskId, assignee);

            } catch (Exception e) {
                log.error("发送催办消息失败：taskId={}", taskId, e);
            }
        } else {
            log.warn("消息服务未启用，无法发送催办通知");
        }
    }

    private void validateTaskAction(Task task, String action, String comment, String signature) {
        TaskApprovalPolicy policy = approvalPolicies.resolve(task);
        if (!policy.isAllowed(action)) {
            throw new RuntimeException("当前节点不允许执行该审批操作");
        }
        if (policy.requireComment() && isBlank(comment)) {
            throw new RuntimeException("请输入审批意见");
        }
        if (policy.requireSignature() && isBlank(signature)) {
            throw new RuntimeException("请完成审批签名");
        }
    }

    private void validateRequiredVariables(Task task, Map<String, Object> variables) {
        FlowNode flowNode = bpmnPolicies.getFlowNode(task);
        if (flowNode == null) {
            return;
        }
        String requiredVariables = bpmnPolicies.readNodeString(flowNode, "requiredVariables");
        if (isBlank(requiredVariables)) {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String variable : requiredVariables.split("[,;，；]")) {
            String key = variable == null ? "" : variable.trim();
            if (key.isEmpty()) {
                continue;
            }
            Object value = variables == null ? null : variables.get(key);
            if (isEmptyVariableValue(value)) {
                missing.add(key);
            }
        }
        if (missing.isEmpty()) {
            return;
        }

        String message = bpmnPolicies.readNodeString(flowNode, "requiredMessage");
        if (isBlank(message)) {
            message = "请补充必填流程表单信息：" + String.join("、", missing);
        }
        throw new RuntimeException(message);
    }

    private boolean isEmptyVariableValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }

    private String findPreviousUserTaskActivityId(Task task) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list();
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        for (HistoricActivityInstance activity : activities) {
            if (!Objects.equals(activity.getActivityId(), task.getTaskDefinitionKey())) {
                return activity.getActivityId();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public TaskFormInfo getTaskFormInfo(String taskId) {
        FlowFormContextReader.TaskContext context = formContexts.readTask(taskId);
        TaskFormInfo formInfo = context.formInfo();
        TaskApprovalPolicy policy = approvalPolicies.resolve(context.task(), context.model(), context.node());
        formInfo.setAllowApprove(policy.allowApprove());
        formInfo.setAllowReject(policy.allowReject());
        formInfo.setAllowDelegate(policy.allowDelegate());
        formInfo.setAllowReturn(policy.allowReturn());
        formInfo.setAllowTerminate(policy.allowTerminate());
        formInfo.setRequireSignature(policy.requireSignature());
        formInfo.setRequireComment(policy.requireComment());
        formInfo.setAllowRejectToStart(true);

        log.info("获取任务表单信息：taskId={}, formType={}, formKey={}",
                taskId, formInfo.getFormType(), formInfo.getFormKey());

        return formInfo;
    }

    @Override
    public TaskFormInfo getProcessFormInfo(String processInstanceId, String businessKey, String processDefKey,
                                           String taskId, String taskDefKey) {
        if (!isBlank(taskId)) {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                return getTaskFormInfo(taskId);
            }
        }
        return formContexts.readProcess(processInstanceId, businessKey, processDefKey, taskId, taskDefKey);
    }

    /**
     * 获取流程审批时间轴
     */
    @Override
    public List<Map<String, Object>> getProcessHistory(String processInstanceId) {
        // 1. 查询该流程实例的所有任务（按创建时间排序）
        LambdaQueryWrapper<FlowTask> wrapper = new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getProcessInstanceId, processInstanceId)
                .orderByAsc(FlowTask::getCreateTime);
        List<FlowTask> tasks = list(wrapper);

        // 2. 获取业务信息（用于展示发起节点）
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(processInstanceId);

        return taskHistoryProjection.assemble(tasks, business);
    }

    private void recordTaskError(String processInstanceId, String taskId, String activityId,
                                  String activityName, String errorStage, Throwable e) {
        FlowErrorLog errorLog = new FlowErrorLog();
        errorLog.setProcessInstanceId(processInstanceId);
        errorLog.setTaskId(taskId);
        errorLog.setActivityId(activityId);
        errorLog.setActivityName(activityName);
        errorLog.setErrorStage(errorStage);
        flowErrorLogService.recordError(errorLog, e);
    }

}
