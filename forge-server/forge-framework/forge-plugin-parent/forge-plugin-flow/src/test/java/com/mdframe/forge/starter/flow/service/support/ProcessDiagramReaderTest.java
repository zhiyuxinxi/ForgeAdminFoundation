package com.mdframe.forge.starter.flow.service.support;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.image.ProcessDiagramGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessDiagramReaderTest {
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" targetNamespace="q04">
              <process id="p" isExecutable="true"><startEvent id="start"/></process>
              <bpmndi:BPMNDiagram id="diagram"><bpmndi:BPMNPlane id="plane" bpmnElement="p">
                <bpmndi:BPMNShape id="shape" bpmnElement="start">
                  <dc:Bounds x="10" y="20" width="30" height="30"/>
                </bpmndi:BPMNShape>
              </bpmndi:BPMNPlane></bpmndi:BPMNDiagram>
            </definitions>
            """;

    @Test
    void rendersHighlightedImageAndClosesStream() {
        Fixture f = new Fixture(true);
        var activity = mock(HistoricActivityInstance.class);
        when(activity.getActivityId()).thenReturn("done");
        when(f.activities.list()).thenReturn(List.of(activity, activity));
        when(f.runtime.getActiveActivityIds("instance")).thenReturn(List.of("active"));
        TrackingStream image = f.generated(false);

        assertThat(f.reader.read("instance")).containsExactly(1, 2, 3);
        assertThat(image.closed).isTrue();
        verify(f.generator).generateDiagram(f.model, "png", List.of("done"), List.of("active"),
                "SansSerif", "SansSerif", "SansSerif", null, 1.0, true);
    }

    @Test
    void completedProcessDoesNotQueryActiveActivities() {
        Fixture f = new Fixture(true);
        when(f.runtime.createProcessInstanceQuery().processInstanceId("instance").singleResult()).thenReturn(null);
        TrackingStream image = f.generated(false);

        assertThat(f.reader.read("instance")).containsExactly(1, 2, 3);
        assertThat(image.closed).isTrue();
        verify(f.runtime, never()).getActiveActivityIds(anyString());
        verify(f.generator).generateDiagram(f.model, "png", List.of(), List.of(),
                "SansSerif", "SansSerif", "SansSerif", null, 1.0, true);
    }

    @Test
    void primaryReadFailureClosesStreamAndStillThrows() {
        Fixture f = new Fixture(true);
        TrackingStream image = f.generated(true);

        assertThatThrownBy(() -> f.reader.read("instance"))
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        assertThat(image.closed).isTrue();
        verify(f.repository, never()).getResourceAsStream(anyString(), anyString());
    }

    @Test
    void readsDeployedImageBeforeXmlAndClosesIt() {
        Fixture f = new Fixture(false);
        TrackingStream image = f.resource("diagram.png", new byte[]{4, 5}, false);

        assertThat(f.reader.read("instance")).containsExactly(4, 5);
        assertThat(image.closed).isTrue();
        verify(f.repository, never()).getResourceAsStream("deployment", "process.bpmn20.xml");
        verifyNoInteractions(f.generator);
    }

    @Test
    void failedDeployedReadClosesItAndFallsBackToXmlRendering() {
        Fixture f = new Fixture(false);
        TrackingStream deployed = f.resource("diagram.png", new byte[0], true);
        TrackingStream xml = f.resource("process.bpmn20.xml", XML.getBytes(StandardCharsets.UTF_8), false);
        TrackingStream image = f.generated(false);

        assertThat(f.reader.read("instance")).containsExactly(1, 2, 3);
        assertThat(deployed.closed && xml.closed && image.closed).isTrue();
        verify(f.generator).generateDiagram(any(BpmnModel.class), eq("png"), eq(List.of()), eq(List.of()),
                eq("宋体"), eq("宋体"), eq("宋体"), isNull(), eq(1.0), eq(true));
    }

    @Test
    void failedXmlReadClosesItAndReturnsNull() {
        Fixture f = new Fixture(false);
        TrackingStream xml = f.resource("process.bpmn20.xml", new byte[0], true);

        assertThat(f.reader.read("instance")).isNull();
        assertThat(xml.closed).isTrue();
        verifyNoInteractions(f.generator);
    }

    @Test
    void failedFallbackImageReadClosesBothStreamsAndReturnsNull() {
        Fixture f = new Fixture(false);
        TrackingStream xml = f.resource("process.bpmn20.xml", XML.getBytes(StandardCharsets.UTF_8), false);
        TrackingStream image = f.generated(true);

        assertThat(f.reader.read("instance")).isNull();
        assertThat(xml.closed && image.closed).isTrue();
    }

    @Test
    void missingInstanceModelDefinitionGeneratorAndResourcesKeepNullResult() {
        Fixture missingInstance = new Fixture(true);
        when(missingInstance.history.createHistoricProcessInstanceQuery().processInstanceId("instance")
                .singleResult()).thenReturn(null);
        assertThat(missingInstance.reader.read("instance")).isNull();
        verifyNoInteractions(missingInstance.generator);
        Fixture missingModel = new Fixture(true);
        when(missingModel.repository.getBpmnModel("definition")).thenReturn(null);
        assertThat(missingModel.reader.read("instance")).isNull();
        Fixture missingDefinition = new Fixture(false);
        when(missingDefinition.repository.createProcessDefinitionQuery().processDefinitionId("definition")
                .singleResult()).thenReturn(null);
        assertThat(missingDefinition.reader.read("instance")).isNull();
        Fixture missingGenerator = new Fixture(true);
        when(missingGenerator.config.getProcessDiagramGenerator()).thenReturn(null);
        assertThat(missingGenerator.reader.read("instance")).isNull();
        Fixture missingResources = new Fixture(false);
        assertThat(missingResources.reader.read("instance")).isNull();
        Fixture emptyGenerated = new Fixture(true);
        assertThat(emptyGenerated.reader.read("instance")).isNull();
    }

    @Test
    void xmlWithoutLayoutReturnsNullAfterClosingResource() {
        Fixture f = new Fixture(false);
        String xmlWithoutLayout = XML.substring(0, XML.indexOf("  <bpmndi:BPMNDiagram")) + "</definitions>";
        TrackingStream xml = f.resource("process.bpmn20.xml", xmlWithoutLayout.getBytes(StandardCharsets.UTF_8), false);

        assertThat(f.reader.read("instance")).isNull();
        assertThat(xml.closed).isTrue();
        verifyNoInteractions(f.generator);
    }

    private static final class Fixture {
        private final HistoryService history = mock(HistoryService.class, RETURNS_DEEP_STUBS);
        private final RepositoryService repository = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
        private final RuntimeService runtime = mock(RuntimeService.class, RETURNS_DEEP_STUBS);
        private final ProcessEngineConfiguration config = mock(ProcessEngineConfiguration.class);
        private final ProcessDiagramGenerator generator = mock(ProcessDiagramGenerator.class);
        private final HistoricActivityInstanceQuery activities = mock(HistoricActivityInstanceQuery.class, RETURNS_SELF);
        private final BpmnModel model = new BpmnModel();
        private final ProcessDiagramReader reader = new ProcessDiagramReader(history, repository, runtime, config);

        Fixture(boolean withLayout) {
            when(history.createHistoricProcessInstanceQuery().processInstanceId("instance")
                    .singleResult().getProcessDefinitionId()).thenReturn("definition");
            when(repository.getBpmnModel("definition")).thenReturn(model);
            when(history.createHistoricActivityInstanceQuery()).thenReturn(activities);
            when(activities.list()).thenReturn(List.of());
            if (withLayout) {
                model.addGraphicInfo("start", new GraphicInfo());
            }
            when(config.getProcessDiagramGenerator()).thenReturn(generator);
            ProcessDefinition definition = repository.createProcessDefinitionQuery()
                    .processDefinitionId("definition").singleResult();
            when(definition.getDeploymentId()).thenReturn("deployment");
            when(definition.getDiagramResourceName()).thenReturn("diagram.png");
            when(definition.getResourceName()).thenReturn("process.bpmn20.xml");
            when(repository.getResourceAsStream(anyString(), anyString())).thenReturn(null);
        }

        TrackingStream resource(String name, byte[] bytes, boolean fail) {
            TrackingStream stream = new TrackingStream(bytes, fail);
            when(repository.getResourceAsStream("deployment", name)).thenReturn(stream);
            return stream;
        }

        TrackingStream generated(boolean fail) {
            TrackingStream stream = new TrackingStream(new byte[]{1, 2, 3}, fail);
            when(generator.generateDiagram(any(BpmnModel.class), eq("png"), anyList(), anyList(),
                    anyString(), anyString(), anyString(), isNull(), eq(1.0), eq(true))).thenReturn(stream);
            return stream;
        }
    }

    private static final class TrackingStream extends InputStream {
        private final byte[] bytes;
        private final boolean fail;
        private int offset;
        private boolean closed;

        TrackingStream(byte[] bytes, boolean fail) {
            this.bytes = bytes;
            this.fail = fail;
        }

        @Override
        public int read() throws IOException {
            if (fail) {
                throw new IOException("controlled stream read failure");
            }
            return offset == bytes.length ? -1 : bytes[offset++] & 0xff;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
