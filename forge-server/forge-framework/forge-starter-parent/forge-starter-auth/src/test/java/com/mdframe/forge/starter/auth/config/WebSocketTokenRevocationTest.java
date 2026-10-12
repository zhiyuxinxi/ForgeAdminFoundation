package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.listener.SaTokenEventCenter;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class WebSocketTokenRevocationTest {

    @Test
    void validConnectionSubscribesAndReceivesWithoutLeakingBearerHeader() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            var client = fixture.connect("valid", fixture.token("pc"));
            fixture.broadcast();
            assertEquals(1, client.deliveries());
            assertTrue(client.open.get());
            assertTrue(client.received.stream().anyMatch(frame -> frame.startsWith("CONNECTED\n")));
            assertFalse(client.received.toString().contains("Authorization"));
        }
    }

    @Test
    void realLogoutClosesOnlyMatchingTokenAndLoginType() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String firstToken = fixture.token("pc");
            var first = fixture.connect("first", firstToken);
            var second = fixture.connect("second", fixture.token("phone"));
            fixture.listener.doLogout("other-login-type", "42", firstToken);
            assertTrue(first.open.get());
            fixture.logic.logoutByTokenValue(firstToken);
            assertFalse(first.open.get());
            assertEquals(CloseStatus.POLICY_VIOLATION, first.closeStatus);
            fixture.broadcast();
            assertEquals(0, first.deliveries());
            assertEquals(1, second.deliveries());
        }
    }

    @Test
    void realKickoutAndReplacementConsumeExactRevocationEvents() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String kickedToken = fixture.token("kicked");
            var kicked = fixture.connect("kicked", kickedToken);
            var replaced = fixture.connect("replaced", fixture.token("replaced"));
            var retained = fixture.connect("retained", fixture.token("retained"));
            fixture.logic.kickoutByTokenValue(kickedToken);
            fixture.logic.replaced("42", "replaced");
            assertFalse(kicked.open.get());
            assertFalse(replaced.open.get());
            fixture.broadcast();
            assertEquals(1, retained.deliveries());
            assertEquals(0, kicked.deliveries() + replaced.deliveries());
        }
    }

    @Test
    void outboundStopsRevocationWithoutLocalEventAndQueuedDelivery() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String token = fixture.token("remote-revoke");
            var client = fixture.connect("remote", token);
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headers.setSessionId("remote");
            Message<?> queued = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            var interceptor = (ExecutorChannelInterceptor) fixture.outbound.getInterceptors().get(0);
            assertNotNull(interceptor.preSend(queued, fixture.outbound));
            SaTokenEventCenter.removeListener(fixture.listener);
            fixture.logic.logoutByTokenValue(token);
            assertNull(interceptor.beforeHandle(queued, fixture.outbound, message -> { }));
            fixture.broadcast();
            assertEquals(0, client.deliveries());
            assertFalse(client.open.get());
        }
    }

    @Test
    void expiredAbsoluteTokenIsRejectedOnInboundAndCannotUseOtherLogin() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String expired = fixture.token("expired");
            var client = fixture.connect("expired", expired);
            String valid = fixture.token("valid");
            fixture.dao.expireMap.put(fixture.logic.splicingKeyTokenValue(expired), System.currentTimeMillis() - 1);
            assertNull(fixture.provider.authenticate(expired));
            assertEquals("42", fixture.provider.authenticate(valid));
            fixture.frame(client, "SUBSCRIBE\nid:again\ndestination:/topic/broadcast\n\n");
            assertFalse(client.open.get());
            fixture.broadcast();
            assertEquals(0, client.deliveries());
        }
    }

    @Test
    void idleDynamicActiveTimeoutClosesSocketWithoutAnyClientFrame() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String expired = fixture.token("idle");
            var client = fixture.connect("idle", expired);
            fixture.dao.set(fixture.logic.splicingKeyLastActiveTime(expired),
                    (System.currentTimeMillis() - 61_000) + ",60", 3600);
            assertNotNull(fixture.logic.getLoginIdByToken(expired));
            assertEquals(-2, fixture.logic.getTokenActiveTimeoutByToken(expired));
            fixture.sessions.closeExpiredSessions();
            assertFalse(client.open.get());
            assertNull(fixture.sessions.validate("idle"));
        }
    }

    @Test
    void failedSocketCloseStaysBlockedAndOtherTokenStillReceives() throws Exception {
        try (var fixture = new WebSocketRevocationFixture()) {
            String revoked = fixture.token("broken-close");
            var blocked = fixture.connect("broken-close", revoked);
            var retained = fixture.connect("healthy", fixture.token("healthy"));
            doThrow(new IOException("controlled close failure"))
                    .when(blocked.socket).close(any(CloseStatus.class));
            fixture.logic.logoutByTokenValue(revoked);
            fixture.broadcast();
            assertNull(fixture.sessions.validate("broken-close"));
            assertEquals(0, blocked.deliveries());
            assertEquals(1, retained.deliveries());
        }
    }
}
