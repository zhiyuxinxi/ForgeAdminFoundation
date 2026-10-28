# 功能全景

本页按业务使用方式组织当前公开源码中的能力。它是功能与代码入口说明，不是全部功能已在任意环境验收通过的承诺。配置项、默认开关、模型支持和外部系统兼容性，以当前版本源码及实际环境为准。

功能包含上游原生基础和本发行版持续维护的改造；归属见[来源与改造说明](upstream-and-changes.md)。建设服务、培训、某次环境部署及计划事项不计为已提供的软件功能。

## 1. 身份、组织与权限

- 用户开通、停用、资料维护、密码管理，以及人员导入和组织/角色关联。
- 组织树、岗位、成员关系和当前组织上下文。
- 租户与租户成员、不同客户端的登录和资源入口。
- 角色与菜单、按钮、API 资源授权，数据范围配置。
- 字典、参数、菜单显示和公共配置。
- 操作日志、登录相关管理和文件等公共管理入口。

这些能力用于建立统一管理边界。隐藏按钮不能代替服务端授权；新增业务接口和表必须接入对应的权限与租户约束。

源码：[系统管理前端](../forge-admin-ui/src/views/system/)、[系统插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/)、[认证 Starter](../forge-server/forge-framework/forge-starter-parent/forge-starter-auth/)、[数据范围 Starter](../forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/)。

## 2. 低代码业务应用

- 业务对象、字段、关系和业务应用聚合管理。
- 表单设计、预览、节点表单配置及运行表单呈现。
- 应用页面、列表、查询、展示与业务应用入口。
- CRUD 组件：搜索、分页、表格、编辑、详情、导入导出和操作按钮。
- 关联选择、子表、字段规则、公式、编号、唯一性和写入策略相关能力。
- 应用发布协调、配置版本与业务扩展接口。
- 代码模板与生成工程，支持继续进行 Java/Vue 定制开发。

设计态配置通过共享协议交给运行态使用。开发者应明确哪些字段参与存储、哪些仅用于展示或计算，并为自定义业务建立自己的校验与权限规则。

源码：[应用中心](../forge-admin-ui/src/views/app-center/)、[公共 CRUD 组件](../forge-admin-ui/src/components/ai-form/)、[生成器插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/)、[低代码 API](../forge-server/forge-framework/forge-lowcode-api/)、[低代码运行时](../forge-server/forge-framework/forge-lowcode-runtime/)、[公式模块](../forge-server/forge-framework/forge-lowcode-formula/)。

## 3. 流程设计与审批

- 基于 Flowable 的流程模型、设计器、节点审批配置和版本相关管理。
- 流程发起、业务记录绑定、流程状态与历史读取。
- 待办、已办、我发起的任务及审批处理入口。
- 节点表单、字段显示权限和历史数据呈现。
- 流程监控、任务操作、通知和业务回调相关能力。
- 流程发起轮次、终态事件发出/接收记录和确认相关机制。

独立流程服务负责流程执行。业务权限、审批人规则、回调与异常恢复需要结合具体业务配置；本页不承诺任意流程设计都可直接运行。

源码：[流程前端](../forge-admin-ui/src/views/flow/)、[流程服务与客户端](../forge-server/forge-flow/)、[流程插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/)、[流程终态事件迁移](../forge-server/db/migration/V1.0.2026100909__flow_terminal_event_outbox.sql)、[接收记录迁移](../forge-server/db/migration/V1.0.2026100910__flow_event_consumer_inbox.sql)、[投递确认迁移](../forge-server/db/migration/V1.0.2026100911__flow_event_delivery_confirmation.sql)。

## 4. 数据与可视化

- 数据连接、数据集、维度和业务数据定义。
- 数据集运行接口与权限相关配置。
- 报表/可视化工程的项目、画布、图表、主题和交互配置。
- 静态数据、接口数据及业务数据接入入口。
- 独立报表服务和 PC 端报表入口的组合使用。

数据连接需要使用者配置数据库或 API。报表编辑器的前端可见效果，不代表数据连接、登录和发布访问权限已经配置完成。

源码：[数据管理前端](../forge-admin-ui/src/views/data/)、[数据插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/)、[报表前端](../forge-report-ui/)、[报表服务](../forge-server/forge-report-server/)。

## 5. AI 应用与能力开放

- 模型供应商和模型类型相关配置、对话及消息结构。
- 知识库、文档处理和检索相关模块。
- 智能体、工具和 AI 辅助业务应用设计相关入口。
- 能力登记、能力目录、调用说明和在线测试界面。
- 客户端身份、令牌、调用授权与审计相关机制。
- REST 开放网关与 MCP 适配，以及高风险动作相关模块。

模型服务、向量/检索依赖及外部凭据由使用者自行提供。**MCP 服务在管理服务默认配置中关闭**；其他开放能力有各自开关，启用前应核对实际授权与对外地址。不能将“包含适配代码”理解为“随仓库提供模型”或“任意工具已授权可用”。

源码：[AI 前端](../forge-admin-ui/src/views/ai/)、[AI 插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/)、[能力模块](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-parent/)、[MCP 适配](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-mcp/)、[默认配置](../forge-server/forge-admin-server/src/main/resources/application.yml)。

## 6. 外部集成与公共服务

- 外部接口代理、连接与出站策略。
- 协同平台连接、登录、组织同步及消息相关入口。
- 消息模板、渠道、发送记录和投递结果相关处理。
- 定时任务、执行策略、日志和告警相关配置。
- 文件上传/访问、对象存储、Excel 导入导出。
- 配置、缓存、字典、WebSocket、日志与异常处理等公共组件。

协同平台账号、回调域名、企业应用权限、邮件和对象存储均属于需要另行配置的依赖。公开快照不提供任何客户数据或第三方生产账号。

源码：[外部接口插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-external/)、[协同插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/)、[消息插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/)、[任务插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/)、[技术 Starter](../forge-server/forge-framework/forge-starter-parent/)。

## 7. 多端、中英显示与公共交互

- PC 管理端与动态业务页面。
- uni-app 移动工程，H5 是本版本文档说明的开发入口。
- 独立报表客户端。
- 中文/英文切换与偏好保持，前端请求语言、后端消息和显示元数据衔接。
- 菜单、字典、表单、表格、报表和导出等显示文本的语言资源。
- 可读详情、附件、表格取值和部分失败反馈等公共交互组件。

国际化改变显示文本，不自动翻译用户填写的业务正文、附件或历史数据。uni-app 中存在多个目标平台脚本，不代表本公开版本已完成各小程序或原生设备验收。

源码：[PC 语言资源](../forge-admin-ui/src/locales/)、[H5 语言资源](../forge-h5-ui/src/locales/)、[报表工程](../forge-report-ui/)、[后端消息](../forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/resources/forge/i18n/)、[显示元数据迁移](../forge-server/db/migration/V1.0.20261004__foundation_bilingual_display_metadata.sql)。

## 使用边界

本仓库没有附带客户业务成果、真实业务数据、私有部署环境或现成模型账号。项目提供的配置开发和 AI 辅助能力仍需要业务设计、权限设置、代码审查以及针对实际场景的验证。首次安装和当前公开限制见[本地开发指南](getting-started.md)与[版本状态](release-status.md)。
