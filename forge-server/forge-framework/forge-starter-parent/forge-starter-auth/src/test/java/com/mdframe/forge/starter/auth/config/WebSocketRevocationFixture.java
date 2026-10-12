package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.listener.SaTokenEventCenter;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.auth.listener.WebSocketTokenRevocationListener;
import com.mdframe.forge.starter.websocket.config.WebSocketConfig;
import com.mdframe.forge.starter.websocket.security.AuthenticatedWebSocketSessions;
import com.mdframe.forge.starter.websocket.security.WebSocketAuthenticationProvider;
import com.mdframe.forge.starter.websocket.security.WebSocketProperties;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real Spring STOMP/broker channels and Sa-Token storage; only the network socket is controlled. */
class WebSocketRevocationFixture implements AutoCloseable {
    final SaTokenDao previousDao = SaManager.getSaTokenDao();
    final StpLogic previousLogic = StpUtil.getStpLogic();
    final SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
    final StpLogic logic = new StpLogic("login").setConfig(new SaTokenConfig()
            .setTimeout(3600).setActiveTimeout(60).setDynamicActiveTimeout(true).setIsShare(false));
    final ExecutorSubscribableChannel inbound = new ExecutorSubscribableChannel();
    final ExecutorSubscribableChannel outbound = new ExecutorSubscribableChannel();
    final ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
    final SimpleBrokerMessageHandler broker = new SimpleBrokerMessageHandler(
            inbound, outbound, brokerChannel, List.of("/topic", "/queue"));
    final SubProtocolWebSocketHandler protocol = new SubProtocolWebSocketHandler(inbound, outbound);
    final WebSocketAuthenticationProvider provider;
    final AuthenticatedWebSocketSessions sessions;
    final WebSocketTokenRevocationListener listener;
    final WebSocketHandler transport;

    WebSocketRevocationFixture() {
        SaManager.setSaTokenDao(dao);
        StpUtil.setStpLogic(logic);
        SaTokenWebSocketAuthenticationConfiguration auth = new SaTokenWebSocketAuthenticationConfiguration();
        provider = auth.webSocketAuthenticationProvider();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("provider", provider);
        WebSocketConfig config = new WebSocketConfig(new WebSocketProperties(),
                beans.getBeanProvider(WebSocketAuthenticationProvider.class));
        sessions = config.authenticatedWebSocketSessions();
        beans.addBean("sessions", sessions);
        listener = auth.webSocketTokenRevocationListener(beans.getBeanProvider(AuthenticatedWebSocketSessions.class));
        SaTokenEventCenter.registerListener(listener);
        TestChannelRegistration input = new TestChannelRegistration();
        TestChannelRegistration output = new TestChannelRegistration();
        config.configureClientInboundChannel(input);
        config.configureClientOutboundChannel(output);
        inbound.setInterceptors(input.interceptors());
        // Spring's normal broker configuration adds this after user interceptors.
        inbound.addInterceptor(new ImmutableMessageChannelInterceptor());
        outbound.setInterceptors(output.interceptors());
        TestTransportRegistration registration = new TestTransportRegistration();
        config.configureWebSocketTransport(registration);
        transport = registration.factories().get(0).decorate(protocol);
        protocol.setDefaultProtocolHandler(new StompSubProtocolHandler());
        protocol.start();
        broker.start();
    }

    String token(String device) {
        return logic.createLoginSession("42", new SaLoginModel().setDevice(device).setActiveTimeout(60));
    }

    Client connect(String id, String token) throws Exception {
        Client client = new Client(id);
        transport.afterConnectionEstablished(client.socket);
        frame(client, "CONNECT\naccept-version:1.2\nAuthorization:Bearer " + token + "\n\n");
        frame(client, "SUBSCRIBE\nid:sub-1\ndestination:/topic/broadcast\n\n");
        return client;
    }

    void frame(Client client, String frame) throws Exception {
        transport.handleMessage(client.socket, new TextMessage(frame + '\0'));
    }

    void broadcast() {
        new SimpMessagingTemplate(brokerChannel).convertAndSend(
                "/topic/broadcast", "safe-content".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        SaTokenEventCenter.removeListener(listener);
        sessions.destroy();
        protocol.stop();
        broker.stop();
        dao.refreshFlag = false;
        if (dao.refreshThread != null) {
            dao.refreshThread.interrupt();
        }
        StpUtil.setStpLogic(previousLogic);
        SaManager.setSaTokenDao(previousDao);
    }

    final class Client {
        final WebSocketSession socket = mock(WebSocketSession.class);
        final AtomicBoolean open = new AtomicBoolean(true);
        final List<String> received = new ArrayList<>();
        CloseStatus closeStatus;

        Client(String id) throws IOException {
            when(socket.getId()).thenReturn(id);
            when(socket.isOpen()).thenAnswer(call -> open.get());
            when(socket.getAttributes()).thenReturn(new HashMap<>());
            when(socket.getTextMessageSizeLimit()).thenReturn(65536);
            doAnswer(call -> {
                received.add(((TextMessage) call.getArgument(0)).getPayload());
                return null;
            }).when(socket).sendMessage(any());
            doAnswer(call -> {
                closeStatus = call.getArgument(0);
                if (open.getAndSet(false)) {
                    transport.afterConnectionClosed(socket, closeStatus);
                }
                return null;
            }).when(socket).close(any(CloseStatus.class));
        }

        long deliveries() {
            return received.stream().filter(frame -> frame.startsWith("MESSAGE\n")).count();
        }
    }

    private static class TestChannelRegistration extends ChannelRegistration {
        List<ChannelInterceptor> interceptors() {
            return getInterceptors();
        }
    }

    private static class TestTransportRegistration extends WebSocketTransportRegistration {
        List<WebSocketHandlerDecoratorFactory> factories() {
            return getDecoratorFactories();
        }
    }
}
