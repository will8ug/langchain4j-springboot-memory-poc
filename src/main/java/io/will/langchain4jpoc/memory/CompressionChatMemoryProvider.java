package io.will.langchain4jpoc.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.springframework.stereotype.Component;

@Component
public class CompressionChatMemoryProvider implements ChatMemoryProvider {
    private final CompressionChatMemoryStore compressionChatMemoryStore;

    public CompressionChatMemoryProvider(CompressionChatMemoryStore compressionChatMemoryStore) {
        this.compressionChatMemoryStore = compressionChatMemoryStore;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return CompressionChatMemory.builder()
                .memoryId(memoryId)
                .chatMemoryStore(compressionChatMemoryStore)
                .build();
    }
}
