# BPMN Configuration

Business workflow node configuration is owned by the real flow designer and persisted on BPMN nodes.

## Runtime Priority

Task form resolution priority:

```text
BPMN node formKey/formUrl/formJson/formFieldPermissions
  > ai_business_binding.binding_config.nodeForms fallback
  > process default form
```

Do not build a separate node configuration workbench in App Center.

## User Task Attributes

A business user task should carry:

```xml
<bpmn:userTask
    id="dept_leader_approve"
    name="部门负责人审批"
    flowable:assignee="${deptLeaderId}"
    flowable:formKey="sample_purchase_order_approval_form"
    flowable:formFieldPermissions='[
      {"field":"arrivalListFileIds","label":"上传清单","readable":true,"writable":true,"required":false},
      {"field":"deptLeaderRemark","label":"部门负责人意见","readable":true,"writable":true,"required":false}
    ]'
    flowable:allowApprove="true"
    flowable:allowReject="true"
    flowable:allowDelegate="true"
    flowable:allowReturn="false"
    flowable:allowTerminate="false"
    flowable:requireComment="true">
</bpmn:userTask>
```

Use `formJson` / `formRef` when the designer supports structured form references:

```json
{
  "type": "BUSINESS_CODE_FORM",
  "formMode": "BUSINESS_CODE_FORM",
  "objectCode": "sample_purchase_order",
  "providerKey": "samplePurchaseOrder",
  "formKey": "sample_purchase_order_approval_form",
  "formUrl": "/business/purchase-order-test"
}
```

## Variables And Expressions

Assignee expressions:

```xml
flowable:assignee="${deptLeaderId}"
```

Multi-instance countersign:

```xml
<bpmn:multiInstanceLoopCharacteristics isSequential="false"
    flowable:collection="${countersignUserList}"
    flowable:elementVariable="assignee">
  <bpmn:completionCondition xsi:type="bpmn:tFormalExpression"><![CDATA[${approved == false || nrOfCompletedInstances == nrOfInstances}]]></bpmn:completionCondition>
</bpmn:multiInstanceLoopCharacteristics>
```

Reject branch:

```xml
<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression"><![CDATA[${approvalResult == 'reject'}]]></bpmn:conditionExpression>
```

Default branch:

- Put the default on the gateway, for example `default="Flow_approve"`.
- Do not put a `conditionExpression` on the default sequence flow.

## Flow Model Initialization

For built-in samples, initialization may call `FlowClient` to create/deploy a model, but use this rule:

- Model absent: create with default BPMN and deploy.
- Model exists, BPMN XML empty: write default BPMN and deploy.
- Model exists, BPMN XML non-empty: preserve it. Deploy existing model if it is not deployed, but do not overwrite XML.

This preserves user-edited node field permissions and approval configuration.

## Form Asset Rules

- Code-first assets use `BUSINESS_CODE_FORM` and `BusinessCodeFormProvider`.
- Low-code assets use `BUSINESS_OBJECT_FORM` and published runtime config.
- External URL forms are advanced fallback only, not default business-user configuration.
- Provider/form assets should expose field catalog so node permission matrix can use real fields.

## Common Failure Points

- BPMN references variables not in start variable mapping.
- BPMN XML contains duplicate sequence flows from the same source/target and creates duplicate tasks.
- `formUrl` or `formKey` has leading/trailing spaces; trim before matching.
- Node permissions configured only in `nodeForms` seed, but deployed BPMN has stale node attributes.
- Flow designer save reintroduces a condition on gateway default flow.
