package io.will.langchain4jpoc.controller;

import io.will.langchain4jpoc.memory.mem0.Mem0ChatMemoryProvider;
import io.will.langchain4jpoc.service.AiAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class AiChatController {
    private static final Logger logger = LoggerFactory.getLogger(AiChatController.class);
    public final static String DEFAULT_MEMORY_ID = "default";

    private final AiAssistantService aiAssistantService;
    private final Mem0ChatMemoryProvider mem0ChatMemoryProvider;

    public AiChatController(AiAssistantService aiAssistantService, Mem0ChatMemoryProvider mem0ChatMemoryProvider) {
        this.aiAssistantService = aiAssistantService;
        this.mem0ChatMemoryProvider = mem0ChatMemoryProvider;
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        try {
            logger.info("Setting current query for chat request");
            mem0ChatMemoryProvider.setCurrentQuery(chatRequest.message());
            
            return Mono.fromCallable(() -> aiAssistantService.chat(DEFAULT_MEMORY_ID, chatRequest.message()))
                    .map(ChatResponse::new)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doFinally(signalType -> {
                        logger.info("Clearing current query after chat request");
                        mem0ChatMemoryProvider.clearCurrentQuery();
                    });
        } catch (Exception e) {
            mem0ChatMemoryProvider.clearCurrentQuery();
            logger.error("Error processing chat request: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/chat/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStreaming(@RequestBody ChatRequest chatRequest) {
        try {
            logger.info("Setting current query for streaming chat request");
            mem0ChatMemoryProvider.setCurrentQuery(chatRequest.message());
            
            return aiAssistantService
                    .chatStreaming(DEFAULT_MEMORY_ID, chatRequest.message())
                    .map(ChatResponse::new)
                    .doFinally(signalType -> {
                        logger.info("Clearing current query after streaming chat request");
                        mem0ChatMemoryProvider.clearCurrentQuery();
                    });
        } catch (Exception e) {
            mem0ChatMemoryProvider.clearCurrentQuery();
            logger.error("Error processing streaming chat request: {}", e.getMessage(), e);
            throw e;
        }
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String content) {}
}
