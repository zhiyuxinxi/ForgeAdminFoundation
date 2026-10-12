package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP 请求内置工具
 */
@Component
@RequiredArgsConstructor
public class HttpTool implements AgentTool {

    private final SecureOutboundClient outboundClient;

    @Override
    public String getKey() {
        return "http_request";
    }

    @Override
    public String getDescription() {
        return "发送HTTP请求获取外部API数据。支持GET/POST方法。";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
          "type": "object",
          "properties": {
            "url": {
              "type": "string",
              "description": "请求URL"
            },
            "method": {
              "type": "string",
              "enum": ["GET", "POST"],
              "description": "HTTP方法（默认GET）"
            },
            "headers": {
              "type": "object",
              "description": "请求头"
            },
            "body": {
              "type": "string",
              "description": "请求体（POST时使用）"
            }
          },
          "required": ["url"]
        }
        """;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            OutboundRequest request = buildRequest(args);
            OutboundResponse response = context != null && context.getCancellation() != null
                    ? outboundClient.execute(request, context.getCancellation()) : outboundClient.execute(request);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                return ToolResult.error(UiMessages.label("HTTP请求返回失败状态", "HTTP request returned an unsuccessful status")
                        + ": " + response.getStatusCode());
            }
            return ToolResult.data(response.bodyAsUtf8());
        } catch (IllegalArgumentException exception) {
            return ToolResult.error(UiMessages.label("HTTP请求参数不合法", "Invalid HTTP request arguments"));
        } catch (Exception exception) {
            return ToolResult.error(UiMessages.label("HTTP请求失败，请检查出站白名单或连接配置",
                    "HTTP request failed; check the outbound allowlist or connection settings"));
        }
    }

    private OutboundRequest buildRequest(Map<String, Object> args) {
        if (args == null || !(args.get("url") instanceof String url) || url.isBlank()) {
            throw new IllegalArgumentException("url");
        }
        Object suppliedMethod = args.getOrDefault("method", "GET");
        if (!(suppliedMethod instanceof String method)) {
            throw new IllegalArgumentException("method");
        }
        method = method.trim().toUpperCase(Locale.ROOT);
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("method");
        }
        Object body = args.getOrDefault("body", "");
        if (!(body instanceof String)) {
            throw new IllegalArgumentException("body");
        }
        return OutboundRequest.builder().scene(OutboundScenes.AI_HTTP_TOOL).url(url).method(method)
                .headers(parseHeaders(args.get("headers")))
                .body("POST".equals(method) ? ((String) body).getBytes(StandardCharsets.UTF_8) : new byte[0])
                .connectTimeout(Duration.ofSeconds(3)).readTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(5)).callTimeout(Duration.ofSeconds(10)).build();
    }

    private Map<String, String> parseHeaders(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> headers)) {
            throw new IllegalArgumentException("headers");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : headers.entrySet()) {
            if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof String headerValue)) {
                throw new IllegalArgumentException("headers");
            }
            result.put(name, headerValue);
        }
        return result;
    }
}
