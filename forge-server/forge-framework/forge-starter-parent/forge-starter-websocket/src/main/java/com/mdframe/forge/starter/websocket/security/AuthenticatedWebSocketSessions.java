package com.mdframe.forge.starter.websocket.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Token bindings exist only for sockets owned by this application instance. */
@Slf4j
public class AuthenticatedWebSocketSessions implements InitializingBean, DisposableBean {

    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Supplier<WebSocketAuthenticationProvider> provider;
    private ScheduledExecutorService expiryCheck;

    public AuthenticatedWebSocketSessions(Supplier<WebSocketAuthenticationProvider> provider) {
        this.provider = provider;
    }

    public void opened(WebSocketSession socket) {
        connections.put(socket.getId(), new Connection(socket));
    }

    public void closed(String sessionId) {
        connections.remove(sessionId);
    }

    public String authenticate(String sessionId, String token) {
        Connection connection = sessionId == null ? null : connections.get(sessionId);
        if (connection == null || token == null) {
            return null;
        }
        synchronized (connection) {
            if (connection.token != null || !connection.socket.isOpen()) {
                return null;
            }
            connection.token = token;
            connection.loginId = resolve(token);
            return validate(sessionId);
        }
    }

    public String validate(String sessionId) {
        Connection connection = sessionId == null ? null : connections.get(sessionId);
        if (connection == null) {
            return null;
        }
        synchronized (connection) {
            if (connection.socket.isOpen() && connection.loginId != null
                    && connection.loginId.equals(resolve(connection.token))) {
                return connection.loginId;
            }
            close(connection);
            return null;
        }
    }

    public void revoke(String token) {
        if (token != null) {
            connections.values().forEach(connection -> {
                synchronized (connection) {
                    if (token.equals(connection.token)) {
                        close(connection);
                    }
                }
            });
        }
    }

    public void closeExpiredSessions() {
        connections.forEach((sessionId, connection) -> {
            if (!connection.socket.isOpen()) {
                closed(sessionId);
            } else if (connection.token != null) {
                validate(sessionId);
            }
        });
    }

    private String resolve(String token) {
        try {
            WebSocketAuthenticationProvider authentication = provider.get();
            String loginId = authentication == null ? null : authentication.authenticate(token);
            return loginId == null || loginId.isBlank() ? null : loginId;
        } catch (RuntimeException exception) {
            // Authentication infrastructure errors must not keep an authenticated socket alive.
            log.warn("WebSocket authentication unavailable; closing affected connection");
            return null;
        }
    }

    private void close(Connection connection) {
        connection.loginId = null;
        try {
            connection.socket.close(CloseStatus.POLICY_VIOLATION);
            connections.remove(connection.socket.getId(), connection);
        } catch (IOException exception) {
            // Keep the invalid binding for retry; it can never authenticate another frame.
            log.warn("Unable to close invalid WebSocket session {}", connection.socket.getId());
        }
    }

    @Override
    public void afterPropertiesSet() {
        expiryCheck = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "forge-websocket-token-expiry");
            thread.setDaemon(true);
            return thread;
        });
        expiryCheck.scheduleWithFixedDelay(this::closeExpiredSessions, 15, 15, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        if (expiryCheck != null) {
            expiryCheck.shutdownNow();
        }
        connections.values().forEach(this::close);
    }

    private static final class Connection {
        private final WebSocketSession socket;
        private volatile String token;
        private String loginId;

        private Connection(WebSocketSession socket) {
            this.socket = socket;
        }
    }
}
