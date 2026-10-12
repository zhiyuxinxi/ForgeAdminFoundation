package com.mdframe.forge.starter.idempotent.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisIdempotentStorageServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RedisIdempotentStorageService storage = new RedisIdempotentStorageService(redis);

    @Test
    void redisFailuresAreNeverConvertedToAvailableOrReleased() {
        IllegalStateException failure = new IllegalStateException("controlled redis failure");
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenThrow(failure);
        when(redis.hasKey(anyString())).thenThrow(failure);
        when(redis.delete(anyString())).thenThrow(failure);
        assertThatThrownBy(() -> storage.tryAcquire("key", 2)).isSameAs(failure);
        assertThatThrownBy(() -> storage.exists("key")).isSameAs(failure);
        assertThatThrownBy(() -> storage.release("key")).isSameAs(failure);
    }

    @Test
    void unknownRedisRepliesFailClosedWhileKnownFalseRemainsFalse() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(null, false);
        when(redis.hasKey("key")).thenReturn(null, false);
        when(redis.delete("key")).thenReturn(null, false);
        assertThatThrownBy(() -> storage.tryAcquire("key", 2)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> storage.exists("key")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> storage.release("key")).isInstanceOf(NullPointerException.class);
        assertThat(storage.tryAcquire("key", 2)).isFalse();
        assertThat(storage.exists("key")).isFalse();
        assertThatCode(() -> storage.release("key")).doesNotThrowAnyException();
    }

    @Test
    void completionExpiryUsesSecondsAndInvalidExpiryNeverWritesRedis() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("key", "1", 7L, TimeUnit.SECONDS)).thenReturn(true);
        assertThat(storage.tryAcquire("key", 7)).isTrue();
        assertThatThrownBy(() -> storage.tryAcquire("key", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.tryAcquire("key", -1)).isInstanceOf(IllegalArgumentException.class);
        verify(values).setIfAbsent("key", "1", 7L, TimeUnit.SECONDS);
        verifyNoMoreInteractions(values);
    }
}
