package com.example.medassistant.controller;

import com.example.medassistant.dto.ChatRequest;
import com.example.medassistant.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

   private final AssistantService assistantService;

    /**
     * Endpoint síncrono y bloqueante.
     * Retiene la conexión hasta que el LLM genera la respuesta completa.
     */

    @PostMapping
    public ResponseEntity <String> chat(
           @Valid @RequestBody ChatRequest request
            ) {
        return ResponseEntity.ok(assistantService.chat(request.prompt(), request.model()));
    }

    /**
     * Endpoint asíncrono y reactivo mediante Server-Sent Events (SSE).
     * Emite fragmentos (tokens) a través de un Flux en tiempo real a medida que se van generando.
     * reduciendo el TTFT (Time to First Token) y optimizando el uso de memoria en el servidor.
     */

    @PostMapping (value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> chatStream(
           @Valid @RequestBody ChatRequest request
    ) {
        return assistantService.chatStream(request.prompt(), request.model());
    }

}
