package com.example.medassistant.config;

import com.example.medassistant.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;


/**
 * Configuración core para el entorno de Producción AWS.
 * Decisión de Arquitectura: Reemplazamos ChromaDB por PostgreSQL + pgvector.
 * Esto nos permite consolidar la base de datos relacional usuarios, citas
 * y la base de datos vectorial que son documentos médicos RAG en una sola instancia
 * de Amazon RDS, reduciendo la superficie de ataque, los costes y la latencia.
 */
@Configuration
@Profile("aws")
@RequiredArgsConstructor
public class AssistantConfigAws {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final ChatMemory chatMemory;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    @Bean("geminiClient")
    ChatClient geminiClient(
            GoogleGenAiChatModel chatModel,
            @Qualifier("googleVectorStore") VectorStore vectorStore) throws IOException {

        // Grounding temporal para evitar que el LLM alucine con fechas relativas ("hoy, mañana
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
                // RAG Pipeline
                // Usamos QuestionAnswerAdvisor para interceptar el prompt del usuario y buscar
                // contexto en pgvector antes de ir al LLM.
                // Filtramos por top 3 resultados con >70% de similitud para meter solo
                // contexto clínico de calidad y ahorrar tokens.
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
    * Motor vectorial respaldado por PostgreSQL.
    */
    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
            @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate) {
        //3072 es la dimensión exacta que genera el modelo gemini-embedding.
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(3072)
                .indexType(PgVectorStore.PgIndexType.NONE)
                // Permite a Spring crear la tabla medassistant_google y la extensión vector
                // automáticamente si no existen en el RDS.
                .initializeSchema(true)
                .vectorTableName("medassistant_google")
                .build();
    }
}
