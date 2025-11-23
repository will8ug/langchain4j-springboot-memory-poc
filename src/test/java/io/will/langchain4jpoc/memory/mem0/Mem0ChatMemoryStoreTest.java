package io.will.langchain4jpoc.memory.mem0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Mem0ChatMemoryStoreTest {

    @Mock
    private Mem0Client mem0Client;

    private Mem0ChatMemoryStore store;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        store = new Mem0ChatMemoryStore(mem0Client, "test-app", 3);
        objectMapper = new ObjectMapper();
    }

    @Test
    void givenArrayResponse_whenGetMessages_thenReturnsParsedMessages() throws Exception {
        Object memoryId = "user123";
        String responseJson = """
            [
              {
                "id": "3c90c3cc-0d44-4b50-8888-8dd25736052a",
                "memory": "Hello, my name is Will",
                "user_id": "user123",
                "metadata": {},
                "categories": ["personal"],
                "immutable": false,
                "expiration_date": null,
                "created_at": "2023-11-07T05:31:56Z",
                "updated_at": "2023-11-07T05:31:56Z"
              },
              {
                "id": "4d91d4dd-1e55-5c61-9999-9ee36847163b",
                "memory": "I like programming",
                "user_id": "user123",
                "metadata": {},
                "categories": ["interest"],
                "immutable": false,
                "expiration_date": null,
                "created_at": "2023-11-07T05:32:00Z",
                "updated_at": "2023-11-07T05:32:00Z"
              }
            ]
            """;

        JsonNode response = objectMapper.readTree(responseJson);
        when(mem0Client.getMemories("user123", "test-app")).thenReturn(response);

        List<ChatMessage> messages = store.getMessages(memoryId);

        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("Hello, my name is Will", ((UserMessage) messages.get(0)).singleText());
        assertEquals("I like programming", ((UserMessage) messages.get(1)).singleText());
        verify(mem0Client, times(1)).getMemories("user123", "test-app");
    }

    @Test
    void givenEmptyArrayResponse_whenGetMessages_thenReturnsEmptyList() throws Exception {
        Object memoryId = "user123";
        String responseJson = "[]";
        JsonNode response = objectMapper.readTree(responseJson);
        when(mem0Client.getMemories("user123", "test-app")).thenReturn(response);

        List<ChatMessage> messages = store.getMessages(memoryId);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void givenArrayResponse_whenSearchMessages_thenReturnsMatchingMessages() throws Exception {
        Object memoryId = "user123";
        String query = "What is my name?";
        String responseJson = """
            [
              {
                "id": "3c90c3cc-0d44-4b50-8888-8dd25736052a",
                "memory": "Hello, my name is Will",
                "user_id": "user123",
                "metadata": {},
                "categories": ["personal"],
                "immutable": false,
                "expiration_date": null,
                "created_at": "2023-11-07T05:31:56Z",
                "updated_at": "2023-11-07T05:31:56Z"
              }
            ]
            """;

        JsonNode response = objectMapper.readTree(responseJson);
        when(mem0Client.searchMemories(query, "user123", "test-app", 3)).thenReturn(response);

        List<ChatMessage> messages = store.searchMessages(memoryId, query);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Hello, my name is Will", ((UserMessage) messages.getFirst()).singleText());
        verify(mem0Client, times(1)).searchMemories(query, "user123", "test-app", 3);
    }

    @Test
    void givenEmptyArrayResponse_whenSearchMessages_thenReturnsEmptyList() throws Exception {
        Object memoryId = "user123";
        String query = "unknown query";
        String responseJson = "[]";
        JsonNode response = objectMapper.readTree(responseJson);
        when(mem0Client.searchMemories(query, "user123", "test-app", 3)).thenReturn(response);

        List<ChatMessage> messages = store.searchMessages(memoryId, query);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }
}

