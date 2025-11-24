package io.will.langchain4jpoc.memory.mem0;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component("mem0ChatMemoryProvider")
@ConditionalOnProperty(name = "chat.memory.provider", havingValue = "mem0")
public class Mem0ChatMemoryProvider implements ChatMemoryProvider {
    private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryProvider.class);
    
    private final Mem0ChatMemoryStore mem0ChatMemoryStore;

    private final ConcurrentMap<Object, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    
    public Mem0ChatMemoryProvider(Mem0ChatMemoryStore mem0ChatMemoryStore) {
        this.mem0ChatMemoryStore = mem0ChatMemoryStore;
    }
    
    @Override
    public ChatMemory get(Object memoryId) {
        logger.info("Getting chat memory for ID: {}", memoryId);
        
        return memoryCache.computeIfAbsent(memoryId, id -> {
            logger.info("Creating new Mem0ChatMemory for ID: {}", id);
            Supplier<String> querySupplier = () -> QueryContext.getQuery(id);
            
            return Mem0ChatMemory.builder()
                    .chatMemoryStore(mem0ChatMemoryStore)
                    .memoryId(id)
                    .querySupplier(querySupplier)
                    .build();
        });
    }
}