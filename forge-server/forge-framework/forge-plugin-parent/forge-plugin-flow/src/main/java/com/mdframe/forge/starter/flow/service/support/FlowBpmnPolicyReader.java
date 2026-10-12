package com.mdframe.forge.starter.flow.service.support;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 读取 BPMN 节点及流程的审批配置，保留属性优先于扩展元素的语义。 */
@Component
public class FlowBpmnPolicyReader {
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private final RepositoryService repository;

    public FlowBpmnPolicyReader(RepositoryService repository) {
        this.repository = repository;
    }

    public FlowNode getFlowNode(Task task) {
        BpmnModel bpmn = repository.getBpmnModel(task.getProcessDefinitionId());
        if (bpmn == null || bpmn.getMainProcess() == null) {
            return null;
        }
        FlowElement element = bpmn.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
        return element instanceof FlowNode ? (FlowNode) element : null;
    }

    public Boolean readNodeBoolean(FlowNode node, String name) {
        return parseBooleanValue(readNodeString(node, name));
    }

    public String readNodeString(FlowNode node, String name) {
        return readStringAttribute(node, name);
    }

    public Boolean readProcessBoolean(String processDefinitionId, String name) {
        return parseBooleanValue(readProcessString(processDefinitionId, name));
    }

    public String readProcessString(String processDefinitionId, String name) {
        Process process = getBpmnProcess(processDefinitionId);
        if (process == null) {
            return null;
        }
        String value = readStringAttribute(process, name);
        if ("autoApprovalMode".equals(name) && !"firstOnly".equals(value) && !"consecutive".equals(value)) {
            return "none";
        }
        return value;
    }

    private Process getBpmnProcess(String processDefinitionId) {
        if (isBlank(processDefinitionId)) {
            return null;
        }
        BpmnModel bpmn = repository.getBpmnModel(processDefinitionId);
        return bpmn == null ? null : bpmn.getMainProcess();
    }

    private String readStringAttribute(BaseElement element, String name) {
        String value = element.getAttributeValue(FLOWABLE_NS, name);
        if (isBlank(value)) {
            Map<String, List<ExtensionElement>> extensions = element.getExtensionElements();
            List<ExtensionElement> elements = extensions != null ? extensions.get(name) : null;
            if (elements != null && !elements.isEmpty()) {
                value = elements.get(0).getElementText();
            }
        }
        return value;
    }

    private Boolean parseBooleanValue(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized)
                || "Y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)
                || "N".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
