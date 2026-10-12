package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowEventAcknowledgementControllerTest {
    @Test
    void authenticatedBoundConsumerAcknowledgesAndReplayIsIdempotent() throws Exception {
        try (var fixture = new FlowEventAckFixture()) {
            fixture.pending("event-one", "business-admin");
            fixture.use(fixture.token("501", 97));
            var request = new FlowEventAcknowledgementController.Acknowledgement("event-one");
            TenantContextHolder.executeWithTenant(98L, () -> {
                assertEquals("event-one", fixture.controller.acknowledge(request).getData());
                assertEquals("event-one", fixture.controller.acknowledge(request).getData());
                assertEquals(98L, TenantContextHolder.getTenantId());
            });
            assertEquals("ACKNOWLEDGED", fixture.status("event-one"));
        }
    }

    @Test
    void wrongAccountTenantAndBoundTargetCannotAcknowledge() throws Exception {
        try (var fixture = new FlowEventAckFixture()) {
            fixture.pending("event-one", "business-admin");
            fixture.pending("event-other", "another-consumer");
            var request = new FlowEventAcknowledgementController.Acknowledgement("event-one");
            fixture.use(fixture.token("502", 97));
            assertThrows(IllegalArgumentException.class, () -> fixture.controller.acknowledge(request));
            fixture.use(fixture.token("501", 98));
            assertThrows(IllegalArgumentException.class, () -> fixture.controller.acknowledge(request));
            fixture.use(fixture.token("501", 97));
            assertThrows(IllegalArgumentException.class, () -> fixture.controller.acknowledge(
                    new FlowEventAcknowledgementController.Acknowledgement("event-other")));
            assertEquals("PENDING", fixture.status("event-one"));
            assertEquals("PENDING", fixture.status("event-other"));
        }
    }

    @Test
    void revokedTokenMissingPermissionAndDisabledDeliveryFailClosed() throws Exception {
        try (var fixture = new FlowEventAckFixture()) {
            fixture.pending("event-one", "business-admin");
            var request = new FlowEventAcknowledgementController.Acknowledgement("event-one");
            String token = fixture.token("501", 97);
            fixture.logic.logoutByTokenValue(token);
            fixture.use(token);
            assertThrows(NotLoginException.class, () -> fixture.controller.acknowledge(request));
            fixture.use(fixture.token("501", 97));
            fixture.permission = false;
            assertThrows(NotPermissionException.class, () -> fixture.controller.acknowledge(request));
            fixture.permission = true;
            fixture.properties.setEnabled(false);
            assertThrows(IllegalArgumentException.class, () -> fixture.controller.acknowledge(request));
            assertEquals("PENDING", fixture.status("event-one"));
        }
    }
}
