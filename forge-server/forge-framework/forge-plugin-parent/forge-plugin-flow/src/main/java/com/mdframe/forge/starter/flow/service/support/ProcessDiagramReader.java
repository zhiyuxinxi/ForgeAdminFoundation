package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Reads process images, preserving deployed-image and BPMN-resource fallbacks. */
@Component
@Slf4j
public class ProcessDiagramReader {
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final ProcessEngineConfiguration engineConfiguration;

    public ProcessDiagramReader(HistoryService historyService, RepositoryService repositoryService,
                                RuntimeService runtimeService, ProcessEngineConfiguration engineConfiguration) {
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.engineConfiguration = engineConfiguration;
    }

    public byte[] read(String processInstanceId) {
        try {
            HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (instance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            String definitionId = instance.getProcessDefinitionId();
            BpmnModel model = repositoryService.getBpmnModel(definitionId);
            if (model == null) {
                log.warn("BPMN模型不存在：{}", definitionId);
                return null;
            }
            if (!hasGraphicInfo(model)) {
                return readDeployedDiagram(definitionId);
            }
            List<String> completed = completedActivities(processInstanceId);
            List<String> current = currentActivities(processInstanceId);
            ProcessDiagramGenerator generator = engineConfiguration.getProcessDiagramGenerator();
            if (generator == null) {
                log.error("ProcessDiagramGenerator 未配置");
                return null;
            }
            return render(generator, model, completed, current, "SansSerif");
        } catch (Exception e) {
            log.error("生成流程图失败：processInstanceId={}, 错误: {}", processInstanceId, e.getMessage(), e);
            throw new RuntimeException(UiMessages.message("customer.diagramFailure", e.getMessage()), e);
        }
    }

    private List<String> completedActivities(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).finished()
                .orderByHistoricActivityInstanceStartTime().asc().list().stream()
                .map(HistoricActivityInstance::getActivityId).distinct().collect(Collectors.toList());
    }

    private List<String> currentActivities(String processInstanceId) {
        if (runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult() == null) {
            return new ArrayList<>();
        }
        return runtimeService.getActiveActivityIds(processInstanceId);
    }

    private boolean hasGraphicInfo(BpmnModel model) {
        if (model.getLocationMap() == null || model.getLocationMap().isEmpty()) {
            return false;
        }
        for (GraphicInfo info : model.getLocationMap().values()) {
            if (info != null && info.getX() >= 0 && info.getY() >= 0) {
                return true;
            }
        }
        return false;
    }

    private byte[] readDeployedDiagram(String definitionId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(definitionId).singleResult();
            if (definition == null) {
                log.warn("流程定义不存在：{}", definitionId);
                return null;
            }
            String resourceName = definition.getDiagramResourceName();
            if (resourceName != null && !resourceName.isEmpty()) {
                byte[] image = readDiagramResource(definition.getDeploymentId(), resourceName);
                if (image != null) {
                    return image;
                }
            }
            return renderBpmnResource(definition);
        } catch (Exception e) {
            log.error("从资源获取流程图失败：processDefinitionId={}", definitionId, e);
            return null;
        }
    }

    private byte[] readDiagramResource(String deploymentId, String resourceName) {
        try (InputStream image = repositoryService.getResourceAsStream(deploymentId, resourceName)) {
            return image == null ? null : image.readAllBytes();
        } catch (Exception e) {
            log.warn("从部署资源获取流程图失败：{}", e.getMessage());
            return null;
        }
    }

    private byte[] renderBpmnResource(ProcessDefinition definition) {
        try {
            byte[] xml = readBpmnResource(definition);
            if (xml == null) {
                return null;
            }
            byte[] utf8 = new String(xml, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(utf8), false, true);
            if (model.getLocationMap() == null || model.getLocationMap().isEmpty()) {
                log.warn("BPMN XML 中没有图形坐标信息");
                return null;
            }
            return render(engineConfiguration.getProcessDiagramGenerator(), model, List.of(), List.of(), "宋体");
        } catch (Exception e) {
            log.error("从 BPMN XML 生成流程图失败", e);
            return null;
        }
    }

    private byte[] readBpmnResource(ProcessDefinition definition) throws IOException {
        try (InputStream xml = repositoryService.getResourceAsStream(
                definition.getDeploymentId(), definition.getResourceName())) {
            return xml == null ? null : xml.readAllBytes();
        }
    }

    private byte[] render(ProcessDiagramGenerator generator, BpmnModel model,
                          List<String> completed, List<String> current, String font) throws IOException {
        try (InputStream image = generator.generateDiagram(model, "png", completed, current,
                font, font, font, null, 1.0, true)) {
            return image == null ? null : image.readAllBytes();
        }
    }
}
