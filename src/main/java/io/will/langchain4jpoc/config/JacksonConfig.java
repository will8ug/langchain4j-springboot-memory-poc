package io.will.langchain4jpoc.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.io.IOException;

@Configuration
public class JacksonConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper;
    
    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleModule module = new SimpleModule();
        module.addSerializer(ChatMessage.class, new ChatMessageSerializer());
        objectMapper.registerModule(module);
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
    }

    static class ChatMessageSerializer extends JsonSerializer<ChatMessage> {
        @Override
        public void serialize(ChatMessage message, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", message.type().name());

            switch (message) {
                case SystemMessage systemMessage -> gen.writeStringField("text", systemMessage.text());
                case UserMessage userMessage -> gen.writeStringField("text", userMessage.singleText());
                case AiMessage aiMessage -> {
                    gen.writeStringField("text", aiMessage.text());
                    if (aiMessage.hasToolExecutionRequests()) {
                        gen.writeBooleanField("hasToolExecutionRequests", true);
                    }
                }
                default -> {
                    // NOP for now
                }
            }
            
            gen.writeEndObject();
        }
    }
}