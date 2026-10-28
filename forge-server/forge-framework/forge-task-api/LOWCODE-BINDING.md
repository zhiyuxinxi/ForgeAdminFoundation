# 普通业务任务的低代码绑定

任务定义是不可变版本配置，任务实例是一次实际待办，两者不能混用。普通任务复用 `BusinessTaskEngine`，不使用流程审批任务或调度作业 ID。

## 配置与发布

1. 使用 `/api/task/definition` 创建 `sourceType=lowcode.record` 的任务定义；`/api/task/definition/{id}` 和 `/api/task/definitions?sourceType=lowcode.record` 读取定义。
2. 业务对象和映射字段先完成低代码发布。通过原有业务能力挂接入口保存 `bindingType=TASK`、`targetType=OBJECT`、真实 `targetId`/`targetCode`。其他 targetType 不适用此契约。
3. `bindingConfig` 对应 `TaskBindingSpec`：

```json
{
  "definitionId": 123,
  "assigneeField": "ownerId",
  "titleField": "title",
  "dueAtField": "dueAt",
  "taskIdField": "taskId",
  "stateField": "state",
  "readyValue": "OPEN",
  "completedValue": "DONE",
  "cancelledValue": "CANCELLED",
  "resultFields": { "comment": "completionComment" }
}
```

`dueAtField` 可省略。`resultFields` 是任务完成结果键到业务字段名的白名单，可为空。所有字段必须存在于已发布模型；系统列、主键和计算写入字段不能绑定。实例字段只由引擎维护；普通表单、导入等入口不得新增、替换或清空其关联，同值回传可接受。

4. 再发布业务对象使新绑定生效。服务端把配置放入现有发布版本的 `options.publicTaskBindings`，后续任务以版本 ID 引用该快照。保存草稿不会改变执行中的任务。撤销绑定后，历史实例引用字段仍受保护。

## 运行入口

| 方法与路径 | 行为 |
| --- | --- |
| `GET /ai/crud/{configKey}/tasks` | 读取该对象已发布的任务定义绑定 |
| `POST /ai/crud/{configKey}/tasks/{bindingKey}/{recordId}` | 从实际记录生成任务、写入实例关联并派发；重复调用返回该记录已有实例 |
| `GET /ai/crud/{configKey}/tasks/{bindingKey}/{recordId}` | 读取该来源记录关联的任务实例 |
| `POST /api/task/{id}/start` | 接收人开始任务 |
| `POST /api/task/{id}/complete` | 提交稳定 `requestKey` 与 JSON 对象字符串 `resultJson` |
| `POST /api/task/{id}/cancel` | 来源业务授权后取消并回写来源状态 |

创建入口没有接收人、来源字段值或回调 URL 参数。服务端从当前已发布业务记录读取接收人、标题和期限，并复用 Runtime CRUD 的数据权限、记录锁与条件更新。查看要求该对象 `ai:business:{objectCode}:query` 权限；生成和变更要求 `:edit`，核心另外检查可信租户、有效接收人和任务执行人。

直接调用公共 `create` 也必须经过 `TaskSourceHandler.validateCreation`，校验定义和每个创建值；不能通过绕过低代码控制器来替换接收人或制造新的幂等键。

来源状态、任务状态和回写使用同一个 master 事务。来源校验或回写失败整体回滚；不支持外接数据源、远程回调或跨库事务。已发布表、主键或绑定列/类型改动不兼容历史任务时会明确拒绝执行，不能悄悄写入新目标。

当前绑定是一条来源记录、一个绑定对应一个单次实例，周期生成沿既有调度另行接入。下载产物在配置的前端 API 目录附带 `public-task/{configId}.js` 和 `public-capabilities/task-{configId}.json`，调用相同公共入口；目标部署仍须包含对应已发布对象、公共任务引擎及 generator 来源适配器，不能把下载文件当成已安装数据库或已运行证据。
