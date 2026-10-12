package com.mdframe.forge.starter.flow.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.flow.event.FlowTaskNotifyEvent;
import com.mdframe.forge.starter.flow.event.FlowTerminalEventPersistenceException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class FlowTerminalEventOutboxIntegrationTest {
    private static FlowTerminalOutboxFixture value;

    @BeforeAll
    static void startDatabase() throws Exception {
        value = new FlowTerminalOutboxFixture();
    }

    @AfterAll
    static void closeDatabase() {
        if (value != null) {
            value.close();
        }
    }

    @ParameterizedTest
    @CsvSource({"approve,PROCESS_COMPLETED,approved", "reject,PROCESS_REJECTED,rejected", "canceled,PROCESS_CANCELED,canceled"})
    void flowableTerminalCommitsWithDurableEvent(String outcome, String type, String status) throws Exception {
        String instance = value.start("terminal-" + outcome, null);
        value.terminate(instance, outcome);
        var entry = value.newReader().selectRecorded(FlowTerminalOutboxFixture.TENANT, value.recordedId(instance));
        assertEquals(type, entry.eventType());
        assertEquals(instance, entry.processInstanceId());
        assertEquals("redis", entry.destinationType());
        assertEquals("flow:event:all", entry.destination());
        assertEquals(entry.eventId(), new ObjectMapper().readTree(entry.payload()).get("eventId").asText());
        assertNull(value.outbox.selectRecorded(98L, entry.eventId()));
        assertEquals(status, businessStatus(instance));
        assertEquals(0, value.runtime.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).count());
    }

    @Test
    void callerRollbackDiscardsTheTerminalAndItsEvent() {
        String instance = value.start("caller-rollback", null);
        assertThrows(IllegalStateException.class, () -> value.runtime.transactions.executeWithoutResult(status -> {
            value.terminate(instance, "approve");
            throw new IllegalStateException("caller rejected transaction");
        }));
        assertActiveWithoutEvent(instance);
    }

    @Test
    void databaseInsertFailureRollsBackRealFlowableAndBusinessState() {
        String instance = value.start("insert-failure", null);
        value.runtime.jdbc.execute("ALTER TABLE sys_flow_event_outbox ADD CONSTRAINT b08_insert_failure "
                + "CHECK (process_instance_id <> '" + instance + "')");
        try {
            assertThrows(RuntimeException.class, () -> value.terminate(instance, "approve"));
            assertActiveWithoutEvent(instance);
        } finally {
            value.runtime.jdbc.execute("ALTER TABLE sys_flow_event_outbox DROP CONSTRAINT b08_insert_failure");
        }
    }

    @Test
    void snapshotFailureEscapesFlowableListenerCatchBlocks() {
        String instance = value.start("snapshot-failure", null);
        when(value.models.selectByModelKey(anyString())).thenThrow(new DataAccessResourceFailureException("model unavailable"));
        try {
            assertThrows(FlowTerminalEventPersistenceException.class, () -> value.terminate(instance, "canceled"));
            assertActiveWithoutEvent(instance);
        } finally {
            doReturn(value.model).when(value.models).selectByModelKey(anyString());
        }
    }

    @Test
    void rejectedAndRestartedRoundsKeepIndependentEventIdentities() {
        String first = value.start("same-business-rounds", null);
        value.terminate(first, "reject");
        String second = value.start("same-business-rounds", first);
        value.terminate(second, "approve");
        assertNotEquals(value.recordedId(first), value.recordedId(second));
        assertEquals("rejected", businessStatus(first));
        assertEquals("approved", businessStatus(second));
    }

    @Test
    void snapshotPreservesOriginalPayloadTargetAndTenantAcrossReaders() throws Exception {
        FlowEventMessage message = message("payload-snapshot", FlowEventMessage.PROCESS_COMPLETED);
        Map<String, Object> variables = new HashMap<>(Map.of("quantity", 3));
        message.setVariables(variables);
        value.model.setNotifyType("webhook");
        value.model.setWebhookUrl("https://flow-consumer.example/events");
        TenantContextHolder.setTenantId(98L);
        try {
            value.runtime.transactions.executeWithoutResult(status -> {
                value.context.publishEvent(FlowTaskNotifyEvent.eventPublish(message, "approval"));
                variables.put("quantity", 100);
                value.model.setWebhookUrl("https://changed.example/events");
            });
            assertEquals(98L, TenantContextHolder.getTenantId());
        } finally {
            TenantContextHolder.clear();
            value.model.setNotifyType("redis");
        }
        var entry = value.newReader().selectRecorded(FlowTerminalOutboxFixture.TENANT, message.getEventId());
        assertEquals("https://flow-consumer.example/events", entry.destination());
        assertEquals(3, new ObjectMapper().readTree(entry.payload()).get("variables").get("quantity").asInt());
    }

    @Test
    void missingTransactionRejectsTerminalButPreservesNonterminalNotifications() {
        var terminal = FlowTaskNotifyEvent.eventPublish(message("without-transaction", FlowEventMessage.PROCESS_CANCELED), "approval");
        assertThrows(FlowTerminalEventPersistenceException.class, () -> value.context.publishEvent(terminal));
        FlowEventMessage task = message("nonterminal", FlowEventMessage.TASK_COMPLETED);
        assertDoesNotThrow(() -> value.context.publishEvent(FlowTaskNotifyEvent.eventPublish(task, "approval")));
        assertNull(task.getEventId());
    }

    private FlowEventMessage message(String instance, String type) {
        FlowEventMessage message = FlowEventMessage.ofProcess(type, instance, "approval", "business-key", "test", "Title", "123", "Applicant");
        message.setTenantId(String.valueOf(FlowTerminalOutboxFixture.TENANT));
        return message;
    }

    private String businessStatus(String instance) {
        return value.runtime.jdbc.queryForObject("SELECT status FROM sys_flow_business WHERE process_instance_id=?", String.class, instance);
    }

    private void assertActiveWithoutEvent(String instance) {
        assertEquals("running", businessStatus(instance));
        assertEquals(1, value.runtime.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).count());
        assertEquals(1, value.runtime.engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertEquals(0, value.runtime.jdbc.queryForObject("SELECT COUNT(*) FROM sys_flow_event_outbox WHERE process_instance_id=?", Integer.class, instance));
    }
}
