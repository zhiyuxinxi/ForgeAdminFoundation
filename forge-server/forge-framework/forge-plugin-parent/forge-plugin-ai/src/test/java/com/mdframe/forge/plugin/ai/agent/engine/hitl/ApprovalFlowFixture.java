package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactLoop;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionEngine;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import com.mdframe.forge.plugin.ai.agent.engine.service.AgentEngineService;
import com.mdframe.forge.plugin.ai.agent.engine.tool.*;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolConfig;
import com.mdframe.forge.plugin.ai.agenttool.service.AgentToolService;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class ApprovalFlowFixture {
    final ApprovalRedis redis = new ApprovalRedis();
    final AiAgent definition = new AiAgent();
    final PermissionEngine permissions = mock(PermissionEngine.class);
    final AgentToolService bindings = mock(AgentToolService.class);
    final AgentChatPersister persistence = mock(AgentChatPersister.class);
    final List<String> trace = new CopyOnWriteArrayList<>();
    final List<AgentEvent> events = new CopyOnWriteArrayList<>();
    final AtomicInteger modelCalls = new AtomicInteger();
    final AgentEngineService engine;
    final ReactAgent agent;
    final ReactContext initial;

    ApprovalFlowFixture(boolean twoCalls) {
        AgentTool first = tool("delete_item");
        AgentTool second = tool("submit_other");
        List<AgentTool> bound = twoCalls ? List.of(first, second) : List.of(first);
        AgentToolContributor contributor = mock(AgentToolContributor.class);
        when(contributor.getSource()).thenReturn("builtin");
        when(contributor.contribute()).thenReturn(bound);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(contributor));
        List<AiAgentToolConfig> configured = bound.stream().map(this::binding).toList();
        when(bindings.listEnabledByAgentId(1L)).thenReturn(configured);
        when(permissions.decide(any(), any())).thenReturn(PermissionDecision.ASK);
        AgentEventPublisher publisher = mock(AgentEventPublisher.class);
        doAnswer(call -> {
            events.add(call.getArgument(0));
            return null;
        }).when(publisher).publish(any());
        AiAgentService definitions = mock(AiAgentService.class);
        definition.setId(1L);
        definition.setAgentCode("agent");
        definition.setTenantId(20L);
        definition.setProviderId(2L);
        definition.setMaxIters(3);
        when(definitions.getByCode("agent")).thenReturn(definition);
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            trace.add("model");
            return Flux.just(response(modelCalls.getAndIncrement() == 0, twoCalls));
        });
        AiProviderAdapterRegistry adapters = mock(AiProviderAdapterRegistry.class);
        when(adapters.createChatModel(any(), any())).thenReturn(model);
        AiProviderMapper providers = mock(AiProviderMapper.class);
        when(providers.selectById(2L)).thenReturn(new AiProvider());
        AiChatSessionMapper sessions = mock(AiChatSessionMapper.class);
        when(sessions.selectById("session")).thenReturn(AiChatSession.builder().id("session")
                .userId(10L).tenantId(20L).status("0").build());
        ReactToolDispatcher dispatcher = new ReactToolDispatcher(permissions, publisher, redis.store);
        ReactLoop loop = new ReactLoop(definitions, publisher, dispatcher, mock(AiModelInvocationRecorder.class),
                new com.mdframe.forge.plugin.ai.agent.engine.ReactExecutionPool(2, 5000));
        agent = new ReactAgent(loop, redis.store, new AiSessionAccessService(sessions));
        engine = new AgentEngineService(agent, definitions, adapters, providers, null, null,
                persistence, null, null, bindings, registry, null);
        initial = InterruptCheckpointTest.context();
        initial.setAgentId(1L);
        initial.setAgentCode("agent");
        initial.setAgent(definition);
        initial.setMaxIters(3);
        initial.setUserMessage("perform the requested action");
        initial.setChatModel(model);
        initial.setBoundTools(bound);
        initial.setPendingToolCalls(new ArrayList<>());
        initial.setBindingFingerprint(AgentRuntimeFingerprint.capture(initial, registry));
    }

    String start() {
        agent.execute(initial).collectList().block(Duration.ofSeconds(5));
        return lastInterrupt();
    }

    String lastInterrupt() {
        return events.stream().filter(event -> event.getEventType() == AgentEventType.REQUIRE_USER_CONFIRM)
                .reduce((left, right) -> right).map(event -> com.alibaba.fastjson2.JSON.parseObject(event.getData())
                        .getString("interruptId")).orElseThrow();
    }

    private AgentTool tool(String name) {
        AgentTool tool = mock(AgentTool.class);
        when(tool.getKey()).thenReturn(name);
        when(tool.getDescription()).thenReturn("A test action");
        when(tool.getParametersSchema()).thenReturn("{\"type\":\"object\"}");
        when(tool.execute(anyMap(), any())).thenAnswer(call -> {
            Map<String, Object> arguments = call.getArgument(0);
            trace.add(name + ":" + arguments.get("id"));
            return ToolResult.text("completed");
        });
        return tool;
    }

    private AiAgentToolConfig binding(AgentTool tool) {
        AiAgentToolConfig config = new AiAgentToolConfig();
        config.setToolSource("builtin");
        config.setToolKey(tool.getKey());
        return config;
    }

    private ChatResponse response(boolean calls, boolean twoCalls) {
        List<AssistantMessage.ToolCall> pending = new ArrayList<>();
        if (calls) {
            pending.add(new AssistantMessage.ToolCall("first", "function", "delete_item", "{\"id\":7}"));
            if (twoCalls) {
                pending.add(new AssistantMessage.ToolCall("second", "function", "submit_other", "{\"id\":8}"));
            }
        }
        AssistantMessage message = AssistantMessage.builder().content(calls ? "before action" : "done")
                .toolCalls(pending).build();
        return new ChatResponse(List.of(new Generation(message)));
    }
}
