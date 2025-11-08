package io.will.langchain4jpoc.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AiAssistantService {
    @SystemMessage("You are a polite assistant")
    String chat(String userMessage);
}
