package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Characterizes the existing service boundary before and after responsibility extraction. */
class FlowTaskAuthorizationTest {
    private BusinessFlowService service;
    private FlowClient client;
    private MockedStatic<SessionHelper> session;
    private Map<String, Object> task;

    @BeforeEach
    void setUp() {
        service = new BusinessFlowService(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        client = mock(FlowClient.class);
        ReflectionTestUtils.setField(service, "flowClient", client);
        session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getUserId).thenReturn(7L);
        task = new HashMap<>(Map.of("status", 1, "assignee", "7", "processInstanceId", "instance-1",
                "businessKey", "document:42:R2", "taskDefKey", "approval", "processDefKey", "process:2:id"));
        when(client.getTaskDetail("task-1")).thenReturn(FlowResult.success(task));
    }

    @AfterEach
    void closeSession() {
        session.close();
    }

    @Test
    void assignedTaskAcceptsWriteAndFillsCanonicalQueryIdentity() {
        BusinessTaskFormContextQueryDTO query = query();
        authorize(query, true);
        assertEquals("task-1", query.getTaskId());
        assertEquals("instance-1", query.getProcessInstanceId());
        assertEquals("document:42:R2", query.getBusinessKey());
        assertEquals("approval", query.getTaskDefKey());
        assertEquals("process:2:id", query.getProcessDefKey());
        verify(client).getTaskDetail("task-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"candidateUsers", "candidateGroups"})
    void unclaimedCandidateCanReadButMustClaimBeforeWrite(String candidateField) {
        task.put("status", "0");
        task.remove("assignee");
        task.put(candidateField, "candidateUsers".equals(candidateField) ? " 3, 7, 8 " : "reviewers");
        authorize(query(), false);
        assertEquals("请先签收任务后再保存业务字段",
                assertThrows(BusinessException.class, () -> authorize(query(), true)).getMessage());
    }

    @Test
    void anotherAssigneeOrNonCandidateCannotRead() {
        task.put("assignee", "9");
        task.put("candidateUsers", "7");
        assertEquals("无权访问当前任务业务表单",
                assertThrows(BusinessException.class, () -> authorize(query(), false)).getMessage());
        task.remove("assignee");
        task.put("candidateUsers", "17,70");
        assertThrows(BusinessException.class, () -> authorize(query(), false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "invalid", ""})
    void closedOrUnknownStatusCannotRead(String status) {
        task.put("status", status);
        assertEquals("当前任务已处理，不能访问待办业务表单",
                assertThrows(BusinessException.class, () -> authorize(query(), false)).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"processInstanceId", "businessKey", "taskDefKey"})
    void mismatchedTaskBindingIsRejected(String property) {
        BusinessTaskFormContextQueryDTO query = query();
        ReflectionTestUtils.setField(query, property, "different");
        assertThrows(BusinessException.class, () -> authorize(query, true));
    }

    @Test
    void retryBusinessKeyAndDisplayDefinitionAliasesKeepExistingCompatibility() {
        BusinessTaskFormContextQueryDTO query = query();
        query.setBusinessKey("document:42:R1");
        query.setProcessDefKey("legacy-display-alias");
        authorize(query, true);
        assertEquals("document:42:R1", query.getBusinessKey());
        assertEquals("legacy-display-alias", query.getProcessDefKey());
        query.setBusinessKey("document:42:Rbad");
        assertThrows(BusinessException.class, () -> authorize(query, true));
    }

    @Test
    void missingInputsKeepGuardOrderingAtPublicEntry() {
        BusinessTaskFormContextQueryDTO query = query();
        query.setTaskId(" ");
        ReflectionTestUtils.setField(service, "flowClient", null);
        assertEquals("任务ID不能为空",
                assertThrows(BusinessException.class, () -> service.getActionableTaskFormContext(query)).getMessage());
        query.setTaskId(" task-1 ");
        assertEquals("流程服务未配置，无法校验任务身份",
                assertThrows(BusinessException.class, () -> authorize(query, true)).getMessage());
        ReflectionTestUtils.setField(service, "flowClient", client);
        session.when(SessionHelper::getUserId).thenThrow(new IllegalStateException("no-session"));
        assertEquals("当前登录用户不能为空",
                assertThrows(BusinessException.class, () -> authorize(query, true)).getMessage());
    }

    @Test
    void upstreamFailuresDoNotTurnIntoAuthorization() {
        when(client.getTaskDetail("task-1")).thenReturn(null);
        assertEquals("任务不存在或无权访问",
                assertThrows(BusinessException.class, () -> authorize(query(), false)).getMessage());
        when(client.getTaskDetail("task-1")).thenReturn(FlowResult.error("upstream-denied"));
        assertEquals("upstream-denied",
                assertThrows(BusinessException.class, () -> authorize(query(), false)).getMessage());
        when(client.getTaskDetail("task-1")).thenThrow(new IllegalStateException("transport-failure"));
        assertEquals("任务身份校验失败，请稍后重试",
                assertThrows(BusinessException.class, () -> authorize(query(), false)).getMessage());
    }

    private BusinessTaskFormContextQueryDTO query() {
        BusinessTaskFormContextQueryDTO query = new BusinessTaskFormContextQueryDTO();
        query.setTaskId(" task-1 ");
        return query;
    }

    private void authorize(BusinessTaskFormContextQueryDTO query, boolean writeRequired) {
        ReflectionTestUtils.invokeMethod(service, "validateTaskAccess", query, writeRequired);
    }
}
