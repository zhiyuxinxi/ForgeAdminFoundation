# Redisson 与 Spring Data Redis 版本兼容

> 来源：变更 fix-redisson-spring-data-35
> 时间：2026-06-27

## 问题描述

Spring Boot 3.5.x（Spring Data Redis 3.5）环境下，`/auth/login` 登录报 `StackOverflowError`。

根因：Spring Data Redis 3.5 的 `RedisKeyCommands` 新增了 `pExpire(byte[], Expiration, ExpirationOptions)` 签名。旧版 Redisson（3.34.1）依赖的 `redisson-spring-data-33` 适配层没有实现该新签名，导致接口的 default 方法与 Redisson 旧方法之间互相递归调用，最终栈溢出。

## 解决方案

- Redisson 的 Spring Data 适配是**按 Spring Data Redis 主版本分模块**的：`redisson-spring-data-33`（3.3.x）、`redisson-spring-data-35`（3.5.x）等。Redisson 主版本必须与 Spring Data Redis 主版本对齐。
- 修复只需升级版本管理：`redisson.version` 从 `3.34.1` → `3.50.0`，`redisson-spring-boot-starter` 即解析到 `redisson-spring-data-35`，与 Spring Data Redis 3.5.x 签名匹配。
- **两处 POM 必须同步**：`forge-server/pom.xml` 与 `forge-server/forge-framework/forge-dependencies/pom.xml`。
- 验证手段（无需启动服务）：
  ```bash
  mvn -pl forge-framework/forge-starter-parent/forge-starter-cache -am \
    dependency:tree -Dincludes=org.redisson -DskipTests
  ```
  确认依赖树解析到 `redisson-spring-data-35` 而非 `-33`。
- 回退方式：还原两个 POM 的 `redisson.version`，或换用其他包含 `redisson-spring-data-35` 的 Redisson 版本。

## 相关文件

- `forge-server/pom.xml`
- `forge-server/forge-framework/forge-dependencies/pom.xml`
- 踩坑记录已追加到 `code-copilot/memory/pitfalls.md`
