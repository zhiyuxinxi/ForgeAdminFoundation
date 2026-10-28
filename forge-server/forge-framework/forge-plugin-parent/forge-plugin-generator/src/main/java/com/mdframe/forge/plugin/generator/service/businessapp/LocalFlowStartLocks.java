package com.mdframe.forge.plugin.generator.service.businessapp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Local flow-start fallback; registrations include callers not yet queued on the lock. */
final class LocalFlowStartLocks {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Supplier<ReentrantLock> lockFactory;

    LocalFlowStartLocks() {
        this(ReentrantLock::new);
    }

    LocalFlowStartLocks(Supplier<ReentrantLock> lockFactory) {
        this.lockFactory = lockFactory;
    }

    Handle tryAcquire(String key, long timeout, TimeUnit unit) throws InterruptedException {
        Entry entry = entries.compute(key, (ignored, current) -> {
            Entry result = current == null ? new Entry(lockFactory.get()) : current;
            result.users++;
            return result;
        });
        boolean acquired = false;
        try {
            acquired = entry.lock.tryLock(timeout, unit);
            return acquired ? new Handle(key, entry) : null;
        } finally {
            if (!acquired) {
                releaseReference(key, entry);
            }
        }
    }

    private void releaseReference(String key, Entry entry) {
        entries.compute(key, (ignored, current) -> {
            if (current != entry) {
                throw new IllegalStateException("Flow-start lock registration changed while in use");
            }
            return --entry.users == 0 ? null : entry;
        });
    }

    final class Handle implements AutoCloseable {
        private final String key;
        private final Entry entry;
        private boolean closed;

        private Handle(String key, Entry entry) {
            this.key = key;
            this.entry = entry;
        }

        @Override
        public void close() {
            if (!closed && entry.lock.isHeldByCurrentThread()) {
                closed = true;
                entry.lock.unlock();
                releaseReference(key, entry);
            }
        }
    }

    private static final class Entry {
        private final ReentrantLock lock;
        // Accessed only under ConcurrentHashMap.compute for this key.
        private int users;

        private Entry(ReentrantLock lock) {
            this.lock = lock;
        }
    }
}
