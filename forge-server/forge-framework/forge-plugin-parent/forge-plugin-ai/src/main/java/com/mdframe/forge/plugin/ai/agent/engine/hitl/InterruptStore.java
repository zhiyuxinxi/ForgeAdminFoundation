package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HITL 中断状态存储（Redis）。
 * 中断状态纯 Redis，TTL 10 分钟，不建表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterruptStore {

    private static final String KEY_PREFIX = "agent:interrupt:";
    private static final long TTL_MINUTES = 10;
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 保存中断状态
     *
     * @return 中断ID
     */
    public String save(ReactContext ctx) {
        String interruptId = UUID.randomUUID().toString();
        String key = ownerKey(ctx.getUserId(), ctx.getTenantId(), interruptId);
        String value = com.alibaba.fastjson2.JSON.toJSONString(new InterruptState(ctx));
        redisTemplate.opsForValue().set(key, value, TTL_MINUTES, TimeUnit.MINUTES);
        log.info("[InterruptStore] 保存中断: id={}, sessionId={}", interruptId, ctx.getSessionId());
        return interruptId;
    }

    /**
     * 获取中断状态
     */
    public InterruptState get(String interruptId) {
        String key = callerKey(interruptId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        InterruptState state = com.alibaba.fastjson2.JSON.parseObject(value, InterruptState.class);
        state.setStoredValue(value);
        ReactContext context = state.getContext();
        AiSessionAccessService.requireSameOwner(SessionHelper.getUserId(), SessionHelper.getTenantId(),
                context.getUserId(), context.getTenantId());
        return state;
    }

    public boolean consume(String interruptId, InterruptState expected) {
        ReactContext context = expected.getContext();
        AiSessionAccessService.requireSameOwner(SessionHelper.getUserId(), SessionHelper.getTenantId(),
                context.getUserId(), context.getTenantId());
        if (expected.getStoredValue() == null) {
            return false;
        }
        Long consumed = redisTemplate.execute(CONSUME, List.of(callerKey(interruptId)), expected.getStoredValue());
        return Long.valueOf(1L).equals(consumed);
    }

    /**
     * 移除中断状态
     */
    public void remove(String interruptId) {
        String key = callerKey(interruptId);
        redisTemplate.delete(key);
    }

    private String callerKey(String interruptId) {
        return ownerKey(SessionHelper.getUserId(), SessionHelper.getTenantId(), interruptId);
    }

    private String ownerKey(Long userId, Long tenantId, String interruptId) {
        AiSessionAccessService.requireSameOwner(userId, tenantId, userId, tenantId);
        return KEY_PREFIX + tenantId + ":" + userId + ":" + interruptId;
    }

    @Data
    public static class InterruptState {
        private AgentContextSnapshot snapshot;
        private String interruptId;
        @JSONField(serialize = false, deserialize = false)
        private String storedValue;

        public InterruptState() {
        }

        public InterruptState(ReactContext context) {
            this.snapshot = AgentContextSnapshot.capture(context);
            this.interruptId = UUID.randomUUID().toString();
        }

        @JSONField(serialize = false, deserialize = false)
        public ReactContext getContext() {
            if (snapshot == null) {
                throw new IllegalArgumentException("Legacy or invalid interrupt checkpoint");
            }
            return snapshot.restoreData();
        }
    }
}
