package com.mdframe.forge.starter.idempotent.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RedissonLockManager implements LockManager {

    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "idempotent:lock:";

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        String fullLockKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullLockKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (acquired) {
                log.debug("获取锁成功: key={}, waitTime={}ms, leaseTime={}ms", lockKey, waitTime, leaseTime);
            } else {
                log.warn("获取锁失败: key={}, waitTime={}ms", lockKey, waitTime);
            }
            return acquired;
        } catch (InterruptedException e) {
            log.error("获取锁被中断: key={}, error={}", lockKey, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean tryLockWithWatchdog(String lockKey, long waitTime) {
        RLock lock = redissonClient.getLock(buildLockKey(lockKey));
        if (lock.isHeldByCurrentThread()) {
            return false;
        }
        try {
            return lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            log.warn("Execution lock acquisition was interrupted", failure);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        String fullLockKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullLockKey);

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放锁成功: key={}", lockKey);
            } else {
                throw new IllegalMonitorStateException("Execution lock is not held by the current thread");
            }
        } catch (Exception e) {
            log.error("释放锁失败: key={}, error={}", lockKey, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        String fullLockKey = buildLockKey(lockKey);
        RLock lock = redissonClient.getLock(fullLockKey);
        return lock.isLocked();
    }

    private String buildLockKey(String lockKey) {
        return LOCK_KEY_PREFIX + lockKey;
    }
}
