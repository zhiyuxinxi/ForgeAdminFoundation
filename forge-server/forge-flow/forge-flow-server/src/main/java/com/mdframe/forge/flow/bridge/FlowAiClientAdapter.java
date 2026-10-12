package com.mdframe.forge.flow.bridge;

import com.mdframe.forge.plugin.generator.service.AiClientAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Standalone flow server uses generator runtime CRUD services only.
 * AI generation remains an admin-server capability.
 */
@Slf4j
@Component
public class FlowAiClientAdapter implements AiClientAdapter {

    private static final String FALLBACK_REASON = "AI generation is not available in flow server";

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars) {
        return fallback(agentCode);
    }

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars,
                               Integer timeoutSeconds) {
        return fallback(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String agentCode, String message, Map<String, String> contextVars) {
        return emptyStream(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                               Map<String, String> contextVars) {
        return emptyStream(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                               Map<String, String> contextVars, Long providerId, Long modelId,
                               Double temperature, Integer maxTokens) {
        return emptyStream(agentCode);
    }

    @Override
    public String loadContextSpec(String agentCode) {
        return "";
    }

    private AiClientResult fallback(String agentCode) {
        log.warn("[FlowAiClientAdapter] Skip AI call in flow server: agentCode={}", agentCode);
        return AiClientResult.fallback(FALLBACK_REASON);
    }

    private Flux<String> emptyStream(String agentCode) {
        log.warn("[FlowAiClientAdapter] Skip AI stream in flow server: agentCode={}", agentCode);
        return Flux.empty();
    }
}
