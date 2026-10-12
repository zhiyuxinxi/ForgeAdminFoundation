package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.PendingToolCall;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionEngine;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Runs the original call queue; an approval covers only its first saved call. */
@Component
@RequiredArgsConstructor
public class ReactToolDispatcher {
    private final PermissionEngine permissionEngine;
    private final AgentEventPublisher eventPublisher;
    private final InterruptStore interruptStore;

    public boolean dispatch(ReactContext context, List<PendingToolCall> calls, Sinks.Many<AgentEvent> sink) {
        for (int index = 0; index < calls.size(); index++) {
            PendingToolCall call = calls.get(index);
            boolean approved = index == 0 && context.isPendingToolApproved();
            context.setPendingToolApproved(false);
            if (context.isCancelled()) {
                emit(context, sink, AgentEventType.AGENT_END, Map.of("aborted", true));
                return false;
            }
            if (!approved) {
                emit(context, sink, AgentEventType.TOOL_CALL_START,
                        Map.of("tool", call.name(), "args", JSON.parseObject(call.arguments())));
            }
            AgentTool tool = boundTool(context, call.name());
            PermissionDecision decision = tool == null ? PermissionDecision.DENY
                    : permissionEngine.decide(context, tool);
            if (decision == PermissionDecision.DENY || decision == null) {
                emit(context, sink, AgentEventType.ALL_TOOLS_DENIED, Map.of("tool", call.name()));
                context.addToolResult(call.name(), call.arguments(), "Tool execution denied");
            } else if (decision == PermissionDecision.ASK && !approved) {
                return interrupt(context, calls.subList(index, calls.size()), sink);
            } else {
                ToolResult result = execute(context, tool, call);
                if (context.isCancelled()) {
                    emit(context, sink, AgentEventType.AGENT_END, Map.of("aborted", true));
                    return false;
                }
                publishResult(context, call, result, sink);
            }
        }
        context.setPendingToolCalls(new ArrayList<>());
        return true;
    }

    private AgentTool boundTool(ReactContext context, String name) {
        List<AgentTool> matches = context.getBoundTools().stream()
                .filter(tool -> tool.getKey().equals(name)).toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean interrupt(ReactContext context, List<PendingToolCall> remaining, Sinks.Many<AgentEvent> sink) {
        context.setPendingToolCalls(new ArrayList<>(remaining));
        String id = interruptStore.save(context);
        PendingToolCall call = remaining.get(0);
        emit(context, sink, AgentEventType.REQUIRE_USER_CONFIRM,
                Map.of("interruptId", id, "tool", call.name(), "args", JSON.parseObject(call.arguments())));
        emit(context, sink, AgentEventType.AGENT_END, Map.of("interrupted", true, "interruptId", id));
        return false;
    }

    private ToolResult execute(ReactContext context, AgentTool tool, PendingToolCall call) {
        try {
            Map<String, Object> args = JSON.parseObject(call.arguments());
            ToolContext toolContext = ToolContext.of(context.getSessionId(), context.getAgentId(),
                    context.getTenantId(), context.getTurnIndex());
            if (context.getCancellation() != null) {
                toolContext.setCancellation(context.getCancellation()::registerTransport);
            }
            String knowledge = context.getAgent() == null ? null : context.getAgent().getKnowledgeIds();
            toolContext.setKnowledgeIds(knowledge == null || knowledge.isBlank()
                    ? List.of() : JSON.parseArray(knowledge, Long.class));
            return tool.execute(args, toolContext);
        } catch (Exception exception) {
            return ToolResult.error(UiMessages.label("工具执行失败", "Tool execution failed"));
        }
    }

    private void publishResult(ReactContext context, PendingToolCall call, ToolResult result, Sinks.Many<AgentEvent> sink) {
        emit(context, sink, AgentEventType.TOOL_RESULT_START,
                Map.of("tool", call.name(), "type", result.getType().name().toLowerCase(java.util.Locale.ROOT)));
        String content = result.isSuccess() ? result.getContent() : "Error: " + result.getError();
        if (content == null) {
            content = "";
        }
        emit(context, sink, result.getType() == ToolResult.Type.DATA
                ? AgentEventType.TOOL_RESULT_DATA_DELTA : AgentEventType.TOOL_RESULT_TEXT_DELTA, Map.of("content", content));
        emit(context, sink, AgentEventType.TOOL_RESULT_END, Map.of("tool", call.name()));
        context.addToolResult(call.name(), call.arguments(), content);
    }

    private void emit(ReactContext context, Sinks.Many<AgentEvent> sink, AgentEventType type, Map<String, Object> data) {
        AgentEvent event = AgentEvent.of(context.getSessionId(), context.getTurnIndex(), type, JSON.toJSONString(data));
        eventPublisher.publish(event);
        sink.tryEmitNext(event);
    }
}
