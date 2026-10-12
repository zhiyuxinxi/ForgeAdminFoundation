package com.mdframe.forge.starter.flow.event;

/** A terminal transition cannot commit without its required durable event. */
public final class FlowTerminalEventPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FlowTerminalEventPersistenceException(String message) {
        super(message);
    }

    public FlowTerminalEventPersistenceException(Throwable cause) {
        super("FLOW_TERMINAL_EVENT_PERSISTENCE_FAILED", cause);
    }
}
