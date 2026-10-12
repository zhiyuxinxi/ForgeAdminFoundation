package com.mdframe.forge.business.core.purchase.support;

import lombok.experimental.UtilityClass;

/**
 * 采购单审批测试流程 BPMN。
 */
@UtilityClass
public class SamplePurchaseOrderFlowBpmn {

    public static String build() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  xmlns:flowable="http://flowable.org/bpmn"
                                  id="Definitions_@MODEL_KEY@"
                                  targetNamespace="http://forge.mdframe.com/bpmn">
                  <bpmn:process id="@MODEL_KEY@" name="@MODEL_NAME@" isExecutable="true">
                    <bpmn:startEvent id="StartEvent_1" name="提交采购单" flowable:initiator="@VAR_INITIATOR@">
                      <bpmn:outgoing>Flow_001</bpmn:outgoing>
                    </bpmn:startEvent>
                    <bpmn:userTask id="@NODE_DEPT_LEADER@" name="部门负责人审批" flowable:assignee="@EXPR_DEPT_LEADER@" flowable:formKey="@FORM_KEY@" flowable:formFieldPermissions='@PERM_DEPT_LEADER@' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_001</bpmn:incoming>
                      <bpmn:incoming>Flow_012</bpmn:incoming>
                      <bpmn:outgoing>Flow_002</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_dept_result" name="部门负责人审批结果" default="Flow_003">
                      <bpmn:incoming>Flow_002</bpmn:incoming>
                      <bpmn:outgoing>Flow_003</bpmn:outgoing>
                      <bpmn:outgoing>Flow_004</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="@NODE_ENGINEERING@" name="工程部经理审批" flowable:assignee="@EXPR_ENGINEERING@" flowable:formKey="@FORM_KEY@" flowable:formFieldPermissions='@PERM_ENGINEERING@' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_003</bpmn:incoming>
                      <bpmn:outgoing>Flow_005</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_engineering_result" name="工程部经理审批结果" default="Flow_006">
                      <bpmn:incoming>Flow_005</bpmn:incoming>
                      <bpmn:outgoing>Flow_006</bpmn:outgoing>
                      <bpmn:outgoing>Flow_007</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="@NODE_COUNTERSIGN@" name="采购会签" flowable:assignee="@EXPR_ASSIGNEE@" flowable:formKey="@FORM_KEY@" flowable:formFieldPermissions='@PERM_COUNTERSIGN@' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="true" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_006</bpmn:incoming>
                      <bpmn:outgoing>Flow_008</bpmn:outgoing>
                      <bpmn:multiInstanceLoopCharacteristics isSequential="false" flowable:collection="@EXPR_COUNTERSIGN_USER_LIST@" flowable:elementVariable="@VAR_ASSIGNEE@">
                        <bpmn:completionCondition xsi:type="bpmn:tFormalExpression"><![CDATA[@COUNTERSIGN_COMPLETION@]]></bpmn:completionCondition>
                      </bpmn:multiInstanceLoopCharacteristics>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_countersign_result" name="会签审批结果" default="Flow_009">
                      <bpmn:incoming>Flow_008</bpmn:incoming>
                      <bpmn:outgoing>Flow_009</bpmn:outgoing>
                      <bpmn:outgoing>Flow_010</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:userTask id="@NODE_APPLICANT_MODIFY@" name="申请人修改" flowable:assignee="@EXPR_INITIATOR@" flowable:formKey="@FORM_KEY@" flowable:formFieldPermissions='@PERM_APPLICANT_MODIFY@' flowable:allowApprove="true" flowable:allowReject="true" flowable:allowDelegate="false" flowable:allowReturn="false" flowable:allowTerminate="false" flowable:requireComment="true">
                      <bpmn:incoming>Flow_004</bpmn:incoming>
                      <bpmn:incoming>Flow_007</bpmn:incoming>
                      <bpmn:incoming>Flow_010</bpmn:incoming>
                      <bpmn:outgoing>Flow_011</bpmn:outgoing>
                    </bpmn:userTask>
                    <bpmn:exclusiveGateway id="Gateway_modify_result" name="修改处理结果" default="Flow_012">
                      <bpmn:incoming>Flow_011</bpmn:incoming>
                      <bpmn:outgoing>Flow_012</bpmn:outgoing>
                      <bpmn:outgoing>Flow_013</bpmn:outgoing>
                    </bpmn:exclusiveGateway>
                    <bpmn:endEvent id="EndEvent_approved" name="审批通过">
                      <bpmn:incoming>Flow_009</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:endEvent id="EndEvent_rejected" name="申请人终止">
                      <bpmn:incoming>Flow_013</bpmn:incoming>
                    </bpmn:endEvent>
                    <bpmn:sequenceFlow id="Flow_001" sourceRef="StartEvent_1" targetRef="@NODE_DEPT_LEADER@" />
                    <bpmn:sequenceFlow id="Flow_002" sourceRef="@NODE_DEPT_LEADER@" targetRef="Gateway_dept_result" />
                    <bpmn:sequenceFlow id="Flow_003" name="通过" sourceRef="Gateway_dept_result" targetRef="@NODE_ENGINEERING@" />
                    <bpmn:sequenceFlow id="Flow_004" name="驳回修改" sourceRef="Gateway_dept_result" targetRef="@NODE_APPLICANT_MODIFY@">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[@REJECT_CONDITION@]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_005" sourceRef="@NODE_ENGINEERING@" targetRef="Gateway_engineering_result" />
                    <bpmn:sequenceFlow id="Flow_006" name="通过" sourceRef="Gateway_engineering_result" targetRef="@NODE_COUNTERSIGN@" />
                    <bpmn:sequenceFlow id="Flow_007" name="驳回修改" sourceRef="Gateway_engineering_result" targetRef="@NODE_APPLICANT_MODIFY@">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[@REJECT_CONDITION@]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_008" sourceRef="@NODE_COUNTERSIGN@" targetRef="Gateway_countersign_result" />
                    <bpmn:sequenceFlow id="Flow_009" name="通过" sourceRef="Gateway_countersign_result" targetRef="EndEvent_approved" />
                    <bpmn:sequenceFlow id="Flow_010" name="驳回修改" sourceRef="Gateway_countersign_result" targetRef="@NODE_APPLICANT_MODIFY@">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[@REJECT_CONDITION@]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                    <bpmn:sequenceFlow id="Flow_011" sourceRef="@NODE_APPLICANT_MODIFY@" targetRef="Gateway_modify_result" />
                    <bpmn:sequenceFlow id="Flow_012" name="重新提交" sourceRef="Gateway_modify_result" targetRef="@NODE_DEPT_LEADER@" />
                    <bpmn:sequenceFlow id="Flow_013" name="终止申请" sourceRef="Gateway_modify_result" targetRef="EndEvent_rejected">
                      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[@REJECT_CONDITION@]]></bpmn:conditionExpression>
                    </bpmn:sequenceFlow>
                  </bpmn:process>
                  <bpmndi:BPMNDiagram id="BPMNDiagram_@MODEL_KEY@">
                    <bpmndi:BPMNPlane id="BPMNPlane_@MODEL_KEY@" bpmnElement="@MODEL_KEY@">
                      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1"><dc:Bounds x="80" y="192" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="@NODE_DEPT_LEADER@_di" bpmnElement="@NODE_DEPT_LEADER@"><dc:Bounds x="220" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_dept_result_di" bpmnElement="Gateway_dept_result" isMarkerVisible="true"><dc:Bounds x="420" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="@NODE_ENGINEERING@_di" bpmnElement="@NODE_ENGINEERING@"><dc:Bounds x="530" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_engineering_result_di" bpmnElement="Gateway_engineering_result" isMarkerVisible="true"><dc:Bounds x="730" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="@NODE_COUNTERSIGN@_di" bpmnElement="@NODE_COUNTERSIGN@"><dc:Bounds x="840" y="170" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_countersign_result_di" bpmnElement="Gateway_countersign_result" isMarkerVisible="true"><dc:Bounds x="1040" y="185" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_approved_di" bpmnElement="EndEvent_approved"><dc:Bounds x="1160" y="192" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="@NODE_APPLICANT_MODIFY@_di" bpmnElement="@NODE_APPLICANT_MODIFY@"><dc:Bounds x="540" y="330" width="140" height="80" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="Gateway_modify_result_di" bpmnElement="Gateway_modify_result" isMarkerVisible="true"><dc:Bounds x="740" y="345" width="50" height="50" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNShape id="EndEvent_rejected_di" bpmnElement="EndEvent_rejected"><dc:Bounds x="880" y="352" width="36" height="36" /></bpmndi:BPMNShape>
                      <bpmndi:BPMNEdge id="Flow_001_di" bpmnElement="Flow_001"><di:waypoint x="116" y="210" /><di:waypoint x="220" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_002_di" bpmnElement="Flow_002"><di:waypoint x="360" y="210" /><di:waypoint x="420" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_003_di" bpmnElement="Flow_003"><di:waypoint x="470" y="210" /><di:waypoint x="530" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_004_di" bpmnElement="Flow_004"><di:waypoint x="445" y="235" /><di:waypoint x="445" y="370" /><di:waypoint x="540" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_005_di" bpmnElement="Flow_005"><di:waypoint x="670" y="210" /><di:waypoint x="730" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_006_di" bpmnElement="Flow_006"><di:waypoint x="780" y="210" /><di:waypoint x="840" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_007_di" bpmnElement="Flow_007"><di:waypoint x="755" y="235" /><di:waypoint x="755" y="370" /><di:waypoint x="680" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_008_di" bpmnElement="Flow_008"><di:waypoint x="980" y="210" /><di:waypoint x="1040" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_009_di" bpmnElement="Flow_009"><di:waypoint x="1090" y="210" /><di:waypoint x="1160" y="210" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_010_di" bpmnElement="Flow_010"><di:waypoint x="1065" y="235" /><di:waypoint x="1065" y="430" /><di:waypoint x="610" y="430" /><di:waypoint x="610" y="410" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_011_di" bpmnElement="Flow_011"><di:waypoint x="680" y="370" /><di:waypoint x="740" y="370" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_012_di" bpmnElement="Flow_012"><di:waypoint x="765" y="345" /><di:waypoint x="765" y="95" /><di:waypoint x="290" y="95" /><di:waypoint x="290" y="170" /></bpmndi:BPMNEdge>
                      <bpmndi:BPMNEdge id="Flow_013_di" bpmnElement="Flow_013"><di:waypoint x="790" y="370" /><di:waypoint x="880" y="370" /></bpmndi:BPMNEdge>
                    </bpmndi:BPMNPlane>
                  </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;
        return xml
                .replace("@MODEL_KEY@", SamplePurchaseOrderFlowDefinition.MODEL_KEY)
                .replace("@MODEL_NAME@", SamplePurchaseOrderFlowDefinition.MODEL_NAME)
                .replace("@VAR_INITIATOR@", SamplePurchaseOrderFlowDefinition.VAR_INITIATOR)
                .replace("@FORM_KEY@", SamplePurchaseOrderFlowDefinition.FORM_KEY)
                .replace("@NODE_DEPT_LEADER@", SamplePurchaseOrderFlowDefinition.NODE_DEPT_LEADER_APPROVE)
                .replace("@NODE_ENGINEERING@", SamplePurchaseOrderFlowDefinition.NODE_ENGINEERING_MANAGER_APPROVE)
                .replace("@NODE_COUNTERSIGN@", SamplePurchaseOrderFlowDefinition.NODE_PURCHASE_COUNTERSIGN)
                .replace("@NODE_APPLICANT_MODIFY@", SamplePurchaseOrderFlowDefinition.NODE_APPLICANT_MODIFY)
                .replace("@VAR_ASSIGNEE@", SamplePurchaseOrderFlowDefinition.VAR_ASSIGNEE)
                .replace("@EXPR_DEPT_LEADER@",
                        SamplePurchaseOrderFlowDefinition.variableExpression(SamplePurchaseOrderFlowDefinition.VAR_DEPT_LEADER_ID))
                .replace("@EXPR_ENGINEERING@",
                        SamplePurchaseOrderFlowDefinition.variableExpression(SamplePurchaseOrderFlowDefinition.VAR_ENGINEERING_MANAGER_ID))
                .replace("@EXPR_ASSIGNEE@",
                        SamplePurchaseOrderFlowDefinition.variableExpression(SamplePurchaseOrderFlowDefinition.VAR_ASSIGNEE))
                .replace("@EXPR_COUNTERSIGN_USER_LIST@",
                        SamplePurchaseOrderFlowDefinition.variableExpression(SamplePurchaseOrderFlowDefinition.VAR_COUNTERSIGN_USER_LIST))
                .replace("@EXPR_INITIATOR@",
                        SamplePurchaseOrderFlowDefinition.variableExpression(SamplePurchaseOrderFlowDefinition.VAR_INITIATOR))
                .replace("@COUNTERSIGN_COMPLETION@",
                        SamplePurchaseOrderFlowDefinition.countersignCompletionConditionExpression())
                .replace("@REJECT_CONDITION@", SamplePurchaseOrderFlowDefinition.rejectConditionExpression())
                .replace("@PERM_DEPT_LEADER@",
                        SamplePurchaseOrderFlowDefinition.nodeFieldPermissionsJson(SamplePurchaseOrderFlowDefinition.NODE_DEPT_LEADER_APPROVE))
                .replace("@PERM_ENGINEERING@",
                        SamplePurchaseOrderFlowDefinition.nodeFieldPermissionsJson(SamplePurchaseOrderFlowDefinition.NODE_ENGINEERING_MANAGER_APPROVE))
                .replace("@PERM_COUNTERSIGN@",
                        SamplePurchaseOrderFlowDefinition.nodeFieldPermissionsJson(SamplePurchaseOrderFlowDefinition.NODE_PURCHASE_COUNTERSIGN))
                .replace("@PERM_APPLICANT_MODIFY@",
                        SamplePurchaseOrderFlowDefinition.nodeFieldPermissionsJson(SamplePurchaseOrderFlowDefinition.NODE_APPLICANT_MODIFY));
    }
}
