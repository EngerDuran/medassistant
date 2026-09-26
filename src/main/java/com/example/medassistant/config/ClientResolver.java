package com.example.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Componente responsable de la resolución dinámica de clientes de IA en runtime.
 * Centraliza la estrategia de selección entre modelos cloud (Gemini) y locales (Ollama)
 * evitando duplicidad de lógica en la capa de servicios.
 */
@Component
public class ClientResolver {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient
    ) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Resuelve el cliente adecuado basándose en el parámetro recibido.
     * Aplica Gemini como proveedor por defecto si no se especifica Ollama.
     */
    public ChatClient resolve(String model) {
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }
}