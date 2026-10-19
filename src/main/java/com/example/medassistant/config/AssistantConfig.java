package com.example.medassistant.config;

import com.example.medassistant.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Configuración centralizada de clientes de IA (Spring AI).
 * Define instancias específicas para Gemini y Ollama inyectables mediante @Qualifier,
 * aplicando un System Prompt global para establecer guardrails y contexto clínico.
 */
@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final ChatMemory chatMemory;

    /**
     * Bean del cliente de Gemini con System Prompt por defecto
     * para asegurar el tono de triaje y evitar diagnósticos prescriptivos.
     */
    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool,
                        patientInfoTool, drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Bean del cliente local Ollama configurado con el mismo System Prompt
     * garantizando paridad de comportamiento entre entornos.
     */
    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool,
                        patientInfoTool, drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Configuración de los almacenes (Vector Stores) respaldados por ChromaDB.
     * Dos beans distintos, Google y Ollama.
     * tienen dimensionalidades y espacios vectoriales incompatibles entre sí.
     * Mezclarlos en una misma colección rompería las búsquedas por similitud coseno.
     * Por ello, aislamos cada modelo en su propia colección dentro de Chroma.
     */


    /**
     * Almacén vectorial optimizado para embeddings generados por Google GenAI.
     */
    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
          @Qualifier("GoogleGenAiTextEmbedding") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_google")
                .initializeSchema(true)
                .build();
    }

    /**
     * Almacén vectorial optimizado para embeddings generados por Ollama.
     */
    @Bean("ollamaVectorStore")
    VectorStore ollamaVectorStore(
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_ollama")
                .initializeSchema(true)
                .build();
    }


}





