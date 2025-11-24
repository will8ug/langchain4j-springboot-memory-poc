package io.will.langchain4jpoc.memory.mem0;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class Mem0ChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemory.class);

    private final ConcurrentMap<Object, ChatMessage> systemMessageStore = new ConcurrentHashMap<>();
    
    private final Object memoryId;
    private final Mem0ChatMemoryStore store;
    private final Supplier<String> querySupplier;

    public Mem0ChatMemory(Object memoryId, Mem0ChatMemoryStore store, Supplier<String> querySupplier) {
        this.memoryId = memoryId;
        this.store = store;
        this.querySupplier = querySupplier;
    }

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        logger.info("Adding message to memory ID: {} | {}", memoryId, message.toString());

        if (message instanceof SystemMessage systemMsg) {
            // only retain the last system message
            systemMessageStore.put(memoryId, systemMsg);
            return;
        }

        List<ChatMessage> messages = new ArrayList<>(messages());
        messages.add(message);
        store.updateMessages(memoryId, messages);
    }

    @Override
    public List<ChatMessage> messages() {
        logger.info("Getting messages for memory ID: {}", memoryId);

        List<ChatMessage> result = new ArrayList<>();
        String currentQuery = "";
        try {
            currentQuery = querySupplier != null ? querySupplier.get() : null;

            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                logger.debug("Searching with query: {}", currentQuery);
                result.addAll(store.searchMessages(memoryId, currentQuery));
            } else { // fallback to return all
                logger.debug("Getting all messages (no query provided)");
                result.addAll(store.getMessages(memoryId));
            }
        } catch (Exception e) {
            logger.error("Failed to get messages: {}", e.getMessage(), e);
            // fallback to return all memories if Exceptions found
            try {
                result.addAll(store.getMessages(memoryId));
            } catch (Exception fallbackException) {
                logger.error("Fallback to empty messages failed: {}", fallbackException.getMessage(), fallbackException);
            }
        }

        // system message and the current user query have to be handled separately under LangChain4j
        result.addFirst(systemMessageStore.get(memoryId));
        result.addLast(new UserMessage(currentQuery));
        return result;
    }

    @Override
    public void clear() {
        logger.info("Clearing messages for memory ID: {}", memoryId);
        store.deleteMessages(memoryId);
        systemMessageStore.remove(memoryId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Mem0ChatMemoryStore store;
        private Object memoryId;
        private Supplier<String> querySupplier = () -> null;

        private Builder() {
        }

        public Builder chatMemoryStore(Mem0ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        public Builder memoryId(Object memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        public Builder querySupplier(Supplier<String> querySupplier) {
            this.querySupplier = querySupplier;
            return this;
        }

        public Mem0ChatMemory build() {
            if (store == null) {
                throw new IllegalStateException("ChatMemoryStore must be set");
            }
            if (memoryId == null) {
                throw new IllegalStateException("MemoryId must be set");
            }
            return new Mem0ChatMemory(memoryId, store, querySupplier);
        }
    }
}