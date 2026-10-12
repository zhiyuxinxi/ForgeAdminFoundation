package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class AgentRuntimeFingerprint {
    private AgentRuntimeFingerprint() { }

    public static String capture(ReactContext context, AgentToolRegistry registry) {
        List<String> tools = context.getBoundTools().stream().map(tool ->
                JSON.toJSONString(List.of(registry.getQualifiedKey(tool), tool.getParametersSchema(),
                        org.springframework.aop.support.AopUtils.getTargetClass(tool).getName())))
                .sorted().toList();
        String source = JSON.toJSONString(context.getAgent()) + JSON.toJSONString(tools);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
