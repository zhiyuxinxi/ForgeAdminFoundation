package com.mdframe.forge.starter.flow.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FlowModelNormalizationContractTest {

    private static final String PREFIX = """
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
              xmlns:flowable="http://flowable.org/bpmn"><process id="test">
            """;
    private static final String SUFFIX = "</process></definitions>";

    @Test
    void currentAndVersionPathsRepairBothKindsAndRetainOperationLogs() throws Exception {
        String xml = PREFIX + """
                <userTask id="review"><multiInstanceLoopCharacteristics flowable:collection="${reviewers}">
                  <loopCardinality>${nrOfInstances}</loopCardinality>
                  <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
                </multiInstanceLoopCharacteristics></userTask>
                <endEvent id="end"/>
                <sequenceFlow id="a" sourceRef="review" targetRef="end"/>
                <sequenceFlow id="b" sourceRef="review" targetRef="end"/>
                """ + SUFFIX;
        for (Object service : services()) {
            Logger logger = (Logger) LoggerFactory.getLogger(service.getClass());
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                String result = normalize(service, xml);
                assertTrue(result.contains("id=\"a\""));
                assertFalse(result.contains("id=\"b\""));
                assertFalse(result.contains("loopCardinality"));
                assertFalse(result.contains("completionCondition"));
                assertTrue(result.contains("${reviewers}"));
                List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
                assertEquals(List.of("contract：已自动清理 BPMN 重复连线，review->end 保留 [a] 删除 [b]",
                        "contract：已自动清理 BPMN 旧版会签表达式，节点=[review]"), messages);
                snapshot(service, "both-repairs", result);
            } finally {
                logger.detachAppender(appender);
                appender.stop();
            }
        }
    }

    @Test
    void conditionalEdgesWithDistinctMeaningRemainByteIdentical() throws Exception {
        String xml = PREFIX + """
                <exclusiveGateway id="start"/><endEvent id="end"/>
                <sequenceFlow id="a" sourceRef="start" targetRef="end">
                  <conditionExpression>${amount &gt; 10}</conditionExpression>
                </sequenceFlow>
                <sequenceFlow id="b" sourceRef="start" targetRef="end">
                  <conditionExpression>${urgent}</conditionExpression>
                </sequenceFlow>
                """ + SUFFIX;
        for (Object service : services()) {
            assertEquals(xml, normalize(service, xml));
        }
    }

    @Test
    void modernMultiInstanceConditionIsNotRemoved() throws Exception {
        String xml = PREFIX + """
                <userTask id="review"><multiInstanceLoopCharacteristics flowable:collection="${reviewers}">
                  <completionCondition>${nrOfCompletedInstances &gt; 1}</completionCondition>
                </multiInstanceLoopCharacteristics></userTask>
                """ + SUFFIX;
        for (Object service : services()) {
            assertEquals(xml, normalize(service, xml));
        }
    }

    @Test
    void absentBlankAndUnchangedDocumentsKeepTheirOriginalValues() throws Exception {
        for (Object service : services()) {
            assertNull(normalize(service, null));
            assertEquals(" \n ", normalize(service, " \n "));
            assertEquals(PREFIX + SUFFIX, normalize(service, PREFIX + SUFFIX));
        }
    }

    @Test
    void malformedXmlRetainsFailureAndCause() {
        for (Object service : services()) {
            RuntimeException error = assertThrows(RuntimeException.class, () -> normalize(service, "<broken>"));
            assertTrue(error.getMessage().startsWith("BPMN XML 解析失败："));
            assertNotNull(error.getCause());
        }
    }

    @Test
    void externalEntityDocumentIsRejectedByBothEntryPoints() {
        String xml = "<!DOCTYPE definitions [<!ENTITY external SYSTEM 'file:///not-readable'>]>"
                + PREFIX + "<documentation>&external;</documentation>" + SUFFIX;
        for (Object service : services()) {
            RuntimeException error = assertThrows(RuntimeException.class, () -> normalize(service, xml));
            assertTrue(error.getMessage().startsWith("BPMN XML 解析失败："));
        }
    }

    private static List<Object> services() {
        return List.of(new FlowModelServiceImpl(), new FlowModelVersionServiceImpl(mock(FlowModelMapper.class)));
    }

    private static String normalize(Object service, String xml) throws Exception {
        Method method = service.getClass().getDeclaredMethod("normalizeBpmnXml", String.class, String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(service, xml, "contract");
        } catch (InvocationTargetException error) {
            throw (Exception) error.getCause();
        }
    }

    private static void snapshot(Object service, String name, String xml) throws Exception {
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(xml.getBytes(StandardCharsets.UTF_8)));
        System.out.println("NORMALIZATION " + service.getClass().getSimpleName() + " " + name + " " + digest);
    }
}
