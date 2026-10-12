package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.file.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Bounds this instance's native uploads, retaining failed cancellations until a later successful retry. */
@Slf4j
final class MultipartSessionRegistry {
    private final Map<String, MultipartUploadSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger reserved = new AtomicInteger();
    private final Supplier<FileStorageProperties> properties;
    private final LongSupplier clock;

    MultipartSessionRegistry(Supplier<FileStorageProperties> properties, LongSupplier clock) {
        this.properties = properties;
        this.clock = clock;
    }

    String create(LongFunction<MultipartUploadSession> factory) {
        FileStorageProperties limits = properties.get();
        long lifetime = limits.getMultipartUploadTimeout().toMillis();
        if (limits.getMultipartMaxSessions() <= 0 || lifetime <= 0) {
            throw new IllegalStateException("Multipart capacity and timeout must be positive");
        }
        long expiresAt = Math.addExact(clock.getAsLong(), lifetime);
        reapExpired();
        if (reserved.incrementAndGet() > limits.getMultipartMaxSessions()) {
            reserved.decrementAndGet();
            throw new BusinessException(429, UiMessages.message("public.error.1701251f0508"));
        }
        try {
            String uploadId = UUID.randomUUID().toString();
            sessions.put(uploadId, factory.apply(expiresAt));
            return uploadId;
        } catch (RuntimeException failure) {
            reserved.decrementAndGet();
            throw failure;
        }
    }

    MultipartUploadSession require(String uploadId) {
        MultipartUploadSession session = uploadId == null ? null : sessions.get(uploadId);
        if (session == null) {
            throw denied();
        }
        return session;
    }

    void requireUnexpired(String uploadId, MultipartUploadSession session) {
        if (session.expiresAt <= clock.getAsLong()) {
            BusinessException failure = denied();
            abort(uploadId, session, failure);
            throw failure;
        }
    }

    void completed(String uploadId, MultipartUploadSession session) {
        session.finished = true;
        if (sessions.remove(uploadId, session)) {
            reserved.decrementAndGet();
        }
    }

    void abort(String uploadId, MultipartUploadSession session, RuntimeException originalFailure) {
        session.finished = true;
        try {
            cancelAndRemove(uploadId, session);
        } catch (RuntimeException cleanupFailure) {
            originalFailure.addSuppressed(cleanupFailure);
            log.error("Multipart cancellation remains pending for upload {}", uploadId, cleanupFailure);
        }
    }

    void reapExpired() {
        reap(false);
    }

    void close() {
        reap(true);
    }

    private void reap(boolean closing) {
        long now = clock.getAsLong();
        sessions.forEach((uploadId, session) -> {
            synchronized (session) {
                if (sessions.get(uploadId) == session && (closing || session.finished || session.expiresAt <= now)) {
                    session.finished = true;
                    try {
                        cancelAndRemove(uploadId, session);
                    } catch (RuntimeException failure) {
                        log.error("Multipart cancellation remains pending for upload {}", uploadId, failure);
                    }
                }
            }
        });
    }

    private void cancelAndRemove(String uploadId, MultipartUploadSession session) {
        session.storage.abortMultipartUpload(session.nativeId);
        completed(uploadId, session);
    }

    private BusinessException denied() {
        return new BusinessException(403, UiMessages.message("error.forbidden"));
    }
}
