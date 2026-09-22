package com.example.medassistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;

    // Inyección de dependencias con @Qualifier: resuelve la ambigüedad indicando
    // a Spring qué bean concreto inyectar cuando existen varias implementaciones de ChatClient.
    public ChatController(
                     @Qualifier("geminiClient") ChatClient geminiClient,
                     @Qualifier("ollamaClient") ChatClient ollamaClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Endpoint síncrono y bloqueante.
     * Retiene la conexión hasta que el LLM genera la respuesta completa.
     */

    @PostMapping
    public String chat(
            @RequestBody String prompt,
            @RequestParam(defaultValue = "gemini") String model
    ) {
        return resolveCliente(model)
                .prompt(prompt).call().content();
    }

    /**
     * Endpoint asíncrono y reactivo mediante Server-Sent Events (SSE).
     * Emite fragmentos (tokens) a través de un Flux en tiempo real a medida que se van generando.
     * reduciendo el TTFT (Time to First Token) y optimizando el uso de memoria en el servidor.
     */

    @PostMapping (value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> chatStream(
            @RequestBody String prompt,
            @RequestParam(defaultValue = "gemini") String model
    ) {
        return resolveCliente(model)
                .prompt(prompt).stream().content();
    }
    // usa el cliente de IA adecuado según el parámetro recibido en la petición,
    // usando Gemini por defecto.
    private ChatClient resolveCliente(String model){
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }
}
