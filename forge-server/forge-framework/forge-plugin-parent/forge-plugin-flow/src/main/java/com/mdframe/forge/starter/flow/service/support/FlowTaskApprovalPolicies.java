package com.mdframe.forge.starter.flow.service.support;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/** 按默认值、BPMN、节点配置的顺序解析当前任务审批策略。 */
@Component
public class FlowTaskApprovalPolicies {
    private final FlowBpmnPolicyReader bpmn;
    private final FlowModelService models;
    private final FlowNodeConfigService configurations;
    private final FlowProcessDefinitions definitions;

    public FlowTaskApprovalPolicies(FlowBpmnPolicyReader bpmn, FlowModelService models,
                                    FlowNodeConfigService configurations, FlowProcessDefinitions definitions) {
        this.bpmn = bpmn;
        this.models = models;
        this.configurations = configurations;
        this.definitions = definitions;
    }

    public TaskApprovalPolicy resolve(Task task) {
        return resolve(task, null, null);
    }

    public TaskApprovalPolicy resolve(Task task, FlowModel model, FlowNode node) {
        TaskApprovalPolicy policy = TaskApprovalPolicy.defaults();
        FlowNode effectiveNode = node != null ? node : bpmn.getFlowNode(task);
        if (effectiveNode != null) {
            policy = applyBpmnPolicy(policy, effectiveNode);
        }
        FlowModel effectiveModel = model != null ? model
                : models.getModelByKey(definitions.resolveProcessDefinitionKey(task.getProcessDefinitionId(), null));
        if (effectiveModel != null) {
            FlowNodeConfig config = configurations.getByModelAndNode(effectiveModel.getId(), task.getTaskDefinitionKey());
            if (config != null) {
                policy = applyNodeConfigPolicy(policy, config);
            }
        }
        return policy;
    }

    private TaskApprovalPolicy applyBpmnPolicy(TaskApprovalPolicy base, FlowNode node) {
        return new TaskApprovalPolicy(
                valueOr(bpmn.readNodeBoolean(node, "allowApprove"), base.allowApprove()),
                valueOr(bpmn.readNodeBoolean(node, "allowReject"), base.allowReject()),
                valueOr(bpmn.readNodeBoolean(node, "allowDelegate"), base.allowDelegate()),
                valueOr(bpmn.readNodeBoolean(node, "allowReturn"), base.allowReturn()),
                valueOr(bpmn.readNodeBoolean(node, "allowTerminate"), base.allowTerminate()),
                valueOr(bpmn.readNodeBoolean(node, "requireSignature"), base.requireSignature()),
                valueOr(bpmn.readNodeBoolean(node, "requireComment"), base.requireComment()));
    }

    private TaskApprovalPolicy applyNodeConfigPolicy(TaskApprovalPolicy base, FlowNodeConfig config) {
        return new TaskApprovalPolicy(
                valueOr(config.getAllowApprove(), base.allowApprove()),
                valueOr(config.getAllowReject(), base.allowReject()),
                valueOr(config.getAllowDelegate(), base.allowDelegate()),
                valueOr(config.getAllowReturn(), base.allowReturn()),
                valueOr(config.getAllowTerminate(), base.allowTerminate()),
                valueOr(config.getRequireSignature(), base.requireSignature()),
                valueOr(config.getRequireComment(), base.requireComment()));
    }

    private boolean valueOr(Boolean override, boolean inherited) {
        return override == null ? inherited : override;
    }
}
