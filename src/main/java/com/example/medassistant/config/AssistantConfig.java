package com.example.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuración centralizada de clientes de IA (Spring AI).
 * Define instancias específicas para Gemini y Ollama inyectables mediante @Qualifier,
 * aplicando un System Prompt global para establecer guardrails y contexto clínico.
 */
@Configuration
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    /**
     * Bean del cliente de Gemini con System Prompt por defecto
     * para asegurar el tono de triaje y evitar diagnósticos prescriptivos.
     */
    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }

    /**
     * Bean del cliente local Ollama configurado con el mismo System Prompt
     * garantizando paridad de comportamiento entre entornos.
     */
    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) throws IOException {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }
}