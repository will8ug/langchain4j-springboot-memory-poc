package io.will.langchain4jpoc.memory.mem0;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "chat.memory.provider", havingValue = "mem0")
public class Mem0ChatMemoryStore implements ChatMemoryStore {
    private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemoryStore.class);
    
    private final Mem0Client mem0Client;
    private final String appId;
    private final int topK;
    
    private final Map<Object, List<ChatMessage>> localCache = new ConcurrentHashMap<>();
    
    public Mem0ChatMemoryStore(Mem0Client mem0Client,
                             @Value("${mem0.app.id:langchain4j-springboot-poc}") String appId,
                             @Value("${mem0.top.k:3}") int topK) {
        this.mem0Client = mem0Client;
        this.appId = appId;
        this.topK = topK;
    }
    
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        logger.info("Updating messages for memory ID: {}", memoryId);
        
        if (messages.isEmpty()) {
            logger.warn("No messages to update for memory ID: {}", memoryId);
            return;
        }
        
        try {
            String userId = memoryId.toString();
            
            List<Mem0Client.Message> mem0Messages = messages.stream()
                    .filter(msg -> msg.type() == ChatMessageType.USER || msg.type() == ChatMessageType.AI)
                    .map(msg -> {
                        String role = msg.type() == ChatMessageType.USER ? "user" : "assistant";
                        String content;
                        
                        if (msg.type() == ChatMessageType.USER) {
                            content = ((UserMessage) msg).singleText();
                        } else {
                            content = ((AiMessage) msg).text();
                        }
                        
                        return new Mem0Client.Message(role, content);
                    })
                    .toList();
            
            if (!mem0Messages.isEmpty()) {
                // only add the last message
                List<Mem0Client.Message> lastMessages = new ArrayList<>();
                lastMessages.add(mem0Messages.get(mem0Messages.size() - 1));
                
                mem0Client.addMemory(userId, appId, lastMessages);
                logger.info("Added messages to mem0 for user: {}", userId);
            }
            
            localCache.put(memoryId, new ArrayList<>(messages));
            
        } catch (Exception e) {
            logger.error("Failed to update messages: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update messages", e);
        }
    }
    
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        logger.info("Getting messages for memory ID: {}", memoryId);
        
        try {
            List<ChatMessage> cachedMessages = localCache.get(memoryId);
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                logger.debug("Returning cached messages for memory ID: {}", memoryId);
                return new ArrayList<>(cachedMessages);
            }
            
            String userId = memoryId.toString();
            JsonNode response = mem0Client.getMemories(userId, appId);
            
            List<ChatMessage> messages = new ArrayList<>();
            
            // Mem0 API returns an array directly
            if (response != null && response.isArray()) {
                for (JsonNode memory : response) {
                    extractMessagesFromMemory(memory, messages);
                }
            }
            
            localCache.put(memoryId, messages);
            
            logger.info("Retrieved {} messages from mem0 for memory ID: {}", messages.size(), memoryId);
            return new ArrayList<>(messages);
        } catch (Exception e) {
            logger.error("Failed to get messages: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get messages", e);
        }
    }
    
    @Override
    public void deleteMessages(Object memoryId) {
        logger.info("Deleting messages for memory ID: {}", memoryId);
        
        localCache.remove(memoryId);
        
        // TODO: call DELETE /v1/memories/{memory_id}/
        logger.warn("Delete operation only removed messages from local cache. Mem0 API may not support deletion.");
    }
    
    /**
     * Extended method to ChatMemoryStore
     */
    public List<ChatMessage> searchMessages(Object memoryId, String query) {
        logger.info("Searching messages for memory ID: {} with query: {}", memoryId, query);
        
        try {
            String userId = memoryId.toString();
            JsonNode response = mem0Client.searchMemories(query, userId, appId, topK);
            
            List<ChatMessage> messages = new ArrayList<>();
            // Mem0 API returns an array directly
            if (response != null && response.isArray()) {
                for (JsonNode memory : response) {
                    extractMessagesFromMemory(memory, messages);
                }
            }
            
            logger.info("Found {} relevant messages for query: {}", messages.size(), query);
            return messages;
        } catch (Exception e) {
            logger.error("Failed to search messages: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search messages", e);
        }
    }
    
    private void extractMessagesFromMemory(JsonNode memory, List<ChatMessage> messages) {
        if (memory.has("memory") && memory.get("memory").isTextual()) {
            String memoryContent = memory.get("memory").asText();
            if (memoryContent != null && !memoryContent.trim().isEmpty()) {
                // Since Mem0 API doesn't provide role information in the response,
                // we treat the memory content as a UserMessage
                messages.add(UserMessage.from(memoryContent));
            }
        }
    }
}