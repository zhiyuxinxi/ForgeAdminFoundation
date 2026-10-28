# Forge Admin Foundation · H5 移动端

本目录是 [ForgeAdminFoundation](../README.md) 的 uni-app 移动工程。本文以浏览器中的 H5 开发为入口；package.json 中存在其他平台命令，不代表这些平台已经完成安装或设备验收。

## 开发前提

先阅读[本地开发指南](../docs/getting-started.md)。移动端普通接口默认连接 `forge-app-server`（8583），审批相关接口连接 `forge-flow-server`（8081），不能仅启动 PC 管理服务就认定移动接口完整。

复制并填写 `forge-server/forge-app-server/src/main/resources/application-dev.example.yml` 对应的本地开发配置，完成该服务所需数据库、Redis 和客户端身份准备。编译入口模块为 `forge-app-server`；服务启动与配置方式参考总指南。

## 启动 H5

在仓库根目录执行：

```bash
cd forge-h5-ui
pnpm install --frozen-lockfile
pnpm dev:h5 --host 127.0.0.1
```

默认访问 **http://127.0.0.1:3009/**。当前模块 Vite 为 5.2.8，依赖与 PC 管理端不同，保留本模块锁文件。上面命令未在本轮文档整理中执行。

[.env.development](.env.development)包含：

| 配置 | 当前值 |
| --- | --- |
| `VITE_HTTP_PORT` | `3009` |
| `VITE_HTTP_PROXY_TARGET` | `http://127.0.0.1:8583/` |
| `VITE_FLOW_PROXY_TARGET` | `http://127.0.0.1:8081/` |
| `VITE_USER_CLIENT` / `VITE_APP_ID` | `h5` / `forge_h5` |

客户端编码、AppId、启用状态与租户必须和后端准备的数据对应。开发服务器的默认配置监听所有网卡，个人本机开发可使用上面的显式回环地址；真机联调时再按自己的网络配置访问。

## 开发内容与边界

- 页面入口和路由配置位于 [src](src/)。
- 语言处理位于 [src/locales](src/locales/)。
- 低代码运行与审批接口需要相应的业务对象、权限和流程配置。
- `pnpm build:h5` 生成 H5 产物，部署路径需与服务端代理及登录回调配置一致。
- 第三方免登、相机等设备权限和其他小程序目标需单独联调，不能由 H5 编译通过推断它们全部可用。

版本限制与反馈方式见[公开版本状态](../docs/release-status.md)和[贡献指南](../CONTRIBUTING.md)。
