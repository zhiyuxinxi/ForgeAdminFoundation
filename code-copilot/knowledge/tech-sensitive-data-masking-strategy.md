# 敏感数据脱敏与上传安全策略

> 来源：变更 security-sensitive-data-hardening
> 时间：2026-06-27

## 问题描述

多个接口暴露密码哈希/盐、默认明文密码、AK/SK、PII；敏感配置键与 Redis 敏感键明文返回；上传接口允许脚本/网页文件。

## 解决方案

### 脱敏占位回写防覆盖（核心模式）
- page/detail 返回敏感字段（configValue、accessKey/secretKey）显示 `******`。
- 保存时若提交值**等于脱敏占位**，则保留原值不覆盖真实凭据。避免前端回显脱敏值后保存把真实凭据冲掉。

### 认证字段彻底排除
- `SysUser.password / salt` 加 `@JsonIgnore`。
- 列表对 phone/idCard/email 脱敏；编辑详情只清认证字段，不脱敏可编辑字段。

### 敏感配置键统一判断
- 识别含 `password / secret / token / key / credential / ak / sk` 的键。
- page/list/detail/configKey 接口拒绝返回明文。

### 缓存键保护
- 定义敏感前缀（`crypto:session:*`、`Authorization:login:*`、`satoken:*`）。
- 列表过滤、详情/删除/清理拒绝，仅超管可访问。

### 强制改密闭环
- `sys_user.force_password_change`：新建/重置置 1，改密成功置 0。
- `ApiPermissionInterceptor` 在权限判断前拦截，强制改密用户只放行 changePassword / userInfo / logout / 密钥交换。

### 上传安全
- `FileManager` 统一校验扩展名黑名单（jsp/jspx/php/html/htm/js 等脚本与网页文件），阻断可执行/可渲染文件上传面。

## 相关文件

- `forge-starter-auth`：`ApiPermissionInterceptor`
- `forge-starter-file`：`FileManager`
- `SysUser` 实体、敏感配置键判断工具类、缓存键管理接口
