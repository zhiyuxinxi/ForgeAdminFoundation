# Forge Admin Foundation · PC 管理端

本目录是 [ForgeAdminFoundation](../README.md) 的 PC 管理端，基于 Vue 3、Naive UI、Pinia 与 Vite。它提供系统管理、应用设计、低代码运行界面、流程与 AI 等模块入口，需连接对应后端服务使用。

上游来源与本发行版加工内容见[来源与改造说明](../docs/upstream-and-changes.md)。本目录旧说明中的 Fusion AI Design 名称和示例仓库地址不再作为本项目入口。

## 开始开发

先完成[本地开发指南](../docs/getting-started.md)中的工具、数据库和后端配置。在仓库根目录进入本模块：

```bash
cd forge-admin-ui
pnpm install --frozen-lockfile
pnpm dev --host 127.0.0.1
```

默认访问 **http://127.0.0.1:3000/**。当前模块使用 Vite 8，Node 18 不适用于该依赖组合；版本选择见[环境要求](../docs/getting-started.md#环境要求)。安装命令没有在本轮文档整理中实际执行。

## 配置与后端

[.env.development](.env.development)定义开发配置；本地覆盖可使用未提交的 `.env.development.local`。

| 配置 | 用途 | 默认值 |
| --- | --- | --- |
| `VITE_HTTP_PORT` | 开发服务器端口 | `3000` |
| `VITE_REQUEST_PREFIX` | 前端接口前缀 | `/dev-api` |
| `VITE_HTTP_PROXY_TARGET` | 管理服务 | `http://127.0.0.1:8580/` |
| `VITE_FLOW_PROXY_TARGET` | 流程和工作台相关接口 | `http://127.0.0.1:8081/` |
| `VITE_APP_ID` / `VITE_USER_CLIENT` | 登录客户端标识 | `pc` / `pc` |
| `VITE_H5_BASE_URL` | H5 入口 | `http://localhost:3009` |

代理规则见[vite.config.js](vite.config.js)。报表入口还有单独的 SSO/目标地址配置，需要对应服务和客户端身份，不能只修改网址就认定登录已打通。

`VITE_*` 值会进入浏览器产物，不要放入服务器密码或模型密钥。公开演示账号及客户端种子待核事项统一见[数据库说明](../forge-server/db/README.md)和[版本状态](../docs/release-status.md)。

## 开发与构建命令

| 命令 | 用途 |
| --- | --- |
| `pnpm dev` | 开发服务 |
| `pnpm build` | 构建静态资源；输出位置由 Vite 配置及环境变量决定 |
| `pnpm preview` | 预览已经构建的产物 |
| `pnpm test` | 执行现有前端测试；可按实际改动选用相关测试文件 |

`build:stage` 会进一步调用部署脚本，首次本地开发无需使用；配置中的部署目标不是公共托管服务。这里的命令说明不表示它们已经在当前机器运行成功。

## 阅读源码

| 路径 | 内容 |
| --- | --- |
| [src/views](src/views/) | 业务与管理页面 |
| [src/views/app-center](src/views/app-center/) | 应用设计及业务运行入口 |
| [src/components/ai-form](src/components/ai-form/) | 表单、表格、CRUD、详情等公共组件 |
| [src/components/lowcode-builder](src/components/lowcode-builder/) | 低代码设计/运行相关组件 |
| [src/locales](src/locales/) | 公共语言资源与显示处理 |

组件、主题、字典、附件和标签页开发请看[前端文档导航](docs/README.md)。相关缺陷反馈到[本仓库 Issues](https://github.com/zhiyuxinxi/ForgeAdminFoundation/issues)，贡献方式见[贡献指南](../CONTRIBUTING.md)。
