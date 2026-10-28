# Forge Admin Foundation

2026-10-28 已同步维护主干的最新可公开源码；变更、验证与未覆盖项见[版本状态](docs/release-status.md)。

**面向业务系统开发的公共应用底座：将管理后台、低代码应用、审批流程、移动端、报表与 AI 能力放在同一套可扩展工程中。**

本项目由 [zhiyuxinxi](https://github.com/zhiyuxinxi) 维护，基于原作者及贡献者的 [Forge Admin](https://github.com/yaomindong1996/forge-admin) 持续修改和扩展。我们尊重上游在管理框架、插件、低代码、工作流和 AI 集成方面的工作；本仓库是独立维护的修改发行版。上游来源、我们的具体改造及第三方归属见[来源与改造说明](docs/upstream-and-changes.md)。

[开始使用](docs/getting-started.md) · [功能全景](docs/features.md) · [文档导航](docs/README.md) · [版本状态与已知限制](docs/release-status.md) · [反馈问题](https://github.com/zhiyuxinxi/ForgeAdminFoundation/issues)

## 适合用来做什么

- **建设企业内部管理系统**：复用用户、组织、角色、菜单、字典、文件、日志等公共能力，集中开发业务。
- **实现表单和审批业务**：将数据对象、表单、列表、业务应用与流程组合起来，减少重复编写 CRUD 页面。
- **为同一业务提供多种入口**：按需要启用 PC 管理端、H5 移动端、报表设计器及独立服务。
- **集成 AI 与外部系统**：通过模型配置、知识库、智能体、能力开放和协同连接等模块扩展应用。
- **在代码层持续维护**：保留 Java/Vue 工程和模块边界，支持配置开发与定制代码共同演进。

它提供的是业务应用基础工程。具体行业的数据、流程、权限规则及外部系统接入，需要由使用者按自己的业务完成配置与开发。

## 两个开源项目如何配合

| 项目 | 主要职责 | 从哪里开始 |
| --- | --- | --- |
| **[ForgeAdminFoundation](https://github.com/zhiyuxinxi/ForgeAdminFoundation)**（本仓库） | 提供业务系统运行和开发的底座，包括界面、服务、数据模型、权限、流程及通用组件 | [本地开发指南](docs/getting-started.md) |
| **[ForgeAdminHarness-open-source](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source)**（交付隔离台） | 为 AI 辅助开发提供对话工作台、隔离工作环境、开发约定、验证与受控交付链路 | [交付隔离台 README](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source#readme) |

Foundation 可以作为独立应用工程开发和运行；使用它不要求先安装 Harness。需要通过 AI 对话组织开发工作时，再按 Harness 文档配置模型、容器环境、项目仓库和基线版本。两者分别发布，安装其中一个不会自动安装另一个，也不会自动同步源码或业务数据。

配套平台的 [Harness AI 能力总览](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source/blob/main/docs/ai-capabilities.md) 按表格介绍 Skill、执行工具、Prompt 与运行组件，并区分公开快照已包含的能力与后续更新。这些能力归属 Harness 平台；Foundation 内置的业务 AI 能力见本仓库[功能全景](docs/features.md)。总览入口在同批文档发布后可见。

## 功能概览

下列能力均能在当前公开源码中找到相应模块；使用范围取决于实际启用的服务、配置和权限。完整说明与代码入口见[功能全景](docs/features.md)。

| 能力领域 | 主要内容 | 对使用者的价值 |
| --- | --- | --- |
| 身份、组织与权限 | 用户、角色、组织、岗位、租户、菜单、按钮/API 资源与数据范围 | 为业务系统提供统一的访问控制基础 |
| 低代码业务应用 | 业务对象、表单、页面、列表、关系数据、应用入口、代码生成与运行时 | 将重复页面工作沉淀为配置和可复用组件 |
| 流程与业务协同 | Flowable 模型、流程发起、待办、审批、历史与业务表单联动 | 让业务记录和审批处理衔接 |
| 数据与可视化 | 数据连接、数据集、维度、报表工程、图表和画布 | 为业务查询与展示提供组合能力 |
| AI 应用能力 | 模型与供应商管理、对话、知识库、智能体及工具入口 | 在业务系统中接入自行选择的模型服务 |
| 对外开放与协同 | 能力目录、客户端与令牌、REST/MCP 入口、外部接口及协同平台连接 | 将受控业务能力提供给其他应用 |
| 公共支撑 | 消息、文件、Excel、任务调度、字典、配置、操作日志与缓存 | 降低基础功能重复建设成本 |
| 多端与双语 | PC、H5、报表客户端；中文/英文显示资源、后端消息与数据库显示元数据 | 支持跨端访问和不同语言的使用者 |

## 在上游基础上做了哪些工作

本发行版重点持续加工以下部分，具体入口见[来源与改造说明](docs/upstream-and-changes.md)：

1. **公共中英双语改造**：将语言能力落实到 PC、H5、报表、Java 消息、导入导出、数据库显示元数据及生成模板；区分显示文本和业务原值。
2. **低代码模块职责整理**：拆分共享 API、运行时与公式执行模块，明确设计配置、查询、写入、公式、编号和流程协作之间的职责。
3. **交互结果与可读性修复**：完善详情与附件呈现、表格字段取值、批量操作部分失败反馈，减少界面显示与真实结果不一致。
4. **后端公共能力维护**：围绕用户关系写权限、客户端登录、角色资源和数据范围、业务流程读写及数据源生命周期等进行修复与整理。
5. **独立公开发行整理**：清理环境配置和历史数据，提供公共示例、数据库说明及独立文档，保留上游和第三方许可。

这些是维护方向与已包含的具体改造，不能理解为底座所有功能均由本维护者原创，也不能据此宣称全部场景已经完成运行验收。

## 快速开始

先取得代码：

```bash
git clone https://github.com/zhiyuxinxi/ForgeAdminFoundation.git
cd ForgeAdminFoundation
```

按[本地开发指南](docs/getting-started.md)依次准备工具、初始化独立数据库、填写服务配置、启动后端，再启动 PC 管理端。建议先理解 PC 与管理服务的基本链路，需要审批时启用流程服务，之后再增加 H5、报表及外部集成。

| 需要准备 | 依据与说明 |
| --- | --- |
| JDK 17、Maven 3.9 系列 | [后端 POM](forge-server/pom.xml)与构建文件 |
| MySQL 8、Redis | 使用独立开发实例或数据库；初始化说明见[数据库文档](forge-server/db/README.md) |
| Node.js、pnpm | 三个前端的依赖版本不同；PC 锁文件已包含 Vite 8，不能沿用旧的 Node 18 安装说明，具体见[环境要求](docs/getting-started.md#环境要求) |
| 可选外部服务 | 模型、对象存储、邮件和协同平台等按功能配置，不随源码提供账号或服务额度 |

**公开版本当前提供源码和配置示例。首次公开及本轮文档整理均未完成全新环境安装、数据库初始化与完整业务运行验收。**现有容器示例还存在路径及说明差异，不能直接视为已验证的一键部署方案；详见[版本状态与已知限制](docs/release-status.md)。

## 仓库结构

| 目录 | 内容 |
| --- | --- |
| [forge-server](forge-server/) | Maven 后端：管理、移动、报表、流程服务，以及公共框架、业务模块、数据库脚本 |
| [forge-admin-ui](forge-admin-ui/) | Vue 3 PC 管理端、业务应用设计器与公共组件 |
| [forge-h5-ui](forge-h5-ui/) | uni-app 移动端工程，当前上手文档以 H5 为入口 |
| [forge-report-ui](forge-report-ui/) | 报表和可视化设计器，保留 GoView 相关 MIT 许可 |
| [docs](docs/README.md) | 产品说明、上手指南、架构、来源与版本状态 |
| [docker](docker/)、[docker-forge-admin](docker-forge-admin/) | 现有容器构建与编排示例，使用前查看[适用边界](docs/release-status.md) |
| [code-copilot](code-copilot/)、[.agents/skills](.agents/skills/) | 可复用开发约定和辅助开发材料，属于开发参考 |

## 更新与反馈

项目持续开发，日常迭代主要在维护仓库进行。公开仓库按阶段同步整理后的源码快照，更新频率可能低于维护仓库。请以本仓库当前代码、[公开版本记录](docs/release-status.md)和已知问题为准；尚未同步的新增能力或修复，不视为当前公开版本已包含。

反馈问题请在[本仓库 Issues](https://github.com/zhiyuxinxi/ForgeAdminFoundation/issues)提供公开提交号、使用模块、环境、最小复现步骤和脱敏错误日志。涉及交付隔离台的问题请提交到[Harness Issues](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source/issues)。参与开发前请阅读[贡献指南](CONTRIBUTING.md)。

## 许可证与致谢

主工程沿用 [Apache License 2.0](LICENSE)，并保留 [NOTICE](NOTICE)、上游原作者及贡献者署名。报表工程同时保留其 [GoView MIT 许可证](forge-report-ui/LICENSE)；其他依赖和素材遵循各自许可。感谢 [Forge Admin 原项目](https://github.com/yaomindong1996/forge-admin)及相关开源社区。
