package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramReader;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramInfoReader;
import com.mdframe.forge.starter.flow.service.support.ProcessNodeSnapshotLoader;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FlowTaskDiagramInfoTest {
    private final HistoryService history = mock(HistoryService.class);
    private final RepositoryService repository = mock(RepositoryService.class);
    private final HistoricProcessInstance instance = mock(HistoricProcessInstance.class);
    private final HistoricProcessInstanceQuery instances = mock(HistoricProcessInstanceQuery.class, RETURNS_SELF);
    private final ProcessDefinitionQuery definitions = mock(ProcessDefinitionQuery.class, RETURNS_SELF);
    private final ProcessDiagramReader images = mock(ProcessDiagramReader.class);
    private final ProcessNodeSnapshotLoader snapshots = mock(ProcessNodeSnapshotLoader.class);
    private final FlowOrgIntegrationService organizations = mock(FlowOrgIntegrationService.class);
    private final BpmnModel model = new BpmnModel();
    private final FlowTaskServiceImpl service = new FlowTaskServiceImpl();

    @BeforeEach
    void configure() {
        var process = new org.flowable.bpmn.model.Process();
        process.setId("definition");
        var task = new UserTask();
        task.setId("review");
        task.setName("审核");
        process.addFlowElement(task);
        model.addProcess(process);
        var definition = mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition");
        when(history.createHistoricProcessInstanceQuery()).thenReturn(instances);
        when(instances.singleResult()).thenReturn(instance);
        when(instance.getProcessDefinitionId()).thenReturn("definition");
        when(instance.getName()).thenReturn("申请流程");
        when(instance.getStartUserId()).thenReturn("user-7");
        when(instance.getStartTime()).thenReturn(new Date(1000));
        when(repository.getBpmnModel("definition")).thenReturn(model);
        when(repository.createProcessDefinitionQuery()).thenReturn(definitions);
        when(definitions.singleResult()).thenReturn(definition);
        when(organizations.getUserInfo("user-7")).thenReturn(Map.of("realName", "张三"));
        when(snapshots.load("instance")).thenReturn(
                new ProcessNodeSnapshotLoader.Snapshot(List.of(), Set.of(), Map.of(), Map.of()));
        ReflectionTestUtils.setField(service, "historyService", history);
        ReflectionTestUtils.setField(service, "processDiagramReader", images);
        ReflectionTestUtils.setField(service, "processDiagramInfoReader",
                new ProcessDiagramInfoReader(history, repository, images, snapshots, organizations));
    }

    @Test
    void metadataXmlNodesAndAllTerminalStatesArePreservedWithoutRenderingImage() {
        var result = service.getProcessDiagramInfo("instance");
        assertThat(result.getProcessInstanceId()).isEqualTo("instance");
        assertThat(result.getProcessDefinitionId()).isEqualTo("definition");
        assertThat(result.getProcessName()).isEqualTo("申请流程");
        assertThat(result.getStartUserId()).isEqualTo("user-7");
        assertThat(result.getStartUserName()).isEqualTo("张三");
        assertThat(result.getStartTime()).isEqualTo(new Date(1000));
        assertThat(result.getStatus()).isEqualTo("running");
        assertThat(result.getBpmnXml()).contains("审核", "review");
        assertThat(result.getNodes()).extracting("nodeId").containsExactly("review");
        when(instance.getEndTime()).thenReturn(new Date(2000));
        assertThat(service.getProcessDiagramInfo("instance").getStatus()).isEqualTo("completed");
        when(instance.getDeleteReason()).thenReturn("cancelled");
        assertThat(service.getProcessDiagramInfo("instance").getStatus()).isEqualTo("terminated");
        verifyNoInteractions(images);
    }

    @Test
    void missingInstanceOrModelReturnsNullBeforeDependentReads() {
        when(instances.singleResult()).thenReturn(null);
        assertThat(service.getProcessDiagramInfo("instance", true)).isNull();
        verifyNoInteractions(repository, images, snapshots, organizations);
        when(instances.singleResult()).thenReturn(instance);
        when(repository.getBpmnModel("definition")).thenReturn(null);
        assertThat(service.getProcessDiagramInfo("instance", true)).isNull();
        verifyNoInteractions(images, snapshots, organizations);
    }

    @Test
    void optionalUserAndXmlFailuresPreserveRemainingView() {
        when(organizations.getUserInfo("user-7")).thenThrow(new IllegalStateException("directory"));
        when(repository.getBpmnModel("definition")).thenReturn(model)
                .thenThrow(new IllegalStateException("xml"));
        var result = service.getProcessDiagramInfo("instance");
        assertThat(result).isNotNull();
        assertThat(result.getStartUserName()).isNull();
        assertThat(result.getBpmnXml()).isNull();
        assertThat(result.getNodes()).hasSize(1);
    }

    @Test
    void definitionQueryFailureKeepsExistingWholeViewFailureBoundary() {
        when(definitions.singleResult()).thenThrow(new IllegalStateException("definition query"));
        assertThat(service.getProcessDiagramInfo("instance", true)).isNull();
        verifyNoInteractions(images, snapshots);
    }

    @Test
    void requestedImageFailureKeepsExistingWholeViewFailureBoundary() {
        when(images.read("instance")).thenThrow(new IllegalStateException("image"));
        assertThat(service.getProcessDiagramInfo("instance", true)).isNull();
        verifyNoInteractions(snapshots);
    }

    @Test
    void nodeReadFailureKeepsExistingWholeViewFailureBoundary() {
        when(snapshots.load("instance")).thenThrow(new IllegalStateException("nodes"));
        assertThat(service.getProcessDiagramInfo("instance")).isNull();
        verifyNoInteractions(images);
    }
}
