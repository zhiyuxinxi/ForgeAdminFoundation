# Code-First Workflow

Use this path for complex business modules that need custom Java service logic, custom business tables, custom forms, or special status repair. The sample is `sample_purchase_order`.

## Runtime API boundary

Read the actual interfaces under `forge-server/forge-framework/forge-lowcode-api/src/main/java/` and the consuming module's `pom.xml` before choosing imports. Modules consuming these contracts explicitly depend on `com.mdframe.forge:forge-lowcode-api`, with the version managed by the existing parent POM.

- Implement `com.mdframe.forge.lowcode.api.form.BusinessCodeFormProvider`; use `BusinessTaskFormContextQueryDTO`, `BusinessTaskFormSaveDTO`, and `BusinessTaskFormContextVO` from that same `form` package. Copy actual method signatures, not the deprecated generator bridge. Business state validation, field-write validation, and persistence remain in the business owner.
- For rule-based numbering, inject `com.mdframe.forge.lowcode.api.code.BusinessCodeGenerator` and call `generate(String ruleCode, Map<String, Object> context)`. Do not depend on `CodeRuleService` or its rule-editing/persistence operations. Tenant and system variables come from trusted server context.
- For flow configuration, inject `com.mdframe.forge.lowcode.api.binding.BusinessFlowBindingQuery` and call `findForObject(Long tenantId, String objectCode)` with an already-authorized server tenant. Handle the returned `Optional<BusinessFlowBinding>` explicitly; the immutable projection exposes `bindingId()` and `bindingConfig()`. Do not copy `BusinessBindingMapper`/`AiBusinessBinding` access from an old sample. This read does not authorize starting or acting on a process.
- Runtime query DTOs (`DynamicCrudQuery`, `CustomQueryConditionDTO`, `CustomQueryExecuteDTO`) and the `LowcodeModelSchema` type closure physically belong to `forge-lowcode-api` while retaining legacy `com.mdframe.forge.plugin.generator.dto...` package names for compatibility. Inspect the owning source/module; do not invent renamed API classes or add generator merely because of the FQN. Designer persistence such as `AiCrudConfig` remains outside this pure API.
- `com.mdframe.forge.lowcode.api.config.LowcodeRuntimeConfig` is the runtime configuration value. Existing design adapters own publication/preview admission and the projection from designer persistence. Do not reproduce that projection in business code or treat a configuration value as action authorization.
- When the current baseline includes `forge-lowcode-runtime`, it owns dynamic CRUD, query/write policies, runtime data scope, datasource resolution, and JDBC consumption. Its service entry is `com.mdframe.forge.lowcode.runtime.service.RuntimeCrudService`; read actual signatures instead of guessing them. The old `DynamicCrudService` design entry remains a compatibility facade. Most internal runtime types retain legacy FQNs, which do not justify adding a generator dependency. Ordinary generated business code consumes the required public APIs rather than assembling a runtime service.
- Formula types and execution belong to `forge-lowcode-formula`; existing types may retain legacy generator package names. Only code that actually owns formula execution needs that module. Ordinary business forms and CRUD reuse the configured runtime instead of creating engines, persistence adapters, function catalogs, or connection pools.

Runtime configuration, datasource, formula, and document-number interfaces are existing platform assembly boundaries, not a requirement to generate an adapter per business. Reuse the application's installed implementations. Datasource snapshots contain no credentials; callers borrow managed `DataSource` objects and must not close their pools. Document-template numbering and rule-based `BusinessCodeGenerator` numbering preserve different tenant/sequence semantics; do not substitute one for the other merely because both return strings. Runtime modules must not depend back on generator persistence or use callbacks/service lookup to conceal that dependency.

Keep the actual module direction `generator → runtime → formula/API/required starters`; formula does not depend back on runtime or generator. Runtime code does not inject design Mappers, entity services, or compatibility facades. Preserve the application's single instance/state and transaction proxy. Page-schema and component-constant types may also retain old package names; use the current owning artifact instead of duplicating them.

The current purchase-order sample and other existing business code may still use generator bridges or implementation classes. Reuse their applicable business behavior, not those legacy dependencies. Do not remove a module's generator dependency until its actual remaining consumers have been migrated within the authorized scope. If this project's baseline lacks a required API, report the concrete baseline gap instead of creating a substitute or assuming a customer helper is a public capability.

Verify the generated or migrated code through the supported compile/test entrypoint using the actual API artifact. Preserve observable behavior and errors, and distinguish source guidance, installed guidance, model consumption, and compiled generated code as separate evidence.

For boundaries changed by this task, also verify the actual owning artifact and application wiring. Old generator caches or a compatible facade can hide an incorrect dependency. Reuse unaffected results; only exercise the configuration admission, formula order, numbering failure/tenant semantics, or borrowed resource boundary actually affected by the change.

## Sequence

1. Create the business contract.
   - Object code: lower snake case, for example `contract_payment`.
   - Model key: `<object_code>_approval`.
   - Business key: `<objectCode>:<recordId>`.
   - Status values: `DRAFT`, `IN_PROCESS`, `NEED_MODIFY`, `APPROVED`, `REJECTED`, `CANCELED`.

2. Add the table and dictionary migration.
   - Include `business_key`, `process_instance_id`, approver fields, node remark fields, and reject/cancel reason fields.
   - Include standard audit and tenant fields.
   - Add unique key on `(tenant_id, order_no/code)` and `(tenant_id, business_key)`.
   - Add status dictionary with `sys_dict_type` and `sys_dict_data`.

3. Add backend module files.
   - Entity extends `TenantEntity`.
   - Mapper extends `BaseMapper<Entity>`.
   - Complex queries go in Mapper XML with explicit columns and tenant predicates.
   - Service owns create/update/delete/submit/task-save/status-callback logic.
   - Controller uses `RespInfo`, `@ApiDecrypt`, `@ApiEncrypt`, and operation logs when the page uses encrypted request flow.

4. Add a `FlowDefinition` support class.
   - Keep constants for object code, model key, provider key, form key, node keys, statuses, variables, action names, and field codes.
   - Expose `formAssets(objectCode)`, `fields(recordId)`, `buildTaskFormContext(...)`, `toTaskSaveDTO(...)`, `recordData(...)`, `businessKey(id)`, and `flowModelPayload(...)`.
   - Keep node field permission defaults here only as seed/defaults. Designer-edited BPMN is the source of truth after deployment.

5. Add a `BusinessCodeFormProvider`.
   - Implement `providerKey()`, `providerName()`, `formAssets(objectCode)`, `buildContext(...)`, and `saveContext(...)`.
   - Use `BusinessTaskFormContextVO` for task/done/history forms.
   - In `saveContext`, convert `BusinessTaskFormSaveDTO` to a business DTO and call a business Service method that validates status and node key.
   - Implement `buildSummaries(...)` with batch query to avoid N+1 lookup in todo/done lists.

6. Start the flow from the business Service.
   - Validate only `DRAFT` records can start.
   - Build variables needed by BPMN assignment, conditions, title, and task forms.
   - Call `flowClient.startProcess(modelKey, businessKey, businessType, title, variables, userId, userName, deptId, deptName)`.
   - Save `businessKey`, `processInstanceId`, approver selections, and set status `IN_PROCESS` in the same transaction after successful start.

7. Register callbacks.
   - Annotate the Service with `@FlowBind(modelKey = MODEL_KEY, businessType = BUSINESS_TYPE)`.
   - Add `@FlowCallback(on = { ON_TASK_CREATED, ON_TASK_COMPLETED, ON_COMPLETED, ON_REJECTED, ON_CANCELED })`.
   - Use callbacks to move status and copy finish variables, but make every transition idempotent.
   - Add active-task reconciliation in page/detail queries for known running statuses.

8. Seed App Center and flow binding.
   - Insert/update `ai_business_suite`, `ai_business_object`, `ai_business_app`.
   - Insert `ai_business_binding` with `binding_type='FLOW'`, `flowModelKey`, `titleTemplate`, `businessBinding`, and `variableMapping`.
   - Keep `nodeForms` only as compatibility fallback; new node form config belongs in BPMN node attributes.

9. Add frontend only if needed.
   - Code-first pages can be custom Vue pages when business UX is complex.
   - Use `DictTag` / `useDict` for status.
   - Keep Long IDs as strings when reading row IDs or flow variables.
   - If the page is used as an external task form, read task form context and obey node field permissions.

## Status Gate Defaults

- Create: status `DRAFT`.
- Edit: allow `DRAFT` and `NEED_MODIFY`.
- Delete: allow `DRAFT`, `REJECTED`, `CANCELED`.
- Submit: allow `DRAFT` only.
- Applicant modify task save: require `NEED_MODIFY`, but repair from `IN_PROCESS` when the active task node is applicant modify.
- Approval task save: require `IN_PROCESS`, but repair from `NEED_MODIFY` when the active task node is an approval node.

## File Pattern

Use this layout under `forge-server/forge-business/forge-business-core/src/main/java/.../<domain>/`:

```text
controller/<Business>Controller.java
domain/<Business>.java
dto/<Business>DTO.java
dto/<Business>Query.java
dto/<Business>SubmitDTO.java
dto/<Business>TaskSaveDTO.java
mapper/<Business>Mapper.java
provider/<Business>CodeFormProvider.java
service/<Business>Service.java
service/impl/<Business>ServiceImpl.java
support/<Business>FlowDefinition.java
support/<Business>FlowBpmn.java
vo/<Business>VO.java
vo/<Business>FlowInitVO.java
```

Mapper XML goes under:

```text
forge-server/forge-business/forge-business-core/src/main/resources/mapper/business/<Business>Mapper.xml
```
