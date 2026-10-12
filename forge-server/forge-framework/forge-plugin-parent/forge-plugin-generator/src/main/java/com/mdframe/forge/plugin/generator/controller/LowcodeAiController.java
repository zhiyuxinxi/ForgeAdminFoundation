package com.mdframe.forge.plugin.generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAppGenerateRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAppGenerateResult;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeAiGenerateService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 低代码 AI 生成接口。SSE 接口不挂 ApiEncrypt，避免流式响应被包装。
 */
@Slf4j
@RestController
@RequestMapping("/ai/lowcode")
@RequiredArgsConstructor
public class LowcodeAiController {

    private final LowcodeAiGenerateService generateService;
    private final ObjectMapper objectMapper;

    @PostMapping("/app/ai/generate")
    public RespInfo<LowcodeAiAppGenerateResult> generateApp(@RequestBody LowcodeAiAppGenerateRequest request) {
        return RespInfo.success(generateService.generateAppDraft(request));
    }

    @PostMapping(value = "/app/ai/stream-generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamGenerateApp(@RequestBody LowcodeAiAppGenerateRequest request) {
        return generateService.streamGenerateApp(request);
    }

    @PostMapping(value = "/app/{id}/ai/refine", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> refineApp(@PathVariable Long id,
                                                   @RequestBody LowcodeAiAppGenerateRequest request) {
        return Flux.defer(() -> {
            try {
                LowcodeAiAppGenerateResult result = generateService.refineApp(id, request);
                return Flux.just(
                        event("result", result),
                        event("complete", Map.of("message", "优化建议生成完成"))
                );
            } catch (Exception e) {
                return Flux.just(event("error", Map.of("message", e.getMessage())));
            }
        });
    }

    @PostMapping(value = "/model/ai/stream-generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamGenerateModel(@RequestBody LowcodeAiAppGenerateRequest request) {
        return generateService.streamGenerateApp(request);
    }

    private ServerSentEvent<String> event(String event, Object value) {
        try {
            return ServerSentEvent.builder(objectMapper.writeValueAsString(value))
                    .event(event)
                    .build();
        } catch (Exception e) {
            log.warn("[LowcodeAiController] serialize refine result failed", e);
            return ServerSentEvent.builder("{\"message\":\"事件序列化失败\"}")
                    .event("error")
                    .build();
        }
    }
}
