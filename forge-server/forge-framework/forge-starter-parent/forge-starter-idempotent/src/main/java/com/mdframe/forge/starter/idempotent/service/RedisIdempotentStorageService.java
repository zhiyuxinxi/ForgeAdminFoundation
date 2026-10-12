package com.mdframe.forge.starter.idempotent.service;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.Objects;

public class RedisIdempotentStorageService implements IdempotentStorageService {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdempotentStorageService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int expireSeconds) {
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("Idempotency completion TTL must be positive");
        }
        return Objects.requireNonNull(stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS), "Redis completion result is unknown");
    }

    @Override
    public void release(String key) {
        Objects.requireNonNull(stringRedisTemplate.delete(key), "Redis deletion result is unknown");
    }

    @Override
    public boolean exists(String key) {
        return Objects.requireNonNull(stringRedisTemplate.hasKey(key), "Redis completion state is unknown");
    }
}
