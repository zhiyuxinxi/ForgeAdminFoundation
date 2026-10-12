# Forge Admin Foundation

由 [zhiyuxinxi](https://github.com/zhiyuxinxi) 维护的 Forge Admin 修改版和公共低代码底座。基于 [yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin)，保留上游许可与署名。

配套 AI 开发交付平台：[ForgeAdminHarness-open-source](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source)。

## 项目内容

- Vue 3 PC 管理端、uni-app H5 移动端、报表设计器。
- Java 17 / Spring Boot 服务、权限与租户基础能力。
- 低代码表单、页面、业务对象、代码生成和 Flowable 流程集成。
- 中英双语显示资源、数据库迁移及公共开发规范。
- 公共底座维护中形成的修复与扩展。

## 目录

| 目录 | 内容 |
| --- | --- |
| `forge-server/` | Maven 后端工程、数据库结构及迁移 |
| `forge-admin-ui/` | PC 管理端 |
| `forge-h5-ui/` | H5 移动端 |
| `forge-report-ui/` | 报表与可视化设计器 |
| `docker/`、`docker-forge-admin/` | 容器配置与构建文件 |
| `code-copilot/rules/`、`.agents/skills/` | 可复用开发规范与 Skills |

## 本地开发

准备 JDK 17、Maven、MySQL 8、Redis、Node.js 和 pnpm。前端包管理器版本以对应 `package.json` 及锁文件为准。

1. 将各服务的 `application-dev.example.yml` 复制为本地 `application-dev.yml`，填写自己的数据库、Redis 等连接信息。
2. 按 [数据库初始化说明](forge-server/db/README.md) 在独立开发数据库初始化。
3. 在 `forge-server/` 中使用 Maven 构建后端，并按服务配置启动需要的模块。
4. 在 `forge-admin-ui/` 中安装依赖后运行 `pnpm dev`。H5 使用 `pnpm dev:h5`；报表端使用 `pnpm dev`。

模型服务、文件存储、邮件及其他外部集成需要自行配置。公开示例不包含可使用的生产凭据。私有镜像、服务器和部署状态不随源码提供。

## 数据与公开版本

本仓库使用清理后的源码快照和独立 Git 历史。原有用户、会话、调用日志、业务记录、数据源连接及密钥配置均不进入公开数据库初始化文件；保留表结构和必要的公共字典、菜单等静态元数据。

仅提供新生成的本地演示管理员 `admin` / `change-me-on-first-login`，首次使用必须修改密码。演示账号不对应任何私有环境账号。禁止直接使用演示凭据对外提供服务。

本次发布进行了源码公开范围及敏感信息检查，没有执行完整构建、数据库运行验收或全部业务测试。这些运行验证需要在使用者自己的环境完成。

## 许可证

沿用上游 [Apache License 2.0](LICENSE)，保留源文件中的作者及版权声明。修改说明与第三方归属见 [NOTICE](NOTICE)。本仓库是独立维护的修改版，不代表上游官方发行版。
