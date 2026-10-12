package com.mdframe.forge.starter.flow.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.bpmn.model.BpmnModel;
import java.util.Objects;

/** Resolves engine definition identities without depending on task or form services. */
@Slf4j
@Component
public class FlowProcessDefinitions {
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;

    public FlowProcessDefinitions(RuntimeService runtimeService, HistoryService historyService,
                                  RepositoryService repositoryService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.repositoryService = repositoryService;
    }

    public String resolveProcessDefinitionId(String processInstanceId, String processDefKey) {
        if (!isBlank(processInstanceId)) {
            try {
                ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (runtimeInstance != null) {
                    return runtimeInstance.getProcessDefinitionId();
                }
            } catch (Exception e) {
                log.debug("从运行实例解析流程定义失败: processInstanceId={}", processInstanceId);
            }
            try {
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicInstance != null) {
                    return historicInstance.getProcessDefinitionId();
                }
            } catch (Exception e) {
                log.debug("从历史实例解析流程定义失败: processInstanceId={}", processInstanceId);
            }
        }
        if (!isBlank(processDefKey)) {
            try {
                ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(processDefKey)
                        .latestVersion()
                        .singleResult();
                return definition != null ? definition.getId() : null;
            } catch (Exception e) {
                log.debug("从流程定义Key解析最新流程定义失败: processDefKey={}", processDefKey);
            }
        }
        return null;
    }

    public String resolveProcessDefinitionKey(String processDefinitionId, String fallbackProcessDefKey) {
        String key = null;
        if (!isBlank(processDefinitionId)) {
            key = processDefinitionId.contains(":") ? extractProcessKey(processDefinitionId) : null;
            if (isBlank(key) || Objects.equals(key, processDefinitionId)) {
                key = readDefinitionKey(processDefinitionId, key);
            }
            if (isBlank(key) || Objects.equals(key, processDefinitionId)) {
                key = readBpmnKey(processDefinitionId, key);
            }
        }
        if (!isBlank(key) && !Objects.equals(key, processDefinitionId)) {
            return key;
        }
        return !isBlank(fallbackProcessDefKey) && fallbackProcessDefKey.contains(":")
                ? extractProcessKey(fallbackProcessDefKey) : fallbackProcessDefKey;
    }

    private String readDefinitionKey(String processDefinitionId, String key) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (definition != null) {
                key = definition.getKey();
            }
        } catch (Exception e) {
            log.debug("从流程定义ID解析流程定义Key失败: processDefinitionId={}", processDefinitionId);
        }
        return key;
    }

    private String readBpmnKey(String processDefinitionId, String key) {
        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel != null && bpmnModel.getMainProcess() != null) {
                key = bpmnModel.getMainProcess().getId();
            }
        } catch (Exception e) {
            log.debug("从BPMN模型解析流程定义Key失败: processDefinitionId={}", processDefinitionId);
        }
        return key;
    }

    private String extractProcessKey(String processDefinitionId) {
        if (processDefinitionId == null) {
            return null;
        }
        // 格式：processKey:version:id
        String[] parts = processDefinitionId.split(":");
        return parts.length > 0 ? parts[0] : processDefinitionId;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
