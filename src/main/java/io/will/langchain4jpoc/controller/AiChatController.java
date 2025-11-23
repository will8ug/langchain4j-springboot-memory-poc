package io.will.langchain4jpoc.controller;

import io.will.langchain4jpoc.memory.mem0.QueryContext;
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

    public AiChatController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ChatResponse> chat(@RequestBody ChatRequest chatRequest) {
        String query = chatRequest.message();
        logger.info("Processing chat request with query: {}", query);
        
        // Set query in Reactor Context first, then propagate to ThreadLocal on execution thread
        return Mono.deferContextual(ctx -> {
                    return Mono.fromCallable(() -> {
                        // Propagate context to ThreadLocal on the execution thread (after subscribeOn)
                        QueryContext.propagateFromContext(ctx);
                        // Also set directly as a fallback
                        QueryContext.setQuery(DEFAULT_MEMORY_ID, query);
                        
                        try {
                            return aiAssistantService.chat(DEFAULT_MEMORY_ID, chatRequest.message());
                        } finally {
                            // Clean up ThreadLocal after execution
                            QueryContext.clearQuery(DEFAULT_MEMORY_ID);
                        }
                    });
                })
                .map(ChatResponse::new)
                .contextWrite(QueryContext.createContext(DEFAULT_MEMORY_ID, query))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> logger.error("Error processing chat request: {}", e.getMessage(), e))
                .doFinally(signalType -> {
                    logger.info("Clearing query for memory ID after chat request");
                    QueryContext.clearQuery(DEFAULT_MEMORY_ID);
                });
    }

    @PostMapping(value = "/chat/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStreaming(@RequestBody ChatRequest chatRequest) {
        String query = chatRequest.message();
        logger.info("Processing streaming chat request with query: {}", query);
        
        // Set query in Reactor Context and propagate to ThreadLocal
        return aiAssistantService.chatStreaming(DEFAULT_MEMORY_ID, query)
                .contextWrite(QueryContext.createContext(DEFAULT_MEMORY_ID, query))
                .transform(QueryContext::propagateContext)
                .doOnSubscribe(subscription -> {
                    // Ensure query is available in ThreadLocal when subscription starts
                    QueryContext.setQuery(DEFAULT_MEMORY_ID, query);
                })
                .map(ChatResponse::new)
                .doOnError(e -> logger.error("Error processing streaming chat request: {}", e.getMessage(), e))
                .doFinally(signalType -> {
                    logger.info("Clearing query for memory ID after streaming chat request");
                    QueryContext.clearQuery(DEFAULT_MEMORY_ID);
                });
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String content) {}
}
