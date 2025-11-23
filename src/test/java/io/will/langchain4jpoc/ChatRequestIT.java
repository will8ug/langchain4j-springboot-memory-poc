package io.will.langchain4jpoc;

import io.will.langchain4jpoc.controller.AiChatController;
import org.junit.jupiter.api.Disabled;
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
                .bodyValue(new AiChatController.CustomChatRequest("This is a test message"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AiChatController.CustomChatResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertNotNull(response.content());
                    assertFalse(response.content().isEmpty());
                });
    }

    @Test
    @Disabled("Not working together with the non-streaming one in current code base")
    void testChatStreaming_thenReturnSuccessfulFluxResponse() {
        webTestClient.post()
                .uri("/chat/streaming")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AiChatController.CustomChatRequest("What is the capital of China?"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE)
                .returnResult(AiChatController.CustomChatResponse.class)
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
