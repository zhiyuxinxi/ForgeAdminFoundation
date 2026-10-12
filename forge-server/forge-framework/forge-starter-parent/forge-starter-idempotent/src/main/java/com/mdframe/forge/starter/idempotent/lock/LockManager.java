package com.mdframe.forge.starter.idempotent.lock;

public interface LockManager {

    boolean tryLock(String lockKey, long waitTime, long leaseTime);

    /** Acquire an execution lock renewed until unlock; unsupported providers must not silently use a fixed lease. */
    default boolean tryLockWithWatchdog(String lockKey, long waitTime) {
        throw new UnsupportedOperationException("Execution-lock renewal is not supported by this lock provider");
    }

    void unlock(String lockKey);

    boolean isLocked(String lockKey);
}
