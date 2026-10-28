# 公共底座中英双语 / Foundation localization

公共代码底座独立维护于 `maintainer/forge-admin-foundation`，平台 AI 编排由 `ForgeAdminHarness` 维护。客户仓库按明确提交继承公共改进；不得用客户工作区整体覆盖公共底座。

The reusable code foundation is maintained in `maintainer/forge-admin-foundation`; AI orchestration belongs to `ForgeAdminHarness`. Consumers inherit reviewed, pinned commits, preserving their own contracts and data.

## 语言与边界 / Languages and boundaries

- 支持 `zh-CN` 与 `en-US`，默认中文。语言偏好独立于登录，切换不重载编辑器、不清空草稿。
- UI text, validation, API errors, notifications, exports, runtime metadata, and public designers support both locales. Preserve the language preference without resetting drafts or editor state.
- 只翻译显示文本。ID、权限串、路由、状态码、枚举值、表字段机器名、租户与流程标识不得翻译或换值。
- Never translate user-entered records, historical free text, attachments, identifiers, permission keys, enum values, or workflow identities. Missing translations fall back compatibly; fallback alone is not evidence of English coverage.

## 实际消费者 / Consumers

- PC：使用 `forge-admin-ui/src/locales` 的现有资源与显示函数；Naive UI、日期、表单/流程设计器消费同一语言。
- H5：使用 `forge-h5-ui/src/locales`，公共固定文本通过 `L`，动态标签通过 `localizeLabel`。规范化工具保留翻译元数据，显示层生成投影，不写回配置或表单值。
- Report：使用 `forge-report-ui` 的现有 vue-i18n 与显示适配器，覆盖设计器、登录、公共配置与 Naive UI；图表数据和持久化组件标识保持原值。
- Every request path, including fetch, streaming, upload and download, carries `Accept-Language`. Server responses and asynchronous export jobs bind the request locale; do not rely on a mutable process-wide locale.
- Java：复用公共 locale/message 实现；校验、鉴权、异常处理与导入导出均沿真实消费者接线。保持错误码与 HTTP 状态不变，参数值不翻译。
- 业务消息使用 `forge-business-core/src/main/resources/forge/i18n/business/messages.properties` 与 `messages_en.properties`，显式错误调用 `UiMessages.message("business.<module>.<key>", ...)`。现有 DTO 中文校验文本经公共异常处理器的 `UiMessages.translate` 处理时，须以 `business.display.<module>.<key>` 成对登记中文原文和英文译文；普通 `business.*` 词条不自动成为中文文本别名。核心词条优先，未知文本与业务数据保持原值。先核项目实际 `UiMessages` 已支持业务显示别名，缺能力时同步公共底座，不能在业务模块另写异常处理器或把业务词条塞进核心资源。

## 数据库与生成器 / Database and generators

- 保留中文字段，增加英文显示字段：菜单 `resourceNameEn`、字典 `dictLabelEn`、导入导出列 `columnNameEn`；动态 schema 使用 `labelI18n`、`placeholderI18n`、`businessNameI18n` 等明确显示元数据。
- Add forward-only migrations with new version numbers. Seed translations by stable identity and the expected original Chinese label, filling only empty English fields. Preserve customer overrides, IDs, permissions, original data and timestamps.
- 生成器必须把英文元数据传递到 Java、SQL、运行配置与前端语言资源；不能只改设计器编辑面板或仅在预览中翻译。未提供英文业务显示名时明确保留兼容回退，不编造客户内容译文。
- Export metadata includes localized column, sheet and filename labels. Asynchronous jobs must capture and restore locale without leaking it between jobs.

## 验证 / Verification

按实际差异验证两语言显示、参数一致、切换保留草稿、请求语言、原值不变、生成产物消费、迁移新装/升级与必要构建。源码、专项测试、隔离浏览器与实际数据库结果分别记录。不要为局部修复重跑无关业务或平台能力验收。

修改公共 `forge-starter-core/src/main/resources/forge/i18n/messages*.properties` 或其公共 locale/message 消费实现时，必须同批对齐中文、英文资源及直接消费行为，并在同一候选上执行现有 `UiMessagesTest`；其他直接消费者测试按实际差异选择。只执行其他 `-Dtest` 具名类不能覆盖变化的公共消息资源，语言回退或其他测试通过也不能替代该结果。具名测试的最小命令与 Harness 入口边界见 [自动化测试标准](automated-testing-standard.md#后端)。

仅修改业务 `forge-business-core/src/main/resources/forge/i18n/business/` bundle 时，同批维护中英词条，使用该业务现有的直接消费者测试；没有公共消息资源或公共 locale/message 消费实现变化时，不因此追加 core 的 `UiMessagesTest`，不扩展成全量回归。已有结果仅在仍覆盖当前候选及本轮差异时复用。

Verify the changed consumers and retain valid prior evidence. Distinguish static checks, builds, isolated browser results, database migration results and deployed runtime acceptance. Never claim customer upgrades from a foundation commit alone.
