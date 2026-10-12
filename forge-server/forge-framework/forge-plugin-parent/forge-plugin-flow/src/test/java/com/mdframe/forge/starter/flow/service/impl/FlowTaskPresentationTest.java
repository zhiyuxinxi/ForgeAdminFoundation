package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayAdapter;
import com.mdframe.forge.flow.client.spi.FlowBusinessListDisplayItem;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.support.FlowUserDisplayNames;
import com.mdframe.forge.starter.flow.service.support.FlowTaskListPresentation;
import com.mdframe.forge.starter.flow.service.support.FlowTaskHistoryProjection;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowTaskPresentationTest {
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();
    private final FlowTaskMapper mapper = mock(FlowTaskMapper.class);
    private final FlowBusinessMapper businesses = mock(FlowBusinessMapper.class);
    private final FlowUserDisplayNames names = mock(FlowUserDisplayNames.class);
    private final FlowBusinessListDisplayAdapter adapter = mock(FlowBusinessListDisplayAdapter.class);
    private final TaskService tasks = mock(TaskService.class);
    private final Page<FlowTask> page = new Page<>(2, 10);
    private final FlowTask task = new FlowTask();

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "flowBusinessMapper", businesses);
        ReflectionTestUtils.setField(service, "taskHistoryProjection", new FlowTaskHistoryProjection(names));
        ReflectionTestUtils.setField(service, "taskListPresentation", new FlowTaskListPresentation(names, adapter));
        ReflectionTestUtils.setField(service, "taskService", tasks);
        when(names.resolve(any(), any())).thenAnswer(call -> call.getArgument(1));
        when(names.resolve(eq("owner"), any())).thenReturn("发起人");
        when(names.resolve(eq("a"), any())).thenReturn("甲");
        when(names.resolve(eq("b"), any())).thenReturn("乙");
        task.setStartUserId("owner");
        task.setAssigneeName(" ");
        task.setCandidateUsers("a, b,a, ");
        task.setBusinessKey("record-7");
        task.setProcessName("Original process");
        task.setProcessDefKey("definition");
        task.setObjectCode("original-object");
        task.setBusinessSummary("original-summary");
        page.setRecords(List.of(task));
        page.setTotal(17);
        when(mapper.selectTodoTasks(page, "user", "title", "category", 2)).thenReturn(page);
        when(mapper.selectDoneTasks(page, "user", "title", "category", 2)).thenReturn(page);
        when(mapper.selectStartedTasks(page, "user", "title", "category", 2)).thenReturn(page);
    }

    @Test
    void threeListEntrypointsKeepPaginationFiltersNamesAndDisplayPrecedence() {
        doAnswer(call -> {
            List<FlowBusinessListDisplayItem> items = call.getArgument(0);
            var item = items.get(0);
            assertThat(item.getBusinessKey()).isEqualTo("record-7");
            item.setObjectCode(" ");
            item.setBusinessSummary("enriched");
            item.setProcessName("Adapter process");
            item.setProcessDefinitionName("Published definition");
            return null;
        }).when(adapter).enrich(anyList());
        assertThat(service.todoTasks(page, "user", "title", "category", 2)).isSameAs(page);
        assertThat(service.doneTasks(page, "user", "title", "category", 2)).isSameAs(page);
        assertThat(service.startedTasks(page, "user", "title", "category", 2)).isSameAs(page);
        assertThat(page.getTotal()).isEqualTo(17);
        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(task.getStartUserName()).isEqualTo("发起人");
        assertThat(task.getAssigneeName()).isEqualTo("甲, 乙");
        assertThat(task.getObjectCode()).isEqualTo("original-object");
        assertThat(task.getBusinessSummary()).isEqualTo("enriched");
        assertThat(task.getProcessName()).isEqualTo("Original process");
        assertThat(task.getProcessDefinitionName()).isEqualTo("Published definition");
        verify(adapter, times(3)).enrich(anyList());
    }

    @Test
    void adapterFailureRetainsUserNamesAndOriginalBusinessFields() {
        doAnswer(call -> {
            List<FlowBusinessListDisplayItem> items = call.getArgument(0);
            items.get(0).setBusinessSummary("must not leak");
            throw new IllegalStateException("adapter unavailable");
        }).when(adapter).enrich(anyList());
        assertThat(service.todoTasks(page, "user", "title", "category", 2)).isSameAs(page);
        assertThat(task.getStartUserName()).isEqualTo("发起人");
        assertThat(task.getBusinessSummary()).isEqualTo("original-summary");
    }

    @Test
    void emptyAndNullPagesDoNotInvokeDisplayConsumers() {
        when(mapper.selectTodoTasks(page, "user", "title", "category", 2)).thenReturn(null);
        assertThat(service.todoTasks(page, "user", "title", "category", 2)).isNull();
        page.setRecords(List.of());
        assertThat(service.doneTasks(page, "user", "title", "category", 2)).isSameAs(page);
        verifyNoInteractions(names, adapter);
    }

    @Test
    void candidatePageUsesSamePresentationAfterEngineSelection() {
        var query = mock(TaskQuery.class, RETURNS_SELF);
        var candidate = mock(Task.class);
        when(candidate.getId()).thenReturn("candidate");
        when(tasks.createTaskQuery()).thenReturn(query);
        when(query.list()).thenReturn(List.of(candidate));
        when(mapper.selectPage(eq(page), any(Wrapper.class))).thenReturn(page);
        assertThat(service.candidateTasks(page, "user", null, "title")).isSameAs(page);
        assertThat(task.getStartUserName()).isEqualTo("发起人");
        assertThat(task.getAssigneeName()).isEqualTo("甲, 乙");
        verify(query).taskCandidateUser("user");
        verify(query).taskUnassigned();
        verify(adapter).enrich(anyList());
    }

    @Test
    void emptyCandidateSelectionReturnsOriginalPageWithoutPresentation() {
        assertThat(service.candidateTasks(page, null, null, "title")).isSameAs(page);
        verifyNoInteractions(mapper, names, adapter, tasks);
    }

    @Test
    void historyKeepsStartFallbackEveryActionAndTaskOrdering() {
        List<FlowTask> history = new ArrayList<>();
        for (int status = 0; status <= 9; status++) {
            var item = new FlowTask();
            item.setTaskId("t" + status);
            item.setTaskName("Task " + status);
            item.setStatus(status);
            item.setAssignee("a");
            history.add(item);
        }
        when(mapper.selectList(any(Wrapper.class))).thenReturn(history);
        var business = new FlowBusiness();
        business.setApplyUserId("owner");
        business.setCreateTime(LocalDateTime.of(2026, 10, 9, 8, 0));
        when(businesses.selectByProcessInstanceId("instance")).thenReturn(business);
        var result = service.getProcessHistory("instance");
        assertThat(result).hasSize(11);
        assertThat(result.get(0)).containsEntry("action", "start").containsEntry("assigneeName", "发起人")
                .containsEntry("createTime", "2026-10-09T08:00").containsEntry("completeTime", "2026-10-09T08:00");
        assertThat(result.subList(1, 11)).extracting(row -> row.get("action")).containsExactly(
                "pending", "claim", "approve", "reject", "delegate", "delegate", "withdraw", "return", "terminate", "pending");
        assertThat(result.get(1)).containsEntry("taskId", "t0").containsEntry("assigneeName", "甲")
                .containsEntry("comment", "").containsEntry("signature", null).containsEntry("completeTime", null);
        var order = inOrder(mapper, businesses);
        order.verify(mapper).selectList(any(Wrapper.class));
        order.verify(businesses).selectByProcessInstanceId("instance");
    }

    @Test
    void missingBusinessHistoryIsEmptyAndPersistenceFailureStillPropagates() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        assertThat(service.getProcessHistory("instance")).isEmpty();
        when(mapper.selectList(any(Wrapper.class))).thenThrow(new IllegalStateException("storage"));
        assertThatThrownBy(() -> service.getProcessHistory("instance"))
                .isInstanceOf(IllegalStateException.class).hasMessage("storage");
    }
}
