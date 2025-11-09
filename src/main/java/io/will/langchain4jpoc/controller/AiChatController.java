package io.will.langchain4jpoc.controller;

import io.will.langchain4jpoc.service.AiAssistantService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
public class AiChatController {
    private final String DEFAULT_MEMORY_ID = "default";

    private final AiAssistantService aiAssistantService;

    public AiChatController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP", "service", "LangChain4j PoC"));
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        return Mono.fromCallable(() -> aiAssistantService.chat(DEFAULT_MEMORY_ID, chatRequest.message()))
                .map(ChatResponse::new)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(value = "/chat/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStreaming(@RequestBody ChatRequest chatRequest) {
        return aiAssistantService
                .chatStreaming(DEFAULT_MEMORY_ID, chatRequest.message())
                .map(ChatResponse::new);
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String content) {}
}
