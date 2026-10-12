package com.mdframe.forge.starter.log.context;

import lombok.Data;

/**
 * 操作审计上下文。
 * 业务代码可在一次请求内主动写入操作前后快照，日志切面在 finally 中读取并清理。
 */
public final class OperationAuditContext {

    private static final ThreadLocal<Snapshot> HOLDER = ThreadLocal.withInitial(Snapshot::new);

    private OperationAuditContext() {
    }

    public static void setOperationContent(Object operationContent) {
        HOLDER.get().setOperationContent(operationContent);
    }

    public static void setBeforeData(Object beforeData) {
        HOLDER.get().setBeforeData(beforeData);
    }

    public static void setAfterData(Object afterData) {
        HOLDER.get().setAfterData(afterData);
    }

    public static void setDiffData(Object diffData) {
        HOLDER.get().setDiffData(diffData);
    }

    public static Snapshot snapshot() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class Snapshot {
        private Object operationContent;
        private Object beforeData;
        private Object afterData;
        private Object diffData;
    }
}
