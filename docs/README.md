# 文档导航

本目录面向 ForgeAdminFoundation 公开版本的使用者和开发者。首次接触项目，从下面的阅读顺序开始。

## 按目的阅读

| 你现在要做什么 | 阅读入口 | 读完后应该明确什么 |
| --- | --- | --- |
| 判断项目是否适合自己 | [项目首页](../README.md)、[功能全景](features.md) | 业务能力、适用场景、需要自行接入的部分 |
| 分清两个项目的关系 | [项目关系](../README.md#两个开源项目如何配合) | Foundation 承载业务，Harness 组织 AI 开发交付；可以分开使用 |
| 系统了解配套 AI 开发能力 | [Harness AI 能力总览](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source/blob/main/docs/ai-capabilities.md) | Skill、执行工具、Prompt 与运行组件归 Harness，按公开快照确认可用范围；Foundation 业务 AI 见[功能全景](features.md)。总览入口在同批文档发布后可见 |
| 在本地开始开发 | [本地开发指南](getting-started.md)、[数据库初始化](../forge-server/db/README.md) | 工具版本、配置位置、启动顺序和成功判断 |
| 了解代码结构 | [架构与模块职责](architecture.md) | 服务边界、前后端路径、低代码模块依赖 |
| 查看来源和维护工作 | [来源与改造说明](upstream-and-changes.md) | 上游已有基础、具体加工内容、许可归属 |
| 判断当前版本能否直接使用 | [版本状态与已知限制](release-status.md) | 源码快照边界、安装待验证事项和公开更新方式 |
| 提交修复或反馈 | [贡献指南](../CONTRIBUTING.md) | 反馈位置、复现材料、PR 内容与许可保留 |

## 模块入口

- [PC 管理端](../forge-admin-ui/README.md)：开发服务、代理配置、组件文档入口。
- [H5 移动端](../forge-h5-ui/README.md)：移动服务与流程服务连接、H5 开发入口。
- [报表与可视化端](../forge-report-ui/README.md)：报表服务、端口、画布和许可。
- [数据库](../forge-server/db/README.md)：公开初始化脚本、Flyway 迁移与演示身份边界。
- [前端文档导航](../forge-admin-ui/docs/README.md)：主题、工具、字典、附件、标签页与设计规范。

## 按需查阅的开发专题

| 主题 | 文档 |
| --- | --- |
| 低代码前端结构 | [低代码 CRUD 前端架构全景报告](低代码%20CRUD%20前端架构全景报告.md) |
| API 权限 | [API 权限控制使用说明](../forge-server/forge-admin-server/src/main/resources/sql/API权限控制使用说明.md) |
| 数据权限 | [数据范围配置](../forge-server/forge-framework/forge-starter-parent/forge-starter-datascope/DATA_SCOPE_CONFIG_GUIDE.md)、[数据集权限](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-data/README_DATASET_PERMISSION.md) |
| 多租户 | [多租户使用说明](../forge-server/forge-framework/forge-starter-parent/forge-starter-tenant/TENANT_USAGE.md) |
| 文件存储 | [RustFS/S3 存储说明](../forge-server/forge-framework/forge-starter-parent/forge-starter-file/RUSTFS_README.md) |
| 流程表达式 | [SpEL 使用说明](../forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/SPEL_GUIDE.md) |
| API 配置与 Excel | [API 配置](../forge-server/forge-framework/forge-starter-parent/forge-starter-api-config/API_CONFIG_USAGE.md)、[Excel 能力](../forge-server/forge-framework/forge-starter-parent/forge-starter-excel/README_ENHANCED.md) |
| 异常与配置 | [异常处理](../forge-server/forge-framework/forge-starter-parent/forge-starter-core/EXCEPTION_USAGE.md)、[动态配置](../forge-server/forge-framework/forge-starter-parent/forge-starter-config/USAGE_EXAMPLE.md) |
| 工程质量 | [质量工具说明](../tools/quality/README.md) |

专题文档保留其技术背景，示例对象、表名和配置需与当前模块源码对照。首次安装以上手指南和版本限制为入口；专题材料不等同于当前版本已完成全部场景验证。

## 文档维护方式

功能变更同步更新功能说明；服务、端口、配置或初始化方式改变时同步更新上手指南；公开源码同步时更新版本记录与已知问题。公开文档使用仓库相对链接，不引用维护者的本机目录、内部运行记录或未公开文件。开发辅助指令和 Skills 单独维护，不用产品介绍替代其执行规则。
