package io.will.langchain4jpoc.memory.mem0;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component("mem0ChatMemoryProvider")
@ConditionalOnProperty(name = "chat.memory.provider", havingValue = "mem0")
public class Mem0ChatMemoryProvider implements ChatMemoryProvider {
    private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryProvider.class);
    
    private final Mem0ChatMemoryStore mem0ChatMemoryStore;
    private final int maxMessages;
    
    private final ThreadLocal<String> currentQuery = ThreadLocal.withInitial(() -> null);
    
    private final ConcurrentMap<Object, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    
    @Autowired
    public Mem0ChatMemoryProvider(Mem0ChatMemoryStore mem0ChatMemoryStore,
                                @Value("${mem0.chat.memory.max-messages:20}") int maxMessages) {
        this.mem0ChatMemoryStore = mem0ChatMemoryStore;
        this.maxMessages = maxMessages;
        logger.info("Mem0ChatMemoryProvider initialized with maxMessages: {}", maxMessages);
    }
    
    @Override
    public ChatMemory get(Object memoryId) {
        logger.info("Getting chat memory for ID: {}", memoryId);
        
        return memoryCache.computeIfAbsent(memoryId, id -> {
            logger.info("Creating new Mem0ChatMemory for ID: {}", id);
            
            Supplier<String> querySupplier = this::getCurrentQuery;
            
            return Mem0ChatMemory.builder()
                    .chatMemoryStore(mem0ChatMemoryStore)
                    .memoryId(id)
                    .querySupplier(querySupplier)
                    .maxMessages(maxMessages)
                    .build();
        });
    }
    
    public void setCurrentQuery(String query) {
        logger.debug("Setting current query: {}", query);
        currentQuery.set(query);
    }
    
    public String getCurrentQuery() {
        return currentQuery.get();
    }
    
    public void clearCurrentQuery() {
        logger.debug("Clearing current query");
        currentQuery.remove();
    }
    
    public void clearMemoryCache(Object memoryId) {
        logger.info("Clearing memory cache for ID: {}", memoryId);
        memoryCache.remove(memoryId);
    }
    
    public void clearAllMemoryCache() {
        logger.info("Clearing all memory cache");
        memoryCache.clear();
    }
    
    public int getCacheSize() {
        return memoryCache.size();
    }
}