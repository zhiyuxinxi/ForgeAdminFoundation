# Status And Callbacks

Forge business workflow status must be maintained by the business module, not inferred only from Flowable screens.

## Recommended Status Set

Use these values unless the domain has a strong reason to extend them:

| Status | Meaning | Typical allowed actions |
| --- | --- | --- |
| `DRAFT` | Created but not submitted | edit, delete, submit |
| `IN_PROCESS` | Flow instance is running in approval nodes | view, save allowed node fields |
| `NEED_MODIFY` | Rejected back to applicant modify node | edit permitted fields, resubmit, terminate |
| `APPROVED` | Flow completed successfully | readonly, domain side effects |
| `REJECTED` | Applicant terminated or flow rejected | readonly/delete depending on domain |
| `CANCELED` | Flow canceled/withdrawn/terminated | readonly/delete depending on domain |

## Business Table Fields

Add at least:

```sql
`status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '业务状态',
`business_key` varchar(128) DEFAULT NULL COMMENT '流程业务Key',
`process_instance_id` varchar(128) DEFAULT NULL COMMENT '流程实例ID'
```

Add per-node approver and remark fields only when the business table must retain them for search/reporting. Otherwise task comments can stay in flow task history.

## Callback Contract

Code-first Services should use:

```java
@FlowBind(modelKey = MODEL_KEY, businessType = BUSINESS_TYPE)
public class XxxServiceImpl {

    @FlowCallback(on = {
            FlowCallback.ON_TASK_CREATED,
            FlowCallback.ON_TASK_COMPLETED,
            FlowCallback.ON_COMPLETED,
            FlowCallback.ON_REJECTED,
            FlowCallback.ON_CANCELED
    })
    @Transactional(rollbackFor = Exception.class)
    public void handleFlowEvent(FlowEventContext context) {
        // verify tenant + businessKey + current processInstanceId; update idempotently
    }
}
```

Required handling:

- Resolve callbacks by the supplied tenant and process instance ID. An explicit instance that is missing must never fall back to the latest business-key row. A business-key-only legacy callback is safe only when there is one historical instance; multiple rounds require the instance ID and must fail without changing records when it is absent.
- Serialize nonterminal callback processing with the owning row lock or equivalent conditional update inside the real service transaction, before reading the state used for idempotency. Keep completed-instance replay separate from business actions: an existing downstream result-repair event may be replayed for that exact instance, but must not repeat posting or mutate a newer round. Test through the transaction proxy and production Mapper/interceptors rather than mocking away the lock.
- `ON_TASK_CREATED`: repair business status from current active node. This is critical for reject-to-modify flows.
- `ON_TASK_COMPLETED`: copy task variables needed by the business and move status for reject/resubmit decisions.
- `ON_COMPLETED`: set approved and copy final variables, but do not assume this event is the only reliable source.
- `ON_REJECTED`: set rejected with reason.
- `ON_CANCELED`: set canceled with reason.

## Repair Rules

Do not trust event ordering as the only source of truth. Read real active task state through the installed public flow query contract; do not query or write sys_flow_task/engine tables from business Mappers. Keep these boundaries:

1. `TASK_CREATED` callback:
   - Applicant modify node: `IN_PROCESS -> NEED_MODIFY`.
   - Approval node: `NEED_MODIFY -> IN_PROCESS`.

2. Task form save:
   - Verify tenant, stable actor ID, real task ownership, current process instance and node before reconciling an expected stale business status.
   - Apply node-state reconciliation and the permitted write in one controlled transaction/CAS, preserving current-node field permissions; do not grant a task action merely because a client supplied a task ID or requested status.

3. Page/detail query:
   - Keep the read endpoint read-only. It may map authorized public flow-query results to a display projection and report detected drift, but must not persist repair, push todos or generate tasks.
   - Persisted reconciliation belongs to existing callbacks, a configured scheduled handler, or an explicit authorized repair action. Bind it to the current process instance, expected prior state/version and audit evidence.
   - If the installed public client cannot provide required task state, report the precise capability gap and fix that owner; do not recreate flow persistence in the business module.

## Variable Safety

- Write approval variables before completing the task so gateways and callbacks can read them.
- Merge runtime variables and historic variables for terminal events when implementing flow side logic.
- Business callback must be idempotent and tolerate missing variables.
- Keep `recordId`, `businessKey`, and user IDs as strings in frontend. Do not convert Snowflake IDs to JS `Number`.

## Idempotency

- Flow start must be guarded by `tenantId + businessKey`.
- With the installed explicit-round FlowClient API, persist one `startRequestId` before the first remote start and retain it with the canonical command and actor scope across timeouts, local rollback and transport retries. A deliberate new round has a new request ID plus its exact `previousProcessInstanceId`. Do not derive a new identity from each HTTP correlation ID or reinterpret pre-upgrade reservations that have no start identity.
- `getProcessStartStatus(businessKey, startRequestId)` reads the exact round; `getProcessStartRecovery(businessKey, startRequestId)` additionally retrieves that round's original terminal outbox event. Both are read-only and require the authenticated tenant and original applicant. `NOT_FOUND` is not proof that an in-flight start failed. A missing terminal event must remain an explicit recovery error, never a fabricated approval or a lookup of the latest round's variables.
- When the remote start succeeded but the local association did not commit, reuse the original request and instance. Restore the association and apply the original event through the existing callback owner in one local transaction; retain its posting, variables and failure semantics. Do not overwrite another local round, acknowledge delivery from a query, or skip required callback effects by directly setting a terminal status. The low-code capability START/SUBMIT path uses its existing committed execution reservation for this identity; do not build a parallel request ledger.
- Low-code platform start already uses locks and `ai_business_flow_instance_link`; do not add duplicate frontend start paths.
- Code-first business Services should prevent double submit by checking status and existing `business_key/process_instance_id`.
- Domain side effects after approval must have their own idempotency key.
- Compare the canonical command content and actor scope on replay; the same key with different content is not a successful retry. Preserve atomic stock/quantity posting and its applied marker.
- For an already committed local start association, compare persisted caller-input identity before rebuilding variables from mutable document fields or submitting another remote start. Keep current record visibility and start permission checks, while separating replay from new-start state restrictions. State changes caused by the original flow must not invalidate an identical retry; changed caller input must still fail. Legacy associations without an identity keep their compatibility path, and remote command-digest checks remain enabled.
- Countersign completion requires all mandatory obligations for this round to be validly approved. Reject/resubmit restores all still-required approvals; concurrent decisions use aggregate locking/current reads or equivalent CAS rather than unversioned JSON replacement.
