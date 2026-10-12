package com.mdframe.forge.starter.websocket.config;

import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketChannelInterceptor;
import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketOutboundInterceptor;
import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketSessions;
import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketTransport;
import com.mdframe.forge.starter.websocket.security.WebSocketAuthenticationProvider;
import com.mdframe.forge.starter.websocket.security.WebSocketProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket配置类
 */
@AutoConfiguration
@EnableWebSocketMessageBroker
@EnableWebSocket
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final AuthenticatedWebSocketSessions sessions;

    public WebSocketConfig(WebSocketProperties properties,
                           ObjectProvider<WebSocketAuthenticationProvider> authenticationProvider) {
        this.properties = properties;
        this.sessions = new AuthenticatedWebSocketSessions(authenticationProvider::getIfAvailable);
    }

    @Bean
    public AuthenticatedWebSocketSessions authenticatedWebSocketSessions() {
        return sessions;
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理
        // /queue - 点对点消息
        // /topic - 广播消息
        registry.enableSimpleBroker("/queue", "/topic");
        
        // 配置客户端发送消息的前缀
        registry.setApplicationDestinationPrefixes(properties.getApplicationDestinationPrefix());
        
        // 配置点对点消息的前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new AuthenticatedWebSocketChannelInterceptor(
                sessions, properties));
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new AuthenticatedWebSocketOutboundInterceptor(sessions));
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new AuthenticatedWebSocketTransport(handler, sessions));
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(properties.getAllowedOriginPatterns().toArray(String[]::new))
                .withSockJS();
    }
}
