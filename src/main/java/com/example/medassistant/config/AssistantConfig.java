package com.example.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de clientes para soportar múltiples LLMs.
 * Nombramos los beans explícitamente para evitar colisiones.
 * y poder inyectar Gemini y Ollama según el caso de uso con @Qualifier.
 */

@Configuration
public class AssistantConfig {

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
