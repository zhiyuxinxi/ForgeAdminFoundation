# 上游来源与本发行版改造

## 原项目与维护关系

ForgeAdminFoundation 基于 [yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin)，感谢原作者及贡献者提供的工程基础。原项目已经具备管理后台、插件化后端、低代码/代码生成、工作流、多租户、权限与 AI 集成等能力，这些基础不应被描述为本维护者从零创造。

本仓库由 [zhiyuxinxi](https://github.com/zhiyuxinxi) 维护，是经过修改、扩展和公开发行整理的独立下游版本。公开仓库使用整理后的源码快照和独立 Git 历史；它不是上游官方发行版，也不代表上游作者对本版本作出支持或兼容性承诺。上游最新版本与本仓库之间可能存在双向差异。

## 本维护者持续加工的内容

下表结合已纳入公开快照的维护改动整理，并提供公开代码入口。表述聚焦具体工作，不以功能总数代替实现证据，也不把维护环境的验证结果当作公开安装验收。

| 改造方向 | 具体内容 | 当前公开代码入口 |
| --- | --- | --- |
| 公共中英双语 | PC、H5、报表语言资源和偏好；请求语言与后端消息；菜单、字典、表单及导出显示；数据库增量显示字段 | [PC locales](../forge-admin-ui/src/locales/)、[H5 locales](../forge-h5-ui/src/locales/)、[Java 消息](../forge-server/forge-framework/forge-starter-parent/forge-starter-core/src/main/resources/forge/i18n/)、[双语元数据迁移](../forge-server/db/migration/V1.0.20261004__foundation_bilingual_display_metadata.sql)、[内置标签迁移](../forge-server/db/migration/V1.0.20261005__foundation_bilingual_builtin_labels.sql) |
| 低代码职责整理 | 共享协议、动态 CRUD 运行与公式执行分别归属；梳理设计配置、查询投影、字段写入、唯一性、隐私字段、编号和数据源访问 | [forge-lowcode-api](../forge-server/forge-framework/forge-lowcode-api/)、[forge-lowcode-runtime](../forge-server/forge-framework/forge-lowcode-runtime/)、[forge-lowcode-formula](../forge-server/forge-framework/forge-lowcode-formula/) |
| 生成与运行衔接 | 将生成器设计能力和业务运行契约分离，维护表单/页面与应用、流程的对应关系 | [生成器插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/)、[应用设计器](../forge-admin-ui/src/views/app-center/) |
| 流程职责及结果衔接 | 拆分流程发起、状态、读取、回调与任务呈现；补充发起轮次、终态事件与确认相关结构 | [流程插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/)、[业务流程起始轮次迁移](../forge-server/db/migration/V1.0.2026100907__flow_business_start_rounds.sql)、[终态确认迁移](../forge-server/db/migration/V1.0.2026100911__flow_event_delivery_confirmation.sql) |
| 公共界面结果完整性 | 详情配置与附件可读性、表格默认列取值；批量删除部分失败反馈及后续选择处理 | [公共 CRUD 与表格组件](../forge-admin-ui/src/components/ai-form/)、[系统管理前端](../forge-admin-ui/src/views/system/)、[岗位批删测试源码](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/SysPostBatchDeleteTest.java) |
| 后端基础维护 | 用户关系写入权限、客户端登录职责、角色授权与数据范围、数据源池生命周期等公共处理的修复和整理 | [系统插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/)、[数据插件](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/) |
| 公开发行准备 | 配置示例化、历史数据清理、演示身份、阅读入口、上手文档与独立公开历史 | [数据库说明](../forge-server/db/README.md)、[版本状态](release-status.md)、[NOTICE](../NOTICE) |

上述公共能力仍继续演进。未同步到公开仓库的维护改动，不在本表的当前版本承诺范围内。功能总览见[功能全景](features.md)。

## 与交付隔离台的关系

[ForgeAdminHarness-open-source](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source) 是独立的 AI 开发交付项目，负责对话工作台、隔离开发环境和交付过程。Foundation 提供可被开发、扩展和部署的应用基础代码。

这里的“AI 应用能力”指 Foundation 内部供业务系统使用的模型、对话、智能体等模块；Harness 的“AI 开发交付能力”指围绕代码和项目工作的外层平台。两者用途不同，使用时需要明确各自的服务、仓库版本、配置和访问权限。

## 许可与第三方归属

- 主工程保留上游 [Apache License 2.0](../LICENSE) 和源文件中的作者、版权及许可信息。
- [NOTICE](../NOTICE) 记录本下游版本与公开发行的修改背景。
- [forge-server/LICENSE](../forge-server/LICENSE) 保留后端工程许可。
- [forge-report-ui/LICENSE](../forge-report-ui/LICENSE) 保留 GoView 的 MIT 许可证及版权声明；报表工程不能被笼统描述为全部由本维护者原创或全部采用根许可证。
- 第三方库、图标、字体、图片和其他素材继续遵循各自许可。项目说明不替代相应许可正文。

贡献和再分发时请保留这些来源信息，并清晰说明自己的修改。关于上游项目本身，请访问[原项目](https://github.com/yaomindong1996/forge-admin)；关于本修改发行版，请在[本仓库](https://github.com/zhiyuxinxi/ForgeAdminFoundation)反馈。
