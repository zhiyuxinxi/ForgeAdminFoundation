package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.session.service.AiSessionAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * ReAct Agent 入口。
 * 提供 execute（新对话）和 resume（HITL恢复）两个方法。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactAgent {

    private final ReactLoop reactLoop;
    private final InterruptStore interruptStore;
    private final AiSessionAccessService sessionAccess;

    /**
     * 执行新对话
     */
    public Flux<AgentEvent> execute(ReactContext ctx) {
        requireContextOwner(ctx);
        return reactLoop.run(ctx);
    }

    public Flux<AgentEvent> execute(ReactContext ctx, Runnable beforeStart) {
        requireContextOwner(ctx);
        return reactLoop.run(ctx, beforeStart);
    }

    /**
     * 停止指定会话
     */
    public void cancel(String sessionId) {
        sessionAccess.requireOwned(sessionId);
        ReactContext ctx = reactLoop.activeContext(sessionId);
        if (ctx != null) {
            requireContextOwner(ctx);
            reactLoop.cancel(ctx);
        }
    }

    /**
     * 恢复中断的对话（HITL）
     *
     * @param interruptId 中断ID
     * @param confirmed   用户是否确认
     */
    public Flux<AgentEvent> resume(String interruptId, boolean confirmed, Runnable beforeResume) {
        return resume(interruptId, confirmed, beforeResume, java.util.function.Function.identity());
    }

    public Flux<AgentEvent> resume(String interruptId, boolean confirmed, Runnable beforeResume,
                                  java.util.function.Function<ReactContext, ReactContext> restoreRuntime) {
        InterruptStore.InterruptState state = interruptStore.get(interruptId);
        if (state == null) {
            return Flux.just(AgentEvent.of("", 0, AgentEventType.AGENT_END,
                    "{\"error\":\"中断已过期或不存在\"}"));
        }

        // 从中断状态恢复上下文
        ReactContext ctx = state.getContext();
        requireContextOwner(ctx);
        if (confirmed) {
            ctx = restoreRuntime.apply(ctx);
            requireContextOwner(ctx);
            if (ctx.getPendingToolCalls() == null || ctx.getPendingToolCalls().isEmpty()) {
                throw new IllegalArgumentException("Interrupt checkpoint has no pending tool call");
            }
        }
        if (confirmed) {
            return resumeApproved(interruptId, state, ctx, beforeResume);
        }
        if (!interruptStore.consume(interruptId, state)) {
            return consumedInterrupt(ctx);
        }
        beforeResume.run();
        return Flux.just(AgentEvent.of(ctx.getSessionId(), ctx.getTurnIndex(),
                AgentEventType.USER_CONFIRM_RESULT, "{\"confirmed\":false}"));
    }

    private Flux<AgentEvent> resumeApproved(String interruptId, InterruptStore.InterruptState state,
                                           ReactContext approved, Runnable beforeResume) {
        try {
            return execute(approved, () -> {
                if (!interruptStore.consume(interruptId, state)) {
                    throw new com.mdframe.forge.starter.core.exception.BusinessException(409,
                            com.mdframe.forge.starter.core.i18n.UiMessages.label("中断已被消费或已过期", "Approval was consumed or expired"));
                }
                beforeResume.run();
                approved.setPendingToolApproved(true);
            });
        } catch (com.mdframe.forge.starter.core.exception.BusinessException failure) {
            // Another approved execution may hold this session after consuming the same checkpoint.
            if (Integer.valueOf(409).equals(failure.getCode()) && interruptStore.get(interruptId) == null) {
                return consumedInterrupt(approved);
            }
            throw failure;
        }
    }

    private Flux<AgentEvent> consumedInterrupt(ReactContext ctx) {
        return Flux.just(AgentEvent.of(ctx.getSessionId(), ctx.getTurnIndex(), AgentEventType.AGENT_END,
                "{\"error\":\"中断已被消费或已过期\"}"));
    }

    private void requireContextOwner(ReactContext context) {
        var session = sessionAccess.requireOwned(context.getSessionId());
        AiSessionAccessService.requireSameOwner(session.getUserId(), session.getTenantId(),
                context.getUserId(), context.getTenantId());
    }
}
