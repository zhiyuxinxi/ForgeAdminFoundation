package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import com.mdframe.forge.starter.outbound.security.OutboundSecurityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpToolTest {
    private final SecureOutboundClient client = mock(SecureOutboundClient.class);
    private final HttpTool tool = new HttpTool(client);

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void springConsumerMustUseControlledGetWithFixedSceneAndBoundedTimeouts() {
        when(client.execute(any())).thenReturn(response(200, "accepted"));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SecureOutboundClient.class, () -> client);
            context.register(HttpTool.class);
            context.refresh();
            var result = context.getBean(HttpTool.class).execute(Map.of("url", "https://allowed.example/data",
                    "scene", "FLOW_API", "callTimeout", 999999), null);
            assertTrue(result.isSuccess());
            assertEquals("accepted", result.getContent());
        }
        OutboundRequest request = captured();
        assertEquals(OutboundScenes.AI_HTTP_TOOL, request.getScene());
        assertEquals("GET", request.getMethod());
        assertEquals(Duration.ofSeconds(10), request.getCallTimeout());
        assertEquals(Duration.ofSeconds(3), request.getConnectTimeout());
        assertEquals(Duration.ofSeconds(5), request.getReadTimeout());
        assertEquals(0, request.getBody().length);
    }

    @Test
    void postMustPreserveUtf8BodyAndExplicitHeaders() {
        when(client.execute(any())).thenReturn(response(201, "created"));
        var result = tool.execute(Map.of("url", "https://allowed.example/data", "method", "post",
                "body", "中文", "headers", Map.of("Content-Type", "text/plain; charset=utf-8", "X-Request", "a")), null);
        assertTrue(result.isSuccess());
        OutboundRequest request = captured();
        assertEquals("POST", request.getMethod());
        assertEquals("中文", new String(request.getBody(), StandardCharsets.UTF_8));
        assertEquals("a", request.getHeaders().get("X-Request"));
    }

    @Test
    void invalidArgumentsMustNotReachClient() {
        List<Map<String, Object>> invalid = List.of(Map.of(), Map.of("url", ""),
                Map.of("url", "https://allowed.example", "method", "DELETE"),
                Map.of("url", "https://allowed.example", "headers", Map.of("X-Request", 1)));
        for (Map<String, Object> args : invalid) {
            assertFalse(tool.execute(args, null).isSuccess());
        }
        assertFalse(tool.execute(null, null).isSuccess());
        verifyNoInteractions(client);
    }

    @Test
    void policyAndTimeoutErrorsMustNotExposeRawDetailsOrFallBack() {
        when(client.execute(any())).thenThrow(new OutboundSecurityException("private-url-token"));
        LocaleContextHolder.setLocale(Locale.US);
        var result = tool.execute(Map.of("url", "https://allowed.example"), null);
        assertFalse(result.isSuccess());
        assertEquals("HTTP request failed; check the outbound allowlist or connection settings", result.getError());
        verify(client, times(1)).execute(any());
    }

    @Test
    void unsuccessfulStatusMustNotReturnResponseBodyAsData() {
        when(client.execute(any())).thenReturn(response(403, "private-error-body"));
        var result = tool.execute(Map.of("url", "https://allowed.example"), null);
        assertFalse(result.isSuccess());
        assertNull(result.getContent());
        assertTrue(result.getError().endsWith("403"));
        assertFalse(result.getError().contains("private-error-body"));
    }

    private OutboundRequest captured() {
        var capture = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(client).execute(capture.capture());
        return capture.getValue();
    }

    private OutboundResponse response(int status, String value) {
        return new OutboundResponse(status, Map.of(), value.getBytes(StandardCharsets.UTF_8));
    }
}
