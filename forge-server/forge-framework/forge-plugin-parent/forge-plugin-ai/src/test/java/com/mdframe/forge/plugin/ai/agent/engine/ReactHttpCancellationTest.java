package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.PendingToolCall;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionEngine;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ReactToolDispatcher;
import com.mdframe.forge.plugin.ai.agent.engine.tool.builtin.HttpTool;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import com.mdframe.forge.starter.outbound.client.OkHttpSecureOutboundClient;
import com.mdframe.forge.starter.outbound.config.OutboundProperties;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundPolicyService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReactHttpCancellationTest {
    @Test
    void ownedStopCancelsTheActualBoundHttpCallBeforeItsTenSecondTimeout() throws Exception {
        try (var server = new MockWebServer(); var pool = new ReactExecutionPool(1, 15000)) {
            server.start();
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            var uri = server.url("/blocked").uri();
            OutboundPolicyService policy = mock(OutboundPolicyService.class);
            when(policy.validate(any())).thenReturn(new ValidatedOutboundTarget(OutboundScenes.AI_HTTP_TOOL,
                    uri, uri.getHost(), uri.getPort(), List.of(InetAddress.getByName("127.0.0.1")),
                    IpAddressClassifier.AddressType.PUBLIC));
            HttpTool http = new HttpTool(new OkHttpSecureOutboundClient(policy, new OutboundProperties()));
            PermissionEngine permissions = mock(PermissionEngine.class);
            when(permissions.decide(any(), any())).thenReturn(PermissionDecision.ALLOW);
            AgentEventPublisher publisher = mock(AgentEventPublisher.class);
            InterruptStore interrupts = mock(InterruptStore.class);
            ReactLoop loop = new ReactLoop(mock(AiAgentService.class), publisher,
                    new ReactToolDispatcher(permissions, publisher, interrupts), mock(AiModelInvocationRecorder.class), pool);
            AiSessionAccessService access = mock(AiSessionAccessService.class);
            when(access.requireOwned("http")).thenReturn(AiChatSession.builder().id("http").userId(10L).tenantId(20L).build());
            ReactAgent agent = new ReactAgent(loop, interrupts, access);
            ReactContext context = new ReactContext();
            context.setSessionId("http");
            context.setUserId(10L);
            context.setTenantId(20L);
            context.setBoundTools(List.of(http));
            context.setPendingToolCalls(List.of(new PendingToolCall("http_request", "{\"url\":\"" + uri + "\"}")));
            var result = agent.execute(context).collectList().toFuture();
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS));
            agent.cancel("http");
            assertTrue(result.get(2, TimeUnit.SECONDS).stream().anyMatch(e -> "{\"aborted\":true}".equals(e.getData())));
            assertNull(pool.activeContext("http"));
        }
    }
}
