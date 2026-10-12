package com.mdframe.forge.starter.idempotent.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.config.IdempotentAutoConfiguration;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.lock.RedissonLockManager;
import com.mdframe.forge.starter.idempotent.properties.LockProperties;
import com.mdframe.forge.starter.idempotent.service.IdempotentStorageService;
import com.mdframe.forge.starter.idempotent.service.RedisIdempotentStorageService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Deterministic component checks: SDK locking and Redis TTL are controlled; no external Redis or business DB runs. */
class StrictStrategyHandlerTest {
    private static final String KEY = "test-request";
    private static final String COMPLETED = "idempotent:strict:completed:" + KEY;
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RLock sdkLock = mock(RLock.class);
    private final ReentrantLock execution = new ReentrantLock();
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final AtomicLong clock = new AtomicLong();
    private final Map<String, Long> expirations = new ConcurrentHashMap<>();
    private final Idempotent annotation = mock(Idempotent.class);
    private final LockProperties properties = new LockProperties();
    private StrictStrategyHandler handler;

    @BeforeEach
    void setUp() throws InterruptedException {
        properties.setWaitTime(0);
        when(annotation.expire()).thenReturn(2);
        when(annotation.message()).thenReturn("duplicate");
        when(redisson.getLock("idempotent:lock:" + KEY)).thenReturn(sdkLock);
        when(sdkLock.isHeldByCurrentThread()).thenAnswer(call -> execution.isHeldByCurrentThread());
        when(sdkLock.tryLock(0, TimeUnit.MILLISECONDS)).thenAnswer(call -> execution.tryLock());
        doAnswer(call -> {
            execution.unlock();
            return null;
        }).when(sdkLock).unlock();
        when(redis.opsForValue()).thenReturn(values);
        when(redis.hasKey(anyString())).thenAnswer(call ->
                expirations.getOrDefault(call.getArgument(0), -1L) > clock.get());
        when(values.setIfAbsent(anyString(), eq("1"), anyLong(), eq(TimeUnit.SECONDS))).thenAnswer(call -> {
            String key = call.getArgument(0);
            if (expirations.getOrDefault(key, -1L) > clock.get()) {
                return false;
            }
            expirations.put(key, clock.get() + TimeUnit.SECONDS.toMillis(call.getArgument(2, Long.class)));
            return true;
        });
        handler = new StrictStrategyHandler(new RedissonLockManager(redisson), properties,
                new RedisIdempotentStorageService(redis));
    }

    @Test
    void longExecutionStaysExclusiveAndCompletionTtlStartsAfterSuccess() throws Throwable {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ProceedingJoinPoint slow = mock(ProceedingJoinPoint.class);
        when(slow.proceed()).thenAnswer(call -> {
            clock.set(6000); // Beyond the previous 5000ms fixed lease, without waiting in real time.
            entered.countDown();
            if (!finish.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test release timed out");
            }
            return "first";
        });
        ProceedingJoinPoint duplicate = point("next");
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> handler.handle(slow, annotation, KEY)));
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> handler.handle(duplicate, annotation, KEY)).isInstanceOf(IdempotentException.class);
            verify(duplicate, never()).proceed();
            assertThat(expirations).isEmpty();
            finish.countDown();
            assertThat(first.get(3, TimeUnit.SECONDS)).isEqualTo("first");
            assertThat(execution.isLocked()).isFalse();
            assertThat(expirations.get(COMPLETED)).isEqualTo(8000L);
            clock.set(7999);
            assertThatThrownBy(() -> handler.handle(duplicate, annotation, KEY)).isInstanceOf(IdempotentException.class);
            assertThat(execution.isLocked()).isFalse();
            clock.set(8000);
            assertThat(handler.handle(duplicate, annotation, KEY)).isEqualTo("next");
            verify(sdkLock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void businessFailureLeavesNoCompletionRecordAndCanRetry() throws Throwable {
        ProceedingJoinPoint request = point("recovered");
        IllegalStateException failure = new IllegalStateException("controlled business failure");
        when(request.proceed()).thenThrow(failure).thenReturn("recovered");
        assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isSameAs(failure);
        assertThat(execution.isLocked()).isFalse();
        assertThat(expirations).isEmpty();
        assertThat(handler.handle(request, annotation, KEY)).isEqualTo("recovered");
        assertThat(execution.isLocked()).isFalse();
    }

    @Test
    void deleteAfterSuccessAllowsAnotherExecutionWithoutLeavingALock() throws Throwable {
        when(annotation.deleteKeyAfterSuccess()).thenReturn(true);
        ProceedingJoinPoint request = point("ok");
        assertThat(handler.handle(request, annotation, KEY)).isEqualTo("ok");
        assertThat(handler.handle(request, annotation, KEY)).isEqualTo("ok");
        verify(request, times(2)).proceed();
        assertThat(expirations).isEmpty();
        assertThat(execution.isLocked()).isFalse();
    }

    @Test
    void redisReadFailureAndUnknownReplyNeverExecuteBusiness() throws Throwable {
        ProceedingJoinPoint request = point("must-not-run");
        IllegalStateException failure = new IllegalStateException("controlled redis read failure");
        when(redis.hasKey(COMPLETED)).thenThrow(failure).thenReturn(null);
        assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isSameAs(failure);
        assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isInstanceOf(NullPointerException.class);
        verify(request, never()).proceed();
        assertThat(execution.isLocked()).isFalse();
    }

    @Test
    void completionWriteFailureIsReportedAndExecutionLockStillReleased() throws Throwable {
        ProceedingJoinPoint request = point("business-returned");
        IllegalStateException failure = new IllegalStateException("controlled completion write failure");
        doThrow(failure).when(values).setIfAbsent(COMPLETED, "1", 2L, TimeUnit.SECONDS);
        assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isSameAs(failure);
        verify(request).proceed();
        assertThat(execution.isLocked()).isFalse();
        assertThat(expirations).isEmpty();
    }

    @Test
    void releaseFailureCannotTurnSuccessfulExecutionIntoReportedSuccess() throws Throwable {
        ProceedingJoinPoint request = point("ok");
        IllegalStateException failure = new IllegalStateException("controlled unlock failure");
        doThrow(failure).when(sdkLock).unlock();
        try {
            assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isSameAs(failure);
            assertThat(expirations).containsKey(COMPLETED);
        } finally {
            execution.unlock();
        }
    }

    @Test
    void releaseFailurePreservesOriginalBusinessFailure() throws Throwable {
        ProceedingJoinPoint request = point("unused");
        IllegalStateException original = new IllegalStateException("business failed");
        IllegalStateException release = new IllegalStateException("unlock failed");
        when(request.proceed()).thenThrow(original);
        doThrow(release).when(sdkLock).unlock();
        try {
            assertThatThrownBy(() -> handler.handle(request, annotation, KEY)).isSameAs(original);
            assertThat(original.getSuppressed()).containsExactly(release);
        } finally {
            execution.unlock();
        }
    }

    @Test
    void sameThreadCannotReenterAnExecutingRequest() throws Throwable {
        ProceedingJoinPoint inner = point("must-not-run");
        ProceedingJoinPoint outer = mock(ProceedingJoinPoint.class);
        when(outer.proceed()).thenAnswer(call -> {
            assertThatThrownBy(() -> handler.handle(inner, annotation, KEY)).isInstanceOf(IdempotentException.class);
            return "outer";
        });
        assertThat(handler.handle(outer, annotation, KEY)).isEqualTo("outer");
        verify(inner, never()).proceed();
        assertThat(execution.isLocked()).isFalse();
    }

    @Test
    void autoConfigurationActuallyWiresTheCompletionStorageIntoStrict() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(IdempotentAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(StringRedisTemplate.class, () -> redis)
                .withBean(RedissonClient.class, () -> redisson)
                .run(context -> {
                    assertThat(context).hasSingleBean(IdempotentStorageService.class);
                    assertThat(context.getBean(IdempotentStorageService.class)).isInstanceOf(RedisIdempotentStorageService.class);
                    IdempotentStrategyHandler configured = context.getBean("strictStrategyHandler", IdempotentStrategyHandler.class);
                    ProceedingJoinPoint request = mock(ProceedingJoinPoint.class);
                    when(redis.hasKey(COMPLETED)).thenReturn(true);
                    when(sdkLock.tryLock(3000, TimeUnit.MILLISECONDS)).thenAnswer(call -> execution.tryLock());
                    assertThatThrownBy(() -> configured.handle(request, annotation, KEY)).isInstanceOf(IdempotentException.class);
                    verifyNoInteractions(request);
                    assertThat(execution.isLocked()).isFalse();
                });
    }

    private ProceedingJoinPoint point(String result) throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn(result);
        return point;
    }
}
