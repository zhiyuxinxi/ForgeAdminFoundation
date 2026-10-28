# 自动化测试与验证标准

> 适用于 `/test`、阶段收尾验证、Review 后修复验证和归档前验收。目标是复用已有测试基线和执行记录，避免每次从零开始摸索。

## 1. 核心原则

- **增量优先**：复用当前变更已有、与本轮有关的需求和验证记录，再决定验证范围；不为普通局部任务补建固定四份材料。
- **证据优先**：不能只写“已通过”，必须记录实际命令、关键输出、接口返回或失败原因。
- **最小闭环**：验证范围跟随本轮改动的风险面扩展，不做无意义全量验证。
- **可复跑**：命令必须可复制执行，环境变量、端口、Token 获取方式和服务启动方式要写清楚。
- **不污染环境**：只停止本轮启动的服务；不清理用户或其他任务留下的进程、数据库和文件。

## 2. 复用本次所需上下文

只补读尚未核实或已变化、与本轮有关的内容；已读且未变的资料直接复用，不固定顺序或反复全读：

1. `AGENTS.md` 或当前会话注入的项目规则。
2. `code-copilot/memory/preferences.md`、`code-copilot/memory/pitfalls.md`、`code-copilot/memory/decisions.md`。
3. `code-copilot/rules/automated-testing-standard.md`。
4. 当前变更的 `spec.md`、`tasks.md`。
5. 当前变更已存在的 `test-spec.md`、`execution-log.md`。
6. `git status --short` 和本轮相关文件的 `git diff --name-only`。

复用已有说明和回执。普通局部修复不强制创建`test-spec.md`或`execution-log.md`；任务明确需要独立交接时才创建缺失材料，不重写已有记录。

## 3. 增量范围判定

根据变更文件选择验证矩阵：

以下按本次实际行为选择，不是固定命令前置。具名测试已经编译目标模块时不再重复compile，发布制品的正式构建可作为相应构建证据。

| 变更类型 | 最小有效验证 | 必要边界 |
| --- | --- | --- |
| 仅文档/说明 | 变化内容、链接和diff检查 | 不运行代码测试或构建 |
| Java逻辑/状态 | 当前模块具名行为用例及相关编译 | 正常、边界、非法状态或零行写入按实际变化选择 |
| import/依赖/组件API | 当前版本的真实解析、classpath或正式构建 | mock虚构模块和未声明缓存依赖不证明实际可用 |
| Mapper查询 | 真实BoundSql与当前解析/租户/数据权限/分页链，相关结果用例 | XML格式检查或绕插件直跑SQL不能替代；数据库特定行为使用相应数据库证据 |
| 权限/事务/幂等 | 通过受影响入口/代理验证授权、回滚、重复或并发 | 直接调用Controller、mock数据源只能证明部分逻辑 |
| API/前后端协议 | 实际参数、序列化、适配器、返回和失败语义 | ID/金额/时间/空值/分页对齐，HTTP200不等于业务成功 |
| 前端交互 | 修改文件检查和相关组件/适配器行为用例 | 回填切换、过期响应、防重、失败恢复；真实布局/浏览器特有行为或用户要求时再补浏览器验证 |
| 迁移/schema | 受影响迁移实跑、最终表/列/数据及相关旧版本升级 | 初始化变化才补空库；不改历史checksum，不把H2当生产MySQL证明 |
| 异步/外部调用/缓存 | 相关提交时机、上下文、重复/乱序、结果未知和恢复 | 不依赖固定sleep或盲目重试副作用，不为普通CRUD增加任务引擎 |
| 配置/资源/性能 | 实际消费者值、上限、查询次数或成功失败释放 | 文件存在不等于生效；小样本不等于大数据验证 |

预期结果来自需求、稳定合同和明确数据，不能复制实现算法生成期望。不mock掉本次要证明的机制；无关用例总数不能证明当前变化。核对实际执行数/失败/跳过，必要环境缺失如实记录并沿平台修复，不降低断言或冒称通过。

默认不全量跑mvn test、构建或完整E2E；只在本次影响确实要求或用户明确要求时扩大，不能因为涉及“权限”“迁移”等词就自动全跑。

## 4. 标准执行流程

### Step 0：复用基线

- 找到最近一次成功验证记录。
- 记录本轮只新增或重新验证的差异点。
- 如果上次失败，先定位并修复或取得环境恢复证据，再复验受影响项；同样输入无变化时不重复失败操作。

### Step 1：生成或更新测试计划

- 已有 `test-spec.md`：追加“本轮增量验证”小节。
- 没有独立计划文件时，在既有任务记录或本轮结果中说明必要范围和证据；不因此增加材料前置。

### Step 2：执行最小验证矩阵

- 按第3节矩阵选择本轮必要检查；具名测试已编译目标模块时，不再单独编译。
- 需要服务级证据时，复用可用服务并执行对应登录、接口或数据库验证；仅为当前证据启动缺失服务，不把这些动作变成每轮固定前置。
- 涉及导入、依赖或发布时取得实际构建证据；已授权发布由候选正式构建完成，不预先重复构建。普通局部交互按变化验证，不机械追加完整浏览器UAT。

### Step 3：记录证据

在当前已有的 `execution-log.md`、任务记录或本轮结果中记录一次。普通局部任务不为此另建日志文件；记录与实际执行有关的以下内容：

- 执行时间。
- 变更范围。
- 命令和结果。
- 关键接口返回或数据库查询结论。
- 警告项和是否阻断。
- 跳过项及原因。
- 本轮启动并已停止的服务 PID。

### Step 4：回填状态

- 只更新本轮已经存在且状态发生变化的 `tasks.md` / `spec.md`；引用同一份结果，避免复制命令和回执。没有这些材料时不为收尾补建。
- 实际新错误按既有项目约定记录到 `code-copilot/memory/pitfalls.md`，保留失败及修复证据；已记录的同一事件引用原件，不再重复抄写。
- 只有本轮产生了需要保留且已确认的决策或偏好，才更新对应项目记录；不把写入所有memory文件当作验证完成条件，不从普通行为推断新增偏好。

## 5. Forge命令参考

下列是工具已安装时的参考，先核当前项目脚本和工具能力，不据历史文档猜测本机路径、nvm、依赖缓存或编造命令。Harness按当前模式调用可用工具。

### 后端

```bash
mvn -pl forge-admin-server -am -Dtest=实际变更测试类 -Dsurefire.failIfNoSpecifiedTests=false test
```

插件级改动可优先跑：

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile
```

后端默认编译并执行测试，不按 dev/prod 环境隐式过滤测试标签。`enable-tests` 保留为兼容的显式测试入口，不排除具名测试类。`-Dtest` 只选择本轮直接相关测试；上游无匹配测试时允许继续，目标模块必须有实际非 skipped 的测试报告，不能把零测试当通过。

公共 `forge-starter-core/src/main/resources/forge/i18n/messages*.properties` 或其公共 locale/message 消费实现发生变化时，中英资源及直接消费行为必须同批对齐，且同一候选的具名测试选择必须包含现有 `UiMessagesTest`。仅执行其他 `-Dtest` 类不能证明变化的公共资源通过。独立公共底座在 `forge-server` 目录使用：

```bash
mvn -pl forge-framework/forge-starter-parent/forge-starter-core -am -Dtest=UiMessagesTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Harness模式以可信上下文为准：direct可用隔离worker现有命令，或按forge-verify实际profiles/capability取得admit后run；controlled使用当前合同及注入准入。选择对应模块与`UiMessagesTest`，已有同候选有效回执已执行该类则复用，不重复运行。仅业务 `forge/i18n/business/` bundle 变化时，同批维护中英词条并选择该业务现有直接测试，不强制无公共改动的任务执行 core 测试。其余范围继续按本轮差异确定，保留可复用结果，不增加全量回归。

只生成制品时，显式使用 `mvn -pl forge-admin-server -am package -Pprod,fast-package`。`fast-package` 默认不激活，跳过测试编译和执行；它只提供打包证据，不能声称测试通过。仅编译也不等于测试通过。需要测试时移除该 profile，或使用 `enable-tests`（其优先级高于 `fast-package`）。

Flyway 脚本变更必须先跑低成本静态检查，确认迁移脚本中没有会被 Flyway 解析的业务 `${...}` 占位符：

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

该命令应无输出；确需入库 `${...}` 模板时，用 SQL `CONCAT('$', '{token}')` 形式拼接。

### 前端

```bash
pnpm build
```

### 常用接口验证

- 登录：`POST /auth/login`
- 单据配置：`GET /ai/business/document/config/{objectId}`
- 单据运行态：`GET /ai/business/document/{objectCode}/{recordId}/runtime`
- 发起流程：`POST /ai/business/flow/start`
- 流程状态：`GET /ai/business/flow/status/{objectCode}/{recordId}`
- 触发器分页：`GET /ai/business/trigger/page?pageNum=1&pageSize=10`
- 场景模板：`GET /ai/business/trigger/scenario-templates`
- 业务指标：`GET /ai/business/stats/{configKey}/metrics`

加密链路排查时可使用 `X-Inner-Call: true` 做内部验证，但最终结论必须说明是否覆盖真实用户调用链路。

## 6. 完成标准

一轮自动化测试完成必须满足：

- 本轮增量范围明确。
- 必跑命令已有执行证据。
- 失败项有根因或下一步，不把失败写成通过。
- 跳过项有具体原因，例如“本地 MySQL 未启动”或“本轮仅文档变更”。
- 当前任务结果已记录一次，已有材料中的相关状态已同步；不要求无关文档或memory文件发生修改。
- 本轮启动的服务已停止或明确说明仍需保留。
