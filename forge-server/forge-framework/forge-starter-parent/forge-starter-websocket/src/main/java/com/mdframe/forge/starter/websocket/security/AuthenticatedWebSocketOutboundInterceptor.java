package com.mdframe.forge.starter.websocket.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/** A broker subscription never outlives the token that created it. */
public class AuthenticatedWebSocketOutboundInterceptor implements ExecutorChannelInterceptor {

    private final AuthenticatedWebSocketSessions sessions;

    public AuthenticatedWebSocketOutboundInterceptor(AuthenticatedWebSocketSessions sessions) {
        this.sessions = sessions;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageType type = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
        if (type == SimpMessageType.DISCONNECT_ACK) {
            return message;
        }
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        return sessions.validate(sessionId) == null ? null : message;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        // Recheck after an asynchronous outbound queue, immediately before the socket consumer.
        return preSend(message, channel);
    }
}
