package com.mdframe.forge.starter.websocket.security;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;

import java.security.Principal;

/**
 * 强制 STOMP 会话认证并限制客户端可访问的消息目标。
 */
public class AuthenticatedWebSocketChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticatedWebSocketSessions sessions;
    private final WebSocketProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticatedWebSocketChannelInterceptor(AuthenticatedWebSocketSessions sessions,
                                                     WebSocketProperties properties) {
        this.sessions = sessions;
        this.properties = properties;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !accessor.isMutable()) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.DISCONNECT) {
            return message;
        }
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            authenticate(message, accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        requireAuthenticated(message, accessor);
        if (command == StompCommand.SEND) {
            requireApplicationDestination(message, accessor.getDestination());
        } else if (command == StompCommand.SUBSCRIBE) {
            requireAllowedSubscription(message, accessor.getDestination());
        }
        return message;
    }

    private void authenticate(Message<?> message, StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        String tokenValue = extractBearerToken(authorization);
        if (tokenValue == null) {
            throw new MessageDeliveryException(message, UiMessages.label(
                    "WebSocket CONNECT 缺少有效 Bearer Token", "WebSocket CONNECT requires a valid Bearer token"));
        }
        String loginId = sessions.authenticate(accessor.getSessionId(), tokenValue);
        if (loginId == null || loginId.isBlank()) {
            throw new MessageDeliveryException(message, UiMessages.label(
                    "WebSocket Token 无效或已过期", "WebSocket token is invalid or expired"));
        }
        accessor.removeNativeHeader(HttpHeaders.AUTHORIZATION);
        accessor.setUser(new LoginPrincipal(loginId));
    }

    private void requireAuthenticated(Message<?> message, StompHeaderAccessor accessor) {
        String loginId = sessions.validate(accessor.getSessionId());
        if (loginId == null || accessor.getUser() == null || !loginId.equals(accessor.getUser().getName())) {
            throw new MessageDeliveryException(message, UiMessages.label(
                    "WebSocket 会话未认证", "WebSocket session is not authenticated"));
        }
    }

    private void requireApplicationDestination(Message<?> message, String destination) {
        String prefix = properties.getApplicationDestinationPrefix();
        if (destination == null || prefix == null
                || !(destination.equals(prefix) || destination.startsWith(prefix + "/"))) {
            throw new MessageDeliveryException(message, "客户端禁止直接向消息 Broker 发送消息");
        }
    }

    private void requireAllowedSubscription(Message<?> message, String destination) {
        if (destination == null || properties.getAllowedSubscribeDestinations() == null
                || properties.getAllowedSubscribeDestinations().stream()
                .noneMatch(pattern -> pathMatcher.match(pattern, destination))) {
            throw new MessageDeliveryException(message, UiMessages.translate("无权订阅 WebSocket 目标: ") + destination);
        }
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String tokenValue = authorization.substring(BEARER_PREFIX.length()).trim();
        return tokenValue.isEmpty() ? null : tokenValue;
    }

    private record LoginPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
