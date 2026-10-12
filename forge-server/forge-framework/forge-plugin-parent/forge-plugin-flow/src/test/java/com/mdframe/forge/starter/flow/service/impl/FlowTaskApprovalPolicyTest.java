package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.support.FlowFormContextReader;
import com.mdframe.forge.starter.flow.service.support.FlowProcessDefinitions;
import com.mdframe.forge.starter.flow.service.support.FlowBpmnPolicyReader;
import com.mdframe.forge.starter.flow.service.support.FlowTaskApprovalPolicies;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FlowTaskApprovalPolicyTest {
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();
    private final RepositoryService repository = mock(RepositoryService.class);
    private final FlowModelService models = mock(FlowModelService.class);
    private final FlowNodeConfigService configs = mock(FlowNodeConfigService.class);
    private final FlowProcessDefinitions definitions = mock(FlowProcessDefinitions.class);
    private final FlowFormContextReader contexts = mock(FlowFormContextReader.class);
    private final TaskService tasks = mock(TaskService.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final Task task = mock(Task.class);
    private final FlowTaskMapper storedTasks = mock(FlowTaskMapper.class);
    private final FlowErrorLogService errors = mock(FlowErrorLogService.class);
    private final FlowModel model = new FlowModel();
    private final UserTask node = new UserTask();
    private final org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "processDefinitions", definitions);
        FlowBpmnPolicyReader bpmnPolicies = new FlowBpmnPolicyReader(repository);
        ReflectionTestUtils.setField(service, "bpmnPolicies", bpmnPolicies);
        ReflectionTestUtils.setField(service, "approvalPolicies",
                new FlowTaskApprovalPolicies(bpmnPolicies, models, configs, definitions));
        ReflectionTestUtils.setField(service, "formContexts", contexts);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "baseMapper", storedTasks);
        ReflectionTestUtils.setField(service, "flowErrorLogService", errors);
        when(task.getTaskDefinitionKey()).thenReturn("review");
        when(task.getProcessDefinitionId()).thenReturn("approval:1:7");
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getAssignee()).thenReturn("42");
        when(definitions.resolveProcessDefinitionKey("approval:1:7", null)).thenReturn("approval");
        model.setId("model");
        when(models.getModelByKey("approval")).thenReturn(model);
        node.setId("review");
        process.setId("approval");
        process.addFlowElement(node);
        BpmnModel bpmn = new BpmnModel();
        bpmn.addProcess(process);
        when(repository.getBpmnModel("approval:1:7")).thenReturn(bpmn);
        context(model, node);
        TaskQuery query = mock(TaskQuery.class, RETURNS_SELF);
        when(tasks.createTaskQuery()).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
        FlowTask stored = new FlowTask();
        stored.setTenantId(1L);
        stored.setAssignee("42");
        stored.setStatus(1);
        when(storedTasks.selectByTaskIdForUpdate("task")).thenReturn(stored);
    }

    @Test
    void formDefaultsRemainAvailableWhenBpmnAndModelAreMissing() {
        context(null, null);
        when(repository.getBpmnModel("approval:1:7")).thenReturn(null);
        when(models.getModelByKey("approval")).thenReturn(null);
        TaskFormInfo form = service.getTaskFormInfo("task");
        assertThat(form.getAllowApprove()).isTrue();
        assertThat(form.getAllowReject()).isTrue();
        assertThat(form.getAllowDelegate()).isTrue();
        assertThat(form.getAllowReturn()).isFalse();
        assertThat(form.getAllowTerminate()).isFalse();
        assertThat(form.getRequireComment()).isTrue();
        assertThat(form.getRequireSignature()).isFalse();
    }

    @Test
    void formUsesAttributeThenExtensionThenNullableConfigurationOverlay() {
        attribute(node, "allowApprove", " no ");
        extension(node, "allowApprove", "yes");
        attribute(node, "allowReject", "N");
        attribute(node, "allowDelegate", "0");
        attribute(node, "allowReturn", "Y");
        attribute(node, "allowTerminate", "1");
        attribute(node, "requireComment", "false");
        attribute(node, "requireSignature", "   ");
        extension(node, "requireSignature", "TRUE");
        FlowNodeConfig config = new FlowNodeConfig();
        config.setAllowApprove(true);
        config.setAllowReturn(false);
        config.setRequireComment(true);
        when(configs.getByModelAndNode("model", "review")).thenReturn(config);
        TaskFormInfo form = service.getTaskFormInfo("task");
        assertThat(form.getAllowApprove()).isTrue();
        assertThat(form.getAllowReject()).isFalse();
        assertThat(form.getAllowDelegate()).isFalse();
        assertThat(form.getAllowReturn()).isFalse();
        assertThat(form.getAllowTerminate()).isTrue();
        assertThat(form.getRequireComment()).isTrue();
        assertThat(form.getRequireSignature()).isTrue();
        verifyNoInteractions(repository, models, definitions);
    }

    @Test
    void invalidBooleanRetainsDefaultAndDoesNotReadExtensionAfterNonblankAttribute() {
        attribute(node, "allowApprove", "invalid");
        extension(node, "allowApprove", "false");
        attribute(node, "allowReturn", "invalid");
        TaskFormInfo form = service.getTaskFormInfo("task");
        assertThat(form.getAllowApprove()).isTrue();
        assertThat(form.getAllowReturn()).isFalse();
    }

    @Test
    void missingContextResolvesNodeAndModelBeforeApplyingPolicy() {
        context(null, null);
        attribute(node, "allowApprove", "false");
        assertThat(service.getTaskFormInfo("task").getAllowApprove()).isFalse();
        verify(repository).getBpmnModel("approval:1:7");
        verify(models).getModelByKey("approval");
        verify(configs).getByModelAndNode("model", "review");
    }

    @Test
    void disabledApprovalStopsBeforeEngineMutation() {
        attribute(node, "allowApprove", "0");
        assertApprovalFails("comment", "signature", "当前节点不允许执行该审批操作");
    }

    @Test
    void defaultCommentRequirementStopsBeforeEngineMutation() {
        assertApprovalFails(" ", "signature", "请输入审批意见");
    }

    @Test
    void configuredSignatureRequirementOverridesBpmnAndStopsBeforeMutation() {
        attribute(node, "requireSignature", "false");
        FlowNodeConfig config = new FlowNodeConfig();
        config.setRequireSignature(true);
        when(configs.getByModelAndNode("model", "review")).thenReturn(config);
        assertApprovalFails("comment", " ", "请完成审批签名");
    }

    @Test
    void requiredVariableExtensionsKeepCustomErrorAfterPolicyValidation() {
        extension(node, "requiredVariables", "amount；reason");
        extension(node, "requiredMessage", "填写必填项目");
        assertApprovalFails("comment", "signature", "填写必填项目");
        verify(repository, times(2)).getBpmnModel("approval:1:7");
    }

    @Test
    void withdrawalReadsProcessExtensionAndPreservesRecordedFailure() {
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        ProcessInstance instance = mock(ProcessInstance.class);
        when(runtime.createProcessInstanceQuery()).thenReturn(query);
        when(query.singleResult()).thenReturn(instance);
        when(instance.getProcessDefinitionId()).thenReturn("approval:1:7");
        extension(process, "allowSubmitterWithdraw", " No ");
        assertThatThrownBy(() -> service.withdraw("instance", "42"))
                .hasMessage("当前流程不允许提交人撤回审批中的申请");
        verify(runtime, never()).deleteProcessInstance(anyString(), anyString());
        verify(errors).recordError(any(), any(Exception.class));
    }

    private void assertApprovalFails(String comment, String signature, String message) {
        assertThatThrownBy(() -> service.approve("task", "42", comment, signature,
                Map.of("amount", 1), 1L, null, null)).hasMessage(message);
        verify(tasks, never()).complete(anyString(), anyMap());
        verify(tasks, never()).addComment(anyString(), anyString(), anyString());
    }

    private void context(FlowModel contextModel, UserTask contextNode) {
        when(contexts.readTask("task")).thenReturn(new FlowFormContextReader.TaskContext(
                task, new TaskFormInfo(), contextModel, contextNode));
    }

    private void attribute(BaseElement element, String name, String value) {
        ExtensionAttribute attribute = new ExtensionAttribute();
        attribute.setNamespace("http://flowable.org/bpmn");
        attribute.setName(name);
        attribute.setValue(value);
        element.addAttribute(attribute);
    }

    private void extension(BaseElement element, String name, String value) {
        ExtensionElement extension = new ExtensionElement();
        extension.setName(name);
        extension.setElementText(value);
        element.addExtensionElement(extension);
    }
}
