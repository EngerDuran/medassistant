package com.example.medassistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AssistantServiceImpl implements AssistantService {
    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;

    // Inyección de dependencias con @Qualifier: resuelve la ambigüedad indicando
    // a Spring qué bean concreto inyectar cuando existen varias implementaciones de ChatClient.
    public AssistantServiceImpl(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient

    ) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    @Override
    public String chat(String prompt, String model) {
        return resolveCliente(model)
                .prompt(prompt).call().content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        return resolveCliente(model)
                .prompt(prompt).stream().content();
    }

    // usa el cliente de IA adecuado según el parámetro recibido en la petición,
    // usando Gemini por defecto.
    private ChatClient resolveCliente(String model){
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }
}
