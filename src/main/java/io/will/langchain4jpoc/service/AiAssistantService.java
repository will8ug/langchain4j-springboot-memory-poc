package io.will.langchain4jpoc.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiAssistantService {
    @SystemMessage("You are a polite assistant")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    @SystemMessage("You are a polite assistant")
    Flux<String> chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);
}
