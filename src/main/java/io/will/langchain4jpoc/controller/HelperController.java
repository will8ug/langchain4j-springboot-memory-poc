package io.will.langchain4jpoc.controller;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class HelperController {
    private final ChatMemoryStore chatMemoryStore;

    public HelperController(ChatMemoryStore chatMemoryStore) {
        this.chatMemoryStore = chatMemoryStore;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP", "service", "LangChain4j PoC"));
    }

    @GetMapping(value = "/memory/{memoryId}")
    public Flux<List<ChatMessage>> memory(@PathVariable("memoryId") String memoryId) {
        if (memoryId == null) {
            memoryId = AiChatController.DEFAULT_MEMORY_ID;
        }
        List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);
        return Flux.just(messages);
    }
}
