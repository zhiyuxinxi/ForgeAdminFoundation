package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReactApprovalFlowTest {
    @Test
    void approvedExactCallMustRunBeforeNextModelRequestAndCannotReplay() {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(false);
        try (var identity = owner(10L, 20L)) {
            String id = flow.start();
            assertEquals(List.of("model"), flow.trace);
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model", "delete_item:7", "model"), flow.trace);
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(3, flow.trace.size());
            verify(flow.persistence, times(1)).openForResume(id, true);
        }
    }

    @Test
    void rejectionMustConsumeApprovalWithoutExecutingOrCallingModel() {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(false);
        try (var identity = owner(10L, 20L)) {
            String id = flow.start();
            flow.engine.resume(id, false).collectList().block(Duration.ofSeconds(5));
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model"), flow.trace);
            verify(flow.persistence).openForResume(id, false);
            verify(flow.persistence, never()).openForResume(id, true);
        }
    }

    @Test
    void approvalMustNotAuthorizeSecondPendingAskCall() {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(true);
        try (var identity = owner(10L, 20L)) {
            String first = flow.start();
            flow.engine.resume(first, true).collectList().block(Duration.ofSeconds(5));
            String second = flow.lastInterrupt();
            assertNotEquals(first, second);
            assertEquals(List.of("model", "delete_item:7"), flow.trace);
            flow.engine.resume(second, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model", "delete_item:7", "submit_other:8", "model"), flow.trace);
        }
    }

    @Test
    void changedBindingOrAgentConfigurationMustRejectBeforeConsumingOrPersisting() {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(false);
        try (var identity = owner(10L, 20L)) {
            String id = flow.start();
            flow.definition.setKnowledgeIds("[999]");
            assertThrows(BusinessException.class, () -> flow.engine.resume(id, true));
            assertEquals(List.of("model"), flow.trace);
            assertNotNull(flow.redis.store.get(id));
            verifyNoInteractions(flow.persistence);
        }
    }

    @Test
    void foreignIdentityAndRevokedDecisionMustNotExecuteApprovedTool() {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(false);
        String id;
        try (var identity = owner(10L, 20L)) {
            id = flow.start();
        }
        try (var identity = owner(11L, 20L)) {
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model"), flow.trace);
            verifyNoInteractions(flow.persistence);
        }
        try (var identity = owner(10L, 21L)) {
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model"), flow.trace);
        }
        when(flow.permissions.decide(any(), any())).thenReturn(PermissionDecision.DENY);
        try (var identity = owner(10L, 20L)) {
            flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
            assertEquals(List.of("model", "model"), flow.trace);
        }
    }

    private MockedStatic<SessionHelper> owner(Long user, Long tenant) {
        MockedStatic<SessionHelper> identity = mockStatic(SessionHelper.class);
        identity.when(SessionHelper::getUserId).thenReturn(user);
        identity.when(SessionHelper::getTenantId).thenReturn(tenant);
        return identity;
    }

    @Test
    void concurrentApprovalRequestsMustExecuteTheOriginalToolOnlyOnce() throws Exception {
        ApprovalFlowFixture flow = new ApprovalFlowFixture(false);
        String id;
        try (var identity = owner(10L, 20L)) {
            id = flow.start();
        }
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Void> resume = () -> {
                try (var identity = owner(10L, 20L)) {
                    flow.engine.resume(id, true).collectList().block(Duration.ofSeconds(5));
                }
                return null;
            };
            for (var result : workers.invokeAll(List.of(resume, resume))) {
                result.get();
            }
        } finally {
            workers.shutdownNow();
        }
        assertEquals(1, flow.trace.stream().filter("delete_item:7"::equals).count());
        verify(flow.persistence, times(1)).openForResume(id, true);
    }
}
