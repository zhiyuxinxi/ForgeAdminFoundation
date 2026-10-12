package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.support.FlowBpmnPolicyReader;
import com.mdframe.forge.starter.flow.service.support.FlowTaskApprovalPolicies;
import com.mdframe.forge.starter.flow.service.support.TaskApprovalPolicy;
import com.mdframe.forge.starter.flow.service.support.FlowTaskCompletion;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Public service behavior with observable engine/mapper boundaries, not a Flowable database test. */
class FlowTaskCompletionTest {
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();
    private final TaskService tasks = mock(TaskService.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final HistoryService history = mock(HistoryService.class);
    private final FlowTaskMapper mapper = mock(FlowTaskMapper.class);
    private final FlowBpmnPolicyReader bpmn = mock(FlowBpmnPolicyReader.class);
    private final FlowTaskApprovalPolicies policies = mock(FlowTaskApprovalPolicies.class);
    private final FlowErrorLogService errors = mock(FlowErrorLogService.class);
    private final TaskQuery taskQuery = mock(TaskQuery.class, RETURNS_SELF);
    private final HistoricTaskInstanceQuery historic = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
    private final FlowTask stored = new FlowTask();
    private Task current;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "completion-test");
        assistant.setCurrentNamespace(FlowTaskMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, FlowTask.class);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "historyService", history);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "bpmnPolicies", bpmn);
        ReflectionTestUtils.setField(service, "approvalPolicies", policies);
        ReflectionTestUtils.setField(service, "flowErrorLogService", errors);
        ReflectionTestUtils.setField(service, "taskCompletion",
                new FlowTaskCompletion(tasks, runtime, history, bpmn, mapper));
        current = task("task", "42");
        stored.setTenantId(1L);
        stored.setAssignee("42");
        stored.setStatus(1);
        when(mapper.selectByTaskIdForUpdate("task")).thenReturn(stored);
        when(mapper.update(any(FlowTask.class), any(Wrapper.class))).thenReturn(1);
        when(tasks.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(current);
        when(history.createHistoricTaskInstanceQuery()).thenReturn(historic);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(runtime.createProcessInstanceQuery()).thenReturn(query);
        when(query.singleResult()).thenReturn(instance);
        when(instance.getProcessDefinitionId()).thenReturn("definition");
        when(policies.resolve(current)).thenReturn(new TaskApprovalPolicy(true, true, true, false, false, false, false));
    }

    @Test
    void firstOnlyResolvesDelegationThenCompletesAndUpdatesBeforeAutomaticApproval() {
        when(current.getDelegationState()).thenReturn(DelegationState.PENDING);
        Task next = task("next", "42");
        when(bpmn.readProcessString("definition", "autoApprovalMode")).thenReturn("firstOnly");
        when(taskQuery.list()).thenReturn(List.of(next));
        when(historic.count()).thenReturn(1L);
        Map<String, Object> original = Map.of("amount", 12, "approved", false);
        service.approve("task", "42", "reviewed", "signed", original, 1L, "request", "digest");
        var order = inOrder(mapper, tasks, runtime);
        order.verify(mapper).selectByTaskIdForUpdate("task");
        order.verify(tasks).addComment("task", "instance", "reviewed");
        order.verify(tasks).resolveTask("task");
        Map<String, Object> completed = Map.of("amount", 12, "approved", true, "approvalResult", "approve");
        order.verify(runtime).setVariables("instance", completed);
        order.verify(tasks).complete("task", completed);
        order.verify(mapper).update(argThat((FlowTask value) -> value.getStatus() == 2
                && "request".equals(value.getActionIdempotencyKey()) && "digest".equals(value.getActionRequestDigest())
                && "APPROVE".equals(value.getActionType()) && "signed".equals(value.getSignature())), any(Wrapper.class));
        order.verify(runtime).createProcessInstanceQuery();
        order.verify(tasks).addComment("next", "instance", "系统自动同意（重复审批人）");
        order.verify(tasks).complete("next", Map.of("approved", true, "approvalResult", "approve"));
        order.verify(mapper).update(argThat((FlowTask value) -> value.getStatus() == 2
                && "系统自动同意（重复审批人）".equals(value.getComment())), any(Wrapper.class));
        verify(tasks, times(1)).complete(eq("next"), anyMap());
        verify(historic).taskAssignee("42");
        assertThat(original).containsEntry("approved", false).doesNotContainKey("approvalResult");
    }

    @Test
    void consecutiveOnlyCompletesWhenLastFinishedAssigneeMatches() {
        Task next = task("next", "42");
        HistoricTaskInstance previous = mock(HistoricTaskInstance.class);
        when(previous.getAssignee()).thenReturn("42");
        when(bpmn.readProcessString("definition", "autoApprovalMode")).thenReturn("consecutive");
        when(taskQuery.list()).thenReturn(List.of(next));
        when(historic.listPage(0, 1)).thenReturn(List.of(previous));
        approve();
        verify(historic).orderByHistoricTaskInstanceEndTime();
        verify(historic).desc();
        verify(tasks).complete("next", Map.of("approved", true, "approvalResult", "approve"));
        verify(historic, never()).count();
    }

    @Test
    void differentLastAssigneeLeavesActiveTaskAndStopsAfterOneRead() {
        Task next = task("next", "7");
        HistoricTaskInstance previous = mock(HistoricTaskInstance.class);
        when(previous.getAssignee()).thenReturn("42");
        when(bpmn.readProcessString("definition", "autoApprovalMode")).thenReturn("consecutive");
        when(taskQuery.list()).thenReturn(List.of(next));
        when(historic.listPage(0, 1)).thenReturn(List.of(previous));
        approve();
        verify(taskQuery, times(1)).list();
        verify(tasks, never()).complete(eq("next"), anyMap());
    }

    @Test
    void automaticApprovalKeepsThirtyStepBound() {
        when(bpmn.readProcessString("definition", "autoApprovalMode")).thenReturn("firstOnly");
        when(historic.count()).thenReturn(1L);
        AtomicInteger counter = new AtomicInteger();
        when(taskQuery.list()).thenAnswer(ignored -> List.of(task("auto-" + counter.incrementAndGet(), "42")));
        approve();
        assertThat(counter.get()).isEqualTo(30);
        verify(tasks, times(30)).complete(startsWith("auto-"), anyMap());
        verify(mapper, times(31)).update(any(FlowTask.class), any(Wrapper.class));
    }

    @Test
    void automaticFailurePropagatesOriginalErrorToPublicBoundary() {
        Task next = task("next", "42");
        when(bpmn.readProcessString("definition", "autoApprovalMode")).thenReturn("firstOnly");
        when(taskQuery.list()).thenReturn(List.of(next));
        when(historic.count()).thenReturn(1L);
        IllegalStateException failure = new IllegalStateException("engine-completion-failed");
        doThrow(failure).when(tasks).complete(eq("next"), anyMap());
        assertThatThrownBy(this::approve).isSameAs(failure);
        verify(errors).recordError(argThat(value -> "TASK_APPROVE".equals(value.getErrorStage())
                && "task".equals(value.getTaskId()) && "instance".equals(value.getProcessInstanceId())), same(failure));
        verify(mapper, times(1)).update(any(FlowTask.class), any(Wrapper.class));
    }

    @Test
    void rejectionUsesSameCompletionButNeverAdvancesRepeatedApprovals() {
        when(current.getDelegationState()).thenReturn(DelegationState.PENDING);
        service.reject("task", "42", "declined", "signed", 1L, "request", "digest");
        var order = inOrder(tasks, runtime, mapper);
        order.verify(tasks).resolveTask("task");
        order.verify(runtime).setVariables("instance", Map.of("approved", false, "approvalResult", "reject"));
        order.verify(tasks).complete("task", Map.of("approved", false, "approvalResult", "reject"));
        order.verify(mapper).update(argThat((FlowTask value) -> value.getStatus() == 3
                && "REJECT".equals(value.getActionType())), any(Wrapper.class));
        verify(runtime, never()).createProcessInstanceQuery();
        verifyNoInteractions(history);
    }

    @Test
    void tenantDenialAndSuccessfulReplayStopBeforeEngineSideEffects() {
        assertThatThrownBy(() -> service.approve("task", "42", null, null, null, 2L, null, null))
                .hasMessage("FLOW_TASK_TENANT_MISMATCH");
        stored.setStatus(2);
        stored.setActionType("APPROVE");
        stored.setActionIdempotencyKey("request");
        stored.setActionRequestDigest("digest");
        service.approve("task", "42", null, null, null, 1L, "request", "digest");
        verifyNoInteractions(tasks, runtime, history);
        verify(mapper, never()).update(any(FlowTask.class), any(Wrapper.class));
    }

    private void approve() {
        service.approve("task", "42", "reviewed", null, null, 1L, null, null);
    }

    private Task task(String id, String assignee) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getAssignee()).thenReturn(assignee);
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getProcessDefinitionId()).thenReturn("definition");
        when(task.getTaskDefinitionKey()).thenReturn("review");
        when(task.getName()).thenReturn("Review");
        return task;
    }
}
