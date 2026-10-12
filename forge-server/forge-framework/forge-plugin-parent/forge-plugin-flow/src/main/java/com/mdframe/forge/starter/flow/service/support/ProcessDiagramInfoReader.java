package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Reads a diagram view; image rendering and node snapshots keep their own owners. */
@Component
@Slf4j
public class ProcessDiagramInfoReader {
    private final HistoryService history;
    private final RepositoryService repository;
    private final ProcessDiagramReader images;
    private final ProcessNodeSnapshotLoader snapshots;
    private final FlowOrgIntegrationService organizations;

    public ProcessDiagramInfoReader(HistoryService history, RepositoryService repository,
                                    ProcessDiagramReader images, ProcessNodeSnapshotLoader snapshots,
                                    @Nullable FlowOrgIntegrationService organizations) {
        this.history = history;
        this.repository = repository;
        this.images = images;
        this.snapshots = snapshots;
        this.organizations = organizations;
    }

    public ProcessDiagramInfo read(String processInstanceId, boolean includeImage) {
        try {
            HistoricProcessInstance instance = history.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (instance == null) {
                log.warn("流程实例不存在：{}", processInstanceId);
                return null;
            }
            String definitionId = instance.getProcessDefinitionId();
            BpmnModel model = repository.getBpmnModel(definitionId);
            if (model == null) {
                log.warn("BPMN模型不存在：{}", definitionId);
                return null;
            }
            ProcessDiagramInfo result = instanceMetadata(instance, processInstanceId, definitionId);
            attachStartUserName(result, instance.getStartUserId());
            attachXml(result, definitionId);
            if (includeImage) {
                attachImage(result, processInstanceId);
            }
            result.setNodes(ProcessNodeViewAssembler.assemble(model, snapshots.load(processInstanceId)));
            return result;
        } catch (Exception e) {
            log.error("获取流程图详情失败：processInstanceId={}", processInstanceId, e);
            return null;
        }
    }

    private ProcessDiagramInfo instanceMetadata(HistoricProcessInstance instance, String instanceId,
                                                String definitionId) {
        ProcessDiagramInfo result = new ProcessDiagramInfo();
        result.setProcessInstanceId(instanceId);
        result.setProcessDefinitionId(definitionId);
        result.setProcessName(instance.getName());
        result.setStartUserId(instance.getStartUserId());
        result.setStartTime(instance.getStartTime());
        result.setEndTime(instance.getEndTime());
        result.setStatus(instance.getEndTime() == null ? "running"
                : instance.getDeleteReason() != null ? "terminated" : "completed");
        return result;
    }

    private void attachStartUserName(ProcessDiagramInfo result, String userId) {
        if (userId != null && organizations != null) {
            try {
                result.setStartUserName((String) organizations.getUserInfo(userId).get("realName"));
            } catch (Exception e) {
                log.warn("获取发起人姓名失败: {}", e.getMessage());
            }
        }
    }

    private void attachXml(ProcessDiagramInfo result, String definitionId) {
        var definition = repository.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (definition != null) {
            result.setBpmnXml(readXml(definition.getId()));
        }
    }

    private String readXml(String definitionId) {
        try {
            BpmnModel model = repository.getBpmnModel(definitionId);
            if (model == null) {
                log.warn("BPMN模型不存在：{}", definitionId);
                return null;
            }
            return new String(new BpmnXMLConverter().convertToXML(model), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("获取BPMN XML失败：processDefinitionId={}", definitionId, e);
            return null;
        }
    }

    private void attachImage(ProcessDiagramInfo result, String processInstanceId) {
        byte[] image = images.read(processInstanceId);
        if (image != null && image.length > 0) {
            result.setDiagramBase64("data:image/png;base64," + Base64.getEncoder().encodeToString(image));
        } else {
            log.warn("未能生成流程图图片");
        }
    }
}
