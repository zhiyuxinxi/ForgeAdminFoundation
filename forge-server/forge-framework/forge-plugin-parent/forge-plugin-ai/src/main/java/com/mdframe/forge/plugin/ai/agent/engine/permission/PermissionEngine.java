package com.mdframe.forge.plugin.ai.agent.engine.permission;

import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import java.util.Set;

/**
 * 工具权限引擎。
 * 决定工具调用的权限：ALLOW/ASK/DENY。
 *
 * 规则：
 * 1. 查 ai_agent_tool_permission 配置 → 有配置按配置
 * 2. 无配置 → 默认 ALLOW
 * 3. 风险工具（删除/提交/取消等关键词）建议 ASK 由配置决定
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionEngine {

    private final AgentToolPermissionService permissionService;
    private final AgentToolRegistry toolRegistry;

    /**
     * 风险工具关键词（名称包含这些词的工具默认需要确认）
     */
    private static final Set<String> RISKY_KEYWORDS = Set.of(
            "delete", "remove", "drop", "cancel", "submit", "commit",
            "删除", "移除", "取消", "提交"
    );

    /** A unique bound runtime tool is required before consulting its persisted policy. */
    public PermissionDecision decide(ReactContext context, AgentTool tool) {
        if (tool == null || context.getBoundTools() == null
                || context.getBoundTools().stream().noneMatch(bound -> bound == tool)
                || context.getBoundTools().stream().filter(bound -> bound.getKey().equals(tool.getKey())).count() != 1) {
            return PermissionDecision.DENY;
        }
        String qualified = toolRegistry.getQualifiedKey(tool);
        String source = qualified.substring(0, qualified.indexOf(':'));
        PermissionDecision configured = permissionService.getDecision(context, source, tool.getKey());
        if (configured != null) {
            return configured;
        }
        return isRiskyTool(tool.getKey()) ? PermissionDecision.ASK : PermissionDecision.ALLOW;
    }

    private boolean isRiskyTool(String toolKey) {
        String lower = toolKey.toLowerCase(java.util.Locale.ROOT);
        return RISKY_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
