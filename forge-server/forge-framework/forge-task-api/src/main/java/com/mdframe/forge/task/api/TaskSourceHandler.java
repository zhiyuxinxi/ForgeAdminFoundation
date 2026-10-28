package com.mdframe.forge.task.api;

/**
 * Explicitly registered source owner, never a client-provided class or callback URL.
 * Implementations must re-read/authorize their real source record. Source hooks are called
 * outside the task owner's narrow internal query scope, preserving source data permissions.
 */
public interface TaskSourceHandler {
    String sourceType();

    /** Explicit transaction ownership; this first implementation only accepts "master". */
    String transactionDataSource();

    /** Throw on denied/missing source; no permissive default is provided. */
    void authorize(String sourceKey, Actor actor, Operation operation);

    /** Verify the selected definition and every creation value against the real source. */
    void validateCreation(TaskDefinition definition, BusinessTaskEngine.Create command, Actor actor);

    /** Validate business completion rules and any file references before writing task state. */
    void validateCompletion(BusinessTask task, String resultJson, Actor actor);

    /**
     * Called once per successful terminal transition, inside the same master transaction.
     * Only local transactional writes are supported: no HTTP, messages or afterCommit writes.
     * Failure rolls back both task transition and source writes; the caller may retry.
     * Cross-datasource/remote sources need a separate durable delivery adapter and are not supported here.
     */
    void onTerminal(BusinessTask task, Actor actor);

    record Actor(long tenantId, long userId) { }
    enum Operation { CREATE, READ, DISPATCH, START, COMPLETE, CANCEL }
}
