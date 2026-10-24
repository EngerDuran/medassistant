package com.example.medassistant.config;

import com.example.medassistant.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
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
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    /**
     * Bean del cliente de Gemini con System Prompt por defecto
     * para asegurar el tono de triaje y evitar diagnósticos prescriptivos.
     */
    @Bean("geminiClient")
    ChatClient geminiClient(
            GoogleGenAiChatModel chatModel,
           @Qualifier("googleVectorStore") VectorStore vectorStore) throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        /**
        * RAG (Retrieval-Augmented Generation): Incorpora QuestionAnswerAdvisor para interceptar
        * el mensaje del usuario y buscar contexto clínico en el VectorStore antes de llamar al LLM.
        * Solo recupera los 3 fragmentos más relevantes (topK=3) que superen un 70% de
         * coincidencia semántica (similarityThreshold=0.7) para minimizar alucinaciones.
         */
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool,
                        patientInfoTool, drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7).topK(3).build())
                                        .promptTemplate(new PromptTemplate("""
                                                
                                                Contexto de documentos médicos (Usalo si es relevante para la pregunta):
                                                {question_answer_context}
                                                
                                                Si la información no está en los documentos, puedes usar tus herramientas o tus conocimientos generales para responder.
                                                """))
                                .build(), medicalAuditAdvisor)
                .build();
    }

    /**
     * Bean del cliente local Ollama configurado con el mismo System Prompt
     * garantizando paridad de comportamiento entre entornos.
     */
    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel,
                            @Qualifier("ollamaVectorStore") VectorStore vectorStore
                            ) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool,
                        patientInfoTool, drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7)
                                        .topK(3)
                                        .build())
                                .promptTemplate(new PromptTemplate("""
                                                
                                                Contexto de documentos médicos (Usalo si es relevante para la pregunta):
                                                {question_answer_context}
                                                
                                                Si la información no está en los documentos, puedes usar tus herramientas o tus conocimientos generales para responder.
                                                """))
                                .build(), medicalAuditAdvisor)

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
          @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embedding,
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

    /**
     * Cliente HTTP para la comunicación con la instancia de ChromaDB.
     * Externaliza la URL de conexión para facilitar
     * la transición entre entornos (ej. contenedor Docker en local vs. clúster en AWS).
     * Este cliente es inyectado posteriormente en la construcción de los VectorStores.
     */
    @Bean
    ChromaApi chromaApi(@Value("${spring.ai.vectorstore.chroma.url}") String chromaUrl) {
        return ChromaApi.builder()
                .baseUrl(chromaUrl)
                .build();
    }

}





