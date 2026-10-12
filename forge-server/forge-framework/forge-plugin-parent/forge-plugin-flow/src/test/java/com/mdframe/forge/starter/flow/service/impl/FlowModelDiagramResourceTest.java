package com.mdframe.forge.starter.flow.service.impl;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.image.ProcessDiagramGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowModelDiagramResourceTest {

    @Test
    void diagramStreamClosesAfterReadFailureAndRetainsNullFallback() throws IOException {
        verifyStreamOwner(true);
    }

    @Test
    void diagramStreamClosesAfterSuccessfulReadAndRetainsBytes() throws IOException {
        verifyStreamOwner(false);
    }

    private static void verifyStreamOwner(boolean fails) throws IOException {
        BpmnModel model = new BpmnModel();
        model.addGraphicInfo("task", new GraphicInfo());
        try (InputStream stream = mock(InputStream.class);
             MockedConstruction<BpmnXMLConverter> converter = mockConstruction(BpmnXMLConverter.class,
                     (mock, context) -> when(mock.convertToBpmnModel(any(), eq(false), eq(true))).thenReturn(model))) {
            if (fails) {
                when(stream.read(any(byte[].class))).thenThrow(new IOException("fixture read failure"));
            } else {
                when(stream.read(any(byte[].class))).thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    buffer[0] = 42;
                    return 1;
                }).thenReturn(-1);
            }
            ProcessDiagramGenerator generator = mock(ProcessDiagramGenerator.class);
            when(generator.generateDiagram(any(BpmnModel.class), eq("png"), anyList(), anyList(),
                    eq("宋体"), eq("宋体"), eq("宋体"), isNull(), eq(1.0), eq(true))).thenReturn(stream);
            ProcessEngineConfiguration engine = mock(ProcessEngineConfiguration.class);
            when(engine.getProcessDiagramGenerator()).thenReturn(generator);
            FlowModelServiceImpl service = new FlowModelServiceImpl();
            ReflectionTestUtils.setField(service, "processEngineConfiguration", engine);

            byte[] result = ReflectionTestUtils.invokeMethod(service, "generateProcessDiagram", "<fixture/>");

            if (fails) {
                assertNull(result);
            } else {
                assertArrayEquals(new byte[]{42}, result);
            }
            verify(stream).close();
        }
    }
}
