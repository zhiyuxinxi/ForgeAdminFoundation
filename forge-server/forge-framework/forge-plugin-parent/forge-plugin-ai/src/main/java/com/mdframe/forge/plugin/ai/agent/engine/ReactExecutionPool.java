package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Process-local admission and lifetime for the agent loop; slots cover real worker lifetime. */
@Component
public final class ReactExecutionPool implements AutoCloseable {
    private final ConcurrentMap<String, ReactCancellation> active = new ConcurrentHashMap<>();
    private final Semaphore slots;
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor deadlines;
    private final long timeoutMillis;

    public ReactExecutionPool(@Value("${forge.ai.engine.max-concurrent:8}") int concurrency,
                              @Value("${forge.ai.engine.timeout-millis:300000}") long timeoutMillis) {
        if (concurrency < 1 || concurrency > 256 || timeoutMillis < 1 || timeoutMillis > 3600000) {
            throw new IllegalArgumentException("Invalid forge.ai.engine execution limits");
        }
        this.timeoutMillis = timeoutMillis;
        slots = new Semaphore(concurrency);
        AtomicInteger sequence = new AtomicInteger();
        workers = new ThreadPoolExecutor(concurrency, concurrency, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(concurrency), task -> {
                    Thread thread = new Thread(task, "react-loop-" + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        workers.allowCoreThreadTimeOut(true);
        deadlines = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "react-loop-deadline");
            thread.setDaemon(true);
            return thread;
        });
        deadlines.setRemoveOnCancelPolicy(true);
    }

    synchronized ReactCancellation start(ReactContext context, Runnable beforeStart, Runnable work, Runnable afterEnd) {
        if (workers.isShutdown()) {
            throw new BusinessException(503, UiMessages.label("AI引擎已停止", "AI engine is stopped"));
        }
        ReactCancellation execution = new ReactCancellation(context);
        if (active.putIfAbsent(context.getSessionId(), execution) != null) {
            throw new BusinessException(409, UiMessages.label("会话正在执行，请等待结束", "Session is already running"));
        }
        if (!slots.tryAcquire()) {
            active.remove(context.getSessionId(), execution);
            throw new BusinessException(429, UiMessages.label("AI执行额度已满，请稍后重试", "AI execution capacity is full; retry later"));
        }
        Locale locale = LocaleContextHolder.getLocale();
        ScheduledFuture<?> timeout = null;
        try {
            beforeStart.run();
            context.setCancellation(execution);
            timeout = deadlines.schedule(execution::cancel, timeoutMillis, TimeUnit.MILLISECONDS);
            ScheduledFuture<?> deadline = timeout;
            workers.execute(() -> run(execution, locale, work, deadline, afterEnd));
            return execution;
        } catch (RuntimeException | Error failure) {
            if (timeout != null) {
                timeout.cancel(false);
            }
            release(execution);
            throw failure;
        }
    }

    private void run(ReactCancellation execution, Locale locale, Runnable work,
                     ScheduledFuture<?> deadline, Runnable afterEnd) {
        execution.attachWorker();
        LocaleContextHolder.setLocale(locale);
        try {
            work.run();
        } finally {
            deadline.cancel(false);
            execution.detachWorker();
            LocaleContextHolder.resetLocaleContext();
            Thread.interrupted();
            release(execution);
            afterEnd.run();
        }
    }

    private void release(ReactCancellation execution) {
        if (active.remove(execution.context().getSessionId(), execution)) {
            slots.release();
        }
    }

    ReactContext activeContext(String sessionId) {
        ReactCancellation execution = active.get(sessionId);
        return execution == null ? null : execution.context();
    }

    void cancel(ReactContext context) {
        ReactCancellation execution = active.get(context.getSessionId());
        if (execution != null && execution.context() == context) {
            execution.cancel();
        }
    }

    @PreDestroy
    @Override
    public synchronized void close() {
        active.values().forEach(ReactCancellation::cancel);
        workers.shutdown();
        deadlines.shutdown();
    }
}
