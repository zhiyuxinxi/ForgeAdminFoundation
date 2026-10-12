package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.service.support.ProcessDiagramReader;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramInfoReader;
import com.mdframe.forge.starter.flow.service.support.ProcessNodeSnapshotLoader;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowTaskDiagramDelegationTest {
    @Test
    void publicImageEndpointDelegatesToReader() {
        var service = new FlowTaskServiceImpl();
        var reader = mock(ProcessDiagramReader.class);
        ReflectionTestUtils.setField(service, "processDiagramReader", reader);
        when(reader.read("instance")).thenReturn(new byte[]{1, 2, 3});

        assertThat(service.getProcessDiagram("instance")).containsExactly(1, 2, 3);
        verify(reader).read("instance");
    }

    @Test
    void diagramInfoOnlyLoadsImageWhenExplicitlyRequested() {
        var service = new FlowTaskServiceImpl();
        var reader = mock(ProcessDiagramReader.class);
        var history = mock(HistoryService.class, RETURNS_DEEP_STUBS);
        var repository = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
        when(history.createHistoricProcessInstanceQuery().processInstanceId("instance")
                .singleResult().getProcessDefinitionId()).thenReturn("definition");
        when(repository.getBpmnModel("definition")).thenReturn(new BpmnModel());
        when(repository.createProcessDefinitionQuery().processDefinitionId("definition").singleResult()).thenReturn(null);
        ReflectionTestUtils.setField(service, "historyService", history);
        ReflectionTestUtils.setField(service, "processDiagramInfoReader",
                new ProcessDiagramInfoReader(history, repository, reader,
                        mock(ProcessNodeSnapshotLoader.class), null));
        ReflectionTestUtils.setField(service, "processDiagramReader", reader);

        assertThat(service.getProcessDiagramInfo("instance").getDiagramBase64()).isNull();
        verifyNoInteractions(reader);
        when(reader.read("instance")).thenReturn(new byte[]{1, 2, 3});
        assertThat(service.getProcessDiagramInfo("instance", true).getDiagramBase64())
                .isEqualTo("data:image/png;base64,AQID");
        verify(reader).read("instance");
    }
}
