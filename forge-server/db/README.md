# 公开版本数据库初始化

本目录包含基础 SQL、必要种子、可选示例和数据库迁移。公开文件已清理历史业务、用户、会话、调用日志、数据源连接和密钥配置；保留结构及公共字典、地区、菜单等静态元数据，并生成独立的本地演示身份。

完整启动顺序见[本地开发指南](../../docs/getting-started.md)，已知安装缺口见[公开版本状态](../../docs/release-status.md)。

## 新开发库初始化

只对新建、专用开发库执行。脚本包含建表与数据操作，不能用来覆盖已有业务或生产数据库。

准备 MySQL 8、MySQL CLI、Bash、sed、find、sort，然后在仓库根目录执行：

```bash
bash forge-server/scripts/db/init-db.sh \
  --host 127.0.0.1 \
  --port 3306 \
  --database forge_admin \
  --user YOUR_LOCAL_DB_USER \
  --password YOUR_LOCAL_DB_PASSWORD
```

账号需具备目标开发库的创建、建表及迁移所需权限。真实密码不能进入共享命令记录、Issue 或截图。Windows 用户需要在具备上述工具的 Bash/WSL 环境执行该脚本。

## 脚本实际处理什么

[init-db.sh](../scripts/db/init-db.sh)按以下顺序执行：

1. 创建调用者指定的数据库。
2. 导入[全量初始化SQL.sql](全量初始化SQL.sql)，过滤其中历史固定库名的 `USE proj_db;`，保留调用者指定的目标库。
3. 导入 [seed/required](seed/required/) 中的必要种子。
4. 仅当指定相应参数时，导入模块、演示或可选种子目录。
5. 执行脚本中明确列出的双语显示元数据、内置标签及 API 资源唯一性公共增量。

| 参数 | 含义 |
| --- | --- |
| `--with-demo` | 导入演示种子；不是恢复真实业务数据 |
| `--with-optional` | 导入存在的可选种子目录 |
| `--with-module` | 导入存在的模块 SQL 目录 |
| `--skip-admin-init` | 跳过基础全量 SQL，但仍执行必要种子及脚本明确的共享增量；不是通用数据库升级命令 |

不要仅凭参数名字就对已有数据库执行脚本；需先审阅实际会运行的 SQL。

## Flyway 与后续迁移

管理服务[application.yml](../forge-admin-server/src/main/resources/application.yml)默认启用 Flyway，迁移目录包含相对于启动位置的 `db/migration` 等路径，迁移记录表为 `forge_schema_history`。

从 `forge-server` 目录启动管理服务可以对应本文的迁移路径。初始化脚本没有逐个导入全部历史迁移，不能再额外将整个 migration 目录手工全量执行。已有业务库升级需要确认其基线、迁移记录和备份，不能把公开基础 SQL 当作升级脚本。

## 演示身份与首次登录

公开 SQL 提供新生成的本地演示管理员：

- 用户名：`admin`
- 密码：`change-me-on-first-login`
- 设置了强制改密标记；首次使用应修改密码。
- 此身份不对应维护者或任何客户环境的账号。

登录仍需要启用的租户、客户端和资源关系。当前基础 SQL 与必要种子中未见 `sys_client` 初始数据，而认证代码要求启用客户端；该首次登录缺口需要在公开版本的空库安装验证中明确补齐。不要误将演示身份存在解释为首次登录已验证。

模型、存储、消息、业务数据源与报表服务等需按各自配置重新准备。

## 容器与验证范围

[docker-forge-admin/init-sql/01-init.sql](../../docker-forge-admin/init-sql/01-init.sql)是容器示例的公开初始化文件。MySQL 容器初始化目录只在符合其首次初始化条件时处理，不等于每次启动都会重新导入。

首次公开和本轮文档整理没有执行真实 MySQL 初始化、Flyway 升级或登录验收。两套容器示例的差异见[版本状态](../../docs/release-status.md#容器示例的适用边界)。
