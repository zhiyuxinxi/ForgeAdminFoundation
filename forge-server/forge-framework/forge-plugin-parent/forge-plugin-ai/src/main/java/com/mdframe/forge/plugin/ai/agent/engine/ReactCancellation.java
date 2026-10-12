package com.mdframe.forge.plugin.ai.agent.engine;

import java.util.concurrent.atomic.AtomicReference;

/** Cancellation belongs to one execution, including its current transport and worker. */
public final class ReactCancellation {
    private final ReactContext context;
    private final AtomicReference<Runnable> transport = new AtomicReference<>();
    private Thread worker;

    ReactCancellation(ReactContext context) {
        this.context = context;
    }

    ReactContext context() {
        return context;
    }

    public void registerTransport(Runnable cancel) {
        transport.set(cancel);
        if (cancel != null && context.isCancelled() && transport.compareAndSet(cancel, null)) {
            cancel.run();
        }
    }

    synchronized void attachWorker() {
        worker = Thread.currentThread();
        if (context.isCancelled()) {
            worker.interrupt();
        }
    }

    synchronized void detachWorker() {
        worker = null;
        transport.set(null);
    }

    public synchronized void cancel() {
        context.setCancelled(true);
        try {
            Runnable cancel = transport.getAndSet(null);
            if (cancel != null) {
                cancel.run();
            }
        } finally {
            if (worker != null) {
                worker.interrupt();
            }
        }
    }
}
