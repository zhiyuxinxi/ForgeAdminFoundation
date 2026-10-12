package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.event.FlowEventDeliveryRecorder;
import com.mdframe.forge.starter.flow.event.FlowEventPublisher;
import com.mdframe.forge.starter.flow.event.FlowTaskNotifyEvent;
import com.mdframe.forge.starter.flow.event.FlowWebhookNotifier;
import com.mdframe.forge.starter.flow.listener.FlowTaskNotifyListener;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FlowEventLegacyPublishTest {
    @Test
    void boundTerminalUsesOutboxWhileUnboundAndNonterminalKeepExistingRoute() throws Exception {
        try (var flow = new FlowEventAckFixture()) {
            var listener = new FlowTaskNotifyListener();
            var models = mock(FlowModelMapper.class);
            var publisher = mock(FlowEventPublisher.class);
            var webhook = mock(FlowWebhookNotifier.class);
            ReflectionTestUtils.setField(listener, "flowModelMapper", models);
            ReflectionTestUtils.setField(listener, "flowEventPublisher", publisher);
            ReflectionTestUtils.setField(listener, "flowWebhookNotifier", webhook);
            ReflectionTestUtils.setField(listener, "flowEventDeliveryRecorder",
                    new FlowEventDeliveryRecorder(flow.properties, flow.mapper));
            flow.pending("bound-event", "business-admin");
            flow.properties.setEnabled(false);
            var bound = message("bound-event", "PROCESS_COMPLETED");
            TenantContextHolder.executeWithTenant(98L, () -> {
                listener.onNotifyEvent(FlowTaskNotifyEvent.eventPublish(bound, "changed-model"));
                assertEquals(98L, TenantContextHolder.getTenantId());
            });
            verifyNoInteractions(models, publisher, webhook);

            var model = new FlowModel();
            model.setNotifyType("redis");
            when(models.selectByModelKey("approval")).thenReturn(model);
            var unbound = message("unbound-event", "PROCESS_COMPLETED");
            var nonterminal = message(null, "TASK_CREATED");
            listener.onNotifyEvent(FlowTaskNotifyEvent.eventPublish(unbound, "approval"));
            listener.onNotifyEvent(FlowTaskNotifyEvent.eventPublish(nonterminal, "approval"));
            verify(publisher).publish(unbound);
            verify(publisher).publish(nonterminal);
            verifyNoInteractions(webhook);
        }
    }

    private FlowEventMessage message(String eventId, String eventType) {
        var message = new FlowEventMessage();
        message.setEventId(eventId);
        message.setEventType(eventType);
        message.setTenantId("97");
        return message;
    }
}
