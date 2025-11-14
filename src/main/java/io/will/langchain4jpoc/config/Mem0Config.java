package io.will.langchain4jpoc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.will.langchain4jpoc.memory.mem0.Mem0Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "chat.memory.provider", havingValue = "mem0")
public class Mem0Config {
    private static final Logger logger = LoggerFactory.getLogger(Mem0Config.class);
    
    @Value("${mem0.api.key:}")
    private String mem0ApiKey;

    @Value("${mem0.app.id:langchain4j-springboot-poc}")
    private String mem0AppId;
    
    @Value("${mem0.top.k:3}")
    private int mem0TopK;

    @Bean
    public Mem0Client mem0Client(WebClient webClient, ObjectMapper objectMapper) {
        if (!isMem0Configured()) {
            logger.warn("Mem0 API key not configured. Please set mem0.api.key property.");
             throw new IllegalStateException("Mem0 API key must be configured");
        }
        
        logger.info("Creating Mem0Client with appId: {}", mem0AppId);
        return new Mem0Client(
                mem0ApiKey,
                webClient,
                objectMapper
        );
    }

    public String getMem0AppId() {
        return mem0AppId;
    }
    
    public int getMem0TopK() {
        return mem0TopK;
    }
    
    public boolean isMem0Configured() {
        return mem0ApiKey != null && !mem0ApiKey.isBlank();
    }
}