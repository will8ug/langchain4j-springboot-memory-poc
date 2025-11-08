package io.will.langchain4jpoc;

import io.will.langchain4jpoc.controller.AiChatController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatRequestIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testChatNonStreaming_thenReturnSuccessResponse() {
        webTestClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AiChatController.ChatRequest("Hello"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AiChatController.ChatResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertNotNull(response.content());
                    assertFalse(response.content().isEmpty());
                });
    }

    @Test
    void testChatStreaming_thenReturnSuccessfulFluxResponse() {
        webTestClient.post()
                .uri("/chat/streaming")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AiChatController.ChatRequest("What is the capital of China?"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .returnResult(AiChatController.ChatResponse.class)
                .getResponseBody()
                .take(1)
                .doOnNext(chunk -> {
                    assertNotNull(chunk);
                    assertNotNull(chunk.content());
                    assertFalse(chunk.content().isEmpty());
                })
                .blockLast();
    }
}
