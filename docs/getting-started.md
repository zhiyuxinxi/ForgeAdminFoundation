# 本地开发指南

目标是帮助你找到最短的源码开发路径：先准备数据库和 Redis，配置管理服务与 PC 端，需要流程时增加流程服务，最后按需接入 H5、报表和外部集成。

**验证边界：下列路径、脚本参数、模块和默认端口已与本公开源码核对；本次没有执行依赖安装、构建、数据库初始化或首次登录。当前公开初始化存在客户端种子待补充核实等问题，详见[版本状态](release-status.md)。本页不承诺执行全部命令后即可无障碍登录。**

## 环境要求

| 工具/服务 | 要求和依据 |
| --- | --- |
| Git | 获取本仓库，并记录使用的提交号 |
| JDK | 17；见[后端 POM](../forge-server/pom.xml) |
| Maven | 使用 3.9 系列；已有容器构建文件使用该系列 |
| MySQL | 8；准备新建、独立的开发数据库以及可建表/迁移的开发账号 |
| Redis | 准备可访问的实例，设置自己的密码并核对数据库编号 |
| Node.js | PC 锁文件包含 Vite 8 和要求 Node 22/24 的依赖。建议使用 Node 24；三个前端需分别确认安装结果，不能依据旧 README 的 Node 18 说明安装 |
| pnpm | 根[package.json](../package.json)声明 11.7.0。各前端保留各自锁文件；请记录实际使用版本，安装时不要随意改写锁文件 |
| Bash 与 MySQL CLI | 数据库脚本需要 Bash、mysql、sed、find、sort；Windows 可在 WSL 或具备这些工具的 Bash 环境执行 |

可选依赖包括对象存储、模型服务、协同平台应用、邮件等。先完成基本开发链路，再按功能配置它们。

## 1. 获取代码

以下路径均相对于克隆后的仓库根目录。Shell 命令使用 Bash 语法；Windows PowerShell 对应复制操作可使用 `Copy-Item`。

```bash
git clone https://github.com/zhiyuxinxi/ForgeAdminFoundation.git
cd ForgeAdminFoundation
git rev-parse HEAD
java -version
mvn -version
node --version
pnpm --version
```

如果 pnpm 尚未安装，可按根文件声明安装对应版本，例如 `npm install -g pnpm@11.7.0`。这是工具准备步骤，不是项目安装已验证的证明。

## 2. 初始化独立开发数据库

先阅读[数据库初始化说明](../forge-server/db/README.md)。在仓库根目录执行现有脚本，替换账号和密码：

```bash
bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user YOUR_LOCAL_DB_USER \
  --password YOUR_LOCAL_DB_PASSWORD
```

只针对新建、专用数据库执行。初始化 SQL 包含建表和数据操作，不能用于覆盖已有业务库。不要将包含真实密码的命令放进截图、Issue 或共享日志。

脚本导入基础 SQL、必要种子和指定的公共增量；其余迁移由管理服务的 Flyway 配置处理。**不要再手工把整个 migration 目录逐个导入**，否则可能与迁移版本记录冲突。公开 SQL 不是生产数据库备份，演示数据和第三方连接信息需自行准备。

## 3. 配置后端

复制管理服务的示例：

```bash
cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-admin-server/src/main/resources/application-dev.yml
```

若要使用流程和工作台相关接口，再复制流程服务示例：

```bash
cp forge-server/forge-flow/forge-flow-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-flow/forge-flow-server/src/main/resources/application-dev.yml
```

在本地文件中填写：

| 配置 | 应如何对应 |
| --- | --- |
| `spring.datasource.dynamic.datasource.master` | MySQL 地址、库名、用户名、密码对应刚初始化的开发数据库 |
| `spring.data.redis` | Redis 地址、端口、编号和密码 |
| Redis 示例中的 `redisson.config.singleServerConfig` | Redisson连接块引用Spring Redis的主机、端口、编号和密码；保持这些引用，统一在Spring配置中设置自己的实例 |
| `config.datasource` | 示例引用主数据库变量；不要改成另一个未初始化的数据库 |
| 流程服务数据库 | 流程示例使用 `forge_flow`，与管理服务默认库名不同。最小共享库开发方案需将其改成同一个已初始化的 `forge_admin`，或自行完成分库配置与验证 |
| 模型、存储、邮件等 | 使用自己的服务和凭据，未使用的功能按实际配置保持关闭或不接入 |

开发配置可能生成本地密钥文件；请按配置注释保留和保护密钥，不能把同一份密钥当作公开示例提交。初次开发不需要为了启动页面启用 MCP、外部 OIDC 或高风险动作。

## 4. 构建和启动管理服务

在后端根目录构建所需服务及其依赖：

```bash
cd forge-server
mvn -Pdev -pl forge-admin-server,forge-flow/forge-flow-server -am package -DskipTests
```

该命令的 `-DskipTests` 只用于生成开发启动产物，不是测试通过的依据。若仅研究管理服务，可只选择 `forge-admin-server`；流程页面和工作台接口仍需要流程服务。

保持工作目录为 `forge-server`，分别在两个终端运行：

```bash
java -jar forge-admin-server/target/forge-admin-server.jar --spring.profiles.active=dev
```

```bash
java -jar forge-flow/forge-flow-server/target/forge-flow-server.jar --spring.profiles.active=dev
```

默认管理服务端口为 **8580**，流程服务为 **8081**。管理服务会按配置查找 `db/migration` 并执行 Flyway 迁移；从任意其他目录启动可能找不到迁移文件。先观察数据源、Redis、Flyway 和服务启动日志，不能只看端口占用就认定初始化成功。

## 5. 启动 PC 管理端

新开终端，从仓库根目录执行：

```bash
cd forge-admin-ui
pnpm install --frozen-lockfile
pnpm dev --host 127.0.0.1
```

默认访问 **http://127.0.0.1:3000/**，实际地址以 Vite 输出为准。[开发配置](../forge-admin-ui/.env.development)当前约定：

| 项目 | 默认值 |
| --- | --- |
| API 前缀 | `/dev-api` |
| 管理代理目标 | `http://127.0.0.1:8580/` |
| 流程代理目标 | `http://127.0.0.1:8081/` |
| PC 客户端编码 / AppId | `pc` / `pc` |

本地覆盖建议放入未提交的 `.env.development.local`。**前端 `VITE_*` 变量会进入浏览器产物，不应放入模型密钥、数据库密码或其他服务器秘密。**

公开 SQL 提供演示管理员 `admin` / `change-me-on-first-login`，并设置改密标记。登录还依赖启用的租户、客户端、角色和资源数据；演示账号本身不保证整个登录链路完整。当前基础 SQL 与必要种子中未见客户端初始化记录，先核对[已知限制](release-status.md)，不要通过关闭认证或跳过权限来解决登录问题。

## 6. 判断基础链路是否建立

按实际运行结果确认以下事项：

1. 后端日志显示服务启动完成，数据库、Redis 与迁移无未处理错误。
2. PC 登录页可加载，请求实际到达对应后端。
3. 客户端及租户初始化完整后，使用本地演示身份登录并修改密码。
4. 菜单与个人资料能正常读取；语言切换保持当前登录和业务原值。
5. 使用流程时，确认待办/流程接口到达 8081；未启动流程服务造成的该类错误，应与管理服务故障区分。

建议首次业务体验从一个不含真实数据的简单登记表开始：建立业务对象和表单，预览列表与详情，再增加审批流程。业务样例是否可直接配置完成，需以实际运行验证为准。

## 7. 按需增加其他客户端

| 场景 | 后端 | 前端和入口 |
| --- | --- | --- |
| H5 移动端 | `forge-app-server` 默认 8583；审批另需流程服务 | [H5 指南](../forge-h5-ui/README.md)，默认 3009 |
| 报表和可视化 | `forge-report-server` 默认 8581 | [报表指南](../forge-report-ui/README.md)，默认 3021、路径 `/forge-report` |
| AI 开发交付 | 单独安装和配置 Harness | [ForgeAdminHarness-open-source](https://github.com/zhiyuxinxi/ForgeAdminHarness-open-source) |

各服务都有自己的 `application-dev.example.yml`，应分别配置。管理服务数据库初始化不代表报表服务 SQL、SSO 客户端或第三方连接已经准备完毕。

## 常见问题

| 现象 | 首先检查 |
| --- | --- |
| 安装提示 Node 版本不满足 | 实际 `node --version` 与模块锁文件的 engines；旧 Node 18 说明不适用于当前 PC 工程 |
| pnpm frozen-lockfile 报不一致 | 确认所在模块和 pnpm 版本；保留错误并反馈，不以删除锁文件作为默认修复 |
| Redis 认证失败 | 实例密码、数据库编号、Spring Redis 配置与保留的 Redisson 配置是否一致 |
| 启动提示业务表不存在 | 初始化是否成功、连接库名是否一致、启动目录与 Flyway 路径是否正确 |
| 登录提示客户端认证失败 | 客户端种子、启用状态以及 `userClient/appId` 是否对应；见版本限制 |
| 页面请求 404 或代理拒绝连接 | 8580/8081/8583/8581 各自是否启动，代理是否指向正确服务 |
| 菜单为空或操作不可用 | 租户、角色、菜单/API 资源及数据范围配置，不以扩大权限作为默认处理 |
| AI、文件或通知不可用 | 对应第三方服务是否配置，不能把这些功能失败全部归为基础服务启动失败 |

首次使用容器前请阅读[容器示例状态](release-status.md#容器示例的适用边界)。反馈需包含公开提交号、模块、工具版本、步骤和脱敏错误信息，入口见[贡献指南](../CONTRIBUTING.md)。
