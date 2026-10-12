# 多租户隔离加固边界

> 来源：变更 harden-multi-tenant-isolation
> 时间：2026-06-27

## 问题描述

部分 `sys_`/`ai_` 业务表缺 `tenant_id` 或被误放进 SQL 拦截器忽略表；数据权限空范围会退化为全量查询；平台定义表不应按租户复制。

## 解决方案

### 两类表的区分
- **租户业务表**：必须含 `tenant_id`，且从拦截器 `ignoreTables` 移除，由 `TenantLineInnerInterceptor` 自动追加 `WHERE tenant_id = ?`。
- **平台定义表**（`sys_resource`、`ai_page_template`、区域码等）：保持全局，可见性由 `sys_role_resource.tenant_id` 授权控制，管理操作加超级管理员服务端兜底（`SessionHelper` 超管断言）。

### 数据权限必须“失败关闭”
- 无组织、无自定义范围、无行政区划时，拼接**恒假条件**，绝不退化为全量查询。

### 历史脏数据回填
- `tenant_id IS NULL OR = 0` 统一回填默认租户 `1`。

### 迁移踩坑
- **回填前必须前置去重**：同名字典（如 `sys_notice_status`/`sys_notice_type`）在 tenant 0 和 1 并存时，直接 `UPDATE tenant_id=1` 会撞唯一键 `uk_tenant_dict_type`。须先删除 tenant 0/null 中已在 tenant 1 存在的重复行，再做归一化。
- 对可能不存在的表（如 `sys_flow_node_operation`）用 `information_schema` 防护分支跳过。
- 租户内唯一索引改造为 `uk_xxx_tenant_key` 形态（含 model_key / business_key / category_code 等业务键 + tenant_id）。

## 相关文件

- 迁移脚本：`V1.0.56__harden_tenant_isolation_boundaries.sql`
- `SessionHelper`（超管断言）
- 各流程/文件/公告 Mapper XML
