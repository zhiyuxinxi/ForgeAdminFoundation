package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import java.util.List;

/** Data-only checkpoint. Providers, tools and file URL functions are rebuilt from current configuration. */
@Data
public class AgentContextSnapshot {
    private String agentCode;
    private Long agentId;
    private Long tenantId;
    private Long userId;
    private String sessionId;
    private int turnIndex;
    private int maxIters;
    private String toolGroupMode;
    private String systemPrompt;
    private String userMessage;
    private List<Long> imageFileIds;
    private List<String> toolCallResults;
    private String bindingFingerprint;
    private List<PendingToolCall> pendingToolCalls;
    private List<SavedMessage> history;

    public record SavedMessage(String role, String text) { }

    public static AgentContextSnapshot capture(ReactContext context) {
        AgentContextSnapshot snapshot = new AgentContextSnapshot();
        snapshot.agentCode = context.getAgentCode();
        snapshot.agentId = context.getAgentId();
        snapshot.tenantId = context.getTenantId();
        snapshot.userId = context.getUserId();
        snapshot.sessionId = context.getSessionId();
        snapshot.turnIndex = context.getTurnIndex();
        snapshot.maxIters = context.getMaxIters();
        snapshot.toolGroupMode = context.getToolGroupMode();
        snapshot.systemPrompt = context.getSystemPrompt();
        snapshot.userMessage = context.getUserMessage();
        snapshot.imageFileIds = context.getImageFileIds();
        snapshot.toolCallResults = context.getToolCallResults();
        snapshot.bindingFingerprint = context.getBindingFingerprint();
        snapshot.pendingToolCalls = context.getPendingToolCalls();
        snapshot.history = context.getHistory().stream()
                .map(message -> new SavedMessage(message.getMessageType().getValue(), message.getText())).toList();
        return snapshot;
    }

    public ReactContext restoreData() {
        ReactContext context = new ReactContext();
        context.setAgentCode(agentCode);
        context.setAgentId(agentId);
        context.setTenantId(tenantId);
        context.setUserId(userId);
        context.setSessionId(sessionId);
        context.setTurnIndex(turnIndex);
        context.setMaxIters(maxIters);
        context.setToolGroupMode(toolGroupMode);
        context.setSystemPrompt(systemPrompt);
        context.setUserMessage(userMessage);
        context.setImageFileIds(imageFileIds);
        context.setToolCallResults(toolCallResults);
        context.setBindingFingerprint(bindingFingerprint);
        context.setPendingToolCalls(pendingToolCalls);
        context.setHistory(history.stream().map(this::restoreMessage).toList());
        return context;
    }

    private Message restoreMessage(SavedMessage message) {
        return switch (message.role()) {
            case "system" -> new SystemMessage(message.text());
            case "user" -> new UserMessage(message.text());
            case "assistant" -> new AssistantMessage(message.text());
            default -> throw new IllegalArgumentException("Unsupported saved message role");
        };
    }
}
