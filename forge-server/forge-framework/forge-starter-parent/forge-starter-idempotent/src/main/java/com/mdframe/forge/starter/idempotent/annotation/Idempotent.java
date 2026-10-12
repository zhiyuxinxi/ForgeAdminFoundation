package com.mdframe.forge.starter.idempotent.annotation;

import com.mdframe.forge.starter.idempotent.constant.IdempotentConstant;
import com.mdframe.forge.starter.idempotent.enums.IdempotentStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解
 *
 * <p>用于标记需要幂等控制的方法或类。支持多种幂等策略，可根据业务场景选择合适的策略。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * // 基础用法：返回缓存结果
 * {@code @Idempotent(key = "'order:' + #orderId")}
 * public Order createOrder(Long orderId) { ... }
 *
 * // 严格模式：直接拒绝重复请求
 * {@code @Idempotent(key = "'payment:' + #paymentId", strategy = IdempotentStrategy.STRICT)}
 * public PaymentResult pay(PaymentRequest request) { ... }
 *
 * // Token模式：需要先获取Token
 * {@code @Idempotent(key = "'transfer:' + #requestId", strategy = IdempotentStrategy.TOKEN_REQUIRED)}
 * public TransferResult transfer(TransferRequest request) { ... }
 * </pre>
 *
 * @author Forge Framework
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等键前缀
     * <p>用于区分不同业务场景的幂等键，避免键冲突</p>
     *
     * @return 幂等键前缀，默认为 "idempotent:"
     */
    String prefix() default IdempotentConstant.DEFAULT_PREFIX;

    /**
     * 幂等键过期时间（秒）
     * <p>STRICT 在成功完成后开始计时；执行期间使用自动续租的锁，不使用此值作为执行租约。</p>
     * <p>STRICT completion TTL starts after success; it is separate from the renewable execution lock.</p>
     *
     * @return 过期时间，默认为 600秒（10分钟）
     */
    int expire() default IdempotentConstant.DEFAULT_EXPIRE;

    /**
     * 幂等键表达式
     * <p>为空时，默认键绑定可信用户、租户、完整方法签名和参数；无可信身份时拒绝。</p>
     * <p>显式 SpEL 保持 prefix + 表达式结果契约，不自动追加身份或方法；调用方必须明确所需隔离范围。</p>
     * <p>Default keys require a trusted user and tenant. Explicit SpEL retains its exact prefix/value contract;
     * callers own its identity and operation scope.</p>
     *
     * @return SpEL表达式，默认为空
     */
    String key() default "";

    /**
     * 重复提交提示消息
     * <p>当触发幂等拦截时返回给客户端的提示信息</p>
     *
     * @return 提示消息，默认为 "请勿重复提交"
     */
    String message() default IdempotentConstant.DEFAULT_MESSAGE;

    /**
     * 成功后是否删除幂等键
     * <p>STRICT 为 true 时不保留完成记录，释放执行锁后允许再次执行；不能代替支付等业务数据库幂等。</p>
     * <p>For STRICT, true allows another execution after unlock; this does not replace business database idempotency.</p>
     *
     * @return 是否删除，默认为 false
     */
    boolean deleteKeyAfterSuccess() default false;

    /**
     * 幂等策略
     * <p>定义如何处理重复请求：</p>
     * <ul>
     *   <li>STRICT: 严格拒绝重复请求，直接抛出异常</li>
     *   <li>RETURN_CACHE: 返回上次缓存的结果（默认策略）</li>
     *   <li>TOKEN_REQUIRED: 必须携带有效的幂等Token</li>
     * </ul>
     *
     * @return 幂等策略，默认为 RETURN_CACHE
     */
    IdempotentStrategy strategy() default IdempotentStrategy.RETURN_CACHE;

    /**
     * 结果缓存过期时间（秒）
     * <p>仅当 strategy=RETURN_CACHE 且 cacheResult=true 时生效</p>
     *
     * @return 缓存过期时间，默认为 3600秒（1小时）
     */
    int cacheExpire() default IdempotentConstant.CACHE_EXPIRE_DEFAULT;

    /**
     * 是否缓存执行结果
     * <p>仅当 strategy=RETURN_CACHE 时生效</p>
     * <ul>
     *   <li>true: 缓存执行结果，重复请求返回缓存结果</li>
     *   <li>false: 不缓存结果，仅做防重拦截</li>
     * </ul>
     *
     * @return 是否缓存结果，默认为 true
     */
    boolean cacheResult() default true;

    /**
     * 是否开启监控指标
     * <p>开启后会记录幂等拦截的次数、耗时等监控指标</p>
     *
     * @return 是否开启监控，默认为 true
     */
    boolean enableMetrics() default true;
}
