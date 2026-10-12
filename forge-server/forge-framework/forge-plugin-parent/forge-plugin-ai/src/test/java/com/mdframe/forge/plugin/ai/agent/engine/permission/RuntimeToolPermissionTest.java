package com.mdframe.forge.plugin.ai.agent.engine.permission;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.PendingToolCall;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ReactToolDispatcher;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuntimeToolPermissionTest {
    @Test
    void savedPolicyChangesAreReadImmediatelyAndMalformedValuesDeny() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            f.policy("allowed");
            assertEquals(PermissionDecision.ALLOW, f.engine.decide(f.context, f.tool));
            f.policy("ask");
            assertEquals(PermissionDecision.ASK, f.engine.decide(f.context, f.tool));
            f.policy("denied");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            for (String malformed : new String[] {null, "allow", "unknown", "__unconfigured__", ""}) {
                f.policy(malformed);
                assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            }
        }
    }

    @Test
    void absentPolicyRetainsDefaultsButOtherTenantAndDeletedRowsCannotGrant() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            assertEquals(PermissionDecision.ALLOW, f.engine.decide(f.context, f.tool));
            assertEquals(PermissionDecision.ASK, f.engine.decide(f.context, f.risky));
            f.sql("INSERT INTO ai_agent_tool_permission VALUES (1,21,1,'delete_item','allowed',0), (2,20,1,'delete_item','allowed',2)");
            assertEquals(PermissionDecision.ASK, f.engine.decide(f.context, f.risky));
            f.sql("INSERT INTO ai_agent_tool_permission VALUES (3,20,1,'lookup','allowed',0), (4,20,1,'lookup','ask',0)");
            assertEquals(PermissionDecision.ASK, f.engine.decide(f.context, f.tool));
            f.sql("INSERT INTO ai_agent_tool_permission VALUES (5,20,1,'lookup','denied',0)");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
        }
    }

    @Test
    void persistedOwnerAndTenantAreRequiredEvenForAnAllowedPolicy() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            f.policy("allowed");
            f.context.setUserId(99L);
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            f.context.setUserId(10L);
            f.context.setTenantId(21L);
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            f.context.setTenantId(20L);
            f.sql("UPDATE ai_chat_session SET status='1'");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            f.context.setUserId(null);
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
        }
    }

    @Test
    void exactBindingAndSourceMustRemainEnabledAndUnambiguous() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            f.policy("allowed");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.other));
            f.context.setBoundTools(List.of(f.tool, f.other));
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            f.context.setBoundTools(List.of(f.tool));
            f.sql("INSERT INTO ai_agent_tool_config VALUES (20,1,0,'1','mcp','lookup')");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
            f.sql("DELETE FROM ai_agent_tool_config WHERE tool_source='mcp'");
            f.sql("UPDATE ai_agent_tool_config SET enabled='0'");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.tool));
        }
    }

    @Test
    void tenantScopeRestoresOnSuccessAndDatabaseFailure() throws Exception {
        TenantContextHolder.setTenantId(99L);
        TenantContextHolder.setIgnore(true);
        try (var f = new PermissionDatabaseFixture()) {
            assertEquals(PermissionDecision.ALLOW, f.engine.decide(f.context, f.tool));
            assertEquals(99L, TenantContextHolder.getTenantId());
            assertTrue(TenantContextHolder.isIgnore());
            f.sql("DROP TABLE ai_agent_tool_permission");
            assertThrows(RuntimeException.class, () -> f.engine.decide(f.context, f.tool));
            assertEquals(99L, TenantContextHolder.getTenantId());
            assertTrue(TenantContextHolder.isIgnore());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void dispatcherConsumesDatabaseDenyAskAndAllowWithoutRepeatingApprovalChain() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            InterruptStore interrupts = mock(InterruptStore.class);
            when(interrupts.save(any())).thenReturn("new-approval");
            var dispatcher = new ReactToolDispatcher(f.engine, mock(AgentEventPublisher.class), interrupts);
            var sink = Sinks.many().multicast().<AgentEvent>onBackpressureBuffer();
            var calls = List.of(new PendingToolCall("lookup", "{}"));
            f.policy("denied");
            f.context.setPendingToolApproved(true);
            assertTrue(dispatcher.dispatch(f.context, calls, sink));
            verify(f.tool, never()).execute(any(), any());
            f.policy("ask");
            assertFalse(dispatcher.dispatch(f.context, calls, sink));
            verify(interrupts).save(f.context);
            verify(f.tool, never()).execute(any(), any());
            f.policy("allowed");
            assertTrue(dispatcher.dispatch(f.context, calls, sink));
            verify(f.tool).execute(any(), any());
        }
    }

    @Test
    void knowledgeBindingRemainsUsableAndStillConsumesDatabaseDeny() throws Exception {
        try (var f = new PermissionDatabaseFixture()) {
            assertEquals(PermissionDecision.ALLOW, f.engine.decide(f.context, f.rag));
            f.sql("INSERT INTO ai_agent_tool_permission VALUES (1,20,1,'rag_search','denied',0)");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.rag));
            f.sql("DELETE FROM ai_agent_tool_permission");
            f.sql("UPDATE ai_agent SET rag_mode='none'");
            assertEquals(PermissionDecision.DENY, f.engine.decide(f.context, f.rag));
        }
    }
}
