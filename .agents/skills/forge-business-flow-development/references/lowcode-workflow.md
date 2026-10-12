# Low-Code Workflow

Use this path when the business object is backed by Forge dynamic CRUD and does not need custom Java Service logic for each record.

## Sequence

1. Create or publish the business object and runtime config.
   - Runtime page uses `AiCrudPage` / `/ai/crud/{configKey}`.
   - Document config identifies the status field, owner field, title field, and default flow.

2. Configure flow binding.
   - Use `ai_business_binding` with `target_type='OBJECT'`, `target_code=<objectCode>`, `binding_type='FLOW'`.
   - Required binding config keys: `flowModelKey`, `titleTemplate`, `businessBinding`, `variableMapping`.
   - Every BPMN assignee/candidate/condition expression must have a matching variable mapping.

3. Start the flow through platform runtime APIs.
   - Frontend action should use the built-in `START_FLOW` path.
   - Backend `BusinessFlowService` locks by `tenantId + businessKey` and writes `ai_business_flow_instance_link`.
   - Do not implement duplicate custom start calls around `AiCrudPage` custom-action events.

4. Configure node forms in the real flow designer.
   - Select business form asset for each user task.
   - Configure field permissions in the node drawer.
   - Save to BPMN node `formKey/formUrl/formJson/formFieldPermissions`.
   - `BusinessFlowBinding.nodeForms` is fallback only.

5. Handle approval actions.
   - Todo form context: `GET /ai/business/flow/task-form-context`.
   - Readonly done/history context: `GET /ai/business/flow/task-form-context/readonly`.
   - Save task business fields: `POST /ai/business/flow/task-form-context`.
   - Complete task: platform action endpoint invokes `FlowClient.approve/reject` and syncs `ai_business_flow_instance_link` and document status.

6. Callback actions and side effects.
   - Use `callbackActions` in flow binding only for domain actions that must run after terminal results.
   - For inventory, finance, or quantity mutations, use platform action/quantity services with idempotency keys. Do not put irreversible side effects in frontend handlers.

## Required Checks

- The object code passed from runtime page must be canonical, not a config key alias.
- `businessKey` must be `<objectCode>:<recordId>`.
- If Flowable engine retries or admin-side link insertion fails, flow service must still be idempotent by business key.
- All low-code SQL seed scripts that contain `${...}`-like templates must avoid Flyway placeholder parsing, for example by using `CONCAT('$', '{field}')`.
