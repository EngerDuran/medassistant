package com.example.medassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
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
    private final ChatModel geminiModel;
    private final ChatModel ollamaModel;
    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("googleGenAiChatModel") ChatModel geminiModel,
            @Qualifier("ollamaChatModel") ChatModel ollamaModel,
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore

    ) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
        this.geminiModel = geminiModel;
        this.ollamaModel = ollamaModel;
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Resuelve el cliente adecuado basándose en el parámetro recibido.
     * Aplica Gemini como proveedor por defecto si no se especifica Ollama.
     */
    public ChatClient resolve(String model) {
        return switch (normalize(model)) {
            case "gemini" -> geminiClient;
            case "ollama" -> ollamaClient;
            // Patrón de FailFast: Si el cliente envía basura, cortamos la ejecución de inmediato.
            default -> throw new IllegalArgumentException(
                    "Modelo no soportado: '%s'. Valores válidos: gemini, ollama".formatted(model));
        };
    }

    public ChatModel resolveModel(String model) {
        return switch (normalize(model)) {
            case  "gemini" -> geminiModel;
            case  "ollama" -> ollamaModel;
            default ->  throw new IllegalArgumentException(
                    "Modelos no soportado: '%s'. Valores válidos: gemini, ollama".formatted(model));
        };
    }

    public VectorStore resolveVectorStore(String model) {
        return switch (normalize(model)) {
            case "gemini" -> googleVectorStore;
            case "ollama" -> ollamaVectorStore;
            default -> throw new IllegalArgumentException(
                    "Modelo no soportado: '%s'. Valores válidos: gemini, ollama".formatted(model));
        };
    }

    /**
     * Sanea el input del usuario (Sanitization).
     * Evita NullPointerExceptions y normaliza mayúsculas/espacios (" GEMINI " -> "gemini").
     */
    private String normalize(String model){
        return model == null ? "gemini" : model.strip().toLowerCase();
    }
}
