package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.ProcessNodeInfo;
import com.mdframe.forge.starter.flow.dto.ProcessNodeTaskInfo;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.support.ProcessNodeSnapshotLoader;
import com.mdframe.forge.starter.flow.service.support.ProcessDiagramInfoReader;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessNodeViewIntegrationTest {
    private static ProcessEngine engine;
    private static FlowTaskServiceImpl service;

    @BeforeAll
    static void startEngine() {
        var config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:b14_nodes;DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate("true");
        config.setHistory("full");
        config.setAsyncExecutorActivate(false);
        config.setDisableIdmEngine(true);
        config.setDisableEventRegistry(true);
        engine = config.buildProcessEngine();
        var beans = new StaticListableBeanFactory();
        var loader = new ProcessNodeSnapshotLoader(engine.getRuntimeService(), engine.getTaskService(),
                engine.getHistoryService(), beans.getBeanProvider(FlowOrgIntegrationService.class));
        service = new FlowTaskServiceImpl();
        ReflectionTestUtils.setField(service, "processDiagramInfoReader", new ProcessDiagramInfoReader(
                engine.getHistoryService(), engine.getRepositoryService(), null, loader, null));
        ReflectionTestUtils.setField(service, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(service, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(service, "historyService", engine.getHistoryService());
        deploy("parallel", true);
        deploy("single", false);
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void threeParallelTasksAndPartialCompletionRemainDistinct() {
        String id = start("parallel");
        List<Task> initial = current(id);
        ProcessNodeInfo before = node(id);
        assertThat(initial).hasSize(3);
        assertThat(before.getTaskInstances()).extracting(ProcessNodeTaskInfo::getTaskId)
                .containsExactlyInAnyOrderElementsOf(initial.stream().map(Task::getId).toList());
        assertThat(before.getAssigneeIds()).containsExactlyInAnyOrder("alice", "bob", "carol");
        assertThat(before.getTaskId()).isNull();
        assertThat(before.getTaskInstances()).allSatisfy(task -> {
            assertThat(task.getExecutionId()).isNotBlank();
            assertThat(task.getActivityInstanceId()).isNotBlank();
            assertThat(task.getStatus()).isEqualTo("running");
        });
        engine.getTaskService().complete(initial.get(0).getId());
        ProcessNodeInfo partial = node(id);
        assertThat(partial.getStatus()).isEqualTo("running");
        assertThat(partial.getTaskId()).isNull();
        assertThat(partial.getTaskInstances()).hasSize(3);
        assertThat(partial.getTaskInstances().stream().filter(task -> "completed".equals(task.getStatus())))
                .singleElement().satisfies(task -> assertThat(task.getEndTime()).isNotNull());
        assertThat(partial.getTaskInstances().stream().filter(task -> "running".equals(task.getStatus()))).hasSize(2);
        assertThat(partial.getTaskInstances()).isSortedAccordingTo(Comparator
                .comparing(ProcessNodeTaskInfo::getStartTime).thenComparing(ProcessNodeTaskInfo::getTaskId));
    }

    @Test
    void reentryKeepsPriorOccurrencesAndNewTasksWithoutOverwritingHistory() {
        String id = start("parallel");
        Set<String> original = current(id).stream().map(Task::getId).collect(Collectors.toSet());
        original.forEach(task -> engine.getTaskService().complete(task));
        engine.getRuntimeService().createChangeActivityStateBuilder().processInstanceId(id)
                .moveActivityIdTo("next", "review").changeState();
        ProcessNodeInfo repeated = node(id);
        assertThat(repeated.getStatus()).isEqualTo("running");
        assertThat(repeated.getTaskId()).isNull();
        assertThat(repeated.getTaskInstances()).hasSize(6);
        assertThat(repeated.getTaskInstances().stream().map(ProcessNodeTaskInfo::getTaskId).distinct()).hasSize(6);
        assertThat(repeated.getTaskInstances().stream().filter(task -> original.contains(task.getTaskId())))
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo("completed"));
        assertThat(repeated.getTaskInstances().stream().filter(task -> !original.contains(task.getTaskId())))
                .allSatisfy(task -> assertThat(task.getStatus()).isEqualTo("running"));
        assertThat(repeated.getTaskInstances().stream().map(ProcessNodeTaskInfo::getActivityInstanceId).distinct())
                .hasSize(6);
    }

    @Test
    void singleTaskCompatibilityNeverPointsToHistoricOrAnotherConcurrentTask() {
        String id = start("single");
        String taskId = current(id).get(0).getId();
        assertThat(node(id).getTaskId()).isEqualTo(taskId);
        engine.getTaskService().complete(taskId);
        ProcessNodeInfo completed = node(id);
        assertThat(completed.getTaskId()).isNull();
        assertThat(completed.getStatus()).isEqualTo("completed");
        assertThat(completed.getTaskInstances()).singleElement().satisfies(task -> {
            assertThat(task.getTaskId()).isEqualTo(taskId);
            assertThat(task.getAssigneeName()).isEqualTo("alice");
            assertThat(task.getStatus()).isEqualTo("completed");
        });
    }

    private static String start(String key) {
        return engine.getRuntimeService().startProcessInstanceByKey(key,
                Map.of("reviewers", List.of("alice", "bob", "carol"))).getId();
    }

    private static List<Task> current(String id) {
        return engine.getTaskService().createTaskQuery().processInstanceId(id).taskDefinitionKey("review").list();
    }

    private static ProcessNodeInfo node(String id) {
        var diagram = service.getProcessDiagramInfo(id, false);
        assertThat(diagram).isNotNull();
        return diagram.getNodes().stream().filter(node -> "review".equals(node.getNodeId())).findFirst().orElseThrow();
    }

    private static void deploy(String key, boolean parallel) {
        String multi = parallel ? """
                <multiInstanceLoopCharacteristics isSequential="false"
                    flowable:collection="reviewers" flowable:elementVariable="reviewer"/>
                """ : "";
        String assignee = parallel ? "${reviewer}" : "alice";
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:flowable="http://flowable.org/bpmn" targetNamespace="b14">
                  <process id="%s" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="a" sourceRef="start" targetRef="review"/>
                    <userTask id="review" name="Review" flowable:assignee="%s">%s</userTask>
                    <sequenceFlow id="b" sourceRef="review" targetRef="next"/>
                    <userTask id="next" flowable:assignee="owner"/>
                    <sequenceFlow id="c" sourceRef="next" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(key, assignee, multi);
        engine.getRepositoryService().createDeployment().addString(key + ".bpmn20.xml", xml).deploy();
    }
}
