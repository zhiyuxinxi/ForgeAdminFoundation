package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

/**
 * 低代码动态 CRUD 当前线程运行数据源上下文。
 */
public final class LowcodeRuntimeDataSourceContextHolder {

    private static final ThreadLocal<LowcodeRuntimeDataSourceContext> CONTEXT = new ThreadLocal<>();

    private LowcodeRuntimeDataSourceContextHolder() {
    }

    public static LowcodeRuntimeDataSourceContext get() {
        return CONTEXT.get();
    }

    public static Scope use(LowcodeRuntimeDataSourceContext context) {
        LowcodeRuntimeDataSourceContext previous = CONTEXT.get();
        CONTEXT.set(context);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {

        private final LowcodeRuntimeDataSourceContext previous;

        private Scope(LowcodeRuntimeDataSourceContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }
}
