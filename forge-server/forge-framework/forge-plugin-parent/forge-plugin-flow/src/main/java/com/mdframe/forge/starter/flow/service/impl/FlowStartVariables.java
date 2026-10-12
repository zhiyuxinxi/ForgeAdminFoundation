package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.ProcessDefinition;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Builds reserved process variables and legacy assignee compatibility without mutating caller input. */
@Slf4j
final class FlowStartVariables {

    private final ProcessEngine processEngine;
    private final FlowStartActorVariables actorVariables;

    FlowStartVariables(ProcessEngine processEngine, FlowStartActorVariables actorVariables) {
        this.processEngine = processEngine;
        this.actorVariables = actorVariables;
    }

    Map<String, Object> build(FlowBusiness business, ProcessDefinition processDefinition,
            Map<String, Object> variables) {
        Map<String, Object> vars = variables != null ? new HashMap<>(variables) : new HashMap<>();

        // 内置变量：发起人信息
        vars.put("initiator", business.getApplyUserId());
        vars.put("startUserId", business.getApplyUserId());
        vars.put("startUserName", business.getApplyUserName());
        vars.put("startDeptId", business.getApplyDeptId());
        vars.put("startDeptName", business.getApplyDeptName());

        // 内置变量：业务主键
        vars.putIfAbsent("businessKey", business.getBusinessKey());
        vars.putIfAbsent("flowBusinessKey", business.getBusinessKey());
        vars.put("businessType", business.getBusinessType());

        // 内置变量：流程标题
        vars.put("processTitle", business.getTitle());

        // 兼容历史设计器把固定审批人写成 ${user_45}。新模型直接保存用户 ID，
        // 这里仅对已部署旧模型的严格数字表达式补变量，不影响正常动态表达式。
        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(processDefinition.getId());
        int legacyFixedAssigneeCount = LegacyFixedAssigneeVariableSupport.enrich(bpmnModel, vars);
        if (legacyFixedAssigneeCount > 0) {
            log.warn("[流程启动兼容] 已补齐历史固定审批人变量: processDefinitionId={}, count={}",
                    processDefinition.getId(), legacyFixedAssigneeCount);
        }
        actorVariables.enrich(business.getApplyUserId(), business.getTenantId(), vars);
        return vars;
    }

}
