# 公共周期任务

`PeriodicTaskEngine` 与普通 `BusinessTaskEngine` 共用任务实例。任务规则由公共任务插件持久化，现有 Quartz 的 `publicPeriodicTaskScan` 每 5 分钟调用一次；`publicPeriodicTask` 是同一入口的 `IJobExecutor` handler，不新增调度器。

## 规则与时间

先用普通定义接口创建 `sourceType=task.periodic` 的定义，再用 `/api/task/schedules` 保存周期规则。内置公共来源固定 `sourceKey=self`，它代表周期规则自身。保存需 `task:schedule:write`，当前组织必须有效；该权限是租户内周期责任分配的管理权限。定义权限仍为 `task:definition:write`。

规则编码/版本、定义 ID、标题、首次日期、时区、频率、提前生成天数、执行天数、结束条件、责任人规则、允许逾期完成均为不可变快照。同一编码/版本不能用不同内容重放。新规则默认停用，同一编码最多启用一个版本；修改创建新版本并显式切换启用版本，历史实例不改写。

周期支持 ONCE、DAILY、WEEKLY、MONTHLY、QUARTERLY、HALF_YEARLY、YEARLY、CUSTOM_DAYS。按首次日期计算每期，月底不发生累积漂移。日期使用规则的 IANA 时区；生成时间、开始时间、截止时间及周期任务的 `dueAt` 以 UTC 的无偏移日期时间表示。执行区间为 `[开始日零点, 开始日加执行天数零点)`，提前生成并不提前开放执行。

`POST /api/task/schedules/preview` 输入 `calendar/fromIndex/count`，返回生成/开始/截止时间；count 为 1–60，index 从 0 开始。结束条件为不限、endCount、endDate 三选一，结束日按开始日期包含边界。频率 1–366、提前 0–366 天、执行期 1–366 天，超限拒绝。

## 人员与运行

PERSON 是一个用户稳定 ID；DEPARTMENT 是同一有效组织的直接有效成员；POST 是真实岗位分配的有效成员。每期最多 200 人，超限显式拒绝，不能静默截断。名称不是身份条件；期次保存当时选中的用户 ID/名称快照。岗位、组织、人员都来自统一 system 主数据。

每次调度按规则保存的发布人和当前组织重新调用 `IUserLoadService` 加载租户、账号和权限，拒绝失效账号、失去组织或权限的发布人，不借用 Flowable 技术身份，不从 jobParam 接收身份。作业线程的租户、数据源和执行身份均在结束后恢复。规则启用和手动生成要求原发布人；其他已获周期管理及来源权限的管理者可以停用规则，避免发布人停用后无法停止重试。不得用普通编辑动作改写历史发布身份。

`POST /api/task/schedules/{id}/enabled` 接收 `{ "enabled": true }`。`POST /api/task/schedules/{id}/generate?limit=10` 是授权的有界补发入口，limit 为 1–60。调度每次至多读取 100 个到期规则，每规则补发至多 10 期；其余期次由后续调度继续。不会跳过停机期间应生成的期次。过期规则仍可生成遗漏实例，但不允许逾期的任务不能完成，应由授权发布人取消或按新的业务决定处理。

期次唯一键为 `(tenantId, scheduleId, occurrenceIndex)`，普通任务 occurrenceKey 为 `period:{scheduleId}:{index}`。期次责任快照先提交，生成阶段在同一个 master 事务中准备来源、创建/派发全部任务、关联期次并推进游标。来源或派发失败整期回滚，PENDING 快照保留；重试不重新分配人员，不生成部分成功的任务。并发由规则行锁和数据库唯一键约束。停止规则后不再生成，但已生成任务保留原状态和时间约束。

## 真实消费与扩展边界

内置 `task.periodic` 是可直接运行的公共确认任务来源。分配人沿已有任务中心查询、开始、完成，结果仅接受空对象 `{}`；未知来源不会自动使用这种表单。公共完成 API 同样检查未到开始日和禁止逾期的约束；已成功完成的同键重试仍返回原结果。

自定义业务通过明确实现 `TaskPeriodSourceHandler` 选择接入。`validateSchedule` 必须校验真实模板、责任选择的业务授权和源状态；`prepareOccurrence` 按稳定期次与分配快照在 master 事务内建立真实来源，随后继续原 `authorize/validateCreation/validateCompletion/onTerminal` 链。不得把客户检查项库、客户字段或回调 URL 放进公共引擎。

既有 `lowcode.record` 是单条来源记录的一次任务绑定，**尚不支持自动周期建源**；不能只改 sourceType 或传自选 occurrenceKey 冒充周期接入。周期业务记录生成需由实际 source owner 接入此契约。源码与本地专项验证不代表目标环境已部署或 Harness 业务验收已完成。
