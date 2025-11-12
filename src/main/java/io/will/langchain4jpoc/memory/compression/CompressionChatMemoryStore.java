package io.will.langchain4jpoc.memory.compression;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompressionChatMemoryStore implements ChatMemoryStore {
    private final static Logger logger = LoggerFactory.getLogger(CompressionChatMemoryStore.class);
    private final ChatMemoryStore delegate;
    private final ChatModel chatModel;

    @Value("${chat-memory.compression.threshold:5}")
    private int threshold;

    private static final String SUMMARY_PREFIX = "Context: The following is a summary of the previous conversation:";

    public CompressionChatMemoryStore(ChatModel chatModel) {
        this.delegate = new InMemoryChatMemoryStore();
        this.chatModel = chatModel;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        logger.info("Updating memory ID: {}, {}", memoryId, messages.toString());
        if (messages.isEmpty()) {
            logger.warn("No messages to compress for memory ID: {}", memoryId);
            return;
        }

       ChatMessage lastMsg = messages.getLast();
       if (lastMsg.type() == ChatMessageType.AI && ((AiMessage) lastMsg).hasToolExecutionRequests()) {
           logger.info("Skipping compression for memory ID: {} [function call in the last message]", memoryId);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       if (lastMsg.type() == ChatMessageType.SYSTEM || lastMsg.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
           logger.info(
                   "Skipping compression for memory ID: {} [system message or function call response in the last message]",
                   memoryId);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       if (messages.size() <= threshold) {
           logger.debug("No compression for memory ID: {} [less than {} messages]", memoryId, threshold);
           delegate.updateMessages(memoryId, messages);
           return;
       }

       String summary = compressMessages(memoryId, messages);

       SystemMessage systemMsg = (SystemMessage) messages.stream()
               .filter(m -> m.type() == ChatMessageType.SYSTEM)
               .findFirst().orElse(null);
       systemMsg = replaceTheLatestSummary(systemMsg, summary);
       logger.info("Generated system message with summary: {}", systemMsg.text());
       logger.info("Updating memory messages of memory ID: {}", memoryId);
       delegate.updateMessages(memoryId, List.of(systemMsg));
        // delegate.updateMessages(memoryId, messages);
    }

    private String compressMessages(Object memoryId, List<ChatMessage> messages) {
        logger.info("Triggering semantic compression for memory ID: {} with {} messages", memoryId, messages.size());
        List<ChatMessage> toBeCompressed = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.type() == ChatMessageType.SYSTEM) {
                extractSummaryFromSystemMessageIfAny((SystemMessage) msg, toBeCompressed);
            } else {
                toBeCompressed.add(msg);
            }
        }

        StringBuilder sb = new StringBuilder("""
                Summarize the following dialogue into a brief summary, preserving context and tone:
                
                """);
        for (ChatMessage msg : toBeCompressed) {
            switch (msg.type()) {
                case ChatMessageType.SYSTEM -> sb.append("Context: ").append(((SystemMessage) msg).text()).append("\n");
                case ChatMessageType.USER -> sb.append("User: ").append(((UserMessage) msg).singleText()).append("\n");
                case ChatMessageType.AI -> sb.append("Assistant: ").append(((AiMessage) msg).text()).append("\n");
                default -> logger.debug("Skipping message of type: {}", msg.type());
            }
        }
        return chatModel.chat(sb.toString());
    }

    private void extractSummaryFromSystemMessageIfAny(SystemMessage systemMsg, List<ChatMessage> compressed) {
        String content = systemMsg.text();
        if (content.contains(SUMMARY_PREFIX)) {
            int startIndex = content.indexOf(SUMMARY_PREFIX) + SUMMARY_PREFIX.length();
            String summary = content.substring(startIndex).strip();
            compressed.add(SystemMessage.systemMessage(summary));
        }
    }

    private SystemMessage replaceTheLatestSummary(SystemMessage systemMsg, String summary) {
        if (systemMsg == null) {
            return SystemMessage.systemMessage(SUMMARY_PREFIX + "\n" + summary);
        }

        String content = systemMsg.text();
        String newContent = "";
        if (content.contains(SUMMARY_PREFIX)) {
            int startIndex = content.indexOf(SUMMARY_PREFIX);
            if (startIndex > 0) {
                newContent = content.substring(0, startIndex) + "\n\n";
            }
            newContent = newContent + SUMMARY_PREFIX + "\n" + summary;
        } else {
            newContent = content + "\n\n" + SUMMARY_PREFIX + "\n" + summary;
        }
        return SystemMessage.systemMessage(newContent);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> currentMessages = delegate.getMessages(memoryId);
        logger.info("getMessages memory ID: {}, {}", memoryId, currentMessages);
        return currentMessages;
    }

    @Override
    public void deleteMessages(Object memoryId) {
        logger.info("Current messages: {}", delegate.getMessages(memoryId));
        logger.info("Deleting memory ID: {}", memoryId);
        Thread.dumpStack();
        delegate.deleteMessages(memoryId);
    }
}
