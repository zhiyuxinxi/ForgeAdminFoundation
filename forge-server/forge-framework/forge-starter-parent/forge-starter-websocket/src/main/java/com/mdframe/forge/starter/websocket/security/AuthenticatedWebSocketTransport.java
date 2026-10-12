package com.mdframe.forge.starter.websocket.security;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/** Binds STOMP session IDs to the actual transport socket, including SockJS sessions. */
public class AuthenticatedWebSocketTransport extends WebSocketHandlerDecorator {

    private final AuthenticatedWebSocketSessions sessions;

    public AuthenticatedWebSocketTransport(WebSocketHandler delegate, AuthenticatedWebSocketSessions sessions) {
        super(delegate);
        this.sessions = sessions;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.opened(session);
        try {
            super.afterConnectionEstablished(session);
        } catch (Exception exception) {
            sessions.closed(session.getId());
            throw exception;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            super.afterConnectionClosed(session, status);
        } finally {
            sessions.closed(session.getId());
        }
    }
}
