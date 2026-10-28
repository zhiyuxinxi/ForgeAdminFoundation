# Forge Admin Foundation · 报表与可视化设计器

本目录是 [ForgeAdminFoundation](../README.md) 的报表/可视化前端，包含项目、画布、图表、主题、数据接入和 AI 辅助相关界面。工程保留 GoView 的 [MIT 许可证](LICENSE)与版权声明；本发行版的来源与加工说明见[上游来源](../docs/upstream-and-changes.md)。

## 使用前提

该前端连接独立的 `forge-report-server`，默认端口 **8581**。需要配置该服务的数据库、Redis、登录身份及按需启用的模型服务。相关配置在 `forge-server/forge-report-server/src/main/resources/application-dev.example.yml`，SQL 位于该服务的 [sql 目录](../forge-server/forge-report-server/sql/)。

管理服务初始化完成不能自动证明报表库和 SSO 已准备完成。首次部署应审阅对应 SQL 与服务配置，不要把不同数据库的初始化脚本混合导入生产库。

## 启动开发服务

在仓库根目录执行：

```bash
cd forge-report-ui
pnpm install --frozen-lockfile
pnpm dev --host 127.0.0.1
```

默认访问 **http://127.0.0.1:3021/forge-report**。命令未在本次文档整理中实际执行。

| 配置/版本 | 当前源码依据 |
| --- | --- |
| 开发端口 | [vite.config.ts](vite.config.ts)中的 `3021` |
| 站点基础路径 | `/forge-report` |
| API 代理 | `/forge-report-api` → `http://localhost:8581` |
| Vite | [package.json](package.json)当前为 `4.3.6` |
| TypeScript | 当前声明 `4.6.3` |
| 包管理器 | 参考根工程声明，保留本模块 [pnpm-lock.yaml](pnpm-lock.yaml) |

当前公开目录没有旧 README 所说的 `.env.example`；开发代理地址直接配置在 Vite 配置中。不要照旧说明复制不存在的模板，也不要沿用旧演示密码。登录身份、客户端及单点登录目标需按实际初始化结果配置。

## 功能与代码入口

| 目录 | 内容 |
| --- | --- |
| [src/views](src/views/) | 项目、画布、预览和相关界面 |
| [src/packages](src/packages/) | 图表与组件定义 |
| [src/components](src/components/) | 公共组件 |
| [src/api](src/api/) | 服务调用 |
| [src/store](src/store/) | 状态管理 |

配置画布时可使用组件、主题、数据和事件等能力；AI 相关功能需要自行配置模型供应商。是否可以成功保存、预览和访问具体项目，要同时验证报表服务、权限和真实数据连接。

`pnpm build` 用于生成静态资源；`build:stage` 会进一步调用部署脚本，首次本地开发无需使用。开发服务、构建与实际发布访问是不同阶段。

## 版本与反馈

本轮没有执行安装、登录、画布保存或发布验收，也没有将原有截图作为当前公开版本的验收依据。已知限制见[版本状态](../docs/release-status.md)，问题提交到 [Foundation Issues](https://github.com/zhiyuxinxi/ForgeAdminFoundation/issues)。
