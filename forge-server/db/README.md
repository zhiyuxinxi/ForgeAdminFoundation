# 公开版本数据库初始化

`全量初始化SQL.sql` 保留数据库结构、字典、地区及菜单静态元数据，已删除历史业务、用户、会话、日志、模型调用及密钥配置数据。

在项目根目录使用既有初始化脚本：

```bash
bash forge-server/scripts/db/init-db.sh --host 127.0.0.1 --database forge_admin --user YOUR_DB_USER --password YOUR_DB_PASSWORD
```

只对新建、专用开发数据库执行。此脚本包含建表操作，不能用于覆盖现有生产数据库。

本地演示管理员为 `admin` / `change-me-on-first-login`，使用新生成的 BCrypt 哈希并要求修改密码。外部模型、文件存储、消息及业务数据源需重新配置。

容器示例 `docker-forge-admin/init-sql/01-init.sql` 使用同样的公开清理规则。未随此次源码发布执行真实 MySQL 初始化验收。
