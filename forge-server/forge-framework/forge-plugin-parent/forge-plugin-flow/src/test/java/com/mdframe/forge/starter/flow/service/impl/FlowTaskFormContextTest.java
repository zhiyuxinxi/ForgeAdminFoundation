package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.*;
import com.mdframe.forge.starter.flow.mapper.*;
import com.mdframe.forge.starter.flow.service.*;
import com.mdframe.forge.starter.flow.service.support.*;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.*;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowTaskFormContextTest {
    private final TaskService tasks = mock(TaskService.class, RETURNS_DEEP_STUBS);
    private final RuntimeService runtime = mock(RuntimeService.class, RETURNS_DEEP_STUBS);
    private final HistoryService history = mock(HistoryService.class, RETURNS_DEEP_STUBS);
    private final RepositoryService repository = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
    private final FlowModelService models = mock(FlowModelService.class);
    private final FlowNodeConfigService policies = mock(FlowNodeConfigService.class);
    private final FlowBusinessMapper businesses = mock(FlowBusinessMapper.class);
    private final FlowTaskMapper storedTasks = mock(FlowTaskMapper.class);
    private final FlowFormService forms = mock(FlowFormService.class);
    private final FlowFormInstanceMapper snapshots = mock(FlowFormInstanceMapper.class);
    private final FlowOrgIntegrationService organizations = mock(FlowOrgIntegrationService.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final Task task = mock(Task.class);
    private final FlowModel model = new FlowModel();
    private final UserTask node = new UserTask();
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();

    @BeforeEach
    void setUp() {
        when(tasks.createTaskQuery()).thenReturn(mock(org.flowable.task.api.TaskQuery.class, RETURNS_SELF));
        when(runtime.createProcessInstanceQuery()).thenReturn(mock(org.flowable.engine.runtime.ProcessInstanceQuery.class, RETURNS_SELF));
        when(history.createHistoricProcessInstanceQuery()).thenReturn(mock(org.flowable.engine.history.HistoricProcessInstanceQuery.class, RETURNS_SELF));
        when(history.createHistoricTaskInstanceQuery()).thenReturn(mock(org.flowable.task.api.history.HistoricTaskInstanceQuery.class, RETURNS_SELF));
        when(history.createHistoricVariableInstanceQuery()).thenReturn(mock(org.flowable.variable.api.history.HistoricVariableInstanceQuery.class, RETURNS_SELF));
        when(repository.createProcessDefinitionQuery()).thenReturn(mock(org.flowable.engine.repository.ProcessDefinitionQuery.class, RETURNS_SELF));
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "historyService", history);
        ReflectionTestUtils.setField(service, "flowBusinessMapper", businesses);
        ReflectionTestUtils.setField(service, "baseMapper", storedTasks);
        FlowUserDisplayNames names = new FlowUserDisplayNames(organizations, users);
        FlowProcessDefinitions definitions = new FlowProcessDefinitions(runtime, history, repository);
        FlowFormSnapshots formSnapshots = new FlowFormSnapshots(forms, snapshots);
        FlowFormConfiguration configuration = new FlowFormConfiguration(new FlowFormMetadata(repository), formSnapshots);
        FlowFormContextReader contexts = new FlowFormContextReader(tasks, runtime, history, businesses, storedTasks,
                models, definitions, names, configuration, formSnapshots);
        ReflectionTestUtils.setField(service, "processDefinitions", definitions);
        FlowBpmnPolicyReader bpmnPolicies = new FlowBpmnPolicyReader(repository);
        ReflectionTestUtils.setField(service, "bpmnPolicies", bpmnPolicies);
        ReflectionTestUtils.setField(service, "approvalPolicies",
                new FlowTaskApprovalPolicies(bpmnPolicies, models, policies, definitions));
        ReflectionTestUtils.setField(service, "formContexts", contexts);
        when(tasks.createTaskQuery().taskId("task").singleResult()).thenReturn(task);
        when(task.getName()).thenReturn("Review");
        when(task.getTaskDefinitionKey()).thenReturn("review");
        when(task.getProcessDefinitionId()).thenReturn("approval:2:7");
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(tasks.getVariables("task")).thenReturn(Map.of("amount", 20));
        model.setId("12");
        model.setFormType("dynamic");
        model.setFormId("global-form");
        model.setFormJson("{\"model\":true}");
        when(models.getModelByKey("approval")).thenReturn(model);
        node.setId("review");
        BpmnModel bpmn = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("approval");
        process.addFlowElement(node);
        bpmn.addProcess(process);
        when(repository.getBpmnModel("approval:2:7")).thenReturn(bpmn);
    }

    @Test
    void taskFormPreservesLookupOrderSnapshotAndTablePolicyPrecedence() {
        FlowBusiness business = business();
        when(businesses.selectByProcessInstanceId("instance")).thenReturn(business);
        when(organizations.getUserInfo("42")).thenReturn(Map.of("realName", " Starter "));
        node.setFormKey("node-form");
        attribute("formJson", "{\"inline\":true}");
        attribute("allowApprove", "false");
        FlowNodeConfig policy = new FlowNodeConfig();
        policy.setAllowApprove(true);
        policy.setAllowReject(false);
        policy.setRequireSignature(true);
        when(policies.getByModelAndNode("12", "review")).thenReturn(policy);
        FlowFormInstance snapshot = snapshot();
        when(snapshots.selectByProcessInstanceId("instance")).thenReturn(snapshot);
        clearInvocations(tasks, businesses, organizations, models, repository, snapshots, policies);

        TaskFormInfo result = service.getTaskFormInfo("task");

        assertThat(result.getStartUserName()).isEqualTo("Starter");
        assertThat(result.getBusinessKey()).isEqualTo("business");
        assertThat(result.getVariables()).containsEntry("amount", 20);
        assertThat(result.getFormKey()).isEqualTo("node-form");
        assertThat(result.getFormJson()).isEqualTo("{\"inline\":true}");
        assertThat(result.getSchemaSnapshot()).isEqualTo("{\"snapshot\":true}");
        assertThat(result.getFormData()).isEqualTo("{\"value\":1}");
        assertThat(result.getAllowApprove()).isTrue();
        assertThat(result.getAllowReject()).isFalse();
        assertThat(result.getRequireSignature()).isTrue();
        assertThat(result.getAllowRejectToStart()).isTrue();
        InOrder order = inOrder(tasks, businesses, organizations, models, repository, snapshots, policies);
        order.verify(tasks).getVariables("task");
        order.verify(businesses).selectByProcessInstanceId("instance");
        order.verify(organizations).getUserInfo("42");
        order.verify(models).getModelByKey("approval");
        order.verify(repository).getBpmnModel("approval:2:7");
        order.verify(snapshots).selectByProcessInstanceId("instance");
        order.verify(policies).getByModelAndNode("12", "review");
        verify(models, times(1)).getModelByKey("approval");
        verify(repository, times(1)).getBpmnModel("approval:2:7");
        verifyNoInteractions(forms, users);
    }

    @Test
    void missingTaskFailsAndActiveProcessFormUsesSamePublicTaskContract() {
        when(tasks.createTaskQuery().taskId("missing").singleResult()).thenReturn(null);
        assertThatThrownBy(() -> service.getTaskFormInfo("missing")).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(models, businesses, snapshots);
        when(tasks.createTaskQuery().singleResult()).thenReturn(task);
        clearInvocations(tasks.createTaskQuery());
        TaskFormInfo result = service.getProcessFormInfo(null, null, null, "task", null);
        assertThat(result.getTaskName()).isEqualTo("Review");
        assertThat(result.getAllowRejectToStart()).isTrue();
        verify(tasks.createTaskQuery().taskId("task"), times(2)).singleResult();
        verifyNoInteractions(storedTasks);
    }

    @Test
    void historicProcessPreservesMetadataAndRuntimeVariablePrecedenceWithoutActionPermissions() {
        when(tasks.createTaskQuery().taskId("finished").singleResult()).thenReturn(null);
        FlowTask stored = new FlowTask();
        stored.setTaskName("Finished review");
        stored.setProcessInstanceId("instance");
        stored.setProcessDefId("approval:2:7");
        stored.setProcessDefKey("approval");
        stored.setTaskDefKey("review");
        when(storedTasks.selectByIdOrTaskId("finished")).thenReturn(stored);
        when(businesses.selectByBusinessKey("business")).thenReturn(business());
        when(runtime.getVariables("instance")).thenReturn(Map.of("amount", 20));
        HistoricVariableInstance historicAmount = variable("amount", 10);
        HistoricVariableInstance historicOnly = variable("historical", "kept");
        when(history.createHistoricVariableInstanceQuery().processInstanceId("instance").list())
                .thenReturn(List.of(historicAmount, historicOnly));
        TaskFormInfo result = service.getProcessFormInfo(null, "business", null, "finished", null);
        assertThat(result.getProcessDefKey()).isEqualTo("approval");
        assertThat(result.getTaskName()).isEqualTo("Finished review");
        assertThat(result.getVariables()).containsEntry("amount", 20).containsEntry("historical", "kept")
                .containsEntry("businessKey", "business");
        assertThat(List.of(result.getAllowApprove(), result.getAllowReject(), result.getAllowDelegate(),
                result.getAllowReturn(), result.getAllowTerminate(), result.getAllowRejectToStart(),
                result.getRequireComment(), result.getRequireSignature())).containsOnly(false);
        verify(runtime.createProcessInstanceQuery().processInstanceId("instance")).singleResult();
        verify(tasks.createTaskQuery().processInstanceId("instance").active().orderByTaskCreateTime().asc()).list();
        verify(history.createHistoricTaskInstanceQuery().processInstanceId("instance")
                .orderByHistoricTaskInstanceStartTime().asc()).list();
        verifyNoInteractions(policies);
    }

    @Test
    void businessFormMergesOnlyNonblankNodeOverridesAndSkipsDynamicSnapshot() {
        model.setFormType("business");
        model.setFormJson("{\"formRef\":{\"formKey\":\"global-key\",\"providerKey\":\"metadata-provider\",\"viewKey\":\"global-view\"},\"formName\":\"Root name\"}");
        attribute("formRef", "{\"formKey\":\"node-key\",\"providerKey\":\" \",\"viewKey\":\"node-view\"}");
        attribute("formMode", "BUSINESS_OBJECT_FORM");
        TaskFormInfo result = service.getTaskFormInfo("task");
        assertThat(result.getFormType()).isEqualTo("business");
        assertThat(result.getFormKey()).isEqualTo("node-key");
        assertThat(result.getProviderKey()).isEqualTo("metadata-provider");
        assertThat(result.getFormName()).isEqualTo("Root name");
        assertThat(result.getFormRef()).containsEntry("viewKey", "node-view")
                .containsEntry("type", "BUSINESS_OBJECT_FORM");
        verifyNoInteractions(forms, snapshots);
    }

    @Test
    void externalNodeAttributesWinExtensionsAndMalformedReferenceFallsBack() {
        attribute("formUrl", "/node-form");
        attribute("formRef", "{broken");
        extension("formUrl", "/ignored");
        extension("formRef", "{\"providerKey\":\"fallback-provider\"}");
        extension("formTarget", "drawer");
        extension("formFieldPermissions", "{\"amount\":\"read\"}");
        TaskFormInfo result = service.getTaskFormInfo("task");
        assertThat(result.getFormType()).isEqualTo("external");
        assertThat(result.getFormUrl()).isEqualTo("/node-form");
        assertThat(result.getFormTarget()).isEqualTo("drawer");
        assertThat(result.getProviderKey()).isEqualTo("fallback-provider");
        assertThat(result.getFormFieldPermissions()).isEqualTo("{\"amount\":\"read\"}");
        verifyNoInteractions(forms, snapshots);
    }

    @Test
    void schemaReadFailureFallsBackToSnapshotAndUserDirectoryFallbackIsPreserved() {
        node.setFormKey("node-form");
        when(forms.getFormSchema("node-form")).thenThrow(new IllegalStateException("schema unavailable"));
        when(snapshots.selectByProcessInstanceId("instance")).thenReturn(snapshot());
        when(businesses.selectByProcessInstanceId("instance")).thenReturn(business());
        when(organizations.getUserInfo("42")).thenThrow(new IllegalStateException("directory unavailable"));
        SysUser user = new SysUser();
        user.setRealName(" Local name ");
        when(users.getById(42L)).thenReturn(user);
        TaskFormInfo result = service.getTaskFormInfo("task");
        assertThat(result.getFormJson()).isEqualTo("{\"snapshot\":true}");
        assertThat(result.getFormKey()).isEqualTo("node-form");
        assertThat(result.getStartUserName()).isEqualTo("Local name");
    }

    @Test
    void modelNumericSchemaAndUnavailableSnapshotKeepResolvedForm() {
        model.setFormId("23");
        model.setFormJson(null);
        FlowForm form = new FlowForm();
        form.setFormSchema("{\"stored\":true}");
        when(forms.getById(23L)).thenReturn(form);
        when(snapshots.selectByProcessInstanceId("instance")).thenThrow(new IllegalStateException("snapshot unavailable"));
        TaskFormInfo result = service.getTaskFormInfo("task");
        assertThat(result.getFormJson()).isEqualTo("{\"stored\":true}");
        assertThat(result.getFormInstanceId()).isNull();
        verify(forms).getById(23L);
    }

    @Test
    void opaqueDefinitionFallsBackFromRepositoryFailureToBpmnWithoutChangingForm() {
        when(task.getProcessDefinitionId()).thenReturn("opaque-definition");
        when(repository.createProcessDefinitionQuery().processDefinitionId("opaque-definition").singleResult())
                .thenThrow(new IllegalStateException("definition unavailable"));
        BpmnModel bpmn = repository.getBpmnModel("approval:2:7");
        when(repository.getBpmnModel("opaque-definition")).thenReturn(bpmn);
        TaskFormInfo result = service.getTaskFormInfo("task");
        assertThat(result.getProcessDefKey()).isEqualTo("approval");
        assertThat(result.getFormJson()).isEqualTo("{\"model\":true}");
        verify(repository, times(2)).getBpmnModel("opaque-definition");
    }

    private FlowBusiness business() {
        FlowBusiness business = new FlowBusiness();
        business.setProcessInstanceId("instance");
        business.setBusinessKey("business");
        business.setApplyUserId("42");
        business.setApplyUserName("Fallback");
        business.setTitle("Request");
        return business;
    }

    private FlowFormInstance snapshot() {
        FlowFormInstance value = new FlowFormInstance();
        value.setId(31L);
        value.setFormKey("snapshot-key");
        value.setSchemaSnapshot("{\"snapshot\":true}");
        value.setFormData("{\"value\":1}");
        return value;
    }

    private HistoricVariableInstance variable(String name, Object value) {
        HistoricVariableInstance variable = mock(HistoricVariableInstance.class);
        when(variable.getVariableName()).thenReturn(name);
        when(variable.getValue()).thenReturn(value);
        return variable;
    }

    private void attribute(String name, String value) {
        ExtensionAttribute attribute = new ExtensionAttribute();
        attribute.setNamespace("http://flowable.org/bpmn");
        attribute.setName(name);
        attribute.setValue(value);
        node.addAttribute(attribute);
    }

    private void extension(String name, String value) {
        ExtensionElement element = new ExtensionElement();
        element.setName(name);
        element.setElementText(value);
        node.addExtensionElement(element);
    }
}
