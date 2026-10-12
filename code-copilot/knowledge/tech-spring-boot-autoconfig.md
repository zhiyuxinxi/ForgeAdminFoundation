# Spring Boot 3 自动配置设计：少写配置，多做组合

> 目标：讲清楚 Spring Boot 3 自动配置的工作机制，以及 Forge Admin 18 个 Starter 如何通过组合实现"引入即生效"。

---

## 1. 为什么自动配置对后台框架重要

企业后台框架的核心痛点是**模块多了配置就乱**。一个 Starter 要能用的前提是：
- 用户加一个 `pom.xml` 依赖，不需要手写任何 Java Config
- 默认配置覆盖 80% 场景，剩下 20% 通过 `application.yml` 调整
- 用户自己的 Bean 可以覆盖框架默认实现

Spring Boot 的自动配置就是这套"少写配置，多做组合"的工程范式。

Forge Admin 有 21 个 Starter 和 6 个 Plugin，如果每个都要用户手动写 `@Configuration` 和 `@Bean`，光是配置代码就得上千行。自动配置把这个成本压到了零。

---

## 2. 自动配置的核心机制

### 2.1 注册入口：`AutoConfiguration.imports`

Spring Boot 3.x 使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册自动配置类（取代老版本 `spring.factories`）：

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.mdframe.forge.starter.core.config.ExceptionAutoConfiguration
com.mdframe.forge.starter.core.config.JacksonConfig
```

Spring Boot 启动时会扫描 classpath 下所有 jar 中的这个文件，加载并执行其中声明的配置类。

### 2.2 装配流程

```
Spring Boot 启动
  → 扫描所有 jar 的 AutoConfiguration.imports 文件
    → 加载声明的 @AutoConfiguration / @Configuration 类
      → 按 @AutoConfigureOrder 排序
        → 逐个评估 @Conditional 条件注解
          → 条件满足 → 执行 @Bean 方法注册
          → 条件不满足 → 跳过该类
```

### 2.3 与旧版 `spring.factories` 的对比

| | `AutoConfiguration.imports` (3.x) | `spring.factories` (2.x) |
|---|---|---|
| 文件路径 | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `META-INF/spring.factories` |
| 每行一个类 | 是，一行一个全限定类名 | 否，key=value 格式 |
| 注解要求 | 必须用 `@AutoConfiguration` | `@Configuration` 即可 |
| 专用性 | 仅用于自动配置 | 可注册各种工厂类 |

**注意**：Forge Admin 同时保留了一个 `spring.factories`，位于 `forge-starter-config`，用于注册 `EnvironmentPostProcessor`（这不是自动配置类，所以不能用 `AutoConfiguration.imports`）：

```
org.springframework.boot.env.EnvironmentPostProcessor=\
com.mdframe.forge.starter.property.DbPropertySourcePostProcessor
```

---

## 3. 条件装配注解体系

Spring Boot 提供了一系列 `@Conditional` 注解，Forge Admin 中使用的按频率排序：

### 3.1 使用频率统计

| 注解 | 用途 | 项目使用次数 |
|------|------|------------|
| `@ConditionalOnMissingBean` | 用户可自定义 Bean 覆盖默认实现 | 24+ |
| `@ConditionalOnProperty` | 通过 yml 配置控制功能开关 | 12 |
| `@ConditionalOnBean` | 依赖其他 Bean 存在才生效 | 6 |
| `@ConditionalOnClass` | 依赖某类在 classpath 才生效 | 2 |
| `@ConditionalOnWebApplication` | 仅在 Web 环境中生效 | 1 |

### 3.2 核心注解详解

#### `@ConditionalOnMissingBean` — 允许用户覆盖

最常见模式：框架提供一个默认实现，但用户可以通过声明自己的 Bean 来覆盖。

```java
// forge-starter-tenant / TenantAutoConfiguration
@Bean
@ConditionalOnMissingBean
public DefaultTenantLineHandler tenantLineHandler(
        @Autowired(required = false) TenantTableChecker tenantTableChecker) {
    return new DefaultTenantLineHandler(tenantTableChecker);
}
```

如果用户在自己项目里也声明了 `@Bean public TenantLineHandler myHandler()`，框架的这个 Bean 就不会创建。

#### `@ConditionalOnProperty` — 功能开关

通过 yml 配置控制整个模块或子功能：

```java
// 类级：控制整个模块
@AutoConfiguration
@ConditionalOnProperty(prefix = "forge.tenant", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class TenantAutoConfiguration { ... }

// 方法级：控制子功能
@Bean
@ConditionalOnProperty(prefix = "forge.tenant",
    name = "auto-detect-tenant-column", havingValue = "true", matchIfMissing = true)
public TenantTableChecker tenantTableChecker(DataSource dataSource) { ... }
```

**关键约定**：所有 Forge Starter 的 `enabled` 开关都设置 `matchIfMissing = true`，意味着不配置默认启用，做到真正的"引入即生效"。

#### `@ConditionalOnBean` — 级联依赖

典型场景在 `forge-starter-crypto`，防重放攻击过滤器依赖于 Redis 缓存服务：

```java
// 第一层：Redis 缓存存在才创建 Token 缓存
@Bean
@ConditionalOnBean(ICacheService.class)
public ReplayTokenCache replayTokenCache(ICacheService cacheService) { ... }

// 第二层：Token 缓存存在 + 配置开关打开，才注册过滤器
@Bean
@ConditionalOnBean(ReplayTokenCache.class)
@ConditionalOnProperty(prefix = "forge.crypto", name = "enableReplayProtection", havingValue = "true")
public FilterRegistrationBean<ReplayAttackFilter> replayAttackFilter(
        ReplayTokenCache replayTokenCache) { ... }
```

#### `@ConditionalOnClass` — 可选依赖

`forge-starter-idempotent` 中，只有 classpath 存在 Redisson 时才启用分布式锁：

```java
@Bean
@ConditionalOnMissingBean
@ConditionalOnClass(RedissonClient.class)
public LockManager lockManager(RedissonClient redissonClient) {
    return new LockManager(redissonClient);
}
```

---

## 4. Forge Admin 的 Starter 装配模式

### 4.1 最简模式：纯 Bean 注册

`forge-starter-core` 的异常处理器，无任何条件判断：

```java
@Slf4j
@AutoConfiguration
public class ExceptionAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        log.info("全局异常处理器已启用");
        return new GlobalExceptionHandler();
    }
}
```

### 4.2 标准模式：条件开关 + 用户可覆盖

`forge-starter-tenant` 的完整模式：

```
@AutoConfiguration                     ← 标记为自动配置类
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)  ← 控制顺序
@EnableConfigurationProperties(TenantProperties.class) ← 属性绑定
@ConditionalOnProperty(prefix = "forge.tenant", ...)   ← 条件开关
public class TenantAutoConfiguration {

    @Bean @ConditionalOnMissingBean   ← 用户可覆盖
    public TenantLineHandler handler() { ... }

    @Bean @ConditionalOnMissingBean(name = "xxx")  ← 指定 bean name 匹配
    public TenantLineInnerInterceptor interceptor() { ... }
}
```

### 4.3 拦截器协作模式

`forge-starter-orm` 的 `MybatisPlusConfig` 是整个框架**最关键的设计点**：

```java
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig {

    @Autowired(required = false)
    private List<InnerInterceptor> innerInterceptors;  // 自动收集所有拦截器

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 先注册其他 Starter 提供的拦截器（租户、数据权限等）
        if (innerInterceptors != null && !innerInterceptors.isEmpty()) {
            innerInterceptors.forEach(interceptor::addInnerInterceptor);
        }
        // 2. 再注册分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        // 3. 最后注册乐观锁
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

> 这是"组合优于配置"的典范：ORM 模块**不需要知道**有哪些拦截器，只需声明 `List<InnerInterceptor>`，其他 Starter 注册的拦截器会自动注入进来。新增一个拦截器模块无需改动 ORM 的任何代码。

### 4.4 策略工厂模式

`forge-starter-idempotent` 把多种幂等策略注册到一个 Map，由 AOP 切面选择：

```java
@Bean
public Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers(
        IdempotentStrategyHandler strictStrategyHandler,
        IdempotentStrategyHandler returnCacheStrategyHandler,
        IdempotentStrategyHandler tokenRequiredStrategyHandler) {
    Map<IdempotentStrategy, IdempotentStrategyHandler> handlers = new HashMap<>();
    handlers.put(IdempotentStrategy.STRICT, strictStrategyHandler);
    handlers.put(IdempotentStrategy.RETURN_CACHE, returnCacheStrategyHandler);
    handlers.put(IdempotentStrategy.TOKEN_REQUIRED, tokenRequiredStrategyHandler);
    return handlers;
}
```

### 4.5 纯 POM 聚合模式

`forge-starter-web` 是整个框架最特殊的"Starter"——它没有任何 Java 代码，仅靠 POM 聚合依赖：

```xml
<dependencies>
    <dependency>org.springframework.boot:spring-boot-starter-web</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-undertow</dependency>
    <dependency>org.springframework.boot:spring-boot-starter-actuator</dependency>
</dependencies>
```

引入 `forge-starter-web`，自动获得 Undertow + Actuator + 全局异常处理（来自 `forge-starter-core`），用户零配置。

---

## 5. Forge Admin 的注册方式全景

```
┌─────────────────────────────────────────────────────┐
│          AutoConfiguration.imports 注册 (18 个模块)   │
│  ┌─ @AutoConfiguration                              │
│  │  ├─ @ConditionalOnProperty → 开关控制             │
│  │  ├─ @ConditionalOnMissingBean → 用户可覆盖        │
│  │  └─ @AutoConfigureOrder → 排序控制                │
│  └─ @Configuration (auth 模块)                       │
├─────────────────────────────────────────────────────┤
│          @ComponentScan 自动发现 (log 模块)           │
├─────────────────────────────────────────────────────┤
│          纯 POM 聚合 (web 模块)                       │
├─────────────────────────────────────────────────────┤
│          spring.factories (config 模块)               │
│          └─ EnvironmentPostProcessor                 │
└─────────────────────────────────────────────────────┘
```

### 所有 Starter 的 imports 注册清单

| Starter | 注册类 | 装配方式 |
|---------|--------|---------|
| forge-starter-core | `ExceptionAutoConfiguration`, `JacksonConfig` | `@AutoConfiguration` |
| forge-starter-orm | `MybatisPlusConfig` | `@Configuration` + `@MapperScan` |
| forge-starter-cache | `RedissonConfig` | `@AutoConfiguration` |
| forge-starter-tenant | `TenantAutoConfiguration` | `@AutoConfiguration` + `@ConditionalOnProperty` |
| forge-starter-datascope | `DataScopeAutoConfiguration` | `@AutoConfiguration` + `@ConditionalOnProperty` |
| forge-starter-crypto | `CryptoAutoConfiguration`, `JacksonCryptoConfiguration` | `@AutoConfiguration` |
| forge-starter-idempotent | `IdempotentAutoConfiguration` | `@AutoConfiguration` + `@ConditionalOnProperty` |
| forge-starter-id | `IdAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-excel | `ExcelAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-file | `FileAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-message | `MessageAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-social | `SocialAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-trans | `TransAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-websocket | `WebSocketConfig` | `@Configuration` |
| forge-starter-api-config | `ApiConfigAutoConfiguration` | `@AutoConfiguration` |
| forge-starter-config | `PropertyRefreshAutoConfiguration`, `ConfigAutoConfiguration` | `@AutoConfiguration` + `spring.factories` |
| forge-starter-auth | `SaTokenConfig` | `@Configuration` + `@ComponentScan` |
| forge-starter-web | (无 Java 代码) | 纯 POM 聚合 |

---

## 6. 设计原则总结

### 6.1 三不原则

1. **不需要手写 `@Configuration`** — 引入 Maven 依赖即生效
2. **不需要手动注册拦截器** — ORM 通过 `List<InnerInterceptor>` 自动收集
3. **不需要关心模块顺序** — `@AutoConfigureOrder` 在框架层排好了

### 6.2 约定优于配置

| 约定 | 体现 |
|------|------|
| 默认启用 | 所有 `forge.*.enabled` 默认 `matchIfMissing = true` |
| 统一属性前缀 | 所有配置以 `forge.` 开头，前缀 = 模块名 |
| 统一命名规范 | `forge-starter-xxx` → 配置前缀 `forge.xxx` |
| 用户覆盖优先 | 所有核心 Bean 加 `@ConditionalOnMissingBean` |

### 6.3 组合优于继承

- **ORM 组合拦截器**：`forge-starter-orm` 通过 `List<InnerInterceptor>` 组合租户、数据权限、分页、乐观锁
- **Web 组合能力**：`forge-starter-web` 通过 POM 聚合 Undertow、Actuator、Jackson 等
- **幂等组合策略**：`forge-starter-idempotent` 通过 Map 组合多种策略处理器

---

## 7. 二开指南：新增一个 Starter

如果要新增一个 `forge-starter-notification`，标准步骤：

1. **创建模块**：`forge-framework/forge-starter-parent/forge-starter-notification/`
2. **定义配置属性**：
```java
@ConfigurationProperties(prefix = "forge.notification")
@Data
public class NotificationProperties {
    private boolean enabled = true;
    private String channel = "sms";
}
```
3. **编写自动配置类**：
```java
@AutoConfiguration
@EnableConfigurationProperties(NotificationProperties.class)
@ConditionalOnProperty(prefix = "forge.notification", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationService notificationService(NotificationProperties props) {
        return new DefaultNotificationService(props);
    }
}
```
4. **注册到 imports 文件**：在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中添加一行：
```
com.mdframe.forge.starter.notification.config.NotificationAutoConfiguration
```
5. **在父 POM 中声明模块**：`forge-starter-parent/pom.xml` 的 `<modules>` 添加。

---

## 8. 常见踩坑

| 问题 | 原因 | 解决 |
|------|------|------|
| 自动配置类不生效 | 没写 `AutoConfiguration.imports` 文件，或文件路径错误 | 检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| `@ConditionalOnProperty` 不工作 | 属性名拼写错误或大小写不一致 | kebab-case 转 camelCase 规则：`enable-replay-protection` → `enableReplayProtection` |
| `@Autowired(required = false)` 注入为 null | 被注入的 Bean 没有被其他模块注册 | 确认依赖的 Starter 已引入，且其自动配置已生效 |
| 两个拦截器顺序不对 | `@AutoConfigureOrder` 值设置不当 | 提高 order 值使该配置延迟加载 |
| 用户自定义 Bean 没覆盖默认 | 忘了加 `@ConditionalOnMissingBean` | 在默认 Bean 方法上加该注解 |
| `spring.factories` 在新版本不生效 | Spring Boot 3.x 已弃用 | 迁移到 `AutoConfiguration.imports` |

---

## 9. 相关文件索引

| 文件 | 说明 |
|------|------|
| `forge-framework/forge-starter-parent/*/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 各 Starter 的自动配置注册文件 |
| `forge-framework/forge-starter-parent/forge-starter-orm/src/main/java/com/mdframe/forge/starter/orm/config/MybatisPlusConfig.java` | 拦截器组合的关键实现 |
| `forge-framework/forge-starter-parent/forge-starter-tenant/src/main/java/com/mdframe/forge/starter/tenant/config/TenantAutoConfiguration.java` | 条件装配的标准模板 |
| `forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoAutoConfiguration.java` | 级联条件装配示例 |
| `forge-framework/forge-starter-parent/forge-starter-idempotent/src/main/java/com/mdframe/forge/starter/idempotent/config/IdempotentAutoConfiguration.java` | 策略模式装配示例 |

---

## 体验 Forge Admin

- 在线演示：后台管理 / 大屏设计器
- 默认账号：admin / 123456
- Gitee：ForgeLab/forge-admin
- GitHub：yaomindong1996/forge-admin
- 文档站：Forge Admin Docs
