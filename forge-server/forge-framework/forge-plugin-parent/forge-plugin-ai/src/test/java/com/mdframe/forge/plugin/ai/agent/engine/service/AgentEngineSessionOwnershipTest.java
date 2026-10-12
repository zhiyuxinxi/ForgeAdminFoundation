package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.context.ContextTrimmer;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.agenttool.service.AgentToolService;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Flux;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentEngineSessionOwnershipTest {
    @Mock private ReactAgent agent;
    @Mock private AiAgentService agents;
    @Mock private AiChatSessionService sessions;
    @Mock private AiChatRecordService records;
    @Mock private AgentChatPersister persistence;
    @Mock private ContextTrimmer trimmer;
    @Mock private AgentToolService tools;
    @Mock private AiProviderMapper providers;
    @Mock private AiProviderAdapterRegistry adapters;
    @InjectMocks private AgentEngineService engine;

    @Test
    void foreignSessionMustFailBeforeHistoryRetryPersistenceOrModel() {
        when(agents.getByCode("agent")).thenReturn(definition());
        when(sessions.getOrCreate(anyString(), anyLong(), anyLong(), anyString(), any()))
                .thenReturn(session(11L));
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            ReactRequest request = request();
            request.setRetryOfRecordId(99L);
            assertThrows(BusinessException.class, () -> engine.stream(request));
            verifyNoInteractions(records, persistence, agent, adapters, providers);
        }
    }

    @Test
    void ownedSessionMustResolveBeforeHistoryAndUseTrustedCallerIdentity() {
        when(agents.getByCode("agent")).thenReturn(definition());
        when(sessions.getOrCreate(anyString(), anyLong(), anyLong(), anyString(), any()))
                .thenReturn(session(10L));
        when(records.listRecentBySession(anyString(), anyInt())).thenReturn(List.of());
        when(trimmer.trim(anyList(), anyInt())).thenReturn(List.of());
        when(tools.listEnabledByAgentId(1L)).thenReturn(List.of());
        when(providers.selectById(2L)).thenReturn(new AiProvider());
        when(adapters.createChatModel(any(), any())).thenReturn(mock(ChatModel.class));
        when(agent.execute(any(), any())).thenAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return Flux.empty();
        });
        try (var identity = mockStatic(SessionHelper.class)) {
            identity.when(SessionHelper::getUserId).thenReturn(10L);
            identity.when(SessionHelper::getTenantId).thenReturn(20L);
            engine.stream(request());
            var order = inOrder(sessions, records);
            order.verify(sessions).getOrCreate("session", 10L, 20L, "agent", "hello");
            order.verify(records).listRecentBySession(eq("session"), anyInt());
            var context = ArgumentCaptor.forClass(ReactContext.class);
            verify(agent).execute(context.capture(), any());
            assertEquals(10L, context.getValue().getUserId());
            assertEquals(20L, context.getValue().getTenantId());
        }
    }

    private ReactRequest request() {
        ReactRequest request = new ReactRequest();
        request.setAgentCode("agent");
        request.setSessionId("session");
        request.setMessage("hello");
        return request;
    }

    private AiChatSession session(Long user) {
        return AiChatSession.builder().id("session").userId(user).tenantId(20L).status("0").build();
    }

    private AiAgent definition() {
        AiAgent definition = new AiAgent();
        definition.setId(1L);
        definition.setAgentCode("agent");
        definition.setProviderId(2L);
        return definition;
    }
}
