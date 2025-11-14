package io.will.langchain4jpoc.memory.mem0;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record Mem0ChatMemory(
        Object memoryId,
        Mem0ChatMemoryStore store,
        Supplier<String> querySupplier,
        int maxMessages
) implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(Mem0ChatMemory.class);

    @Override
    public Object id() {
        return memoryId;
    }

    @Override
    public void add(ChatMessage message) {
        logger.info("Adding message to memory ID: {}", memoryId);
        List<ChatMessage> messages = new ArrayList<>(messages());
        messages.add(message);

        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }

        store.updateMessages(memoryId, messages);
    }

    @Override
    public List<ChatMessage> messages() {
        logger.info("Getting messages for memory ID: {}", memoryId);

        try {
            String currentQuery = querySupplier != null ? querySupplier.get() : null;

            // Search memory if current query exists
            if (currentQuery != null && !currentQuery.trim().isEmpty()) {
                logger.debug("Using search with query: {}", currentQuery);
                return store.searchMessages(memoryId, currentQuery);
            } else { // otherwise, fallback to return all
                logger.debug("Getting all messages");
                return store.getMessages(memoryId);
            }
        } catch (Exception e) {
            logger.error("Failed to get messages: {}", e.getMessage(), e);
            // fallback to return all memories if Exceptions found
            try {
                return store.getMessages(memoryId);
            } catch (Exception fallbackException) {
                logger.error("Fallback to empty messages failed: {}", fallbackException.getMessage(), fallbackException);
                return new ArrayList<>();
            }
        }
    }

    @Override
    public void clear() {
        logger.info("Clearing messages for memory ID: {}", memoryId);
        store.deleteMessages(memoryId);
    }

    public List<ChatMessage> searchMessages(String query) {
        logger.info("Searching messages with query: {}", query);
        return store.searchMessages(memoryId, query);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Mem0ChatMemoryStore store;
        private Object memoryId;
        private Supplier<String> querySupplier = () -> null;
        private int maxMessages = 20;

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

        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public Mem0ChatMemory build() {
            if (store == null) {
                throw new IllegalStateException("ChatMemoryStore must be set");
            }
            if (memoryId == null) {
                throw new IllegalStateException("MemoryId must be set");
            }
            return new Mem0ChatMemory(memoryId, store, querySupplier, maxMessages);
        }
    }
}