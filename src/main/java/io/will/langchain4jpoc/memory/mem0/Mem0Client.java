package io.will.langchain4jpoc.memory.mem0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "chat.memory.provider", havingValue = "mem0")
public class Mem0Client {
    private static final Logger logger = LoggerFactory.getLogger(Mem0Client.class);
    private static final String ADD_MEMORIES_URL = "https://api.mem0.ai/v1/memories/";
    private static final String GET_MEMORIES_URL = "https://api.mem0.ai/v2/memories/";
    private static final String SEARCH_MEMORIES_URL = "https://api.mem0.ai/v2/memories/search/";
    
    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public Mem0Client(@Value("${mem0.api.key}") String apiKey,
                      WebClient webClient,
                     ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }
    
    public JsonNode addMemory(String userId, String appId, List<Message> messages) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messages);
            requestBody.put("user_id", userId);
            requestBody.put("app_id", appId);
            requestBody.put("version", "v2");
            
            String response = postRequest(ADD_MEMORIES_URL, requestBody);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to add memory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add memory", e);
        }
    }
    
    public JsonNode getMemories(String userId, String appId) {
        try {
            Map<String, Object> filters = new HashMap<>();
            List<Map<String, String>> andConditions = new ArrayList<>();
            
            Map<String, String> userIdCondition = new HashMap<>();
            userIdCondition.put("user_id", userId);
            
            Map<String, String> appIdCondition = new HashMap<>();
            appIdCondition.put("app_id", appId);
            
            andConditions.add(userIdCondition);
            andConditions.add(appIdCondition);
            filters.put("AND", andConditions);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filters", filters);
            
            String response = postRequest(GET_MEMORIES_URL, requestBody);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to get memories: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get memories", e);
        }
    }
    
    public JsonNode searchMemories(String query, String userId, String appId, int topK) {
        try {
            Map<String, Object> filters = new HashMap<>();
            List<Map<String, String>> andConditions = new ArrayList<>();
            
            Map<String, String> userIdCondition = new HashMap<>();
            userIdCondition.put("user_id", userId);
            
            Map<String, String> appIdCondition = new HashMap<>();
            appIdCondition.put("app_id", appId);
            
            andConditions.add(userIdCondition);
            andConditions.add(appIdCondition);
            filters.put("AND", andConditions);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("filters", filters);
            requestBody.put("top_k", String.valueOf(topK));
            
            String response = postRequest(SEARCH_MEMORIES_URL, requestBody);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to search memories: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search memories", e);
        }
    }
    
    private String postRequest(String url, Object requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + apiKey);
        
        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    
    public record Message(String role, String content) {}
}