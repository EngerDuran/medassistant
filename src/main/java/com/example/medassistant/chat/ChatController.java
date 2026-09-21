package com.example.medassistant.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    /**
     * Endpoint síncrono y bloqueante.
     * Retiene la conexión hasta que el LLM genera la respuesta completa.
     */

    @PostMapping
    public String chat( @RequestBody String prompt) {
        return chatClient
                .prompt(prompt).call().content();
    }

    /**
     * Endpoint asíncrono y reactivo mediante Server-Sent Events (SSE).
     * Emite fragmentos (tokens) a través de un Flux en tiempo real a medida que se van generando.
     * reduciendo el TTFT (Time to First Token) y optimizando el uso de memoria en el servidor.
     */

    @PostMapping (value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> chatStream(@RequestBody String prompt) {
        return chatClient
                .prompt(prompt).stream().content();
    }
}
