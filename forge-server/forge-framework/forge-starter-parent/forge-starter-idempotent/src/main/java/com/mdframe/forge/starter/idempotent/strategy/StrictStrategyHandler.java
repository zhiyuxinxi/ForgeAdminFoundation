package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.lock.LockManager;
import com.mdframe.forge.starter.idempotent.properties.LockProperties;
import com.mdframe.forge.starter.idempotent.service.IdempotentStorageService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;

@RequiredArgsConstructor
public class StrictStrategyHandler implements IdempotentStrategyHandler {

    private final LockManager lockManager;
    private final LockProperties lockProperties;
    private final IdempotentStorageService storage;

    @Override
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent annotation, String idempotentKey) throws Throwable {
        int expire = annotation.expire();
        if (expire <= 0) {
            throw new IllegalArgumentException("Idempotency completion TTL must be positive");
        }
        if (!lockManager.tryLockWithWatchdog(idempotentKey, lockProperties.getWaitTime())) {
            throw new IdempotentException(annotation.message());
        }
        String completedKey = "idempotent:strict:completed:" + idempotentKey;
        try (AutoCloseable executionLock = () -> lockManager.unlock(idempotentKey)) {
            if (storage.exists(completedKey)) {
                throw new IdempotentException(annotation.message());
            }
            Object result = joinPoint.proceed();
            if (!annotation.deleteKeyAfterSuccess() && !storage.tryAcquire(completedKey, expire)) {
                throw new IdempotentException(annotation.message());
            }
            return result;
        }
    }
}
