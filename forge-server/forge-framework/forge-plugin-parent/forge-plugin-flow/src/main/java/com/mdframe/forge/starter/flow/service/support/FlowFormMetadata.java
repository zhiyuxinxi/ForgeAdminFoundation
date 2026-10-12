package com.mdframe.forge.starter.flow.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads BPMN form nodes, attribute/extension precedence and embedded form references. */
@Slf4j
@Component
public class FlowFormMetadata {
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RepositoryService repositoryService;

    public FlowFormMetadata(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> readBusinessGlobalFormRef(String formJson) {
        if (isBlank(formJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(formJson, Map.class);
            Object nested = root.get("formRef");
            if (nested instanceof Map<?, ?> nestedMap) {
                Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) nestedMap);
                root.forEach(merged::putIfAbsent);
                return merged;
            }
            return root;
        } catch (Exception e) {
            log.warn("解析流程全局业务表单引用失败: {}", e.getMessage());
            return Map.of();
        }
    }

    public FlowNode resolveFormFlowNode(String processDefinitionId, String taskDefKey) {
        if (isBlank(processDefinitionId)) {
            return null;
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            return null;
        }
        Process process = bpmnModel.getMainProcess();
        if (!isBlank(taskDefKey)) {
            FlowElement element = process.getFlowElement(taskDefKey);
            if (element instanceof FlowNode) {
                return (FlowNode) element;
            }
        }

        FlowNode firstUserTask = null;
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof UserTask) {
                FlowNode candidate = (FlowNode) element;
                if (firstUserTask == null) {
                    firstUserTask = candidate;
                }
                if (readNodeFormConfig(candidate).hasForm()) {
                    return candidate;
                }
            }
        }
        return firstUserTask;
    }

    NodeFormConfig readNodeFormConfig(FlowNode flowNode) {
        NodeFormConfig config = new NodeFormConfig();
        if (flowNode == null) {
            return config;
        }
        if (flowNode instanceof UserTask) {
            config.formKey = ((UserTask) flowNode).getFormKey();
        }

        config.formUrl = flowNode.getAttributeValue(FLOWABLE_NS, "formUrl");
        config.formJson = flowNode.getAttributeValue(FLOWABLE_NS, "formJson");
        config.formTarget = flowNode.getAttributeValue(FLOWABLE_NS, "formTarget");
        config.formName = flowNode.getAttributeValue(FLOWABLE_NS, "formName");
        config.providerKey = flowNode.getAttributeValue(FLOWABLE_NS, "providerKey");
        config.viewKey = flowNode.getAttributeValue(FLOWABLE_NS, "viewKey");
        config.formMode = firstNonBlank(
                flowNode.getAttributeValue(FLOWABLE_NS, "formMode"),
                flowNode.getAttributeValue(FLOWABLE_NS, "formType"),
                flowNode.getAttributeValue(FLOWABLE_NS, "type"));
        config.formRef = readBusinessGlobalFormRef(flowNode.getAttributeValue(FLOWABLE_NS, "formRef"));
        config.formFieldPermissions = flowNode.getAttributeValue(FLOWABLE_NS, "formFieldPermissions");

        Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
        if (extensions == null) {
            return config;
        }
        config.formJson = extensionFallback(config.formJson, extensions, "formJson");
        config.formUrl = extensionFallback(config.formUrl, extensions, "formUrl");
        config.formTarget = extensionFallback(config.formTarget, extensions, "formTarget");
        config.formName = extensionFallback(config.formName, extensions, "formName");
        config.providerKey = extensionFallback(config.providerKey, extensions, "providerKey");
        config.viewKey = extensionFallback(config.viewKey, extensions, "viewKey");
        config.formMode = extensionFallback(config.formMode, extensions, "formMode");
        config.formMode = extensionFallback(config.formMode, extensions, "formType");
        if (config.formRef == null || config.formRef.isEmpty()) {
            String reference = extensionFallback(null, extensions, "formRef");
            if (reference != null) {
                config.formRef = readBusinessGlobalFormRef(reference);
            }
        }
        config.formFieldPermissions = extensionFallback(config.formFieldPermissions, extensions, "formFieldPermissions");
        return config;
    }

    private String extensionFallback(String attribute, Map<String, List<ExtensionElement>> extensions, String name) {
        List<ExtensionElement> values = extensions.get(name);
        return isBlank(attribute) && values != null && !values.isEmpty()
                ? values.get(0).getElementText() : attribute;
    }
    static class NodeFormConfig {
        String formKey;
        String formJson;
        String formUrl;
        String formTarget;
        String formMode;
        String formName;
        String providerKey;
        String viewKey;
        Map<String, Object> formRef = Map.of();
        String formFieldPermissions;

        private boolean hasForm() {
            return (formKey != null && !formKey.isBlank())
                    || (formJson != null && !formJson.isBlank())
                    || (formUrl != null && !formUrl.isBlank());
        }
    }
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
